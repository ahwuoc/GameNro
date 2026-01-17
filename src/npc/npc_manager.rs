use crate::npc::RtNpc;
use std::collections::HashMap;

pub struct NpcManager {
    npcs: HashMap<i32, RtNpc>,
    npcs_by_map: HashMap<i32, Vec<i32>>,
}

impl NpcManager {
    pub fn new() -> Self {
        Self {
            npcs: HashMap::new(),
            npcs_by_map: HashMap::new(),
        }
    }
    pub fn add_npc(&mut self, npc: RtNpc) {
        let npc_id = npc.temp_id;
        self.npcs.insert(npc_id, npc.clone());

        let map_npcs = self.npcs_by_map.entry(npc.map_id).or_default();
        map_npcs.push(npc_id);
    }
    pub fn get_npc(&self, npc_id: i32) -> Option<&RtNpc> {
        self.npcs.get(&npc_id)
    }
    pub fn get_npc_by_id_and_map(&self, npc_id: i32, map_id: i32) -> Option<&RtNpc> {
        self.npcs
            .values()
            .find(|npc| npc.temp_id == npc_id && npc.map_id == map_id)
    }
    pub fn get_npcs_by_map(&self, map_id: i32) -> Vec<&RtNpc> {
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
    pub fn remove_npc(&mut self, npc_id: i32) -> bool {
        if let Some(npc) = self.npcs.remove(&npc_id) {
            if let Some(map_npcs) = self.npcs_by_map.get_mut(&npc.map_id) {
                map_npcs.retain(|&id| id != npc_id);
            }
            true
        } else {
            false
        }
    }
    pub fn update_all_npcs(&mut self) {
        for npc in self.npcs.values_mut() {
            npc.update();
        }
    }
    pub fn get_npc_count(&self) -> usize {
        self.npcs.len()
    }
    pub fn clear_all_npcs(&mut self) {
        self.npcs.clear();
        self.npcs_by_map.clear();
    }
}
