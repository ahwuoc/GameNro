#![allow(dead_code)]
use crate::entities::item_template::Model as ItemMap;
use crate::map::map_manager;
use crate::mob::RtMob;
use crate::network::message::Message;
use crate::network::session::{AsyncSession, SessionArc};
use crate::player::player::Player;
use anyhow::Result;
use dashmap::DashMap;
use std::sync::Arc;
use std::time::{SystemTime, UNIX_EPOCH};
use tokio::sync::RwLock;

pub struct Zone {
    pub map_id: i32,
    pub zone_id: i32,
    pub max_player: i32,

    pub players: Arc<DashMap<u64, Player>>,
    pub active_mobs: Arc<RwLock<Vec<RtMob>>>,
    pub active_items: Arc<RwLock<Vec<ItemMap>>>,
}

impl Zone {
    pub fn new(map_id: i32, zone_id: i32, max_player: i32) -> Self {
        Self {
            map_id,
            zone_id,
            max_player,
            players: Arc::new(DashMap::new()),
            active_mobs: Arc::new(RwLock::new(Vec::new())),
            active_items: Arc::new(RwLock::new(Vec::new())),
        }
    }

    pub async fn is_empty(&self) -> bool {
        self.players.is_empty()
    }

    pub async fn is_full(&self) -> bool {
        self.players.len() >= self.max_player as usize
    }

    pub async fn get_num_players(&self) -> usize {
        self.players.len()
    }

    pub async fn add_player(&self, player: Player) -> anyhow::Result<()> {
        if self.players.len() >= self.max_player as usize {
            return Err(anyhow::anyhow!("Zone is full"));
        }
        let player_id = player.id;
        self.players.insert(player_id, player);
        Ok(())
    }

    pub async fn remove_player(&self, player_id: u64) -> anyhow::Result<()> {
        self.players.remove(&player_id);
        Ok(())
    }

    pub async fn get_player(&self, player_id: u64) -> Option<Player> {
        self.players.get(&player_id).map(|p| p.clone())
    }

    pub async fn get_all_players(&self) -> Vec<Player> {
        self.players.iter().map(|p| p.value().clone()).collect()
    }

    pub async fn add_mob(&self, mob: RtMob) -> anyhow::Result<()> {
        let mut mobs = self.active_mobs.write().await;
        mobs.push(mob);
        Ok(())
    }

    pub async fn remove_mob(&self, mob_id: u64) -> anyhow::Result<()> {
        let mut mobs = self.active_mobs.write().await;
        mobs.retain(|mob| mob.id != mob_id);
        Ok(())
    }

    pub async fn get_all_mobs(&self) -> Vec<RtMob> {
        let mobs = self.active_mobs.read().await;
        mobs.clone()
    }

    pub async fn add_item(&self, item: ItemMap) -> anyhow::Result<()> {
        let mut items = self.active_items.write().await;
        items.push(item);
        Ok(())
    }

    pub async fn remove_item(&self, item_id: i16) -> anyhow::Result<()> {
        let mut items = self.active_items.write().await;
        items.retain(|item| item.id != item_id);
        Ok(())
    }

    pub async fn get_all_items(&self) -> Vec<ItemMap> {
        let items = self.active_items.read().await;
        items.clone()
    }

    pub async fn update(&self) -> anyhow::Result<()> {
        crate::services::mob_service::update(self).await;

        let mut items = self.active_items.write().await;
        for _item in items.iter_mut() {
            // TODO: Implement item update logic
            // item.update();
        }

        Ok(())
    }
    pub async fn get_zone_info(&self) -> ZoneInfo {
        let mobs = self.active_mobs.read().await;
        let items = self.active_items.read().await;

        ZoneInfo {
            map_id: self.map_id,
            zone_id: self.zone_id,
            max_player: self.max_player,
            current_players: self.players.len() as i32,
            mob_count: mobs.len() as i32,
            item_count: items.len() as i32,
        }
    }

    pub async fn send_message_to_all_players(&self, msg: Message) -> anyhow::Result<()> {
        for player in self.players.iter() {
            player.send_message(msg.clone()).await;
        }
        Ok(())
    }

    pub async fn send_message_all_player_in_map(
        &self,
        player: &Player,
        msg: Message,
    ) -> anyhow::Result<()> {
        if player.zone.is_none() {
            return Ok(());
        }
        for pl in self.players.iter() {
            pl.send_message(msg.clone()).await;
        }
        Ok(())
    }

    pub async fn send_message_to_other_players(
        &self,
        except_player_id: u64,
        msg: Message,
    ) -> anyhow::Result<()> {
        for entry in self.players.iter() {
            if *entry.key() != except_player_id {
                entry.value().send_message(msg.clone()).await;
            }
        }
        Ok(())
    }

    pub async fn load_me_to_another(&self, player_id: u64) -> anyhow::Result<()> {
        if !self.players.contains_key(&player_id) {
            return Ok(());
        }
        let target_and_receivers: Vec<u64> = self
            .players
            .iter()
            .filter_map(|entry| {
                if *entry.key() != player_id {
                    Some(*entry.key())
                } else {
                    None
                }
            })
            .collect();
        let target_player = self.players.get(&player_id).map(|p| p.clone());

        if let Some(info_player) = target_player {
            for receiver_id in target_and_receivers {
                if let Some(receiver) = self.get_player(receiver_id).await {
                    let _ = Self::send_player_info(&receiver, &info_player).await;

                    if info_player.is_die() {
                        let death_msg = Self::build_player_death_message(&info_player);
                        receiver.send_message(death_msg);
                    }
                }
            }
        }
        Ok(())
    }

    pub async fn load_another_to_me(&self, player_id: u64) -> anyhow::Result<()> {
        let Some(receiver) = self.players.get(&player_id).map(|p| p.clone()) else {
            return Ok(());
        };
        let others: Vec<Player> = self
            .players
            .iter()
            .filter_map(|entry| {
                if *entry.key() != player_id {
                    Some(entry.value().clone())
                } else {
                    None
                }
            })
            .collect();

        for other in others.into_iter() {
            let _ = Self::send_player_info(&receiver, &other).await;

            if other.is_die() {
                let death_msg = Self::build_player_death_message(&other);
                let _ = receiver.send_message(death_msg);
            }
        }
        Ok(())
    }

    pub async fn load_player_to_zone(
        &self,
        mut player: Player,
        session: &crate::network::session::SessionArc,
    ) -> anyhow::Result<()> {
        player.set_zone(self.clone());
        self.add_player(player.clone()).await?;
        self.load_another_to_me(player.id).await?;
        self.load_me_to_another(player.id).await?;
        self.map_info(session, player.id).await?;
        Ok(())
    }

    pub async fn send_player_info(pl_receiver: &Player, pl_target: &Player) -> Result<()> {
        let mut msg = Message::new(-5);
        let mockup_level = 10;
        let _ = msg.write_int(pl_target.id as i32);
        let _ = msg.write_int(-1); // clan id (unknown)
        let _ = msg.write_byte(mockup_level);
        let _ = msg.write_bool(false);
        let _ = msg.write_byte(pl_target.type_pk);
        let _ = msg.write_byte(pl_target.gender);
        let _ = msg.write_byte(pl_target.gender);
        let _ = msg.write_short(pl_target.get_head());
        let _ = msg.write_utf(pl_target.get_name());
        let _ = msg.write_int(pl_target.n_point.hp);
        let _ = msg.write_int(pl_target.n_point.hp_max);
        let _ = msg.write_short(pl_target.get_body());
        let _ = msg.write_short(pl_target.get_leg());
        let _ = msg.write_byte(0); // flag bag
        let _ = msg.write_byte(-1); // unknown
        let _ = msg.write_short(pl_target.location.x);
        let _ = msg.write_short(pl_target.location.y);
        let _ = msg.write_short(0);
        let _ = msg.write_short(0);
        let _ = msg.write_byte(0);
        let _ = msg.write_byte(0); // spaceship id
        let _ = msg.write_byte(0); // is monkey
        let _ = msg.write_short(0); // mount
        let _ = msg.write_byte(0); // cFlag
        let _ = msg.write_byte(0);

        if pl_target.is_pl() {
            let _ = msg.write_short(0); // idAura
            let _ = msg.write_short(0); // aura
            let _ = msg.write_byte(0); // eff front
        }
        pl_receiver.send_message(msg).await?;
        Ok(())
    }

    fn build_player_death_message(pl_info: &Player) -> Message {
        let mut msg = Message::new(-8);
        let _ = msg.write_int(pl_info.id as i32);
        let _ = msg.write_byte(0);
        let _ = msg.write_short(pl_info.location.x);
        let _ = msg.write_short(pl_info.location.y);
        msg
    }

    pub async fn map_info(&self, session: &SessionArc, player_id: u64) -> anyhow::Result<()> {
        let Some(player) = self.players.get(&player_id) else {
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
            let mobs_guard = self.active_mobs.read().await;
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
                    crate::npc::npc_template_manager::get_all()
                        .iter()
                        .map(|t| (t.id, t.avatar.unwrap_or(0)))
                        .collect();
                (npcs, avatars)
            };
            let count: i8 = (npcs_for_map.len().min(127)) as i8;
            let _ = msg.write_byte(count)?;
            for npc in npcs_for_map.into_iter().take(count as usize) {
                let status: i8 = 1;
                let avatar: i16 = avatar_lookup.get(&npc.id).cloned().unwrap_or(0) as i16;
                msg.write_byte(status)?;
                msg.write_short(npc.x)?;
                msg.write_short(npc.y)?;
                msg.write_byte(npc.id as i8)?;
                msg.write_short(avatar)?;
            }
        }
        let _ = msg.write_byte(0)?;
        {
            let bg_item_path = format!("data/girlkun/map/item_bg_map_data/{}", self.map_id);
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
            let eff_item_path = format!("data/girlkun/map/eff_map/{}", self.map_id);
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

        println!(
            "[MAP_INFO] Sending map_info for map {} zone {} to player {}, message size: {} bytes",
            self.map_id,
            self.zone_id,
            player_id,
            msg.get_data().len()
        );

        session.send_message(&msg).await?;
        Ok(())
    }
}

/// Zone information for client
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
            players: Arc::clone(&self.players),
            active_mobs: Arc::clone(&self.active_mobs),
            active_items: Arc::clone(&self.active_items),
        }
    }
}
