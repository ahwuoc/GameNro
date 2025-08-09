use crate::{entities::mob_template::Model as MobTemplate, utils::location};
use chrono::{DateTime, Utc};

#[derive(Debug, Clone)]
pub struct Mob {
    pub id: i32,
    pub temp_id: i32,
    pub name: String,
    pub level: i32,
    pub hp: i64,
    pub max_hp: i64,
    pub damage: i64,
    pub defense: i64,
    pub exp: i64,
    pub map_id: i32,
    pub zone_id: i32,
    pub location: location::Location,
    pub status: i32,
    pub r#type: i32,
    pub p_dame: i32,
    pub p_tiem_nang: i32,
    pub last_time_die: i64,
    pub lv_mob: i32,
    pub create_time: DateTime<Utc>,
    pub update_time: DateTime<Utc>,
}

impl Mob {
    pub fn from_template(template: &MobTemplate, id: i32, map_id: i32, zone_id: i32, x: i32, y: i32) -> Self {
        let current_time = Utc::now();
        
        Self {
            id,
            temp_id: template.id,
            name: template.name.clone(),
            level: 1, // Default level
            hp: template.hp as i64,
            max_hp: template.hp as i64,
            damage: template.percent_dame as i64,
            defense: 0, // No defense field in template
            exp: 10, // Default exp
            map_id,
            zone_id,
            location: {
                let mut loc = location::Location::new();
                loc.set_position(x as i16, y as i16);
                loc
            },
            status: 5, // Alive
            r#type: template.r#type,
            p_dame: template.percent_dame as i32,
            p_tiem_nang: template.percent_tiem_nang as i32,
            last_time_die: 0,
            lv_mob: 0,
            create_time: current_time,
            update_time: current_time,
        }
    }

    /// Check if mob is dead
    pub fn is_die(&self) -> bool {
        self.hp <= 0
    }

    /// Get current HP percentage
    pub fn get_hp_percentage(&self) -> f64 {
        if self.max_hp > 0 {
            (self.hp as f64 / self.max_hp as f64) * 100.0
        } else {
            0.0
        }
    }

    /// Take damage
    pub fn take_damage(&mut self, damage: i64) {
        self.hp = (self.hp - damage).max(0);
    }

    /// Heal mob
    pub fn heal(&mut self, amount: i64) {
        self.hp = (self.hp + amount).min(self.max_hp);
    }

    /// Set mob as dead
    pub fn set_die(&mut self) {
        self.hp = 0;
        self.status = 0;
        self.last_time_die = Utc::now().timestamp_millis();
    }

    /// Respawn mob
    pub fn respawn(&mut self) {
        self.hp = self.max_hp;
        self.status = 5; // Alive status
    }

    /// Get attack damage with random variation
    pub fn get_attack_damage(&self) -> i64 {
        if self.damage > 0 {
            // Add some random variation to damage (±10%)
            let variation = (self.damage as f64 * 0.1) as i64;
            let random_factor = (std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .unwrap()
                .as_millis() % 100) as f64 / 100.0;
            let random_variation = (random_factor * variation as f64 * 2.0) as i64 - variation;
            (self.damage + random_variation).max(1)
        } else {
            (self.max_hp * self.p_dame as i64 / 100) + (self.level as i64 * 10)
        }
    }
    pub fn can_attack(&self) -> bool {
        !self.is_die() && self.status > 0
    }

    pub fn get_level(&self) -> i32 {
        self.level
    }

    /// Get mob template ID
    pub fn get_temp_id(&self) -> i32 {
        self.temp_id
    }

    /// Get mob position
    pub fn get_position(&self) -> (i16, i16) {
        self.location.get_position()
    }

    /// Set mob position
    pub fn set_position(&mut self, x: i32, y: i32) {
        self.location.set_position(x as i16, y as i16);
    }

    /// Get mob status
    pub fn get_status(&self) -> i32 {
        self.status
    }

    /// Set mob status
    pub fn set_status(&mut self, status: i32) {
        self.status = status;
    }

    /// Get mob type
    pub fn get_type(&self) -> i32 {
        self.r#type
    }

    /// Set mob type
    pub fn set_type(&mut self, mob_type: i32) {
        self.r#type = mob_type;
    }

    /// Check if mob is a boss
    pub fn is_boss(&self) -> bool {
        // Define boss mob template IDs
        let boss_ids = vec![
            1,   // HIRUDEGARN
            2,   // VUA_BACH_TUOC  
            3,   // ROBOT_BAO_VE
            4,   // GAU_TUONG_CUOP
            // Add more boss IDs as needed
        ];
        boss_ids.contains(&self.temp_id)
    }

    /// Check if mob is a big boss
    pub fn is_big_boss(&self) -> bool {
        // Define big boss mob template IDs
        let big_boss_ids = vec![
            1,   // HIRUDEGARN
            2,   // VUA_BACH_TUOC
            3,   // ROBOT_BAO_VE
            4,   // GAU_TUONG_CUOP
            // Add more big boss IDs as needed
        ];
        big_boss_ids.contains(&self.temp_id)
    }

    /// Get mob reward experience
    pub fn get_exp_reward(&self) -> i64 {
        self.exp
    }

    /// Get mob reward gold (calculated based on level and type)
    pub fn get_gold_reward(&self) -> i64 {
        let base_gold = self.level as i64 * 10;
        if self.is_boss() {
            base_gold * 5
        } else {
            base_gold
        }
    }

    pub fn update(&mut self) {
        if self.is_die() {
            let current_time = Utc::now().timestamp_millis();
            let respawn_time = 30000; 
            
            if current_time - self.last_time_die > respawn_time {
                self.respawn();
            }
        }
        
        self.update_time = Utc::now();
    }

    pub fn new(
        id: i32,
        temp_id: i32,
        name: String,
        level: i32,
        max_hp: i64,
        damage: i64,
        map_id: i32,
        zone_id: i32,
        x: i32,
        y: i32,
    ) -> Self {
        let current_time = Utc::now();
        
        Self {
            id,
            temp_id,
            name,
            level,
            hp: max_hp,
            max_hp,
            damage,
            defense: 0,
            exp: level as i64 * 10,
            map_id,
            zone_id,
            location: {
                let mut loc = location::Location::new();
                loc.set_position(x as i16, y as i16);
                loc
            },
            status: 5, // Alive
            r#type: 1, // Normal mob
            p_dame: 10, // 10% damage
            p_tiem_nang: 5, // 5% potential
            last_time_die: 0,
            lv_mob: 0,
            create_time: current_time,
            update_time: current_time,
        }
    }
}
