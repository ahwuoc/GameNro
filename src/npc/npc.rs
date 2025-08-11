use std::collections::HashMap;
use chrono::{DateTime, Utc};
use crate::{entities::npc_template::Model as NpcTemplate, utils::Location};

#[derive(Debug, Clone)]
pub struct BaseMenu {
    pub npc_id: i32,
    pub npc_say: String,
    pub menu_select: Vec<String>,
}

impl BaseMenu {
    pub fn new(npc_id: i32, npc_say: String, menu_select: Vec<String>) -> Self {
        Self {
            npc_id,
            npc_say,
            menu_select,
        }
    }

    /// Get menu options count
    pub fn get_menu_count(&self) -> usize {
        self.menu_select.len()
    }

    pub fn get_menu_option(&self, index: usize) -> Option<&String> {
        self.menu_select.get(index)
    }
}

#[derive(Debug, Clone)]
pub struct Npc {
    pub map_id: i32,
    pub status: i32,
    pub location: Location,
    pub temp_id: i32,
    pub avatar: i32,
    pub base_menu: Option<BaseMenu>,
    pub create_time: DateTime<Utc>,
}

impl Npc {
    /// Create new NPC
    pub fn new(map_id: i32, status: i32, x: i32, y: i32, temp_id: i32, avatar: i32) -> Self {
        Self {
            map_id,
            status,
            location: {
                let mut loc = Location::new();
                loc.set_position(x as i16, y as i16);
                loc
            },
            temp_id,
            avatar,
            base_menu: None,
            create_time: Utc::now(),
        }
    }

    pub fn from_template(template: &NpcTemplate, map_id: i32, x: i32, y: i32) -> Self {
        Self {
            map_id,
            status: 1, 
            location: {
                let mut loc = Location::new();
                loc.set_position(x as i16, y as i16);
                loc
            },
            temp_id: template.id,
            avatar: template.avatar.unwrap_or(0),
            base_menu: None,
            create_time: Utc::now(),
        }
    }
    pub fn init_base_menu(&mut self, text: &str) {
        if text.is_empty() {
            return;
        }
        
        let text = &text[1..]; // Remove first character
        let parts: Vec<&str> = text.split('|').collect();
        
        if parts.is_empty() {
            return;
        }
        
        let npc_say = parts[0].replace("<>", "\n");
        let menu_select: Vec<String> = parts[1..]
            .iter()
            .map(|s| s.replace("<>", "\n"))
            .collect();
        
        self.base_menu = Some(BaseMenu::new(self.temp_id, npc_say, menu_select));
    }

    pub fn get_name(&self) -> String {
        format!("NPC_{}", self.temp_id)
    }

    pub fn get_position(&self) -> (i16, i16) {
        self.location.get_position()
    }

    pub fn set_position(&mut self, x: i32, y: i32) {
        self.location.set_position(x as i16, y as i16);
    }

    pub fn has_menu(&self) -> bool {
        self.base_menu.is_some()
    }

    pub fn get_base_menu(&self) -> Option<&BaseMenu> {
        self.base_menu.as_ref()
    }

    /// Check if NPC can be opened by player
    pub fn can_open_npc(&self, _player_id: i64) -> bool {
        // TODO: Implement player-specific checks
        true
    }

    /// Update NPC
    pub fn update(&mut self) {
        // Basic update logic
        // TODO: Implement NPC-specific update logic
    }

    /// Check if NPC is in range of player
    pub fn is_in_range(&self, player_x: i32, player_y: i32, range: i32) -> bool {
        let (nx, ny) = self.location.get_position();
        let dx = nx as i32 - player_x;
        let dy = ny as i32 - player_y;
        let distance = (((dx * dx) + (dy * dy)) as f64).sqrt();
        distance <= range as f64
    }
}

/// NpcManager manages all NPCs in the game
pub struct NpcManager {
    npcs: HashMap<i32, Npc>, // npc_id -> Npc
    npcs_by_map: HashMap<i32, Vec<i32>>, // map_id -> Vec<npc_id>
}

impl NpcManager {
    pub fn new() -> Self {
        Self {
            npcs: HashMap::new(),
            npcs_by_map: HashMap::new(),
        }
    }



    /// Add NPC to manager
    pub fn add_npc(&mut self, npc: Npc) {
        let npc_id = npc.temp_id;
        self.npcs.insert(npc_id, npc.clone());
        
        // Add to map index
        let map_npcs = self.npcs_by_map.entry(npc.map_id).or_insert_with(Vec::new);
        map_npcs.push(npc_id);
    }

    /// Get NPC by ID
    pub fn get_npc(&self, npc_id: i32) -> Option<&Npc> {
        self.npcs.get(&npc_id)
    }

    /// Get NPC by ID and map
    pub fn get_npc_by_id_and_map(&self, npc_id: i32, map_id: i32) -> Option<&Npc> {
        self.npcs.values().find(|npc| npc.temp_id == npc_id && npc.map_id == map_id)
    }

    /// Get NPCs by map
    pub fn get_npcs_by_map(&self, map_id: i32) -> Vec<&Npc> {
        let mut npcs = Vec::new();
        if let Some(npc_ids) = self.npcs_by_map.get(&map_id) {
            for npc_id in npc_ids {
                if let Some(npc) = self.npcs.get(npc_id) {
                    npcs.push(npc);
                }
            }
        }
        npcs
    }

    /// Remove NPC
    pub fn remove_npc(&mut self, npc_id: i32) -> bool {
        if let Some(npc) = self.npcs.remove(&npc_id) {
            // Remove from map index
            if let Some(map_npcs) = self.npcs_by_map.get_mut(&npc.map_id) {
                map_npcs.retain(|&id| id != npc_id);
            }
            true
        } else {
            false
        }
    }

    /// Update all NPCs
    pub fn update_all_npcs(&mut self) {
        for npc in self.npcs.values_mut() {
            npc.update();
        }
    }

    /// Get NPC count
    pub fn get_npc_count(&self) -> usize {
        self.npcs.len()
    }

    /// Clear all NPCs
    pub fn clear_all_npcs(&mut self) {
        self.npcs.clear();
        self.npcs_by_map.clear();
    }
}
