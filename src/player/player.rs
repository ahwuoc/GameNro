use crate::network::async_net::message::Message;
use crate::player::n_point::NPoint;
use crate::player::inventory::Inventory;
use crate::models::IntrinsicPlayer;
use crate::utils::Location;
use crate::entities;
use serde_json::Value;

use std::time::SystemTime;

#[derive(Debug, Clone)]
pub struct Player {
    // Basic info
    pub id: u64,
    pub name: String,
    pub gender: u8,
    pub head: i16,
    pub session_id: Option<String>,
    
    pub n_point: NPoint,
    pub inventory: Inventory,
    pub intrinsic: IntrinsicPlayer,
    pub location: Location,
    
    // Status
    pub is_die: bool,
    pub is_new_member: bool,
    pub before_dispose: bool,
    
    // Training
    pub is_train: bool,
    pub type_train: u8,
    pub time_off: u64,
    
    // PK system
    pub type_pk: u8,
    
    // Zone/Map
    pub zone_id: u32,
    pub map_id: u32,
    pub last_time_use_option: u64,
    pub last_time_revived: u64,
    
    // Flags
    pub just_revived: bool,
    pub is_fight: bool,
    pub is_fight1: bool,
    pub is_try: bool,
    pub is_try1: bool,
    
    // Admin
    pub is_admin: bool,
    pub admin_key: bool,
    
    // Notifications
    pub notify: Option<String>,


}

impl Player {
    pub fn new(id: u64, name: String, gender: u8) -> Self {
        let current_time = SystemTime::now()
            .duration_since(SystemTime::UNIX_EPOCH)
            .unwrap()
            .as_millis() as u64;
            
        Player {
            id,
            name,
            gender,
            head: 0,
            session_id: None,
            n_point: NPoint::new(),
            inventory: Inventory::new(),
            intrinsic: IntrinsicPlayer::new(),
            location: Location::new(),
            is_die: false,
            is_new_member: true,
            before_dispose: false,
            is_train: false,
            type_train: 0,
            time_off: 0,
            type_pk: 0,
            zone_id: 0,
            map_id: 0,
            last_time_use_option: current_time,
            last_time_revived: 0,
            just_revived: false,
            is_fight: false,
            is_fight1: false,
            is_try: false,
            is_try1: false,
            is_admin: false,
            admin_key: false,
            notify: None,
        }
    }
    
    // Core methods
    pub fn is_die(&self) -> bool {
        self.is_die || self.n_point.hp <= 0
    }

    /// Build runtime Player from DB entity
    pub fn from_entity(model: &entities::player::Model) -> Result<Self, String> {
        let mut p = Player::new(model.id as u64, model.name.clone(), model.gender as u8);
        p.head = model.head as i16;

        // inventory
        p.inventory = Self::parse_inventory_json(&model.data_inventory)?;

        // location: expect [map_id, x, y]
        if let Ok((map_id, x, y)) = Self::parse_location_array(&model.data_location) {
            p.map_id = map_id as u32;
            p.location.set_map(p.map_id, 0);
            p.location.set_position(p.location.x, p.location.y);
        }


        Ok(p)
    }

    fn parse_inventory_json(s: &str) -> Result<Inventory, String> {
        if s.is_empty() { return Ok(Inventory::new()); }
        let v: Value = serde_json::from_str(s).map_err(|e| e.to_string())?;
        let mut inv = Inventory::new();
        if let Some(obj) = v.as_object() {
            if let Some(gold) = obj.get("gold").and_then(|x| x.as_i64()) { inv.gold = gold; }
            if let Some(gem) = obj.get("gem").and_then(|x| x.as_i64()) { inv.gem = gem as i32; }
            if let Some(ruby) = obj.get("ruby").and_then(|x| x.as_i64()) { inv.ruby = ruby as i32; }
        }
        Ok(inv)
    }

    fn parse_location_array(s: &str) -> Result<(i64, i64, i64), String> {
        if s.is_empty() { return Err("empty location".into()); }
        let v: Value = serde_json::from_str(s).map_err(|e| e.to_string())?;
        let arr = v.as_array().ok_or("location not array")?;
        let map_id = arr.get(0).and_then(|x| x.as_i64()).ok_or("no map id")?;
        let x = arr.get(1).and_then(|x| x.as_i64()).ok_or("no x")?;
        let y = arr.get(2).and_then(|x| x.as_i64()).ok_or("no y")?;
        Ok((map_id, x, y))
    }
    
    pub fn set_session_id(&mut self, session_id: String) {
        self.session_id = Some(session_id);
    }
    
    pub fn get_session_id(&self) -> Option<String> {
        self.session_id.clone()
    }
    
    pub fn send_message(&self, _msg: Message) -> Result<(), std::io::Error> {
        // TODO: Implement message sending through session manager
        // For now, just return Ok
        Ok(())
    }
    
    pub fn is_pl(&self) -> bool {
        !self.is_die && self.session_id.is_some()
    }
    
    pub fn update(&mut self) {
        if !self.before_dispose {
            // Update NPoint
            self.n_point.update();
            
            // Update location
            self.location.update();
            
            // Check if player is dead
            if self.n_point.hp <= 0 && !self.is_die {
                self.is_die = true;
            }
        }
    }
    
    // Combat methods
    pub fn injured(&mut self, damage: u64, piercing: bool) -> u64 {
        if self.is_die {
            return 0;
        }
        
        let actual_damage = if piercing {
            damage
        } else {
            // TODO: Calculate with defense
            damage
        };
        
        if actual_damage >= self.n_point.hp {
            self.n_point.hp = 0;
            self.is_die = true;
        } else {
            self.n_point.hp -= actual_damage;
        }
        
        actual_damage
    }
    
    pub fn set_die(&mut self) {
        self.is_die = true;
        self.n_point.hp = 0;
        self.n_point.mp = 0;
    }
    
    pub fn revive(&mut self) {
        self.is_die = false;
        self.n_point.hp = self.n_point.hp_max;
        self.n_point.mp = self.n_point.mp_max;
        self.just_revived = true;
        self.last_time_revived = SystemTime::now()
            .duration_since(SystemTime::UNIX_EPOCH)
            .unwrap()
            .as_millis() as u64;
    }
    
    // Position methods
    pub fn set_position(&mut self, x: i16, y: i16) {
        self.location.x = x;
        self.location.y = y;
    }
    
    pub fn get_position(&self) -> (i16, i16) {
        (self.location.x, self.location.y)
    }
    
    // Chat method
    pub fn chat(&self, text: &str) {
        println!("[{}]: {}", self.name, text);
        // TODO: Send chat message to other players in zone
    }
    
    // Admin methods
    pub fn is_admin(&self) -> bool {
        self.is_admin
    }
    
    pub fn admin_key(&self) -> bool {
        self.admin_key
    }
    
    // Disposal
    pub fn prepared_to_dispose(&mut self) {
        self.before_dispose = true;
    }
    
    pub fn dispose(&mut self) {
        self.before_dispose = true;
        self.session_id = None;
        println!("Player {} disposed", self.name);
    }
    
    // Fight methods
    pub fn set_fight(&mut self, _type_fight: u8, _type_target: u8) {
        self.is_fight = true;
        // TODO: Implement fight logic
    }
    
    pub fn reset_fight(&mut self) {
        self.is_fight = false;
        self.is_fight1 = false;
        self.is_try = false;
        self.is_try1 = false;
    }
    
    // Training methods
    pub fn start_training(&mut self, type_train: u8) {
        self.is_train = true;
        self.type_train = type_train;
        self.time_off = 0;
    }
    
    pub fn stop_training(&mut self) {
        self.is_train = false;
        self.type_train = 0;
        self.time_off = 0;
    }
    
    // Notification
    pub fn set_notify(&mut self, notify: String) {
        self.notify = Some(notify);
    }
    
    pub fn clear_notify(&mut self) {
        self.notify = None;
    }
}
