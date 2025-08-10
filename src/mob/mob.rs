use crate::entities::mob_template::Model as MobTemplate;
use crate::utils::location::Location;

#[derive(Debug, Clone)]
pub struct RtMob {
    pub id: u64,
    pub template_id: i32,
    pub name: String,
    pub level: i32,
    pub hp: i32,
    pub max_hp: i32,
    pub mp: i32,
    pub max_mp: i32,
    pub location: Location,
    pub map_id: u32,
    pub zone_id: u32,
    pub is_alive: bool,
    pub template: Option<MobTemplate>,
    pub status: i32,
    pub lv_mob: i32,
}

impl RtMob {
    pub fn new(id: u64, template_id: i32, name: String) -> Self {
        Self {
            id,
            template_id,
            name,
            level: 1,
            hp: 100,
            max_hp: 100,
            mp: 50,
            max_mp: 50,
            location: Location::new(),
            map_id: 0,
            zone_id: 0,
            is_alive: true,
            template: None,
            status: 5, // Alive status
            lv_mob: 0,
        }
    }

    pub fn from_template(template: MobTemplate, id: u64) -> Self {
        let mut mob = Self::new(id, template.id, template.name.clone());
        mob.template = Some(template.clone());
        mob.level = 1; 
        mob.max_hp = template.hp;
        mob.hp = template.hp;
        mob.max_mp = 50;
        mob.mp = 50;
        mob
    }

    pub fn get_hp_percent(&self) -> i32 {
        if self.max_hp > 0 {
            (self.hp * 100) / self.max_hp
        } else {
            0
        }
    }

    pub fn get_mp_percent(&self) -> i32 {
        if self.max_mp > 0 {
            (self.mp * 100) / self.max_mp
        } else {
            0
        }
    }

    pub fn is_dead(&self) -> bool {
        !self.is_alive || self.hp <= 0
    }

    pub fn take_damage(&mut self, damage: i32) {
        self.hp = (self.hp - damage).max(0);
        if self.hp <= 0 {
            self.is_alive = false;
        }
    }

    pub fn heal(&mut self, amount: i32) {
        self.hp = (self.hp + amount).min(self.max_hp);
    }

    pub fn restore_mp(&mut self, amount: i32) {
        self.mp = (self.mp + amount).min(self.max_mp);
    }

    pub fn set_location(&mut self, map_id: u32, zone_id: u32, x: i16, y: i16) {
        self.map_id = map_id;
        self.zone_id = zone_id;
        self.location.set_map(map_id, zone_id);
        self.location.set_position(x, y);
    }

    pub fn get_x(&self) -> i16 {
        self.location.x
    }

    pub fn get_y(&self) -> i16 {
        self.location.y
    }

    pub fn get_map_id(&self) -> u32 {
        self.map_id
    }

    pub fn get_zone_id(&self) -> u32 {
        self.zone_id
    }
}
