#![allow(dead_code)]
use crate::entities::item_template::Model as ItemTemplate;
use crate::item::item_option::ItemOption;
use chrono::{DateTime, Utc};

// Time constants (milliseconds)
pub const OWNER_RESET_TIME_MS: i64 = 45_000; // Reset player_id to -1 after 45s
pub const ITEM_EXPIRE_TIME_MS: i64 = 50_000; // Remove normal items after 50s
pub const NAMEC_EXPIRE_TIME_MS: i64 = 1_800_000; // Namec balls last 30 minutes
pub const BLACK_BALL_MOVE_INTERVAL_MS: i64 = 10_000; // Black ball moves every 10s

// Satellite item IDs
pub const SATELLITE_MP: i16 = 342;
pub const SATELLITE_INTELLIGENT: i16 = 343;
pub const SATELLITE_DEFEND: i16 = 344;
pub const SATELLITE_HP: i16 = 345;

// Special maps (no auto-expire)
pub const SPECIAL_MAPS: &[i32] = &[21, 22, 23];

// Special item IDs
pub const ITEM_DHVT: i16 = 726;
pub const ITEM_992: i16 = 992;
pub const ITEM_460: i16 = 460;
pub const ITEM_78: i16 = 78;

// Item type for satellite
pub const ITEM_TYPE_SATELLITE: i32 = 22;

/// Satellite buff types
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum SatelliteType {
    Mp,          // Item 342 - Restore MP
    Intelligent, // Item 343 - Intelligence buff
    Defend,      // Item 344 - Defense buff
    Hp,          // Item 345 - Restore HP
}

/// Events that can occur during ItemMap update
#[derive(Debug, Clone)]
pub enum ItemMapEvent {
    OwnerReset,
    Expired,
    Moved {
        new_x: i32,
        new_y: i32,
    },
    SatelliteBuff {
        satellite_type: SatelliteType,
        player_id: u64,
    },
}

/// Result of update operation
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
    pub last_time_move_to_player: DateTime<Utc>,
}

impl ItemMap {
    pub fn new(
        item_map_id: i32,
        template: Option<ItemTemplate>,
        quantity: i32,
        x: i32,
        y: i32,
        player_id: i64,
    ) -> Self {
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
            last_time_move_to_player: current_time,
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

    pub fn is_null_item(&self) -> bool {
        self.item_template.is_none()
    }

    pub fn get_item_name(&self) -> String {
        self.item_template
            .as_ref()
            .map(|t| t.name.clone())
            .unwrap_or_else(|| "Empty Item".to_string())
    }

    pub fn get_item_id(&self) -> i16 {
        self.item_template.as_ref().map(|t| t.id).unwrap_or(-1)
    }

    pub fn get_item_type(&self) -> i32 {
        self.item_template.as_ref().map(|t| t.r#type).unwrap_or(0)
    }

    pub fn get_quantity(&self) -> i32 {
        self.quantity
    }

    pub fn set_quantity(&mut self, quantity: i32) {
        self.quantity = quantity.max(1);
    }

    pub fn get_position(&self) -> (i32, i32) {
        (self.x, self.y)
    }

    pub fn set_position(&mut self, x: i32, y: i32) {
        self.x = x;
        self.y = y;
    }

    pub fn get_player_id(&self) -> i64 {
        self.player_id
    }

    pub fn set_player_id(&mut self, player_id: i64) {
        self.player_id = player_id;
    }

    pub fn get_clan_id(&self) -> i32 {
        self.clan_id
    }

    pub fn set_clan_id(&mut self, clan_id: i32) {
        self.clan_id = clan_id;
    }

    pub fn is_black_ball(&self) -> bool {
        self.is_black_ball
    }

    pub fn is_namec_ball(&self) -> bool {
        self.is_namec_ball
    }

    pub fn get_create_time(&self) -> DateTime<Utc> {
        self.create_time
    }

    pub fn get_last_move_time(&self) -> DateTime<Utc> {
        self.last_time_move_to_player
    }

    pub fn update_last_move_time(&mut self) {
        self.last_time_move_to_player = Utc::now();
    }

    pub fn get_age_ms(&self) -> i64 {
        let now = Utc::now();
        (now - self.create_time).num_milliseconds()
    }

    pub fn get_time_since_last_move_ms(&self) -> i64 {
        let now = Utc::now();
        (now - self.last_time_move_to_player).num_milliseconds()
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
        let age = self.get_age_ms();
        if age > ITEM_EXPIRE_TIME_MS {
            return true;
        }

        false
    }

    pub fn is_satellite_item(&self) -> bool {
        self.get_item_type() == ITEM_TYPE_SATELLITE
    }

    pub fn get_satellite_type(&self) -> Option<SatelliteType> {
        match self.get_item_id() {
            SATELLITE_MP => Some(SatelliteType::Mp),
            SATELLITE_INTELLIGENT => Some(SatelliteType::Intelligent),
            SATELLITE_DEFEND => Some(SatelliteType::Defend),
            SATELLITE_HP => Some(SatelliteType::Hp),
            _ => None,
        }
    }

    pub fn should_move_to_player(&self) -> bool {
        if !self.is_black_ball {
            return false;
        }
        self.get_time_since_last_move_ms() > BLACK_BALL_MOVE_INTERVAL_MS
    }

    /// Check if player can pick up this item
    pub fn can_pickup(&self, player_id: u64, player_clan_id: Option<i32>) -> bool {
        if self.is_picked_up {
            return false;
        }

        // Owner can always pick up
        if self.player_id == player_id as i64 {
            return true;
        }

        // If no owner restriction, anyone can pick up
        if self.player_id == -1 {
            return true;
        }

        // Clan members can pick up clan items
        if self.clan_id != -1 {
            if let Some(clan_id) = player_clan_id {
                if clan_id == self.clan_id {
                    return true;
                }
            }
        }

        false
    }

    /// Main update function - returns events and whether item should be removed
    pub fn update(&mut self) -> UpdateResult {
        let mut result = UpdateResult::default();

        if !self.is_not_null_item() {
            return result;
        }

        // Check owner reset
        if self.should_reset_owner() {
            self.player_id = -1;
            result.events.push(ItemMapEvent::OwnerReset);
        }

        // Check expiration
        if self.should_expire() {
            result.should_remove = true;
            result.events.push(ItemMapEvent::Expired);
        }

        result
    }

    pub fn get_info(&self) -> String {
        self.item_template
            .as_ref()
            .map(|t| format!("{} x{}", t.name, self.quantity))
            .unwrap_or_else(|| "Empty Item".to_string())
    }

    pub fn add_option(&mut self, option: ItemOption) {
        self.options.push(option);
    }

    pub fn get_option_param(&self, option_id: i8) -> i16 {
        self.options
            .iter()
            .find(|o| o.get_option_id() == option_id)
            .map(|o| o.get_param())
            .unwrap_or(0)
    }

    pub fn has_option(&self, option_id: i8) -> bool {
        self.options.iter().any(|o| o.get_option_id() == option_id)
    }

    pub fn get_options(&self) -> &Vec<ItemOption> {
        &self.options
    }

    pub fn clear_options(&mut self) {
        self.options.clear();
    }

    // Black ball IDs: 86-105 (20 items)
    pub fn is_black_ball_template(template_id: i16) -> bool {
        (86..=105).contains(&template_id)
    }

    // Namec ball IDs: 106-115 (10 items)
    pub fn is_namec_ball_template(template_id: i16) -> bool {
        (106..=115).contains(&template_id)
    }

    pub fn is_valuable_item(template_id: i16) -> bool {
        (200..=209).contains(&template_id)
    }

    pub fn get_item_rarity(&self) -> &'static str {
        if let Some(ref template) = self.item_template {
            if Self::is_black_ball_template(template.id) {
                "Black Ball"
            } else if Self::is_namec_ball_template(template.id) {
                "Namec Ball"
            } else if Self::is_valuable_item(template.id) {
                "Valuable"
            } else {
                "Common"
            }
        } else {
            "Unknown"
        }
    }
}
