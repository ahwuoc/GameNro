#![allow(dead_code)]
use crate::entities::mob_template::Model as MobTemplate;
use crate::models::EffectSkill;
use crate::utils::location::Location;
use crate::utils::time;

#[derive(Debug, Clone)]
pub struct RtMob {
    pub id: u64,
    pub template_id: i8,
    pub name: String,
    pub level: i8,
    pub hp: i32,
    pub max_hp: i32,
    pub mp: i32,
    pub max_mp: i32,
    pub location: Location,
    pub map_id: i32,
    pub zone_id: i32,
    pub is_alive: bool,
    pub template: Option<MobTemplate>,
    pub status: i8,
    pub lv_mob: i8,
    pub last_time_die: u64,
    pub percent_dame: i16,
    pub start_time_attack_player: u64,
    pub temporary_enemies: Vec<u64>,
    pub spawn_status: i8,
    pub last_time_recovery: u64,
    pub origin_x: i16,
    pub origin_y: i16,
    pub last_time_move: u64,
    pub effect_skill: EffectSkill,
}

impl RtMob {
    pub fn new(id: u64, template_id: i8, name: String) -> Self {
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
            status: 5,
            lv_mob: 0,
            last_time_die: 0,
            percent_dame: 0,
            start_time_attack_player: 0,
            temporary_enemies: Vec::new(),
            spawn_status: 5,
            last_time_recovery: 0,
            origin_x: 0,
            origin_y: 0,
            last_time_move: 0,
            effect_skill: EffectSkill::new(),
        }
    }

    pub fn from_template(template: MobTemplate, id: u64) -> Self {
        let mut mob = Self::new(id, template.id as i8, template.name.clone());
        mob.template = Some(template.clone());
        mob.level = 1;
        mob.max_hp = template.hp;
        mob.hp = template.hp;
        mob.max_mp = 50;
        mob.mp = 50;
        mob.percent_dame = template.percent_dame;
        mob.spawn_status = 5;
        mob
    }

    pub fn add_temporary_enemy(&mut self, player_id: u64) {
        if self.is_alive && !self.temporary_enemies.contains(&player_id) {
            self.temporary_enemies.push(player_id);
        }
    }

    pub fn get_dame_attack(&self) -> i32 {
        if self.percent_dame > 0 {
            (self.max_hp as i64 * self.percent_dame as i64 / 100) as i32
        } else {
            100
        }
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

    pub fn take_damage(&mut self, mut damage: i32, die_when_hp_full: bool) -> i32 {
        if !self.is_alive || self.hp <= 0 {
            return 0;
        }

        if (self.template_id == crate::constant::const_mob::MOC_NHAN
            || self.template_id == crate::constant::const_mob::BU_NHIN_MA_QUAI)
            && damage > self.max_hp / 10
        {
            damage = self.max_hp / 10;
        }

        if !die_when_hp_full && self.hp == self.max_hp && damage >= self.hp {
            damage = self.hp - 1;
        }

        self.hp = (self.hp - damage).max(0);
        if self.hp <= 0 {
            self.die();
        }
        damage
    }

    pub fn heal(&mut self, amount: i32) {
        self.hp = (self.hp + amount).min(self.max_hp);
    }

    pub fn restore_mp(&mut self, amount: i32) {
        self.mp = (self.mp + amount).min(self.max_mp);
    }

    pub fn set_location(&mut self, map_id: i32, zone_id: i32, x: i16, y: i16) {
        self.map_id = map_id;
        self.zone_id = zone_id;

        self.location.set_position(x, y);
        self.origin_x = x;
        self.origin_y = y;
    }

    pub fn die(&mut self) {
        if !self.is_alive {
            return;
        }
        self.is_alive = false;
        self.status = 0;
        self.temporary_enemies.clear();
        self.last_time_die = time::current_time_millis();
        self.effect_skill.clear();
    }

    pub fn get_x(&self) -> i16 {
        self.location.x
    }

    pub fn get_y(&self) -> i16 {
        self.location.y
    }

    pub fn get_map_id(&self) -> i32 {
        self.map_id
    }

    pub fn get_zone_id(&self) -> i32 {
        self.zone_id
    }
}
