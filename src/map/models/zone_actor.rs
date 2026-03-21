#![allow(dead_code)]
use crate::map::map_manager::{self, MAP_MANAGER};
use crate::map::services::mob_service;
use crate::map::ItemMapService;
use crate::mob::RtMob;
use crate::network::message::Message;
use crate::network::session::SessionArc;
use crate::player::player_actor::message::PlayerMessage;
use crate::player::player_actor::PlayerHandle;
use crate::player::player_manager::PLAYER_MANAGER;
use crate::services::effect_skill_service::{EffectAction, EffectSkillService};
use crate::services::player_tnsm_services::TypeTNSM;
use crate::services::task_utils::TaskUtils;
use crate::templates::item_template_manager;
use crate::{
    constant::const_item::{ITEM_DUI_GA_BINH_THUONG, ITEM_DUI_GA_NUONG, ITEM_EM_BE},
    map::item_map::ItemMap,
};
use anyhow::Result;
use std::collections::HashMap;
use tokio::sync::mpsc;

use super::zone::{ZoneInfo, ZoneMessage};

pub struct ZoneActor {
    pub map_id: i32,
    pub zone_id: i32,
    pub max_player: i32,
    pub active_players: HashMap<u64, PlayerHandle>,
    pub active_mobs: Vec<RtMob>,
    pub active_items: Vec<ItemMap>,
    pub rx: mpsc::Receiver<ZoneMessage>,
    pub public_state:
        std::sync::Arc<tokio::sync::RwLock<crate::map::models::zone::ZonePublicState>>,
}

impl ZoneActor {
    pub fn new(
        map_id: i32,
        zone_id: i32,
        max_player: i32,
        rx: mpsc::Receiver<ZoneMessage>,
    ) -> Self {
        Self {
            map_id,
            zone_id,
            max_player,
            active_players: HashMap::new(),
            active_mobs: Vec::new(),
            active_items: Vec::new(),
            rx,
            public_state: std::sync::Arc::new(tokio::sync::RwLock::new(
                crate::map::models::zone::ZonePublicState::default(),
            )),
        }
    }

    pub async fn run(mut self) {
        let mut interval = tokio::time::interval(tokio::time::Duration::from_millis(1000));
        interval.set_missed_tick_behavior(tokio::time::MissedTickBehavior::Skip);

        loop {
            tokio::select! {
                Some(msg) = self.rx.recv() => {
                    if let Err(e) = self.handle_message(msg).await {
                        tracing::error!("Error handling message in Zone {}-{}: {:?}", self.map_id, self.zone_id, e);
                    }
                }
                _ = interval.tick() => {
                    if !self.active_players.is_empty() {
                        if let Err(e) = self.update().await {
                            tracing::error!("Error updating Zone {}-{}: {:?}", self.map_id, self.zone_id, e);
                        }
                    }
                }
            }
        }
    }

    async fn handle_message(&mut self, msg: ZoneMessage) -> Result<()> {
        match msg {
            ZoneMessage::AddPlayer { handle } => {
                self.handle_add_player(handle);
            }
            ZoneMessage::RemovePlayer { id } => {
                self.active_players.remove(&id);
            }
            ZoneMessage::AddMob { mob } => {
                self.active_mobs.push(mob);
            }
            ZoneMessage::RemoveMob { id } => {
                self.active_mobs.retain(|m| m.id != id);
            }
            ZoneMessage::AddItem { item } => {
                self.active_items.push(item);
            }
            ZoneMessage::RemoveItem { id, tx } => {
                self.handle_remove_item(id, tx);
            }
            ZoneMessage::GetPlayer { id, tx } => {
                let _ = tx.send(self.active_players.get(&id).cloned());
            }
            ZoneMessage::GetAllPlayers { tx } => {
                let _ = tx.send(self.active_players.values().cloned().collect());
            }
            ZoneMessage::GetAllMobs { tx } => {
                let _ = tx.send(self.active_mobs.clone());
            }
            ZoneMessage::GetAllItems { tx } => {
                let _ = tx.send(self.active_items.clone());
            }
            ZoneMessage::GetZoneInfo { tx } => {
                self.handle_get_zone_info(tx);
            }
            ZoneMessage::MapInfo {
                session,
                player_id,
                x,
                y,
                task_info,
                spaceship_id,
            } => {
                self.handle_map_info(&session, player_id, x, y, task_info, spaceship_id)
                    .await?;
            }
            ZoneMessage::LoadAnotherToMe { player_id } => {
                self.handle_load_another_to_me(player_id).await?;
            }
            ZoneMessage::LoadMeToAnother { player_id } => {
                self.handle_load_me_to_another(player_id).await?;
            }
            ZoneMessage::CheckSpawnTaskItem {
                player_id,
                task_info,
            } => {
                self.handle_check_spawn_task_item(player_id, task_info)
                    .await;
            }
            ZoneMessage::UpdateTick => {
                self.update().await?;
            }
            ZoneMessage::StartStunMob { mob_id, time_stun } => {
                self.handle_start_stun_mob(mob_id, time_stun);
            }
            ZoneMessage::AttackMob {
                player_id,
                mob_id,
                damage,
                is_crit,
                die_when_hp_full,
                player_power,
            } => {
                self.handle_attack_mob(
                    player_id,
                    mob_id,
                    damage,
                    is_crit,
                    die_when_hp_full,
                    player_power,
                )
                .await;
            }
            ZoneMessage::SyncMobEffects {
                mob_id,
                effect_skill,
            } => {
                self.set_mob_effects(mob_id, effect_skill);
            }
            ZoneMessage::RemoveMobHold { mob_id, caster_id } => {
                self.handle_remove_mob_hold(mob_id, caster_id).await;
            }
            ZoneMessage::RemovePlayerHold {
                target_id,
                caster_id,
            } => {
                self.handle_remove_player_hold(target_id, caster_id).await;
            }
            ZoneMessage::Broadcast { msg, except_id } => {
                self.broadcast(msg, except_id);
            }
            ZoneMessage::AreaDamage {
                attacker_id,
                x,
                y,
                range,
                damage,
                is_player,
                die_when_hp_full,
                player_power,
            } => {
                self.handle_area_damage(
                    attacker_id,
                    x,
                    y,
                    range,
                    damage,
                    is_player,
                    die_when_hp_full,
                    player_power,
                )
                .await;
            }
        }
        Ok(())
    }

    // ─────────────────────────────────────────────────────────
    //  Handler functions
    // ─────────────────────────────────────────────────────────

    fn handle_add_player(&mut self, handle: PlayerHandle) {
        let is_pet = handle.is_pet;
        let is_boss = handle.boss_info.is_some();
        let player_count = self
            .active_players
            .values()
            .filter(|p| !p.is_pet && p.boss_info.is_none())
            .count();
        if is_pet || is_boss || player_count < self.max_player as usize {
            let id = handle.id;
            self.active_players.insert(id, handle);
        }
    }

    fn handle_remove_item(&mut self, id: i32, tx: tokio::sync::oneshot::Sender<Option<ItemMap>>) {
        let pos = self.active_items.iter().position(|i| i.item_map_id == id);
        let item = pos.map(|p| self.active_items.remove(p));
        let _ = tx.send(item);
    }

    fn handle_get_zone_info(&self, tx: tokio::sync::oneshot::Sender<ZoneInfo>) {
        let current_players = self.active_players.values().filter(|p| !p.is_pet).count() as i32;
        let _ = tx.send(ZoneInfo {
            map_id: self.map_id,
            zone_id: self.zone_id,
            max_player: self.max_player,
            current_players,
            mob_count: self.active_mobs.len() as i32,
            item_count: self.active_items.len() as i32,
        });
    }

    async fn handle_check_spawn_task_item(&mut self, player_id: u64, task_info: (i32, i32)) {
        self.spawn_special_item_task(Some(task_info));
        if let Some(player_handle) = self.active_players.get(&player_id) {
            let filtered_items = self.get_filtered_items_with_task(Some(task_info)).await;
            for item in filtered_items {
                let item_id = item.get_item_id();
                if item_id == ITEM_DUI_GA_BINH_THUONG
                    || item_id == ITEM_DUI_GA_NUONG
                    || item_id == ITEM_EM_BE
                {
                    let msg = ItemMapService::build_item_appear_for_me_message(&item);
                    player_handle.send_forget(PlayerMessage::SendPacket(msg));
                }
            }
        }
    }

    fn handle_start_stun_mob(&mut self, mob_id: u64, time_stun: u64) {
        if let Some(mob) = self.active_mobs.iter_mut().find(|m| m.id == mob_id) {
            EffectSkillService::start_stun_mob(mob, time_stun);
        }
    }

    async fn handle_attack_mob(
        &mut self,
        player_id: u64,
        mob_id: u64,
        damage: i32,
        is_crit: bool,
        die_when_hp_full: bool,
        player_power: i64,
    ) {
        mob_service::attack_mob_actor(
            self,
            player_id,
            mob_id,
            damage,
            is_crit,
            die_when_hp_full,
            player_power,
        )
        .await;
    }

    fn set_mob_effects(&mut self, mob_id: u64, effect_skill: crate::models::EffectSkill) {
        if let Some(mob) = self.active_mobs.iter_mut().find(|m| m.id == mob_id) {
            mob.effect_skill = effect_skill;
        }
    }

    async fn handle_remove_mob_hold(&mut self, mob_id: u64, caster_id: u64) {
        if let Some(mob) = self.active_mobs.iter_mut().find(|m| m.id == mob_id) {
            if mob.effect_skill.an_troi && mob.effect_skill.pl_troi_id == Some(caster_id) {
                EffectSkillService::remove_troi_mob(mob);
                let msg = EffectSkillService::build_effect_message(
                    caster_id,
                    mob_id,
                    true,
                    EffectAction::REMOVE,
                    EffectSkillService::HOLD_EFFECT,
                );
                self.broadcast(msg, None);
            }
        }
    }

    async fn handle_remove_player_hold(&self, target_id: u64, caster_id: u64) {
        if let Some(target_handle) = self.active_players.get(&target_id) {
            target_handle.send_forget(PlayerMessage::HandleAnTroi(false, 0, None));

            let msg = EffectSkillService::build_effect_message(
                caster_id,
                target_id,
                false,
                EffectAction::REMOVE,
                EffectSkillService::HOLD_EFFECT,
            );
            self.broadcast(msg, None);
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Internal helpers
    // ─────────────────────────────────────────────────────────

    pub async fn update(&mut self) -> anyhow::Result<()> {
        mob_service::update_actor(self).await;
        for handle in self.active_players.values() {
            handle.send_forget(PlayerMessage::UpdateTick);
        }

        let mut expired_ids = Vec::new();
        self.active_items.retain_mut(|item| {
            let result = item.update();
            if result.should_remove {
                expired_ids.push(item.item_map_id);
                false
            } else {
                true
            }
        });

        for id in expired_ids {
            let msg = ItemMapService::build_item_disappear_message(id);
            for handle in self.active_players.values() {
                handle.send_forget(PlayerMessage::SendPacket(msg.clone()));
            }
        }

        self.sync_public_state();
        Ok(())
    }

    pub fn sync_public_state(&self) {
        let public_state = self.public_state.clone();
        let mob_alive_count = self.active_mobs.iter().filter(|m| m.is_alive).count() as i32;
        let player_count = self
            .active_players
            .values()
            .filter(|p| !p.is_pet && p.boss_info.is_none())
            .count() as i32;
        let has_boss = self.active_players.values().any(|p| p.boss_info.is_some());

        tokio::spawn(async move {
            let mut state = public_state.write().await;
            state.mob_alive_count = mob_alive_count;
            state.player_count = player_count;
            state.has_boss = has_boss;
        });
    }

    fn spawn_special_item_task(&mut self, player_task_info: Option<(i32, i32)>) {
        if matches!(self.map_id, 42 | 43 | 44 | 21 | 22 | 23) {
            if let Some((task_id, task_index)) = player_task_info {
                let mut item_to_spawn = None;

                if matches!(self.map_id, 21 | 22 | 23) && task_id > 2 {
                    if !self
                        .active_items
                        .iter()
                        .any(|it| it.get_item_id() == ITEM_DUI_GA_NUONG)
                    {
                        let (x, y) = if self.map_id == 21 {
                            (633, 315)
                        } else if self.map_id == 22 {
                            (56, 315)
                        } else {
                            (633, 320)
                        };
                        item_to_spawn = Some((ITEM_DUI_GA_NUONG, x, y));
                    }
                }
                if matches!(self.map_id, 42 | 43 | 44) && task_id == 3 && task_index == 1 {
                    if !self
                        .active_items
                        .iter()
                        .any(|i| i.get_item_id() == ITEM_EM_BE)
                    {
                        let x = 70;
                        let y = if self.map_id == 43 { 264 } else { 288 };
                        item_to_spawn = Some((ITEM_EM_BE, x, y));
                    }
                }

                if let Some((template_id, x, y)) = item_to_spawn {
                    if let Some(template) = item_template_manager::get(template_id) {
                        let mut item_map = ItemMap::new(Some(template), 1, x as i32, y as i32, -1);
                        item_map.set_location(self.map_id, self.zone_id, x as i32, y as i32);
                        self.active_items.push(item_map);
                    }
                }
            }
        }
    }

    async fn handle_map_info(
        &mut self,
        session: &SessionArc,
        player_id: u64,
        x: i16,
        y: i16,
        player_task_info: Option<(i32, i32)>,
        spaceship_id: i8,
    ) -> Result<()> {
        tracing::info!(
            "[MAP_INFO] Start for player {} Map {}",
            player_id,
            self.map_id
        );
        let (planet_id, tile_id, bg_id, bg_type, map_type, map_name) = {
            if let Some(map) = map_manager::MAP_MANAGER.find_by_id(self.map_id) {
                (
                    map.info.planet_id as i8,
                    map.info.tile_id as i8,
                    map.info.bg_id as i8,
                    map.info.bg_type as i8,
                    map.info.r#type as i8,
                    map.info.name.clone(),
                )
            } else {
                (0i8, 0i8, 0i8, 0i8, 0i8, format!("Map {}", self.map_id))
            }
        };
        self.spawn_special_item_task(player_task_info);
        let mut msg = Message::new(-24);
        msg.write_byte((self.map_id as u8) as i8)?;
        msg.write_byte(planet_id)?;
        msg.write_byte(tile_id)?;
        msg.write_byte(bg_id)?;
        msg.write_byte(map_type)?;
        msg.write_utf(&map_name)?;
        msg.write_byte(self.zone_id as i8)?;
        msg.write_short(x)?;
        msg.write_short(y)?;

        // Waypoints
        if let Some(map) = MAP_MANAGER.find_by_id(self.map_id) {
            let wps = &map.info.waypoints;
            let count = (wps.len().min(127)) as i8;
            msg.write_byte(count)?;
            for wp in wps.iter().take(count as usize) {
                msg.write_short(wp.min_x)?;
                msg.write_short(wp.min_y)?;
                msg.write_short(wp.max_x)?;
                msg.write_short(wp.max_y)?;
                msg.write_boolean(wp.is_enter)?;
                msg.write_boolean(wp.is_offline)?;
                msg.write_utf(&wp.name)?;
            }
        } else {
            msg.write_byte(0)?;
        }

        // Mobs
        let mob_count: i8 = (self.active_mobs.len().min(127)) as i8;
        msg.write_byte(mob_count)?;
        for mob in self.active_mobs.iter().take(mob_count as usize) {
            msg.write_boolean(false)?;
            msg.write_boolean(false)?;
            msg.write_boolean(false)?;
            msg.write_boolean(false)?;
            msg.write_boolean(false)?;
            msg.write_byte(mob.template_id as i8)?;
            msg.write_byte(0)?;
            msg.write_int(mob.hp)?;
            msg.write_byte(mob.level)?;
            msg.write_int(mob.max_hp)?;
            msg.write_short(mob.location.x)?;
            msg.write_short(mob.location.y)?;
            msg.write_byte(mob.status)?;
            msg.write_byte(mob.lv_mob)?;
            msg.write_boolean(false)?;
        }

        msg.write_byte(0)?;

        // NPCs
        let (npcs_for_map, avatar_lookup) = {
            let npcs =
                if let Some(map) = crate::map::map_manager::MAP_MANAGER.find_by_id(self.map_id) {
                    map.info.npcs.clone()
                } else {
                    Vec::new()
                };
            let avatars: std::collections::HashMap<i32, i32> =
                crate::templates::npc_template_manager::get_all()
                    .iter()
                    .map(|t| (t.id, t.avatar.unwrap_or(0)))
                    .collect();
            (npcs, avatars)
        };
        let count: i8 = (npcs_for_map.len().min(127)) as i8;
        msg.write_byte(count)?;
        for npc in npcs_for_map.into_iter().take(count as usize) {
            let status: i8 = 1;
            let avatar: i16 = avatar_lookup.get(&npc.temp_id).cloned().unwrap_or(0) as i16;
            msg.write_byte(status)?;
            msg.write_short(npc.x)?;
            msg.write_short(npc.y)?;
            msg.write_byte(npc.temp_id as i8)?;
            msg.write_short(avatar)?;
        }

        let filtered_items = self.get_filtered_items_with_task(player_task_info).await;
        let item_count = filtered_items.len().min(127) as i8;
        msg.write_byte(item_count)?;
        for item in filtered_items.iter().take(item_count as usize) {
            msg.write_short(item.item_map_id as i16)?;
            msg.write_short(item.get_item_id())?;
            msg.write_short(item.x as i16)?;
            msg.write_short(item.y as i16)?;
            msg.write_int(item.player_id as i32)?;
        }
        let bg_item_path = format!("data/arc/map/item_bg_map_data/{}", self.map_id);
        if let Ok(data) = std::fs::read(&bg_item_path) {
            msg.write(&data)?;
        } else {
            msg.write_short(0)?;
        }

        let eff_item_path = format!("data/arc/map/eff_map/{}", self.map_id);
        if let Ok(data) = std::fs::read(&eff_item_path) {
            msg.write(&data)?;
        } else {
            msg.write_short(0)?;
        }

        msg.write_byte(bg_type)?;
        msg.write_byte(spaceship_id)?;
        msg.write_byte(if self.map_id == 148 { 1 } else { 0 })?;

        session.transmit(msg);
        tracing::info!("[MAP_INFO] Transmitted MapInfo -24 to player {}", player_id);
        Ok(())
    }

    async fn get_filtered_items(&self, player_id: u64) -> Vec<ItemMap> {
        let player_task_info = if let Some(handle) = PLAYER_MANAGER.get(player_id) {
            handle
                .get_snapshot()
                .await
                .map(|p| (TaskUtils::get_id_task(&p), TaskUtils::get_task_index(&p)))
        } else {
            None
        };
        self.get_filtered_items_with_task(player_task_info).await
    }

    async fn get_filtered_items_with_task(
        &self,
        player_task_info: Option<(i32, i32)>,
    ) -> Vec<ItemMap> {
        let mut filtered = Vec::new();

        for item in &self.active_items {
            let item_temp_id = item.get_item_id();

            match item_temp_id {
                ITEM_EM_BE => {
                    if let Some((3, 1)) = player_task_info {
                        filtered.push(item.clone());
                    }
                }
                ITEM_DUI_GA_BINH_THUONG | ITEM_DUI_GA_NUONG => {
                    if let Some((task_id, _)) = player_task_info {
                        if task_id >= 2 {
                            filtered.push(item.clone());
                        }
                    }
                }
                726 => {
                    filtered.push(item.clone());
                }
                _ => filtered.push(item.clone()),
            }
        }
        filtered
    }

    async fn handle_load_another_to_me(&self, player_id: u64) -> Result<()> {
        let Some(receiver_handle) = self.active_players.get(&player_id) else {
            return Ok(());
        };
        for (other_id, other_handle) in &self.active_players {
            if *other_id != player_id {
                other_handle.send_forget(PlayerMessage::SendInfoTo(receiver_handle.clone()));
            }
        }
        Ok(())
    }

    async fn handle_load_me_to_another(&self, player_id: u64) -> Result<()> {
        let Some(player_handle) = self.active_players.get(&player_id) else {
            return Ok(());
        };

        let mut others = Vec::new();
        for (receiver_id, receiver_handle) in &self.active_players {
            if *receiver_id != player_id {
                others.push(receiver_handle.clone());
            }
        }

        if !others.is_empty() {
            player_handle.send_forget(PlayerMessage::SendInfoToAll(others));
        }
        Ok(())
    }

    async fn handle_area_damage(
        &mut self,
        attacker_id: u64,
        x: i16,
        y: i16,
        range: i16,
        damage: i64,
        is_player: bool,
        die_when_hp_full: bool,
        player_power: i64,
    ) {
        let center = crate::utils::Location { x, y };
        let mut messages = Vec::new();
        for mob in self.active_mobs.iter_mut() {
            if crate::utils::MapUtils::is_position_in_range(&center, &mob.location, range) {
                if !mob.is_alive {
                    continue;
                }
                let real_damage = mob.take_damage(damage as i32, die_when_hp_full);
                if is_player {
                    mob.add_temporary_enemy(attacker_id);
                    if let Some(handle) = self.active_players.get(&attacker_id) {
                        let tnsm_amount =
                            mob.get_tiemnang_for_player(player_power, real_damage as i64);
                        handle.send_forget(PlayerMessage::AddTNSM {
                            type_tnsm: TypeTNSM::All,
                            param: tnsm_amount,
                            is_ori: true,
                        });
                    }
                }
                let msg = if mob.is_dead() {
                    mob_service::build_mob_die_message(mob.id as i8, real_damage, false)
                } else {
                    mob_service::build_mob_take_dame_client(
                        mob.id as i8,
                        mob.hp,
                        real_damage,
                        false,
                    )
                };
                messages.push(msg);
            }
        }

        for msg in messages {
            self.broadcast(msg, None);
        }
    }

    pub fn broadcast(&self, msg: Message, except_id: Option<u64>) {
        for handle in self.active_players.values() {
            if let Some(eid) = except_id {
                if handle.id == eid {
                    continue;
                }
            }
            handle.send_forget(PlayerMessage::SendPacket(msg.clone()));
        }
    }
}
