use chrono::{DateTime, Utc};
use crate::item::item::Item;
use crate::item::item_option::ItemOption;
use crate::entities::item_template::Model as ItemTemplate;

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
        let is_black_ball = if let Some(ref template) = template {
            Self::is_black_ball_template(template.id)
        } else {
            false
        };
        
        let is_namec_ball = if let Some(ref template) = template {
            Self::is_namec_ball_template(template.id)
        } else {
            false
        };

        Self {
            item_map_id,
            item_template: template,
            quantity,
            x,
            y,
            player_id: if player_id != -1 { player_id.abs() } else { player_id },
            options: Vec::new(),
            create_time: current_time,
            clan_id: -1,
            is_black_ball,
            is_namec_ball,
            last_time_move_to_player: current_time,
        }
    }

    pub fn is_not_null_item(&self) -> bool {
        self.item_template.is_some()
    }

    pub fn is_null_item(&self) -> bool {
        self.item_template.is_none()
    }

    pub fn get_item_name(&self) -> String {
        if let Some(ref template) = self.item_template {
            template.name.clone()
        } else {
            "Empty Item".to_string()
        }
    }

    pub fn get_item_id(&self) -> i32 {
        if let Some(ref template) = self.item_template {
            template.id
        } else {
            -1
        }
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

    pub fn get_age(&self) -> i64 {
        let now = Utc::now();
        (now - self.create_time).num_seconds()
    }

    pub fn is_expired(&self, max_age_seconds: i64) -> bool {
        self.get_age() > max_age_seconds
    }

    pub fn update(&mut self) {
        // Update item map logic
        // For example, check if item should expire or move
    }

    pub fn get_info(&self) -> String {
        if let Some(ref template) = self.item_template {
            format!("{} x{}", template.name, self.quantity)
        } else {
            "Empty Item".to_string()
        }
    }

    pub fn get_content(&self) -> String {
        // TODO: Implement content generation
        String::new()
    }

    pub fn add_option(&mut self, option: ItemOption) {
        self.options.push(option);
    }

    pub fn get_option_param(&self, option_id: i32) -> i32 {
        for option in &self.options {
            if option.get_option_id() == option_id {
                return option.get_param();
            }
        }
        0
    }

    pub fn has_option(&self, option_id: i32) -> bool {
        for option in &self.options {
            if option.get_option_id() == option_id {
                return true;
            }
        }
        false
    }

    pub fn get_options(&self) -> &Vec<ItemOption> {
        &self.options
    }

    pub fn clear_options(&mut self) {
        self.options.clear();
    }

    pub fn is_black_ball_template(template_id: i32) -> bool {
        let black_ball_ids = vec![
            86, 87, 88, 89, 90, 91, 92, 93, 94, 95,
            96, 97, 98, 99, 100, 101, 102, 103, 104, 105
        ];
        black_ball_ids.contains(&template_id)
    }

    pub fn is_namec_ball_template(template_id: i32) -> bool {
        let namec_ball_ids = vec![
            106, 107, 108, 109, 110, 111, 112, 113, 114, 115
        ];
        namec_ball_ids.contains(&template_id)
    }

    pub fn is_valuable_item(template_id: i32) -> bool {
        let valuable_ids = vec![
            200, 201, 202, 203, 204, 205, 206, 207, 208, 209
        ];
        valuable_ids.contains(&template_id)
    }

    pub fn get_item_rarity(&self) -> String {
        if let Some(ref template) = self.item_template {
            if Self::is_black_ball_template(template.id) {
                "Black Ball".to_string()
            } else if Self::is_namec_ball_template(template.id) {
                "Namec Ball".to_string()
            } else if Self::is_valuable_item(template.id) {
                "Valuable".to_string()
            } else {
                "Common".to_string()
            }
        } else {
            "Unknown".to_string()
        }
    }
}
