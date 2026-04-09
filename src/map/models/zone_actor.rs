#![allow(dead_code)]
use crate::map::map_manager::{self, MAP_MANAGER};
use crate::map::services::map_packet_service::MapPacketService;
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
use std::time::Duration;
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
        let mut interval = tokio::time::interval(Duration::from_millis(500));
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
                self.handle_map_info(&session, player_id, x, y, task_info, spaceship_id)?;
            }
            ZoneMessage::LoadAnotherToMe { player_id } => {
                self.handle_load_another_to_me(player_id)?;
            }
            ZoneMessage::LoadMeToAnother { player_id } => {
                self.handle_load_me_to_another(player_id)?;
            }
            ZoneMessage::CheckSpawnTaskItem {
                player_id,
                task_info,
            } => {
                self.handle_check_spawn_task_item(player_id, task_info);
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
                self.handle_remove_mob_hold(mob_id, caster_id);
            }
            ZoneMessage::RemovePlayerHold {
                target_id,
                caster_id,
            } => {
                self.handle_remove_player_hold(target_id, caster_id);
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
                );
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

    fn handle_check_spawn_task_item(&mut self, player_id: u64, task_info: (i32, i32)) {
        self.spawn_special_item_task(Some(task_info));
        if let Some(player_handle) = self.active_players.get(&player_id) {
            let filtered_items = self.get_filtered_items_with_task(Some(task_info));
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

    fn handle_remove_mob_hold(&mut self, mob_id: u64, caster_id: u64) {
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

    fn handle_remove_player_hold(&self, target_id: u64, caster_id: u64) {
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
        // Update mobs
        mob_service::update_actor(self).await;

        // Drive player ticks
        for handle in self.active_players.values() {
            handle.send_forget(PlayerMessage::UpdateTick);
        }

        // Update items and broadcast disappearances
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
            self.broadcast(msg, None);
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

    fn handle_map_info(
        &mut self,
        session: &SessionArc,
        player_id: u64,
        x: i16,
        y: i16,
        player_task_info: Option<(i32, i32)>,
        spaceship_id: i8,
    ) -> Result<()> {
        self.spawn_special_item_task(player_task_info);
        MapPacketService::send_map_info(
            self,
            session,
            player_id,
            x,
            y,
            player_task_info,
            spaceship_id,
        )?;
        Ok(())
    }

    async fn get_filtered_items(&self, player_id: u64) -> Vec<ItemMap> {
        let player_task_info = if let Some(handle) = self.active_players.get(&player_id) {
            let cache = handle.public_state.read().await;
            Some((cache.task_id, cache.task_index))
        } else {
            None
        };
        self.get_filtered_items_with_task(player_task_info)
    }

    pub fn get_filtered_items_with_task(
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

    fn handle_load_another_to_me(&self, player_id: u64) -> Result<()> {
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

    fn handle_load_me_to_another(&self, player_id: u64) -> Result<()> {
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

    fn handle_area_damage(
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
