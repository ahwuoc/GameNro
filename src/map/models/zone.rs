#![allow(dead_code)]
use crate::map::item_map::ItemMap;
use crate::map::map_manager;
use crate::map::services::mob_service;
use crate::mob::RtMob;
use crate::network::message::Message;
use crate::network::session::SessionArc;
use crate::player::player::Player;
use crate::player::player_actor::PlayerHandle;
use crate::player::player_manager::PLAYER_MANAGER;
use crate::services::player_service;
use anyhow::Result;
use std::collections::HashMap;
use std::sync::Arc;
use tokio::sync::{mpsc, oneshot};

pub enum ZoneMessage {
    AddPlayer {
        handle: PlayerHandle,
    },
    RemovePlayer {
        id: u64,
    },
    AddMob {
        mob: RtMob,
    },
    RemoveMob {
        id: u64,
    },
    AddItem {
        item: ItemMap,
    },
    RemoveItem {
        id: i32,
        tx: oneshot::Sender<Option<ItemMap>>,
    },
    GetPlayer {
        id: u64,
        tx: oneshot::Sender<Option<PlayerHandle>>,
    },
    GetAllPlayers {
        tx: oneshot::Sender<Vec<PlayerHandle>>,
    },
    GetAllMobs {
        tx: oneshot::Sender<Vec<RtMob>>,
    },
    GetAllItems {
        tx: oneshot::Sender<Vec<ItemMap>>,
    },
    GetZoneInfo {
        tx: oneshot::Sender<ZoneInfo>,
    },
    MapInfo {
        session: SessionArc,
        player_id: u64,
    },
    LoadAnotherToMe {
        player_id: u64,
    },
    LoadMeToAnother {
        player_id: u64,
    },
    UpdateTick,
    StartStunMob {
        mob_id: u64,
        time_stun: u64,
    },
    AttackMob {
        player_id: u64,
        mob_id: u64,
        damage: i32,
    },
    SyncMobEffects {
        mob_id: u64,
        effect_skill: crate::models::EffectSkill,
    },
    RemoveMobHold {
        mob_id: u64,
        caster_id: u64,
    },
    RemovePlayerHold {
        target_id: u64,
        caster_id: u64,
    },
    Broadcast {
        msg: Message,
        except_id: Option<u64>,
    },
    AreaDamage {
        attacker_id: u64,
        x: i16,
        y: i16,
        range: i16,
        damage: i64,
        is_player: bool,
    },
}

#[derive(Clone, Debug)]
pub struct ZoneHandle {
    pub map_id: i32,
    pub zone_id: i32,
    pub tx: mpsc::Sender<ZoneMessage>,
}

impl ZoneHandle {
    pub async fn add_player(&self, handle: PlayerHandle) -> Result<()> {
        self.tx.send(ZoneMessage::AddPlayer { handle }).await?;
        Ok(())
    }

    pub async fn remove_player(&self, id: u64) -> Result<()> {
        self.tx.send(ZoneMessage::RemovePlayer { id }).await?;
        Ok(())
    }

    pub async fn add_mob(&self, mob: RtMob) -> Result<()> {
        self.tx.send(ZoneMessage::AddMob { mob }).await?;
        Ok(())
    }

    pub async fn remove_mob(&self, id: u64) -> Result<()> {
        self.tx.send(ZoneMessage::RemoveMob { id }).await?;
        Ok(())
    }

    pub async fn start_stun_mob(&self, mob_id: u64, time_stun: u64) -> Result<()> {
        self.tx
            .send(ZoneMessage::StartStunMob { mob_id, time_stun })
            .await?;
        Ok(())
    }

    pub async fn get_all_mobs(&self) -> Result<Vec<RtMob>> {
        let (tx, rx) = oneshot::channel();
        self.tx.send(ZoneMessage::GetAllMobs { tx }).await?;
        Ok(rx.await?)
    }

    pub async fn add_item(&self, item: ItemMap) -> Result<()> {
        self.tx.send(ZoneMessage::AddItem { item }).await?;
        Ok(())
    }

    pub async fn remove_item(&self, id: i32) -> Result<Option<ItemMap>> {
        let (tx, rx) = oneshot::channel();
        self.tx.send(ZoneMessage::RemoveItem { id, tx }).await?;
        Ok(rx.await?)
    }

    pub async fn get_item(&self, id: i32) -> Result<Option<ItemMap>> {
        let (tx, rx) = oneshot::channel();
        self.tx.send(ZoneMessage::GetAllItems { tx }).await?;
        let items = rx.await?;
        Ok(items.into_iter().find(|i| i.item_map_id == id))
    }

    pub async fn get_all_items(&self) -> Result<Vec<ItemMap>> {
        let (tx, rx) = oneshot::channel();
        self.tx.send(ZoneMessage::GetAllItems { tx }).await?;
        Ok(rx.await?)
    }

    pub async fn get_player(&self, id: u64) -> Result<Option<PlayerHandle>> {
        let (tx, rx) = oneshot::channel();
        self.tx.send(ZoneMessage::GetPlayer { id, tx }).await?;
        Ok(rx.await?)
    }

    pub async fn get_all_players(&self) -> Result<Vec<PlayerHandle>> {
        let (tx, rx) = oneshot::channel();
        self.tx.send(ZoneMessage::GetAllPlayers { tx }).await?;
        Ok(rx.await?)
    }

    pub async fn get_zone_info(&self) -> Result<ZoneInfo> {
        let (tx, rx) = oneshot::channel();
        self.tx.send(ZoneMessage::GetZoneInfo { tx }).await?;
        Ok(rx.await?)
    }

    pub async fn map_info(&self, session: SessionArc, player_id: u64) -> Result<()> {
        self.tx
            .send(ZoneMessage::MapInfo { session, player_id })
            .await?;
        Ok(())
    }

    pub async fn load_another_to_me(&self, player_id: u64) -> Result<()> {
        self.tx
            .send(ZoneMessage::LoadAnotherToMe { player_id })
            .await?;
        Ok(())
    }

    pub async fn load_me_to_another(&self, player_id: u64) -> Result<()> {
        self.tx
            .send(ZoneMessage::LoadMeToAnother { player_id })
            .await?;
        Ok(())
    }

    pub fn broadcast(&self, msg: Message) {
        let _ = self.tx.try_send(ZoneMessage::Broadcast {
            msg,
            except_id: None,
        });
    }

    pub fn sync_mob_effects(&self, mob_id: u64, effect_skill: crate::models::EffectSkill) {
        let _ = self.tx.try_send(ZoneMessage::SyncMobEffects {
            mob_id,
            effect_skill,
        });
    }

    pub fn remove_mob_hold(&self, mob_id: u64, caster_id: u64) {
        let _ = self
            .tx
            .try_send(ZoneMessage::RemoveMobHold { mob_id, caster_id });
    }

    pub fn remove_player_hold(&self, target_id: u64, caster_id: u64) {
        let _ = self.tx.try_send(ZoneMessage::RemovePlayerHold {
            target_id,
            caster_id,
        });
    }

    pub fn broadcast_except(&self, msg: Message, except_id: u64) {
        let _ = self.tx.try_send(ZoneMessage::Broadcast {
            msg,
            except_id: Some(except_id),
        });
    }
}

pub struct Zone {
    pub map_id: i32,
    pub zone_id: i32,
    pub max_player: i32,
    pub players: HashMap<u64, PlayerHandle>,
    pub active_mobs: Vec<RtMob>,
    pub active_items: Vec<ItemMap>,
    pub rx: mpsc::Receiver<ZoneMessage>,
}

impl Zone {
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
            players: HashMap::new(),
            active_mobs: Vec::new(),
            active_items: Vec::new(),
            rx,
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
                    if !self.players.is_empty() {
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
                let is_pet = handle.is_pet;
                let is_boss = handle.boss_info.is_some();
                let player_count = self
                    .players
                    .values()
                    .filter(|p| !p.is_pet && p.boss_info.is_none())
                    .count();
                if is_pet || is_boss || player_count < self.max_player as usize {
                    let id = handle.id;
                    self.players.insert(id, handle);
                }
            }
            ZoneMessage::RemovePlayer { id } => {
                self.players.remove(&id);
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
                let pos = self.active_items.iter().position(|i| i.item_map_id == id);
                let item = pos.map(|p| self.active_items.remove(p));
                let _ = tx.send(item);
            }
            ZoneMessage::GetPlayer { id, tx } => {
                let _ = tx.send(self.players.get(&id).cloned());
            }
            ZoneMessage::GetAllPlayers { tx } => {
                let _ = tx.send(self.players.values().cloned().collect());
            }
            ZoneMessage::GetAllMobs { tx } => {
                let _ = tx.send(self.active_mobs.clone());
            }
            ZoneMessage::GetAllItems { tx } => {
                let _ = tx.send(self.active_items.clone());
            }
            ZoneMessage::GetZoneInfo { tx } => {
                let current_players = self.players.values().filter(|p| !p.is_pet).count() as i32;
                let _ = tx.send(ZoneInfo {
                    map_id: self.map_id,
                    zone_id: self.zone_id,
                    max_player: self.max_player,
                    current_players,
                    mob_count: self.active_mobs.len() as i32,
                    item_count: self.active_items.len() as i32,
                });
            }
            ZoneMessage::MapInfo { session, player_id } => {
                self.handle_map_info(&session, player_id).await?;
            }
            ZoneMessage::LoadAnotherToMe { player_id } => {
                self.handle_load_another_to_me(player_id).await?;
            }
            ZoneMessage::LoadMeToAnother { player_id } => {
                self.handle_load_me_to_another(player_id).await?;
            }
            ZoneMessage::UpdateTick => {
                self.update().await?;
            }
            ZoneMessage::StartStunMob { mob_id, time_stun } => {
                if let Some(mob) = self.active_mobs.iter_mut().find(|m| m.id == mob_id) {
                    crate::services::effect_skill_service::EffectSkillService::start_stun_mob(
                        mob, time_stun,
                    );
                }
            }
            ZoneMessage::AttackMob {
                player_id,
                mob_id,
                damage,
            } => {
                mob_service::attack_mob_actor(self, player_id, mob_id, damage).await;
            }
            ZoneMessage::SyncMobEffects {
                mob_id,
                effect_skill,
            } => {
                if let Some(mob) = self.active_mobs.iter_mut().find(|m| m.id == mob_id) {
                    mob.effect_skill = effect_skill;
                }
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
            } => {
                self.handle_area_damage(attacker_id, x, y, range, damage, is_player)
                    .await;
            }
        }
        Ok(())
    }

    pub async fn update(&mut self) -> anyhow::Result<()> {
        mob_service::update_actor(self).await;
        for handle in self.players.values() {
            handle.send_forget(crate::player::player_actor::PlayerMessage::UpdateTick);
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
            let msg = crate::map::services::item_map_service::ItemMapService::build_item_disappear_message(id);
            for handle in self.players.values() {
                handle.send_forget(crate::player::player_actor::PlayerMessage::SendPacket(
                    msg.clone(),
                ));
            }
        }
        Ok(())
    }

    async fn handle_map_info(&self, session: &SessionArc, player_id: u64) -> Result<()> {
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

        let snapshot = {
            if let Some(handle) = self.players.get(&player_id) {
                let (tx, rx) = oneshot::channel();
                handle
                    .send(crate::player::player_actor::PlayerMessage::GetSnapshot(tx))
                    .await?;
                rx.await?
            } else {
                return Err(anyhow::anyhow!("Player not in zone"));
            }
        };

        let mut msg = Message::new(-24);
        msg.write_byte((self.map_id as u8) as i8)?;
        msg.write_byte(planet_id)?;
        msg.write_byte(tile_id)?;
        msg.write_byte(bg_id)?;
        msg.write_byte(map_type)?;
        msg.write_utf(&map_name)?;
        msg.write_byte(self.zone_id as i8)?;
        msg.write_short(snapshot.location.x)?;
        msg.write_short(snapshot.location.y)?;

        // Waypoints
        if let Some(map) = map_manager::MAP_MANAGER.find_by_id(self.map_id) {
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

        msg.write_byte(0)?;

        // Map Graphics/Effects
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
        msg.write_byte(0)?;
        msg.write_byte(if self.map_id == 148 { 1 } else { 0 })?;

        session.transmit(msg);
        Ok(())
    }

    async fn handle_load_another_to_me(&self, player_id: u64) -> Result<()> {
        let Some(receiver_handle) = self.players.get(&player_id) else {
            return Ok(());
        };

        for (other_id, other_handle) in &self.players {
            if *other_id != player_id {
                let (tx, rx) = oneshot::channel();
                other_handle
                    .send(crate::player::player_actor::PlayerMessage::GetSnapshot(tx))
                    .await?;
                let other_snapshot = rx.await?;
                let _ = crate::services::ServiceHandles::send_player_info_to_handle(
                    receiver_handle,
                    &other_snapshot,
                );
            }
        }
        Ok(())
    }

    async fn handle_load_me_to_another(&self, player_id: u64) -> Result<()> {
        let Some(player_handle) = self.players.get(&player_id) else {
            return Ok(());
        };
        let (tx, rx) = oneshot::channel();
        player_handle
            .send(crate::player::player_actor::PlayerMessage::GetSnapshot(tx))
            .await?;
        let snapshot = rx.await?;

        for (receiver_id, receiver_handle) in &self.players {
            if *receiver_id != player_id {
                let _ = crate::services::ServiceHandles::send_player_info_to_handle(
                    receiver_handle,
                    &snapshot,
                );
            }
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
    ) {
        let center = crate::utils::Location { x, y };
        let mut messages = Vec::new();
        // Damage mobs
        for mob in self.active_mobs.iter_mut() {
            if crate::utils::MapUtils::is_position_in_range(&center, &mob.location, range) {
                if !mob.is_alive {
                    continue;
                }
                let real_damage = mob.take_damage(damage as i32);
                if is_player {
                    mob.add_temporary_enemy(attacker_id);
                }
                let msg = if mob.is_dead() {
                    mob_service::build_mob_die_message(mob.id as i8, real_damage, false)
                } else {
                    mob_service::build_mob_alive_message(mob.id as i8, mob.hp, real_damage, false)
                };
                messages.push(msg);
            }
        }

        for msg in messages {
            self.broadcast(msg, None);
        }
    }

    async fn handle_remove_mob_hold(&mut self, mob_id: u64, caster_id: u64) {
        if let Some(mob) = self.active_mobs.iter_mut().find(|m| m.id == mob_id) {
            // Verification that it's the same caster
            if mob.effect_skill.an_troi && mob.effect_skill.pl_troi_id == Some(caster_id) {
                crate::services::effect_skill_service::EffectSkillService::remove_troi_mob(mob);
                let msg = crate::services::effect_skill_service::EffectSkillService::build_effect_mob_message(
                    caster_id,
                    mob_id,
                    crate::services::effect_skill_service::EffectAction::REMOVE,
                    crate::services::effect_skill_service::EffectSkillService::HOLD_EFFECT,
                );
                self.broadcast(msg, None);
            }
        }
    }

    async fn handle_remove_player_hold(&self, target_id: u64, caster_id: u64) {
        if let Some(target_handle) = self.players.get(&target_id) {
            target_handle.send_forget(crate::player::player_actor::PlayerMessage::HandleAnTroi(
                false, 0, None,
            ));

            let msg = crate::services::effect_skill_service::EffectSkillService::build_effect_player_message(
                caster_id,
                target_id,
                crate::services::effect_skill_service::EffectAction::REMOVE,
                crate::services::effect_skill_service::EffectSkillService::HOLD_EFFECT,
            );
            self.broadcast(msg, None);
        }
    }

    pub fn broadcast(&self, msg: Message, except_id: Option<u64>) {
        for handle in self.players.values() {
            if let Some(eid) = except_id {
                if handle.id == eid {
                    continue;
                }
            }
            handle.send_forget(crate::player::player_actor::PlayerMessage::SendPacket(
                msg.clone(),
            ));
        }
    }
}

pub struct ZoneInfo {
    pub map_id: i32,
    pub zone_id: i32,
    pub max_player: i32,
    pub current_players: i32,
    pub mob_count: i32,
    pub item_count: i32,
}
