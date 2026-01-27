#![allow(dead_code)]
use crate::entities::item_template::Model as ItemMap;
use crate::map::map_manager;
use crate::mob::RtMob;
use crate::network::message::Message;
use crate::network::session::{AsyncSession, SessionArc};
use crate::player::player::Player;
use crate::player::player_manager::PLAYER_MANAGER;
use anyhow::Result;
use dashmap::DashMap;
use dashmap::DashSet;
use std::sync::Arc;
use std::sync::RwLock;
use std::time::{SystemTime, UNIX_EPOCH};

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
        // Note: We DO NOT remove from PLAYER_MANAGER here, as player might just be changing zones.
        // Removal from PLAYER_MANAGER happens on session disconnect.
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

    pub fn get_all_mobs(&self) -> Vec<RtMob> {
        let mobs = self.active_mobs.read().unwrap();
        mobs.clone()
    }

    pub fn add_item(&self, item: ItemMap) -> anyhow::Result<()> {
        let mut items = self.active_items.write().unwrap();
        items.push(item);
        Ok(())
    }

    pub fn remove_item(&self, item_id: i16) -> anyhow::Result<()> {
        let mut items = self.active_items.write().unwrap();
        items.retain(|item| item.id != item_id);
        Ok(())
    }

    pub fn get_all_items(&self) -> Vec<ItemMap> {
        let items = self.active_items.read().unwrap();
        items.clone()
    }

    pub fn update(&self) -> anyhow::Result<()> {
        crate::services::mob_service::update(self);

        let mut items = self.active_items.write().unwrap();
        for _item in items.iter_mut() {
            // TODO: Implement item update logic
            // item.update();
        }

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

    pub fn send_message_all_player_in_map(
        &self,
        player: &Player,
        msg: Message,
    ) -> anyhow::Result<()> {
        if player.zone_id == 0 && player.map_id == 0 {
            return Ok(());
        }
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
        // Load myself (player_id) to all other players in this zone
        if !self.player_ids.contains(&player_id) {
            return Ok(());
        }

        let target_player = PLAYER_MANAGER.get(player_id);

        // Get all receivers (other players in zone)
        let receivers: Vec<Player> = self
            .player_ids
            .iter()
            .filter(|id_ref| **id_ref != player_id)
            .filter_map(|id_ref| PLAYER_MANAGER.get(*id_ref))
            .collect();

        if let Some(info_player) = target_player {
            for receiver in receivers {
                let _ = Self::send_player_info(&receiver, &info_player);

                if info_player.is_die() {
                    let death_msg = Self::build_player_death_message(&info_player);
                    let _ = receiver.send_to_client(death_msg);
                }
            }
        }
        Ok(())
    }

    pub fn load_another_to_me(&self, player_id: u64) -> anyhow::Result<()> {
        // Load all other players in zone to myself (player_id)
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
            let _ = Self::send_player_info(&receiver, &other);

            if other.is_die() {
                let death_msg = Self::build_player_death_message(&other);
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

    pub fn send_player_info(pl_receiver: &Player, pl_target: &Player) -> Result<()> {
        let mut msg = Message::new(-5);

        let id = pl_target.id as i32;
        let clan_id = -1;
        let level = 10;
        let is_invis = false;
        let type_pk = pl_target.type_pk;
        let gender = pl_target.gender;
        let class = pl_target.gender;
        let head = pl_target.get_head();
        let name = pl_target.get_name();
        let hp = pl_target.n_point.hp;
        let max_hp = pl_target.n_point.hp_max;
        let body = pl_target.get_body();
        let leg = pl_target.get_leg();
        let bag = 0;
        let unknown_byte = -1;
        let x = pl_target.location.x;
        let y = pl_target.location.y;
        let eff_buff_1 = 0;
        let eff_buff_2 = 0;
        let eff_buff_3 = 0;
        let spaceship_id = 0;
        let is_monkey = 0;
        let mount_id = 0;
        let c_flag = 0;
        let none = 0;

        let _ = msg.write_int(id);
        let _ = msg.write_int(clan_id);
        let _ = msg.write_byte(level);
        let _ = msg.write_bool(is_invis);
        let _ = msg.write_byte(type_pk);
        let _ = msg.write_byte(gender);
        let _ = msg.write_byte(class);
        let _ = msg.write_short(head);
        let _ = msg.write_utf(name);
        let _ = msg.write_int(hp);
        let _ = msg.write_int(max_hp);
        let _ = msg.write_short(body);
        let _ = msg.write_short(leg);
        let _ = msg.write_byte(bag);
        let _ = msg.write_byte(unknown_byte);
        let _ = msg.write_short(x);
        let _ = msg.write_short(y);
        let _ = msg.write_short(eff_buff_1);
        let _ = msg.write_short(eff_buff_2);
        let _ = msg.write_byte(eff_buff_3);
        let _ = msg.write_byte(spaceship_id);
        let _ = msg.write_byte(is_monkey);
        let _ = msg.write_short(mount_id);
        let _ = msg.write_byte(c_flag);
        let _ = msg.write_byte(none);

        if pl_target.is_pl() {
            let id_aura = 0;
            let aura = 0;
            let eff_front = 0;

            let _ = msg.write_short(id_aura);
            let _ = msg.write_short(aura);
            let _ = msg.write_byte(eff_front);
        }
        pl_receiver.send_to_client(msg)?;
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

        println!(
            "[MAP_INFO] Sending map_info for map {} zone {} to player {}, message size: {} bytes",
            self.map_id,
            self.zone_id,
            player_id,
            msg.get_data().len()
        );

        let _ = session.transmit(msg);
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
            player_ids: Arc::clone(&self.player_ids),
            active_mobs: Arc::clone(&self.active_mobs),
            active_items: Arc::clone(&self.active_items),
        }
    }
}
