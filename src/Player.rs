use crate::{
    Friend::{Enemy, Friend},
    Inventory::Inventory,
    Location::Location,
    PlayerSkill::PlayerSkill,
    TaskPlayer::TaskPlayer,
    Zone::Zone,
    nPoint::NPoint,
};
use std::{alloc::System, sync::Arc, time::SystemTime};
use tokio::sync::Mutex;

#[derive(Debug, Clone)]
pub struct Player {
    //basic info
    pub id: u32,
    pub name: String,
    pub gender: i8,
    pub head: i32,
    pub is_new_member: bool,

    //session
    pub session_id: String,
    pub account_id: u32,
    pub is_logged_in: bool,

    //Combat
    pub type_pk: i8,
    pub just_revived: bool,
    pub last_time_revived: u64,

    //Game state
    pub is_pet: bool,
    pub is_boss: bool,
    pub is_die: bool,

    //stats and attributes
    pub location: Location,
    pub n_point: NPoint,
    pub inventory: Inventory,
    pub player_skill: PlayerSkill,
    pub player_task: TaskPlayer,

    pub last_time_use_options: u64,
    pub time_change_zone: u64,

    //Zone info
    pub zone: Option<Arc<Mutex<Zone>>>,
    pub map_id_before_logout: i16,

    //social
    pub friends: Vec<Friend>,
    pub enemies: Vec<Enemy>,

    //time tracking
    pub mob_target: Option<u32>, // mob id
    pub last_time_target_mob: u64,
    pub time_target_mob: u64,
    pub last_time_attack: u64,
}
impl Player {
    pub fn new(id: u32, name: String, session_id: String) -> Self {
        Player {
            id,
            name,
            gender: 0,
            head: 0,
            is_new_member: true,
            session_id,
            account_id: 0,
            is_logged_in: false,
            is_pet: false,
            is_boss: false,
            is_die: false,
            type_pk: 0,
            just_revived: false,
            last_time_revived: 0,
            location: Location::new(0, 0),
            n_point: NPoint::new(),
            inventory: Inventory::new(),
            player_skill: PlayerSkill::new(),
            player_task: TaskPlayer::new(),
            zone: None,
            map_id_before_logout: -1,
            friends: Vec::new(),
            enemies: Vec::new(),
            last_time_use_options: 0,
            time_change_zone: 0,
            mob_target: None,
            last_time_target_mob: 0,
            time_target_mob: 500,
            last_time_attack: 0,
        }
    }
    pub fn is_player(&self) -> bool {
        !self.is_pet && !self.is_boss
    }
    pub fn is_die(&mut self) {
        self.n_point.set_hp(0);
        self.n_point.set_mp(0);
        self.is_die = true;
    }
    pub fn revive(&mut self) {
        self.n_point.set_hp(self.n_point.hp_max / 2);
        self.n_point.set_mp(self.n_point.mp_max / 2);
        self.is_die = false;
        self.just_revived = true;
        self.last_time_revived = SystemTime::now()
            .duration_since(SystemTime::UNIX_EPOCH)
            .unwrap()
            .as_millis() as u64;
    }
    pub fn injured(&mut self, damage: f64, piercing: bool) -> f64 {
        if self.is_die == true {
            return 0.0;
        }
        let mut final_damage = damage;

        if !piercing {
            final_damage = damage - (self.n_point.defense as f64 * 0.1);
            if final_damage < 1.0 {
                final_damage = 1.0;
            }
        }
        self.n_point.sub_hp(final_damage);
        final_damage
    }
    pub fn move_to(&mut self, x: i32, y: i32) {
        self.location.x = x;
        self.location.y = y;
        self.location.last_time_player_move = SystemTime::now()
            .duration_since(SystemTime::UNIX_EPOCH)
            .unwrap()
            .as_millis() as u64;
    }

    pub fn get_head(&self) -> i32 {
        if self.inventory.items_body.len() > 5 && self.inventory.items_body[5].is_not_null_item() {
            return self.inventory.items_body[5].template_id;
        }
        self.head
    }
    pub fn get_body(&self) -> i16 {
        if !self.inventory.items_body.is_empty() && self.inventory.items_body[0].is_not_null_item()
        {
            return self.inventory.items_body[0].template_id as i16;
        }
        if self.gender == 2 { 59 } else { 57 } // Namek : Saiyan/Human
    }
    pub fn get_leg(&self) -> i16 {
        if self.inventory.items_body.len() > 1 && self.inventory.items_body[1].is_not_null_item() {
            return self.inventory.items_body[1].template_id as i16;
        }
        if self.gender == 1 { 60 } else { 58 }
    }
    pub fn update(&mut self) {
        let current_time = SystemTime::now()
            .duration_since(SystemTime::UNIX_EPOCH)
            .unwrap()
            .as_millis() as u64;
    }
    pub fn add_friend(&mut self, friend: Friend) {
        if self.friends.len() < 50 {
            // Max friends limit
            self.friends.push(friend);
        }
    }

    // Add enemy
    pub fn add_enemy(&mut self, enemy: Enemy) {
        if self.enemies.len() < 50 {
            // Max enemies limit
            self.enemies.push(enemy);
        }
    }
    pub fn dispose(&mut self) {
        self.friends.clear();
        self.enemies.clear();
        self.inventory.items_bag.clear();
        self.inventory.items_body.clear();
        self.player_skill.skills.clear();
        self.zone = None;
    }
}
