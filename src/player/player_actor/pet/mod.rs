use crate::item::{Item, ItemService};
use crate::map::zone_manager::ZONE_MANAGER;
use crate::player::player_actor::pet::message::PetMessage;
use crate::player::player_actor::{pet, PlayerHandle};
use crate::player::player_manager::PLAYER_MANAGER;
use crate::player::Player;
use crate::services::ServiceHandles;
use crate::utils::{skill_util, time};
use crate::{network::message::Message, player::player_actor::message::PlayerMessage};
use std::time::Duration;
use tokio::sync::mpsc;
use tracing::{error, info};

pub mod handle;
pub mod message;

pub use handle::PetHandle;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum PetStatus {
    Follow = 0,
    Protect = 1,
    Attack = 2,
    GoHome = 3,
    Fusion = 4,
    HTVV = 5,
}

#[derive(Debug, Clone)]
pub struct Pet {
    pub player: Player,
    pub master_id: u64,
    pub status: PetStatus,
    pub type_pet: i8,
    pub is_tranform: bool,
    pub last_time_die: u64,
    pub last_time_unfusion: u64,
    pub is_gohome: bool,
    pub master_location: Option<(i16, i16)>,
    pub target_mob_id: Option<u64>,
    pub target_player_id: Option<u64>,
    pub last_time_chat: u64,
    pub chat_index: usize,
}

pub struct PetActor {
    pub pet: Pet,
    pub receiver: mpsc::Receiver<PlayerMessage>,
}

impl PetActor {
    pub fn new(pet: Pet, receiver: mpsc::Receiver<PlayerMessage>) -> Self {
        Self { pet, receiver }
    }

    pub async fn run(mut self) {
        info!(
            "PetActor started for {} (Master: {})",
            self.pet.player.name, self.pet.master_id
        );

        let mut interval = tokio::time::interval(Duration::from_millis(500));
        loop {
            tokio::select! {
                Some(msg) = self.receiver.recv() => {
                    match msg {
                        PlayerMessage::Logout => break,
                        PlayerMessage::Pet(pet_msg) => self.handle_pet_message(pet_msg).await,
                        PlayerMessage::UpdateTick => self.update().await,
                        PlayerMessage::Injured { damage, piercing } => {
                            let actual_damage = self.pet.player.injured(damage, piercing);
                            info!("Pet {} took {} damage (after armor: {}). HP left: {}",
                                self.pet.player.id, damage, actual_damage, self.pet.player.n_point.hp_current);
                        }
                        PlayerMessage::GetSnapshot(tx) => {
                            let _ = tx.send(self.pet.player.clone());
                        }
                        PlayerMessage::ChangeMap {
                            map_id,
                            zone_id,
                            x,
                            y,
                            space_type,
                        } => {
                            self.handle_change_map(map_id, zone_id, x, y, space_type)
                                .await;
                        }
                        _ => {}
                    }
                }
                _ = interval.tick() => {
                    self.update().await;
                }
            }
        }
        self.dispose().await;
    }

    async fn dispose(&mut self) {
        info!(
            "PetActor disposing for {} (ID: {})",
            self.pet.player.name, self.pet.player.id
        );

        let _ = crate::map::ChangeMapService::exit_map_actor(&mut self.pet.player).await;

        crate::player::player_manager::PLAYER_MANAGER.remove(self.pet.player.id);
    }

    async fn handle_change_map(
        &mut self,
        map_id: i32,
        zone_id: i32,
        x: i16,
        y: i16,
        space_type: crate::map::services::change_map_models::SpaceShipType,
    ) {
        if let Some(zone) = crate::map::zone_manager::ZONE_MANAGER.get_zone(map_id, zone_id) {
            let _ = crate::map::ChangeMapService::exit_map_actor(&mut self.pet.player).await;
            self.pet.player.location.x = x;
            self.pet.player.location.y = y;
            let _ =
                crate::map::ChangeMapService::go_to_map(&mut self.pet.player, &zone, None).await;
            tracing::info!(
                "Pet {} changed map to {} zone {}",
                self.pet.player.id,
                map_id,
                zone_id
            );
        }
    }

    async fn handle_pet_message(&mut self, msg: PetMessage) {
        match msg {
            PetMessage::ChangeStatus(status) => {
                self.pet.status = status;
            }
            PetMessage::UpdateTick => {
                self.update().await;
            }
            PetMessage::MasterLocation(x, y) => {
                tracing::debug!(
                    "Pet {} received master location: ({}, {})",
                    self.pet.player.id,
                    x,
                    y
                );
                self.pet.master_location = Some((x, y));
            }
            PetMessage::MasterAttackTarget(_target_id, _target_type) => {
                // Sẽ xử lý logic tấn công mục tiêu cùng chủ nhân
            }
            PetMessage::Fusion(is_porata) => {
                self.pet.status = PetStatus::Fusion;
                // Xóa pet khỏi zone
                let zone_opt = crate::map::zone_manager::ZONE_MANAGER
                    .get_zone(self.pet.player.map_id, self.pet.player.zone_id);
                if let Some(zone) = zone_opt {
                    let mut msg = crate::network::message::Message::new(-8);
                    let _ = msg.write_int(self.pet.player.id as i32);
                    let _ = crate::services::ServiceHandles::send_to_all_in_zone(&zone, msg);
                    let _ = zone.remove_player(self.pet.player.id).await;
                }
            }
            PetMessage::GetSnapshot(tx) => {
                let _ = tx.send(self.pet.clone());
            }
        }
    }

    async fn update(&mut self) {
        info!(
            "Pet {} update tick. HP: {}, Dead: {}",
            self.pet.player.id, self.pet.player.n_point.hp_current, self.pet.player.dead_flag
        );

        if self.pet.player.n_point.hp_current <= 0 && !self.pet.player.dead_flag {
            info!(
                "Pet {} died (HP <= 0). Setting die status.",
                self.pet.player.id
            );
            self.pet.player.set_die();
            self.pet.last_time_die = time::current_time_millis();
            return;
        }

        if self.pet.player.is_die() {
            let now = time::current_time_millis();
            if self.pet.last_time_die == 0 {
                self.pet.last_time_die = now;
            }

            let time_dead = now - self.pet.last_time_die;
            if time_dead % 10000 < 500 {
                // Log mỗi 10s khi đang chết
                info!(
                    "Pet {} is dead. Waiting for revival: {}/50000ms",
                    self.pet.player.id, time_dead
                );
            }

            if now - self.pet.last_time_die > 50000 {
                info!("Pet {} reviving...", self.pet.player.id);
                let hp_max = self.pet.player.n_point.hp_max;
                let mp_max = self.pet.player.n_point.mp_max;
                self.pet.player.revive();
                self.pet.player.n_point.set_hp(hp_max);
                self.pet.player.n_point.set_mp(mp_max);
                self.pet.last_time_die = 0;
            }
            return;
        }

        match self.pet.status {
            PetStatus::Follow => {
                self.follow_master(60).await;
            }
            PetStatus::Protect | PetStatus::Attack => {
                if let Some(target_mob_id) = self.find_mob_attack().await {
                    self.ai_attack_mob(target_mob_id).await;
                } else {
                    self.follow_master(60).await;
                }
            }
            _ => {}
        }
    }

    async fn ai_attack_mob(&mut self, mob_id: u64) {
        if self.pet.player.is_die() {
            info!(
                "Pet {} attempted to attack mob {} while dead. Aborting.",
                self.pet.player.id, mob_id
            );
            return;
        }
        let zone_opt = crate::map::zone_manager::ZONE_MANAGER
            .get_zone(self.pet.player.map_id, self.pet.player.zone_id);

        if let Some(zone) = zone_opt {
            if let Ok(mobs) = zone.get_all_mobs().await {
                if let Some(mob) = mobs.into_iter().find(|m| m.id == mob_id) {
                    let dx = self.pet.player.location.x - mob.location.x;
                    let dist = dx.abs() as f32; // Đệ tử chủ yếu di chuyển theo trục X

                    let skill_index = if dist <= 60.0 {
                        0
                    } else if self.pet.player.player_skill.skills.len() > 1 {
                        1
                    } else {
                        0
                    };

                    if let Some(skill) = self.pet.player.player_skill.skills.get(skill_index) {
                        self.pet.player.player_skill.skill_select = Some(skill.clone());
                    }

                    if skill_index == 0 && dist > 60.0 {
                        let target_x = if self.pet.player.location.x < mob.location.x {
                            mob.location.x - 40
                        } else {
                            mob.location.x + 40
                        };
                        self.move_to(target_x, mob.location.y).await;
                    } else if dist > 350.0 {
                        self.move_to(mob.location.x + 100, mob.location.y).await;
                    }
                    if (skill_index == 0 && dist <= 80.0) || (skill_index == 1 && dist <= 350.0) {
                        let mut mob_clone = mob.clone();
                        crate::services::skill_service::execute_skill(
                            &mut self.pet.player,
                            None,
                            Some(&mut mob_clone),
                        )
                        .await;
                        zone.sync_mob_effects(mob_clone.id, mob_clone.effect_skill.clone());
                        self.pet_chat(None).await;
                    }
                }
            }
        }
    }

    async fn move_to(&mut self, x: i16, y: i16) {
        self.pet.player.location.x = x;
        self.pet.player.location.y = y;

        let zone_opt = crate::map::zone_manager::ZONE_MANAGER
            .get_zone(self.pet.player.map_id, self.pet.player.zone_id);

        if let Some(zone) = zone_opt {
            let mut msg = Message::new(-7);
            let _ = msg.write_int(self.pet.player.id as i32);
            let _ = msg.write_short(self.pet.player.location.x);
            let _ = msg.write_short(self.pet.player.location.y);
            let _ = crate::services::ServiceHandles::send_to_all_in_zone(&zone, msg);
        }
    }

    async fn follow_master(&mut self, dis: i16) {
        if self.pet.player.is_die() {
            return;
        }
        if let Some((m_x, m_y)) = self.pet.master_location {
            let p_x = self.pet.player.location.x;
            let p_y = self.pet.player.location.y;

            let dx = p_x - m_x;
            let dy = p_y - m_y;
            let distance = ((dx as f32).powi(2) + (dy as f32).powi(2)).sqrt();

            if distance >= dis as f32 || dx.abs() < 50 {
                let target_x = if dx < 0 { m_x - 50 } else { m_x + 50 };
                let target_y = m_y;

                self.pet.player.location.x = target_x;
                self.pet.player.location.y = target_y;

                let zone_opt =
                    ZONE_MANAGER.get_zone(self.pet.player.map_id, self.pet.player.zone_id);

                if let Some(zone) = zone_opt {
                    let mut msg = Message::new(-7);
                    let _ = msg.write_int(self.pet.player.id as i32);
                    let _ = msg.write_short(self.pet.player.location.x);
                    let _ = msg.write_short(self.pet.player.location.y);
                    let _ = ServiceHandles::send_to_all_in_zone(&zone, msg);
                }
            }
        }
    }

    async fn find_mob_attack(&mut self) -> Option<u64> {
        let zone_opt = ZONE_MANAGER.get_zone(self.pet.player.map_id, self.pet.player.zone_id);

        if let Some(zone) = zone_opt {
            if let Ok(mobs) = zone.get_all_mobs().await {
                let mut closest_mob = None;
                let mut min_dist = 300.0;

                for mob in mobs {
                    if mob.is_dead() {
                        continue;
                    }
                    let dx = self.pet.player.location.x - mob.location.x;
                    let dy = self.pet.player.location.y - mob.location.y;
                    let dist = ((dx as f32).powi(2) + (dy as f32).powi(2)).sqrt();
                    if dist < min_dist {
                        min_dist = dist;
                        closest_mob = Some(mob.id);
                    }
                }
                return closest_mob;
            }
        }
        None
    }

    async fn find_player_attack(&mut self) -> Option<u64> {
        None
    }

    async fn pet_chat(&mut self, _target_name: Option<&str>) {
        let now = crate::utils::time::current_time_millis();
        if now - self.pet.last_time_chat < 5000 {
            return;
        }

        let chats = [
            "Mày chán sống rồi à?",
            "Sư phụ ơi, xem con xử nó nè!",
            "Ngày này năm sau sẽ là ngày giỗ của mày",
            "Đừng hòng đụng vào sư phụ ta!",
        ];

        let chat_text = chats[self.pet.chat_index % chats.len()];
        self.pet.chat_index += 1;
        self.pet.last_time_chat = now;

        let mut msg = Message::new(44);
        let _ = msg.write_int(self.pet.player.id as i32);
        let _ = msg.write_utf(chat_text);

        // Broadcast chat to zone
        let zone_opt = ZONE_MANAGER.get_zone(self.pet.player.map_id, self.pet.player.zone_id);
        if let Some(zone) = zone_opt {
            let _ = crate::services::ServiceHandles::send_to_all_in_zone(&zone, msg);
        }
    }
}

pub struct PetService;

impl PetService {
    pub async fn spawn_pet(master: &mut Player) -> anyhow::Result<PetHandle> {
        let mut pet_player = Player::new(
            master.id + 1000000,
            format!("$ Đệ tử {}", master.name),
            master.gender as u8,
        );
        master.pet_id = Some(pet_player.id);
        pet_player.is_pet = true;
        pet_player.location = master.location.clone();
        pet_player.map_id = master.map_id;
        pet_player.zone_id = master.zone_id;
        for _ in 0..6 {
            pet_player
                .inventory
                .items_body
                .push(ItemService::create_item_null());
        }

        let skill_id = (pet_player.gender * 2) as i32;
        if let Some(skill) = skill_util::create_skill(skill_id, 1).await {
            pet_player.player_skill.skills.push(skill);
        }

        let (tx, rx) = tokio::sync::mpsc::channel(100);
        let handle = PetHandle::new(pet_player.id, tx.clone());

        let pet = Pet {
            player: pet_player,
            master_id: master.id,
            status: PetStatus::Follow,
            type_pet: 0,
            is_tranform: false,
            last_time_die: 0,
            last_time_unfusion: 0,
            is_gohome: false,
            master_location: Some((master.location.x, master.location.y)),
            target_mob_id: None,
            target_player_id: None,
            last_time_chat: 0,
            chat_index: 0,
        };

        if let Some(zone) = ZONE_MANAGER.get_zone(pet.player.map_id, pet.player.zone_id) {
            let handle = PlayerHandle::new(pet.player.id, true, tx);
            PLAYER_MANAGER.add(pet.player.id, handle.clone());
            let _ = zone.add_player(handle).await;
            let _ = zone.load_me_to_another(pet.player.id).await;
        }

        let actor = PetActor::new(pet, rx);
        tokio::spawn(actor.run());

        Ok(handle)
    }

    pub async fn load_pet(
        master: &mut Player,
        data: crate::player::player_data::PetData,
    ) -> anyhow::Result<PetHandle> {
        let mut pet_player = Player::new(master.id + 1000000, data.name, data.gender as u8);
        master.pet_id = Some(pet_player.id);
        pet_player.is_pet = true;
        pet_player.head = data.head;
        pet_player.location = master.location.clone();
        pet_player.map_id = master.map_id;
        pet_player.zone_id = master.zone_id;

        // Load points
        pet_player.n_point.hp_base = data.n_point.hp_goc;
        pet_player.n_point.mp_base = data.n_point.mp_goc;
        pet_player.n_point.dame_base = data.n_point.damege_goc;
        pet_player.n_point.def_base = data.n_point.defen_goc;
        pet_player.n_point.crit_base = data.n_point.crit_goc;
        pet_player.n_point.hp_current = data.n_point.pl_hp;
        pet_player.n_point.mp_current = data.n_point.pl_mp;
        pet_player.n_point.power = data.n_point.power;
        pet_player.n_point.tiem_nang = data.n_point.tiem_nang;
        pet_player.n_point.stamina = data.n_point.stamina;
        pet_player.n_point.max_stamina = data.n_point.max_stamina;
        pet_player.n_point.limit_power = data.n_point.limit_power;

        // Load items body
        for item_data in data.items_body {
            if item_data.id != -1 {
                if let Some(mut item) =
                    crate::item::item_service::ItemService::create_new_item_with_quantity(
                        item_data.id as i16,
                        item_data.quantity,
                    )
                {
                    for opt in item_data.options {
                        item.add_option_param(opt.id as i8, opt.value as i16);
                    }
                    pet_player.inventory.items_body.push(item);
                }
            } else {
                pet_player
                    .inventory
                    .items_body
                    .push(crate::item::item_service::ItemService::create_item_null());
            }
        }

        // Load skills
        for skill_data in data.skills {
            if let Some(mut skill) =
                crate::utils::skill_util::create_skill(skill_data.template_id, skill_data.point)
                    .await
            {
                skill.start_time_use = skill_data.last_time_use;
                skill.curr_level = skill_data.curr_level;
                pet_player.player_skill.skills.push(skill);
            }
        }

        if let Some(first_skill) = pet_player.player_skill.skills.first() {
            pet_player.player_skill.skill_select = Some(first_skill.clone());
        }

        let (tx, rx) = tokio::sync::mpsc::channel(100);
        let handle = PetHandle::new(pet_player.id, tx.clone());

        let pet = Pet {
            player: pet_player,
            master_id: master.id,
            status: match data.status {
                0 => PetStatus::Follow,
                1 => PetStatus::Protect,
                2 => PetStatus::Attack,
                3 => PetStatus::GoHome,
                4 => PetStatus::Fusion,
                5 => PetStatus::HTVV,
                _ => PetStatus::Follow,
            },
            type_pet: data.type_pet,
            is_tranform: false,
            last_time_die: 0,
            last_time_unfusion: 0,
            is_gohome: false,
            master_location: Some((master.location.x, master.location.y)),
            target_mob_id: None,
            target_player_id: None,
            last_time_chat: 0,
            chat_index: 0,
        };

        if let Some(zone) =
            crate::map::zone_manager::ZONE_MANAGER.get_zone(pet.player.map_id, pet.player.zone_id)
        {
            let handle = PlayerHandle::new(pet.player.id, true, tx);
            crate::player::player_manager::PLAYER_MANAGER.add(pet.player.id, handle.clone());
            let _ = zone.add_player(handle).await;
            let _ = zone.load_me_to_another(pet.player.id).await;
        }

        let actor = PetActor::new(pet, rx);
        tokio::spawn(actor.run());

        Ok(handle)
    }
}
