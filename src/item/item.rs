#![allow(dead_code)]
use crate::entities::item_template::Model as ItemTemplate;
use crate::item::item_option::ItemOption;
use chrono::{DateTime, Utc};
use serde::de::value::BoolDeserializer;

#[derive(Debug, Clone)]
pub struct Item {
    pub template: Option<ItemTemplate>,
    pub info: String,
    pub quantity: i32,
    pub quantity_gd: i32,
    pub item_options: Vec<ItemOption>,
    pub create_time: DateTime<Utc>,
}

impl Item {
    pub fn new() -> Self {
        Self {
            template: None,
            info: String::new(),
            quantity: 0,
            quantity_gd: 0,
            item_options: Vec::new(),
            create_time: Utc::now(),
        }
    }

    pub fn with_template(template: ItemTemplate, quantity: i32) -> Self {
        Self {
            template: Some(template),
            info: String::new(),
            quantity,
            quantity_gd: 0,
            item_options: Vec::new(),
            create_time: Utc::now(),
        }
    }

    pub fn is_not_null_item(&self) -> bool {
        self.template.is_some()
    }

    pub fn is_null_item(&self) -> bool {
        self.template.is_none()
    }

    pub fn get_template_id(&self) -> Option<i16> {
        self.template.as_ref().map(|t| t.id)
    }
    pub fn add_option(&mut self, option: ItemOption) {
        self.item_options.push(option);
    }

    pub fn add_option_param(&mut self, option_id: i8, param: i16) {
        for option in &mut self.item_options {
            if option.get_option_id() == option_id {
                option.set_param(option.get_param() + param);
                return;
            }
        }
        if Self::is_valid_option_id(option_id) {
            self.item_options.push(ItemOption::new(option_id, param));
        } else {
            println!("Warning: Invalid option ID {} for item", option_id);
        }
    }

    fn is_valid_option_id(option_id: i8) -> bool {
        use crate::item::option_template_manager;
        option_template_manager::get(option_id).is_some()
    }

    pub fn sub_option_param(&mut self, option_id: i8, param: i16) {
        for option in &mut self.item_options {
            if option.get_option_id() == option_id {
                option.set_param((option.get_param() - param).max(0));
                return;
            }
        }
    }

    pub fn get_option_info(&self) -> String {
        "todos".to_string()
    }

    pub fn get_info(&self) -> String {
        if let Some(ref template) = self.template {
            format!("{} - {}", template.name, self.get_option_info())
        } else {
            "Empty Item".to_string()
        }
    }
    pub fn get_type(&self) -> i8 {
        self.template.as_ref().map(|t| t.r#type as i8).unwrap_or(-1)
    }
    pub fn is_item_body(&self) -> bool {
        if self.get_type() >= 0 && self.get_type() <= 6 {
            return true;
        }
        return false;
    }
    pub fn get_content(&self) -> String {
        if let Some(strpower) = self.get_str_require() {
            format!("Yeu cau suc manh {:?}", strpower)
        } else {
            "OKem".to_string()
        }
    }

    pub fn is_cong_thuc_vip(&self) -> bool {
        if let Some(ref template) = self.template {
            template.id >= 1084 && template.id <= 1086
        } else {
            false
        }
    }

    /// Check if item is Cong Thuc Thuong (Normal Recipe)
    pub fn is_cong_thuc_thuong(&self) -> bool {
        if let Some(ref template) = self.template {
            template.id >= 1071 && template.id <= 1073
        } else {
            false
        }
    }
    pub fn is_da_nang_cap(&self) -> bool {
        if let Some(ref template) = self.template {
            template.id >= 1087 && template.id <= 1089
        } else {
            false
        }
    }

    /// Check if item is Da May Man (Lucky Stone)
    pub fn is_da_may_man(&self) -> bool {
        if let Some(ref template) = self.template {
            template.id >= 1090 && template.id <= 1092
        } else {
            false
        }
    }
    pub fn get_icon_id(&self) -> Option<i32> {
        self.template.as_ref().map(|t| t.icon_id as i32)
    }

    pub fn get_part(&self) -> Option<i32> {
        self.template.as_ref().map(|t| t.part as i32)
    }

    pub fn get_gold(&self) -> Option<i64> {
        self.template.as_ref().map(|t| t.gold as i64)
    }

    pub fn get_gem(&self) -> Option<i32> {
        self.template.as_ref().map(|t| t.gem as i32)
    }
    pub fn get_str_require(&self) -> Option<i32> {
        self.template.as_ref().map(|t| t.power_require as i32)
    }
    pub fn get_name(&self) -> Option<&str> {
        self.template.as_ref().map(|t| t.name.as_str())
    }

    pub fn can_use(&self, player_str: i32) -> bool {
        if let Some(str_require) = self.get_str_require() {
            player_str >= str_require
        } else {
            true
        }
    }
    pub fn get_description(&self) -> String {
        if let Some(ref template) = self.template {
            template.description.clone()
        } else {
            String::new()
        }
    }
    pub fn get_gender(&self) -> Option<i32> {
        self.template.as_ref().map(|t| t.gender as i32)
    }
    pub fn matches_gender(&self, player_gender: i32) -> bool {
        if let Some(item_gender) = self.get_gender() {
            item_gender == -1 || item_gender == player_gender
        } else {
            true
        }
    }
}

impl Default for Item {
    fn default() -> Self {
        Self::new()
    }
}
