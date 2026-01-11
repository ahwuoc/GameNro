use crate::entities::item_template::Model as ItemMap;
use crate::map::map_manager;
use crate::mob::RtMob;
use crate::network::message::Message;
use crate::network::session::{self, AsyncSession};
use crate::player::player::Player;
use anyhow::Result;
use std::collections::HashMap;
use std::sync::Arc;
use tokio::sync::RwLock;
pub struct Zone {
    pub map_id: i32,
    pub zone_id: i32,
    pub max_player: i32,

    pub players: Arc<RwLock<HashMap<u64, Player>>>,
    pub mobs: Arc<RwLock<Vec<RtMob>>>,
    pub items: Arc<RwLock<Vec<ItemMap>>>,
}

impl Zone {
    pub fn new(map_id: i32, zone_id: i32, max_player: i32) -> Self {
        Self {
            map_id,
            zone_id,
            max_player,
            players: Arc::new(RwLock::new(HashMap::new())),
            mobs: Arc::new(RwLock::new(Vec::new())),
            items: Arc::new(RwLock::new(Vec::new())),
        }
    }

    pub async fn is_empty(&self) -> bool {
        let players = self.players.read().await;
        players.is_empty()
    }

    pub async fn is_full(&self) -> bool {
        let players = self.players.read().await;
        players.len() >= self.max_player as usize
    }

    pub async fn get_num_players(&self) -> usize {
        let players = self.players.read().await;
        players.len()
    }

    pub async fn add_player(&self, player: Player) -> anyhow::Result<()> {
        let mut players = self.players.write().await;
        if players.len() >= self.max_player as usize {
            return Err(anyhow::anyhow!("Zone is full"));
        }
        let player_id = player.id;
        players.insert(player_id, player);
        Ok(())
    }

    pub async fn remove_player(&self, player_id: u64) -> anyhow::Result<()> {
        let mut players = self.players.write().await;

        if players.remove(&player_id).is_some() {
            // Player removed successfully
        }

        Ok(())
    }

    pub async fn get_player(&self, player_id: u64) -> Option<Player> {
        let players = self.players.read().await;
        players.get(&player_id).cloned()
    }

    pub async fn get_all_players(&self) -> Vec<Player> {
        let players = self.players.read().await;
        players.values().cloned().collect()
    }

    pub async fn add_mob(&self, mob: RtMob) -> anyhow::Result<()> {
        let mut mobs = self.mobs.write().await;
        mobs.push(mob);
        Ok(())
    }

    pub async fn remove_mob(&self, mob_id: u64) -> anyhow::Result<()> {
        let mut mobs = self.mobs.write().await;
        mobs.retain(|mob| mob.id != mob_id);
        Ok(())
    }

    pub async fn get_all_mobs(&self) -> Vec<RtMob> {
        let mobs = self.mobs.read().await;
        mobs.clone()
    }

    pub async fn add_item(&self, item: ItemMap) -> anyhow::Result<()> {
        let mut items = self.items.write().await;
        items.push(item);
        Ok(())
    }

    pub async fn remove_item(&self, item_id: i16) -> anyhow::Result<()> {
        let mut items = self.items.write().await;
        items.retain(|item| item.id != item_id);
        Ok(())
    }

    pub async fn get_all_items(&self) -> Vec<ItemMap> {
        let items = self.items.read().await;
        items.clone()
    }

    pub async fn update(&self) -> anyhow::Result<()> {
        let mut mobs = self.mobs.write().await;
        for mob in mobs.iter_mut() {
            // TODO: Implement mob update logic
            // mob.update();
        }

        // Update items
        let mut items = self.items.write().await;
        for item in items.iter_mut() {
            // TODO: Implement item update logic
            // item.update();
        }

        Ok(())
    }
    pub async fn get_zone_info(&self) -> ZoneInfo {
        let players = self.players.read().await;
        let mobs = self.mobs.read().await;
        let items = self.items.read().await;

        ZoneInfo {
            map_id: self.map_id,
            zone_id: self.zone_id,
            max_player: self.max_player,
            current_players: players.len() as i32,
            mob_count: mobs.len() as i32,
            item_count: items.len() as i32,
        }
    }

    pub async fn send_message_to_all_players(&self, msg: Message) -> anyhow::Result<()> {
        let players = self.players.read().await;
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
        let players = self.players.read().await;
        for pl in players.values() {
            let _ = pl.send_message(msg.clone()).await;
        }
        Ok(())
    }

    pub async fn send_message_to_other_players(
        &self,
        except_player_id: u64,
        msg: Message,
    ) -> anyhow::Result<()> {
        let players = self.players.read().await;
        for (player_id, player) in players.iter() {
            if *player_id != except_player_id {
                let _ = player.send_message(msg.clone()).await;
            }
        }
        Ok(())
    }

    pub async fn load_me_to_another(&self, player_id: u64) -> anyhow::Result<()> {
        let players_guard = self.players.read().await;
        if !players_guard.contains_key(&player_id) {
            return Ok(());
        }
        let target_and_receivers: Vec<u64> = players_guard
            .iter()
            .filter_map(|(other_id, _)| {
                if *other_id != player_id {
                    Some(*other_id)
                } else {
                    None
                }
            })
            .collect();
        let target_player = players_guard.get(&player_id).cloned();
        drop(players_guard);

        if let Some(info_player) = target_player {
            for receiver_id in target_and_receivers {
                if let Some(receiver) = self.get_player(receiver_id).await {
                    let _ = Self::send_player_info(&receiver, &info_player).await;

                    if info_player.is_die() {
                        let mut death_msg = Self::build_player_death_message(&info_player);
                        let _ = receiver.send_message(death_msg);
                    }
                }
            }
        }
        Ok(())
    }

    // gui tat ca player cho toi
    pub async fn load_another_to_me(&self, player_id: u64) -> anyhow::Result<()> {
        let players_guard = self.players.read().await;
        let Some(receiver) = players_guard.get(&player_id).cloned() else {
            return Ok(());
        };
        let others: Vec<Player> = players_guard
            .iter()
            .filter_map(|(other_id, pl)| {
                if *other_id != player_id {
                    Some(pl.clone())
                } else {
                    None
                }
            })
            .collect();
        drop(players_guard);

        for other in others.into_iter() {
            let _ = Self::send_player_info(&receiver, &other).await;

            if other.is_die() {
                let mut death_msg = Self::build_player_death_message(&other);
                let _ = receiver.send_message(death_msg);
            }
        }
        Ok(())
    }

    /// Load a player into this zone and send map info
    pub async fn load_player_to_zone(
        &self,
        mut player: Player,
        session: &mut crate::network::session::AsyncSession,
    ) -> anyhow::Result<()> {
        // Set zone for player
        player.set_zone(self.clone());

        // Add player to zone
        self.add_player(player.clone()).await?;

        // Send map info to player
        self.map_info(session, player.id).await?;

        // Load other players to this player
        self.load_another_to_me(player.id).await?;

        // Load this player to others
        self.load_me_to_another(player.id).await?;

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

    pub async fn map_info(&self, session: &mut AsyncSession, player_id: u64) -> anyhow::Result<()> {
        let players = self.players.read().await;
        let Some(player) = players.get(&player_id) else {
            return Ok(());
        };

        let (planet_id, tile_id, bg_id, bg_type, map_type, map_name) = {
            if let Some(map) = map_manager::get_map(self.map_id) {
                (
                    map.planet_id as i8,
                    map.tile_id as i8,
                    map.bg_id as i8,
                    map.bg_type as i8,
                    map.r#type as i8,
                    map.map_name.clone(),
                )
            } else {
                (0i8, 0i8, 0i8, 0i8, 0i8, format!("Map {}", self.map_id))
            }
        };

        let mut msg = Message::new(-24);
        // Map meta
        msg.write_byte((self.map_id as u8) as i8); // mapId
        msg.write_byte(planet_id); // planetId
        msg.write_byte(tile_id); // tileId
        msg.write_byte(bg_id); // bgId
        msg.write_byte(map_type); // type
        msg.write_utf(&map_name); // mapName
        msg.write_byte((self.zone_id as u8) as i8); // zoneId
                                                    // Player position
        msg.write_short(player.location.x);
        msg.write_short(player.location.y);

        // Waypoints
        let wp_count: i8 = {
            if let Some(map) = map_manager::get_map(self.map_id) {
                let wps = map.way_points.read().await;
                let count = (wps.len().min(127)) as i8;
                msg.write_byte(count);
                for wp in wps.iter().take(count as usize) {
                    msg.write_short(wp.min_x);
                    msg.write_short(wp.min_y);
                    msg.write_short(wp.max_x);
                    msg.write_short(wp.max_y);
                    msg.write_boolean(wp.is_enter);
                    msg.write_boolean(wp.is_offline)?;
                    msg.write_utf(&wp.name)?;
                }
                count
            } else {
                msg.write_byte(0);
                0
            }
        };
        let _ = wp_count;
        // Load Mobs
        {
            let mobs_guard = self.mobs.read().await;
            let mob_count: i8 = (mobs_guard.len().min(127)) as i8;
            println!("DEBUG: Sending {} mobs (map_id: {}) | first mob temp_id: {:?}", mob_count, self.map_id, mobs_guard.first().map(|m| m.template_id));
            msg.write_byte(mob_count)?;
            for mob in mobs_guard.iter().take(mob_count as usize) {
                msg.write_boolean(false)?; // is disable
                msg.write_boolean(false)?; // is dont move
                msg.write_boolean(false)?; // is fire
                msg.write_boolean(false)?; // is ice
                msg.write_boolean(false)?; // is wind

                msg.write_byte((mob.template_id as u8) as i8)?;
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
        msg.write_byte(0);
        {
            let (npcs_for_map, avatar_lookup) = {
                let mgr = crate::services::Manager::get_instance();
                let guard = mgr.lock().unwrap();
                let npcs = guard
                    .map_npcs
                    .get(&self.map_id)
                    .cloned()
                    .unwrap_or_default();
                let avatars: std::collections::HashMap<i32, i32> = guard
                    .get_npc_templates()
                    .iter()
                    .map(|t| (t.id, t.avatar.unwrap_or(0)))
                    .collect();
                (npcs, avatars)
            };
            let count: i8 = (npcs_for_map.len().min(127)) as i8;
            msg.write_byte(count);
            for (id, x, y) in npcs_for_map.into_iter().take(count as usize) {
                let status: i8 = 1; // default active
                let avatar: i16 = avatar_lookup.get(&id).cloned().unwrap_or(0) as i16;
                msg.write_byte(status); // status
                msg.write_short(x); // cx
                msg.write_short(y); // cy
                msg.write_byte(id as i8); // tempId
                msg.write_short(avatar); // avatar
            }
        }

        // Items
        msg.write_byte(0); // items count

        // bgItem - read from file data/girlkun/map/item_bg_map_data/{mapId}
        {
            let bg_item_path = format!("data/girlkun/map/item_bg_map_data/{}", self.map_id);
            match std::fs::read(&bg_item_path) {
                Ok(data) => {
                    let _ = msg.write(&data);
                }
                Err(_) => {
                    // If file doesn't exist, write short 0
                    msg.write_short(0)?;
                }
            }
        }
        
        // effItem - read from file data/girlkun/map/eff_map/{mapId}
        {
            let eff_item_path = format!("data/girlkun/map/eff_map/{}", self.map_id);
            match std::fs::read(&eff_item_path) {
                Ok(data) => {
                    let _ = msg.write(&data);
                }
                Err(_) => {
                    // If file doesn't exist, write short 0
                    msg.write_short(0)?;
                }
            }
        }

        // Trailer bytes
        msg.write_byte(bg_type); // bgType from map template
        msg.write_byte(0); // idSpaceShip (0 for now) - TODO: should be player.idMark.idSpaceShip
        msg.write_byte(if self.map_id == 148 { 1 } else { 0 }); // special flag for map 148

        drop(players);
        
        println!("[MAP_INFO] Sending map_info for map {} zone {} to player {}, message size: {} bytes", 
            self.map_id, self.zone_id, player_id, msg.get_data().len());
        
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
            mobs: Arc::clone(&self.mobs),
            items: Arc::clone(&self.items),
        }
    }
}
