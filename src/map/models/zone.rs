#![allow(dead_code)]
use crate::map::item_map::ItemMap;
use crate::map::map_manager;
use crate::map::services::mob_service;
use crate::mob::RtMob;
use crate::network::message::Message;
use crate::network::session::{AsyncSession, SessionArc};
use crate::player::player::Player;
use crate::player::player_manager::PLAYER_MANAGER;
use crate::services::effect_skill_service::{EffectAction, EffectSkillService};
use crate::services::player_service;
use crate::utils::time;
use anyhow::Result;
use dashmap::DashMap;
use dashmap::DashSet;
use std::sync::Arc;
use std::sync::RwLock;

pub struct Zone {
    pub map_id: i32,
    pub zone_id: i32,
    pub max_player: i32,

    pub player_ids: Arc<DashSet<u64>>,
    pub active_mobs: Arc<RwLock<Vec<RtMob>>>,
    pub active_items: Arc<RwLock<Vec<ItemMap>>>,
}

impl Zone {
    pub fn new(map_id: i32, zone_id: i32, max_player: i32) -> Self {
        Self {
            map_id,
            zone_id,
            max_player,
            player_ids: Arc::new(DashSet::new()),
            active_mobs: Arc::new(RwLock::new(Vec::new())),
            active_items: Arc::new(RwLock::new(Vec::new())),
        }
    }

    pub fn is_empty(&self) -> bool {
        self.player_ids.is_empty()
    }

    pub fn is_full(&self) -> bool {
        self.player_ids.len() >= self.max_player as usize
    }

    pub fn get_num_players(&self) -> usize {
        self.player_ids.len()
    }

    pub fn add_player(&self, player: Player) -> anyhow::Result<()> {
        if self.player_ids.len() >= self.max_player as usize {
            return Err(anyhow::anyhow!("Zone is full"));
        }
        let player_id = player.id;
        PLAYER_MANAGER.add(player); // Central update
        self.player_ids.insert(player_id);
        Ok(())
    }

    pub fn remove_player(&self, player_id: u64) -> anyhow::Result<()> {
        self.player_ids.remove(&player_id);
        Ok(())
    }

    pub fn get_player(&self, player_id: u64) -> Option<Player> {
        if self.player_ids.contains(&player_id) {
            PLAYER_MANAGER.get(player_id)
        } else {
            None
        }
    }

    pub fn get_all_players(&self) -> Vec<Player> {
        self.player_ids
            .iter()
            .filter_map(|id_ref| PLAYER_MANAGER.get(*id_ref))
            .collect()
    }

    pub fn add_mob(&self, mob: RtMob) -> anyhow::Result<()> {
        let mut mobs = self.active_mobs.write().unwrap();
        mobs.push(mob);
        Ok(())
    }

    pub fn remove_mob(&self, mob_id: u64) -> anyhow::Result<()> {
        let mut mobs = self.active_mobs.write().unwrap();
        mobs.retain(|mob| mob.id != mob_id);
        Ok(())
    }

    pub fn start_stun_mob(&self, mob_id: u64, time_stun: u64) -> anyhow::Result<()> {
        let mut mobs = self.active_mobs.write().unwrap();
        if let Some(mob) = mobs.iter_mut().find(|m| m.id == mob_id) {
            crate::services::effect_skill_service::EffectSkillService::start_stun_mob(
                mob, time_stun,
            );
        }
        Ok(())
    }

    pub fn get_all_mobs(&self) -> Vec<RtMob> {
        let mobs = self.active_mobs.read().unwrap();
        mobs.clone()
    }

    pub fn add_item(&self, item: ItemMap) -> anyhow::Result<()> {
        let mut items = self.active_items.write().unwrap();
        items.push(item);
        Ok(())
    }

    pub fn remove_item(&self, item_map_id: i32) -> Option<ItemMap> {
        let mut items = self.active_items.write().unwrap();
        if let Some(pos) = items.iter().position(|i| i.item_map_id == item_map_id) {
            Some(items.remove(pos))
        } else {
            None
        }
    }

    pub fn get_item(&self, item_map_id: i32) -> Option<ItemMap> {
        let items = self.active_items.read().unwrap();
        items.iter().find(|i| i.item_map_id == item_map_id).cloned()
    }

    pub fn get_all_items(&self) -> Vec<ItemMap> {
        let items = self.active_items.read().unwrap();
        items.clone()
    }

    pub fn update(&self) -> anyhow::Result<()> {
        mob_service::update(self);
        player_service::update(self);

        let mut items = self.active_items.write().unwrap();
        items.retain_mut(|item| {
            let result = item.update();
            !result.should_remove
        });

        Ok(())
    }

    pub fn get_zone_info(&self) -> ZoneInfo {
        let mobs = self.active_mobs.read().unwrap();
        let items = self.active_items.read().unwrap();

        ZoneInfo {
            map_id: self.map_id,
            zone_id: self.zone_id,
            max_player: self.max_player,
            current_players: self.player_ids.len() as i32,
            mob_count: mobs.len() as i32,
            item_count: items.len() as i32,
        }
    }

    pub fn send_message_to_all_players(&self, msg: Message) -> anyhow::Result<()> {
        for player_id in self.player_ids.iter() {
            if let Some(player) = PLAYER_MANAGER.get(*player_id) {
                let _ = player.send_to_client(msg.clone());
            }
        }
        Ok(())
    }

    pub fn send_message_to_other_players(
        &self,
        except_player_id: u64,
        msg: Message,
    ) -> anyhow::Result<()> {
        for player_id in self.player_ids.iter() {
            if *player_id != except_player_id {
                if let Some(player) = PLAYER_MANAGER.get(*player_id) {
                    let _ = player.send_to_client(msg.clone());
                }
            }
        }
        Ok(())
    }

    pub fn load_me_to_another(&self, player_id: u64) -> anyhow::Result<()> {
        if !self.player_ids.contains(&player_id) {
            return Ok(());
        }

        let target_player = PLAYER_MANAGER.get(player_id);
        let receivers: Vec<Player> = self
            .player_ids
            .iter()
            .filter(|id_ref| **id_ref != player_id)
            .filter_map(|id_ref| PLAYER_MANAGER.get(*id_ref))
            .collect();

        if let Some(info_player) = target_player {
            for receiver in receivers {
                let _ = crate::services::ServiceHandles::send_player_info(&receiver, &info_player);

                if info_player.is_die() {
                    let death_msg =
                        crate::services::ServiceHandles::build_player_death_message(&info_player);
                    let _ = receiver.send_to_client(death_msg);
                }
            }
        }
        Ok(())
    }

    pub fn load_another_to_me(&self, player_id: u64) -> anyhow::Result<()> {
        let Some(receiver) = PLAYER_MANAGER.get(player_id) else {
            return Ok(());
        };

        let others: Vec<Player> = self
            .player_ids
            .iter()
            .filter(|id_ref| **id_ref != player_id)
            .filter_map(|id_ref| PLAYER_MANAGER.get(*id_ref))
            .collect();

        for other in others.into_iter() {
            let _ = crate::services::ServiceHandles::send_player_info(&receiver, &other);

            if other.is_die() {
                let death_msg = crate::services::ServiceHandles::build_player_death_message(&other);
                let _ = receiver.send_to_client(death_msg);
            }
        }
        Ok(())
    }

    pub fn load_player_to_zone(
        &self,
        mut player: Player,
        session: &crate::network::session::SessionArc,
    ) -> anyhow::Result<()> {
        player.zone_id = self.zone_id;
        player.map_id = self.map_id;
        self.add_player(player.clone())?;
        self.load_another_to_me(player.id)?;
        self.load_me_to_another(player.id)?;
        self.map_info(session, player.id)?;
        Ok(())
    }

    pub fn map_info(&self, session: &SessionArc, player_id: u64) -> anyhow::Result<()> {
        let Some(player) = PLAYER_MANAGER.get(player_id) else {
            return Ok(());
        };
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

        let mut msg = Message::new(-24);
        msg.write_byte((self.map_id as u8) as i8)?;
        msg.write_byte(planet_id)?;
        msg.write_byte(tile_id)?;
        msg.write_byte(bg_id)?;
        msg.write_byte(map_type)?;
        msg.write_utf(&map_name)?;
        msg.write_byte(self.zone_id as i8)?;
        msg.write_short(player.location.x)?;
        msg.write_short(player.location.y)?;

        // Waypoints
        let wp_count: i8 = {
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
                count
            } else {
                let _ = msg.write_byte(0)?;
                0
            }
        };
        let _ = wp_count;
        {
            let mobs_guard = self.active_mobs.read().unwrap();
            let mob_count: i8 = (mobs_guard.len().min(127)) as i8;
            msg.write_byte(mob_count)?;
            for mob in mobs_guard.iter().take(mob_count as usize) {
                msg.write_boolean(false)?; // is disable
                msg.write_boolean(false)?; // is dont move
                msg.write_boolean(false)?; // is fire
                msg.write_boolean(false)?; // is ice
                msg.write_boolean(false)?; // is wind

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
        }
        let _ = msg.write_byte(0)?;
        {
            let (npcs_for_map, avatar_lookup) = {
                let npcs = if let Some(map) =
                    crate::map::map_manager::MAP_MANAGER.find_by_id(self.map_id)
                {
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
            let _ = msg.write_byte(count)?;
            for npc in npcs_for_map.into_iter().take(count as usize) {
                let status: i8 = 1;
                let avatar: i16 = avatar_lookup.get(&npc.temp_id).cloned().unwrap_or(0) as i16;
                msg.write_byte(status)?;
                msg.write_short(npc.x)?;
                msg.write_short(npc.y)?;
                msg.write_byte(npc.temp_id as i8)?;
                msg.write_short(avatar)?;
            }
        }
        let _ = msg.write_byte(0)?;
        {
            let bg_item_path = format!("data/arc/map/item_bg_map_data/{}", self.map_id);
            match std::fs::read(&bg_item_path) {
                Ok(data) => {
                    let _ = msg.write(&data)?;
                }
                Err(_) => {
                    msg.write_short(0)?;
                }
            }
        }

        {
            let eff_item_path = format!("data/arc/map/eff_map/{}", self.map_id);
            match std::fs::read(&eff_item_path) {
                Ok(data) => {
                    let _ = msg.write(&data)?;
                }
                Err(_) => {
                    msg.write_short(0)?;
                }
            }
        }

        msg.write_byte(bg_type)?;
        msg.write_byte(0)?;
        msg.write_byte(if self.map_id == 148 { 1 } else { 0 })?;

        let _ = session.transmit(msg);
        Ok(())
    }
}

#[derive(Debug, Clone)]
pub struct ZoneInfo {
    pub map_id: i32,
    pub zone_id: i32,
    pub max_player: i32,
    pub current_players: i32,
    pub mob_count: i32,
    pub item_count: i32,
}

impl Clone for Zone {
    fn clone(&self) -> Self {
        Self {
            map_id: self.map_id,
            zone_id: self.zone_id,
            max_player: self.max_player,
            player_ids: Arc::clone(&self.player_ids),
            active_mobs: Arc::clone(&self.active_mobs),
            active_items: Arc::clone(&self.active_items),
        }
    }
}
