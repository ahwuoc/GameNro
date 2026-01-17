#![allow(dead_code)]
use crate::combine::model::Combine;
use crate::entities;
use crate::item::inventory::{self, Inventory};
use crate::map::Zone;
use crate::models::IntrinsicPlayer;
use crate::network::message::Message;
use crate::network::session::AsyncSession;
use crate::player::id_mark::IdMark;
use crate::player::n_point::NPoint;
use crate::utils::Location;
use serde_json::Value;

use std::sync::Arc;
use std::time::SystemTime;
use tokio::sync::RwLock;

#[derive(Clone)]
pub struct Player {
    pub id: u64,
    pub name: String,
    pub gender: i8,
    pub head: i16,
    pub session_id: Option<String>,

    pub n_point: NPoint,
    pub inventory: Inventory,
    pub intrinsic: IntrinsicPlayer,
    pub location: Location,
    pub combine_new: Combine,

    pub is_die: bool,
    pub is_new_member: bool,
    pub before_dispose: bool,

    pub is_train: bool,
    pub type_train: u8,
    pub time_off: u64,

    pub type_pk: i8,

    pub zone_id: i32,
    pub map_id: i32,
    pub last_time_use_option: u64,
    pub last_time_revived: u64,

    pub just_revived: bool,
    pub is_fight: bool,
    pub is_fight1: bool,
    pub is_try: bool,
    pub is_try1: bool,

    pub zone: Option<Zone>,
    pub is_admin: bool,
    pub admin_key: bool,

    pub id_mark: IdMark,

    pub task_id: i32,
    pub is_boss: bool,
    pub notify: Option<String>,
    pub session: Option<Arc<RwLock<AsyncSession>>>,
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
            gender: 0,
            head: 0,
            session_id: None,
            n_point: NPoint::new(),
            inventory: Inventory::new(),
            intrinsic: IntrinsicPlayer::new(),
            location: Location::new(),
            combine_new: Combine::new(),
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
            zone: None,
            is_admin: false,
            admin_key: false,
            id_mark: IdMark::new(),
            task_id: 0,
            is_boss: false,
            notify: None,
            session: None,
        }
    }

    pub fn is_die(&self) -> bool {
        self.is_die || self.n_point.hp <= 0
    }

    pub fn get_name(&self) -> &str {
        &self.name
    }
    pub fn get_head(&self) -> i16 {
        if let Some(item) = self.inventory.items_body.get(5) {
            if item.is_not_null_item() {
                if let Some(tpl) = &item.template {
                    let head = tpl.head;
                    if head != -1 {
                        return head as i16;
                    }
                }
            }
        }
        self.head
    }
    pub fn get_body(&self) -> i16 {
        if let Some(item) = self.inventory.items_body.get(5) {
            if item.is_not_null_item() {
                if let Some(tpl) = &item.template {
                    let body = tpl.body;
                    if body != -1 {
                        return body as i16;
                    }
                }
            }
        }
        if self.gender == 1 {
            59
        } else {
            57
        }
    }
    pub fn get_leg(&self) -> i16 {
        if let Some(item) = self.inventory.items_body.get(5) {
            if item.is_not_null_item() {
                if let Some(tpl) = &item.template {
                    let leg = tpl.leg;
                    if leg != -1 {
                        return leg as i16;
                    }
                }
            }
        }
        if self.gender == 1 {
            60
        } else {
            58
        }
    }
    pub async fn send_message(&self, msg: Message) -> anyhow::Result<()> {
        if let Some(session) = &self.session {
            let session_clone = session.clone();
            tokio::spawn(async move {
                let mut session_guard = session_clone.write().await;
                session_guard.send_message(&msg).await;
            });
        }
        Ok(())
    }

    pub fn is_pl(&self) -> bool {
        !self.is_die && self.session_id.is_some()
    }

    pub fn update(&mut self) {
        if !self.before_dispose {
            self.n_point.set_base_point();
            self.location.update();
            if self.n_point.hp <= 0 && !self.is_die {
                self.is_die = true;
            }
        }
    }

    pub fn injured(&mut self, damage: u64, piercing: bool) -> u64 {
        0
    }

    pub fn set_die(&mut self) {
        self.is_die = true;
        self.n_point.hp = 0;
    }

    pub fn revive(&mut self) {
        self.is_die = false;
        if self.n_point.hp <= 0 {
            self.n_point.hp = 1;
        }
        self.just_revived = true;
        self.last_time_revived = SystemTime::now()
            .duration_since(SystemTime::UNIX_EPOCH)
            .unwrap()
            .as_millis() as u64;
    }
    pub fn chat(&self, text: &str) {
        println!("[{}]: {}", self.name, text);
    }

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

    pub fn set_fight(&mut self, _type_fight: u8, _type_target: u8) {
        self.is_fight = true;
    }

    pub fn reset_fight(&mut self) {
        self.is_fight = false;
        self.is_fight1 = false;
        self.is_try = false;
        self.is_try1 = false;
    }

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
    pub fn set_notify(&mut self, notify: String) {
        self.notify = Some(notify);
    }

    pub fn clear_notify(&mut self) {
        self.notify = None;
    }

    pub fn set_session(&mut self, session: Arc<RwLock<AsyncSession>>) {
        self.session = Some(session);
    }

    pub fn set_zone(&mut self, zone: Zone) {
        self.zone = Some(zone);
    }

    pub fn clear_zone(&mut self) {
        self.zone = None;
    }

    pub fn has_tennis_spaceship(&self) -> bool {
        false
    }

    pub fn get_task_id(&self) -> i32 {
        self.task_id
    }

    pub fn set_task_id(&mut self, task_id: i32) {
        self.task_id = task_id;
    }

    pub fn is_boss(&self) -> bool {
        self.is_boss
    }

    pub fn has_previous_capsule_location(&self) -> bool {
        false
    }

    pub fn save_capsule_location(&mut self, map_id: i32, zone_id: i32) {
        println!("Saving capsule location: map {} zone {}", map_id, zone_id);
    }

    pub fn get_previous_capsule_location(&self) -> Option<(i32, i32)> {
        None
    }

    pub fn update_zone_change_time(&mut self) {
        println!("Updated zone change time for player {}", self.name);
    }
}
