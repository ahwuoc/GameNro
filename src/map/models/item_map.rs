#![allow(dead_code)]
use crate::entities::item_template::Model as ItemTemplate;
use crate::item::item_option::ItemOption;
use chrono::{DateTime, Utc};

// Time constants (milliseconds)
pub const OWNER_RESET_TIME_MS: i64 = 45_000;
pub const ITEM_EXPIRE_TIME_MS: i64 = 50_000;

// Special maps (no auto-expire)
pub const SPECIAL_MAPS: &[i32] = &[21, 22, 23];

// Special item IDs
pub const ITEM_DHVT: i16 = 726;
pub const ITEM_992: i16 = 992;
pub const ITEM_78: i16 = 78;

// Item type for satellite
pub const ITEM_TYPE_SATELLITE: i32 = 22;

#[derive(Debug, Clone)]
pub enum ItemMapEvent {
    OwnerReset,
    Expired,
}

#[derive(Debug, Default)]
pub struct UpdateResult {
    pub should_remove: bool,
    pub events: Vec<ItemMapEvent>,
}

#[derive(Debug, Clone)]
pub struct ItemMap {
    pub item_map_id: i32,
    pub item_template: Option<ItemTemplate>,
    pub quantity: i32,
    pub x: i32,
    pub y: i32,
    pub player_id: i64,
    pub options: Vec<ItemOption>,
    pub create_time: DateTime<Utc>,
    pub clan_id: i32,
    pub is_black_ball: bool,
    pub is_namec_ball: bool,
    pub is_picked_up: bool,
    pub map_id: i32,
    pub zone_id: i32,
}

impl ItemMap {
    pub fn new(
        template: Option<ItemTemplate>,
        quantity: i32,
        x: i32,
        y: i32,
        player_id: i64,
    ) -> Self {
        let item_map_id = crate::map::services::item_map_service::next_item_map_id();
        let current_time = Utc::now();
        let is_black_ball = template
            .as_ref()
            .map(|t| Self::is_black_ball_template(t.id))
            .unwrap_or(false);

        let is_namec_ball = template
            .as_ref()
            .map(|t| Self::is_namec_ball_template(t.id))
            .unwrap_or(false);

        Self {
            item_map_id,
            item_template: template,
            quantity,
            x,
            y,
            player_id: if player_id != -1 {
                player_id.abs()
            } else {
                player_id
            },
            options: Vec::new(),
            create_time: current_time,
            clan_id: -1,
            is_black_ball,
            is_namec_ball,
            is_picked_up: false,
            map_id: 0,
            zone_id: 0,
        }
    }

    pub fn set_location(&mut self, map_id: i32, zone_id: i32, x: i32, y: i32) {
        self.map_id = map_id;
        self.zone_id = zone_id;
        self.x = x;
        self.y = y;
    }

    pub fn is_not_null_item(&self) -> bool {
        self.item_template.is_some()
    }

    pub fn get_item_id(&self) -> i16 {
        self.item_template.as_ref().map(|t| t.id).unwrap_or(-1)
    }

    pub fn get_item_type(&self) -> i32 {
        self.item_template.as_ref().map(|t| t.r#type).unwrap_or(0)
    }

    pub fn get_age_ms(&self) -> i64 {
        let now = Utc::now();
        (now - self.create_time).num_milliseconds()
    }

    pub fn should_reset_owner(&self) -> bool {
        if self.player_id == -1 {
            return false;
        }
        let item_id = self.get_item_id();
        if item_id == ITEM_DHVT || item_id == ITEM_992 {
            return false;
        }
        self.get_age_ms() > OWNER_RESET_TIME_MS
    }

    pub fn should_expire(&self) -> bool {
        if self.is_namec_ball {
            return false;
        }
        if SPECIAL_MAPS.contains(&self.map_id) {
            return false;
        }
        let item_id = self.get_item_id();
        let item_type = self.get_item_type();
        if item_type == ITEM_TYPE_SATELLITE {
            return false;
        }
        if item_id == ITEM_78 || item_id == ITEM_DHVT {
            return false;
        }
        self.get_age_ms() > ITEM_EXPIRE_TIME_MS
    }

    pub fn can_pickup(&self, player_id: u64, player_clan_id: Option<i32>) -> bool {
        if self.is_picked_up {
            return false;
        }
        if self.player_id == player_id as i64 {
            return true;
        }
        if self.player_id == -1 {
            return true;
        }
        if self.clan_id != -1 {
            if let Some(clan_id) = player_clan_id {
                if clan_id == self.clan_id {
                    return true;
                }
            }
        }
        false
    }

    pub fn update(&mut self) -> UpdateResult {
        let mut result = UpdateResult::default();
        if !self.is_not_null_item() {
            return result;
        }
        if self.should_reset_owner() {
            self.player_id = -1;
            result.events.push(ItemMapEvent::OwnerReset);
        }
        if self.should_expire() {
            result.should_remove = true;
            result.events.push(ItemMapEvent::Expired);
        }
        result
    }

    pub fn is_black_ball_template(template_id: i16) -> bool {
        (372..=378).contains(&template_id)
    }

    pub fn is_namec_ball_template(template_id: i16) -> bool {
        (353..=360).contains(&template_id)
    }
}
