use std::{
    os::raw::c_int,
    time::{Duration, Instant},
};

use chrono::Local;
use rand::seq::IndexedRandom;

use crate::{
    entities::player,
    map::{zone::ZoneHandle, ChangeMapService},
    models::{boss::BossStage, skill_model::Skill},
    network::message::Message,
    player::{
        player_actor::{handle::PlayerHandle, message::PlayerMessage, Type_PK},
        Player,
    },
    services::ServiceHandles,
    templates::{self, boss_template_manager},
    utils::{skill_util, time, Location},
};
use tokio::sync::mpsc;

#[derive(Debug)]
pub struct BossActor {
    pub player: Player,
    pub template_id: String,
    pub current_stage: usize,
    pub state: BossState,
    pub target_id: Option<u64>,
    pub last_update: Instant,
    pub zone_handle: ZoneHandle,
    pub receiver: mpsc::Receiver<PlayerMessage>,
    pub chat_queue: Vec<String>,
    pub chat_index: usize,
    pub next_chat_time: Instant,
}

#[derive(Debug)]
pub enum BossState {
    Resting,
    Appearing,
    Chatting,
    Fighting,
    Waiting,
    Changing,
    Escaping,
}

impl BossActor {
    pub fn new(
        player: Player,
        template_id: String,
        zone_handle: ZoneHandle,
        receiver: mpsc::Receiver<PlayerMessage>,
    ) -> Self {
        Self {
            player,
            template_id,
            current_stage: 0,
            state: BossState::Appearing,
            target_id: None,
            last_update: Instant::now(),
            zone_handle,
            receiver,
            chat_queue: Vec::new(),
            chat_index: 0,
            next_chat_time: Instant::now(),
        }
    }

    pub async fn run(mut self) {
        tracing::info!("BossActor::run started for {}", self.player.id);
        let mut interval = tokio::time::interval(std::time::Duration::from_millis(200));
        loop {
            tokio::select! {
                msg = self.receiver.recv() => {
                    match msg {
                        Some(PlayerMessage::Logout) | None => {
                            tracing::info!("BossActor::run ending for {}", self.player.id);
                            break;
                        }
                        Some(m) => self.handle_message(m).await,
                    }
                }
                _ = interval.tick() => {
                    self.update().await;
                    if let BossState::Escaping = self.state {
                        break;
                    }
                }
            }
        }
        self.dispose().await;
    }

    async fn dispose(&mut self) {
        let _ = ChangeMapService::exit_map_actor(&mut self.player).await;
    }

    async fn handle_message(&mut self, msg: PlayerMessage) {
        match msg {
            PlayerMessage::GetSnapshot(tx) => {
                let _ = tx.send(self.player.clone());
            }
            PlayerMessage::Injured { damage, piercing } => {
                self.handle_injured(damage, piercing).await;
            }
            _ => {}
        }
    }

    async fn handle_injured(&mut self, damage: u64, piercing: bool) {
        let real_damage = self.player.injured(damage, piercing);

        let _ = ServiceHandles::send_player_injured(&self.player, real_damage as i32, false, 0);
        let _ = ServiceHandles::send_hp_sync(&self.player);

        if self.player.n_point.hp_current <= 0 {
            self.chat_end();
            self.state = BossState::Changing;
        }
    }
    pub async fn update(&mut self) {
        if self.last_update.elapsed().as_millis() < 1000 {
            return;
        }
        self.last_update = Instant::now();
        tracing::debug!(
            "BossActor::update ticking for {} (State: {:?})",
            self.player.id,
            self.state
        );

        let template = boss_template_manager::get(&self.template_id);
        let boss_type = template
            .as_ref()
            .map(|t| t.r#type.as_str())
            .unwrap_or("solo");

        match self.state {
            BossState::Resting => self.handle_rest().await,
            BossState::Appearing => self.handle_appear().await,
            BossState::Chatting => self.handle_chatting().await,
            BossState::Fighting => {
                if boss_type == "scripted" {
                    self.handle_fighting().await;
                } else {
                    self.handle_fighting().await;
                }
            }
            BossState::Waiting => self.handle_waiting().await,
            BossState::Changing => self.handle_changing().await,
            _ => {}
        }
    }

    async fn handle_changing(&mut self) {
        if let Some(template) = boss_template_manager::get(&self.template_id) {
            if template.r#type != "solo" && self.current_stage + 1 < template.stages.0.len() {
                self.transform_to_next_stage().await;
            } else {
                // Hết stage, kiểm tra xem có sequence tiếp theo không
                if template.r#type == "sequence" {
                    if let Some(comp) = &mut self.player.boss_component {
                        if !comp.sequence.is_empty() {
                            let mut next_sequence = comp.sequence.clone();
                            let next_boss_id = next_sequence.remove(0);
                            crate::boss::manager::BossManager::spawn_boss_async(
                                next_boss_id,
                                self.player.map_id,
                                self.player.zone_id,
                                self.player.location.x,
                                self.player.location.y,
                                None,
                                -1,
                                next_sequence,
                            );
                        }
                    }
                }

                tracing::info!("Boss {} defeated, removing...", self.player.id);
                let _ = self.receiver.close();
                self.state = BossState::Escaping;
            }
        }
    }
    async fn handle_appear(&mut self) {
        if self.chat_start() {
            self.state = BossState::Chatting;
        } else {
            if self.is_turn_to_attack().await {
                self.player.type_pk = Type_PK::PK_ALL;
                let _ = ServiceHandles::send_type_pk(&self.player);
                self.state = BossState::Fighting;
            } else {
                self.player.type_pk = Type_PK::PK_NON;
                let _ = ServiceHandles::send_type_pk(&self.player);
                self.state = BossState::Waiting;
            }
        }
    }

    async fn handle_chatting(&mut self) {
        if Instant::now() < self.next_chat_time {
            return;
        }

        if self.chat_index < self.chat_queue.len() {
            let text = &self.chat_queue[self.chat_index].clone();
            self.send_chat_single(text);
            self.chat_index += 1;
            self.next_chat_time = Instant::now() + Duration::from_millis(2000);
        } else {
            // Đã chat xong
            if self.is_turn_to_attack().await {
                self.player.type_pk = Type_PK::PK_ALL;
                let _ = ServiceHandles::send_type_pk(&self.player);
                let _ = ServiceHandles::send_revive_player(&self.player); // Refresh info
                self.state = BossState::Fighting;
                tracing::info!(
                    "Boss {} finished chatting and starting fight",
                    self.player.id
                );
            } else {
                self.player.type_pk = Type_PK::PK_NON;
                let _ = ServiceHandles::send_type_pk(&self.player);
                self.state = BossState::Waiting;
                tracing::info!(
                    "Boss {} finished chatting, waiting for turn",
                    self.player.id
                );
            }
        }
    }

    async fn handle_waiting(&mut self) {
        if self.is_turn_to_attack().await {
            self.player.type_pk = Type_PK::PK_ALL;
            let _ = ServiceHandles::send_type_pk(&self.player);
            let _ = ServiceHandles::send_revive_player(&self.player); // Refresh info
            self.state = BossState::Fighting;
            tracing::info!("Boss {} turn start! entering PK_ALL", self.player.id);
        }
    }

    async fn is_turn_to_attack(&self) -> bool {
        let Some(comp) = &self.player.boss_component else {
            return true;
        };
        let Some(group_id) = comp.group_id else {
            return true;
        };

        let Ok(players) = self.zone_handle.get_all_players().await else {
            return true;
        };

        for handle in players {
            if handle.id == self.player.id {
                continue;
            }
            // Chỉ kiểm tra boss cùng group (using handle data to avoid deadlock)
            if let Some(other_info) = &handle.boss_info {
                if other_info.group_id == Some(group_id) {
                    // Nếu tôi là Leader (rank 99), tôi đợi tất cả Buddies chết
                    if comp.group_index == 99 {
                        return false;
                    }
                    // Nếu tôi là Buddy, tôi đợi Buddies có index thấp hơn tôi
                    if other_info.group_index != 99 && other_info.group_index < comp.group_index {
                        return false;
                    }
                }
            }
        }
        true
    }
    async fn handle_rest(&mut self) {
        self.state = BossState::Appearing;
    }
    async fn find_target_enemy(&self) -> Option<u64> {
        let Ok(players) = self.zone_handle.get_all_players().await else {
            return None;
        };

        let mut nearest_id = None;
        let mut min_distance = f32::MAX;
        for handle in players {
            if handle.id == self.player.id || handle.is_pet || handle.boss_info.is_some() {
                continue;
            }
            if let Some(other) = handle.get_snapshot().await {
                if other.is_die() {
                    continue;
                }

                let dist = self.calculate_distance(other.location);
                if dist < min_distance {
                    min_distance = dist;
                    nearest_id = Some(handle.id);
                }
            }
        }
        nearest_id
    }
    fn calculate_distance(&self, other_loc: Location) -> f32 {
        let dx = (self.player.location.x - other_loc.x) as f32;
        let dy = (self.player.location.y - other_loc.y) as f32;
        (dx * dx + dy * dy).sqrt()
    }
    fn send_chat_single(&self, text: &str) {
        let mut msg = Message::new(44);
        msg.write_int(self.player.id as i64 as i32);
        msg.write_utf(text);
        msg.write_byte(0);
        self.zone_handle.broadcast(msg);
    }

    fn chat(&self, lines: &[String]) {
        if let Some(text) = lines.choose(&mut rand::rng()) {
            self.send_chat_single(text);
        }
    }

    fn chat_sequence_non_blocking(&self, lines: Vec<String>) {
        if lines.is_empty() {
            return;
        }
        let player_id = self.player.id;
        let zone_handle = self.zone_handle.clone();
        tokio::spawn(async move {
            for text in lines {
                let mut msg = Message::new(44);
                let _ = msg.write_int(player_id as i32);
                let _ = msg.write_utf(&text);
                let _ = msg.write_byte(0);
                let _ = zone_handle.broadcast(msg);
                tokio::time::sleep(tokio::time::Duration::from_millis(2000)).await;
            }
        });
    }
    async fn handle_fighting(&mut self) {
        if self.player.n_point.hp_current <= 0 {
            self.state = BossState::Changing;
            return;
        }

        if self.target_id.is_none() {
            self.target_id = self.find_target_enemy().await;
            if self.target_id.is_none() {
                return;
            }
            tracing::info!(
                "Boss {} found target: {}",
                self.player.id,
                self.target_id.unwrap()
            );
        }

        let target_id = self.target_id.unwrap();
        let Some(pl_handle) = self.zone_handle.get_player(target_id).await.unwrap_or(None) else {
            self.target_id = None;
            return;
        };

        if let Some(target_snapshot) = pl_handle.get_snapshot().await {
            if target_snapshot.is_boss || target_snapshot.is_die() {
                self.target_id = None;
                return;
            }
            let dist = self.calculate_distance(target_snapshot.location);

            if let Some(skill) = self.choose_skill() {
                let range = match skill.template_id {
                    0 | 2 | 4 => 50,  // Skill đấm
                    1 | 3 | 5 => 300, // Skill chưởng
                    _ => 150,
                };

                if dist > range as f32 {
                    self.move_to(
                        target_snapshot.location.x,
                        target_snapshot.location.y as i16,
                    )
                    .await;
                } else {
                    self.use_skill(skill, target_id).await;
                }
            } else {
                if dist > 200.0 {
                    self.move_to(
                        target_snapshot.location.x,
                        target_snapshot.location.y as i16,
                    )
                    .await;
                }
            }

            if rand::random::<f32>() < 0.05 {
                self.chat_random_middle().await;
            }
        }
    }

    async fn chat_random_middle(&self) {
        if let Some(template) = boss_template_manager::get(&self.template_id) {
            let stage = &template.stages.0[self.current_stage];
            self.chat(&stage.chat.m);
        }
    }
    pub fn chat_start(&mut self) -> bool {
        if let Some(template) = boss_template_manager::get(&self.template_id) {
            let stage = &template.stages.0[self.current_stage];
            if !stage.chat.s.is_empty() {
                self.chat_queue = stage.chat.s.clone();
                self.chat_index = 0;
                self.next_chat_time = Instant::now();
                return true;
            }
        }
        false
    }

    pub fn chat_end(&mut self) {
        if let Some(template) = boss_template_manager::get(&self.template_id) {
            let stage = &template.stages.0[self.current_stage];
            if !stage.chat.e.is_empty() {
                self.chat_queue = stage.chat.e.clone();
                self.chat_index = 0;
                self.next_chat_time = Instant::now();
                self.state = BossState::Chatting;
            }
        }
    }

    async fn move_to(&mut self, target_x: i16, target_y: i16) {
        let current_x = self.player.location.x;
        let current_y = self.player.location.y;

        let dx = target_x - current_x;
        let dy = target_y - current_y;
        let dist = ((dx as f32).powi(2) + (dy as f32).powi(2)).sqrt();

        if dist < 1.0 {
            return;
        }
        let speed = 60.0;
        if dist > 500.0 {
            self.player.location.x = target_x;
            self.player.location.y = target_y;
        } else if dist <= speed {
            self.player.location.x = target_x;
            self.player.location.y = target_y;
        } else {
            let ratio = speed / dist;
            self.player.location.x = (current_x as f32 + dx as f32 * ratio) as i16;
            self.player.location.y = (current_y as f32 + dy as f32 * ratio) as i16;
        }

        let mut msg = Message::new(-7);
        msg.write_int(self.player.id as i64 as i32);
        msg.write_short(self.player.location.x);
        msg.write_short(self.player.location.y);
        self.zone_handle.broadcast(msg);
    }

    fn choose_skill(&self) -> Option<Skill> {
        if self.player.player_skill.skills.is_empty() {
            return None;
        }
        let now = time::current_time_millis();
        let mut available_skills: Vec<_> = self
            .player
            .player_skill
            .skills
            .iter()
            .filter(|s| now > s.start_time_use + s.cool_down as u64)
            .cloned()
            .collect();

        if available_skills.is_empty() {
            return None;
        }
        let mut rng = rand::rng();
        available_skills.choose(&mut rng).cloned()
    }

    async fn use_skill(&mut self, skill: Skill, target_id: u64) {
        tracing::info!(
            "Boss {} using skill: {} (target: {})",
            self.player.id,
            skill.template_id,
            target_id
        );

        self.player.player_skill.skill_select = Some(skill);

        if let Ok(Some(pl_handle)) = self.zone_handle.get_player(target_id).await {
            if let Some(mut target_snapshot) = pl_handle.get_snapshot().await {
                crate::services::skill_service::execute_skill(
                    &mut self.player,
                    Some(&mut target_snapshot),
                    None,
                )
                .await;
            }
        } else {
            tracing::warn!(
                "Boss {} could not find handle for target {}",
                self.player.id,
                target_id
            );
        }
    }

    pub async fn transform_to_next_stage(&mut self) {
        self.current_stage += 1;
        self.player.revive();
        self.player.type_pk = crate::player::player_actor::Type_PK::PK_NON;

        if let Some(template) = boss_template_manager::get(&self.template_id) {
            let stage = &template.stages.0[self.current_stage];

            if let Some(new_name) = &stage.name {
                self.player.name = new_name.clone();
            }

            self.player.n_point.hp_max = stage.hp;
            self.player.n_point.hp_current = stage.hp;
            self.player.n_point.mp_max = stage.mp;
            self.player.n_point.mp_current = stage.mp;
            self.player.n_point.dame = stage.dame;

            self.player.player_skill.skills.clear();
            self.player.player_skill.skills.clear();
            for skill_info in &stage.skills {
                if skill_info.is_empty() {
                    continue;
                }
                let skill_template_id = skill_info[0];
                let level = skill_info.get(1).cloned().unwrap_or(1);

                let sk = match skill_util::create_skill(skill_template_id, level).await {
                    Some(s) => Some(s),
                    None => skill_util::create_skill(skill_template_id, 1).await,
                };
                if let Some(sk) = sk {
                    self.player.player_skill.skills.push(sk);
                }
            }

            if stage.outfit.len() >= 3 {
                self.player.head = stage.outfit[0];
                self.player.body = stage.outfit[1];
                self.player.leg = stage.outfit[2];
            }

            let _ = ServiceHandles::send_revive_player(&self.player);
            let _ = ServiceHandles::send_message_eat_dauthan(&self.player);
            let _ = ServiceHandles::send_cai_trang(&self.player);
            let _ = ServiceHandles::send_type_pk(&self.player);

            if self.chat_start() {
                self.state = BossState::Chatting;
            } else {
                if self.is_turn_to_attack().await {
                    self.player.type_pk = crate::player::player_actor::Type_PK::PK_ALL;
                    let _ = ServiceHandles::send_type_pk(&self.player);
                    self.state = BossState::Fighting;
                } else {
                    self.player.type_pk = crate::player::player_actor::Type_PK::PK_NON;
                    let _ = ServiceHandles::send_type_pk(&self.player);
                    self.state = BossState::Waiting;
                }
            }

            if !stage.together.is_empty() {
                let map_id = self.player.map_id;
                let zone_id = self.player.zone_id;
                let x = self.player.location.x;
                let y = self.player.location.y;

                let my_group_id = if let Some(comp) = &self.player.boss_component {
                    comp.group_id.unwrap_or(self.player.id)
                } else {
                    self.player.id
                };

                // Đảm bảo mình là leader/group_id được thiết lập
                if let Some(comp) = &mut self.player.boss_component {
                    comp.group_id = Some(my_group_id);
                }

                for (idx, sub_boss_id) in stage.together.iter().enumerate() {
                    crate::boss::manager::BossManager::spawn_boss_async(
                        sub_boss_id.clone(),
                        map_id,
                        zone_id,
                        x + rand::random_range(-50..50),
                        y,
                        Some(my_group_id),
                        idx as i32,
                        Vec::new(),
                    );
                }
            }
        }
    }
}
