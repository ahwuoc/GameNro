use crate::entities::item_option_template::Model as ItemOptionTemplate;
use crate::entities::item_template::Model as ItemTemplate;
use crate::item::item_option::ItemOption;
use chrono::{DateTime, Utc};
use std::collections::HashMap;

#[derive(Debug, Clone)]
pub struct Item {
    pub template: Option<ItemTemplate>,
    pub info: String,
    pub content: String,
    pub quantity: i32,
    pub quantity_gd: i32,
    pub item_options: Vec<ItemOption>,
    pub create_time: DateTime<Utc>,
}

impl Item {
    /// Create a new empty item
    pub fn new() -> Self {
        Self {
            template: None,
            info: String::new(),
            content: String::new(),
            quantity: 0,
            quantity_gd: 0,
            item_options: Vec::new(),
            create_time: Utc::now(),
        }
    }

    /// Create item from template ID
    pub fn from_template_id(template_id: i32, template: ItemTemplate) -> Self {
        Self {
            template: Some(template),
            info: String::new(),
            content: String::new(),
            quantity: 1,
            quantity_gd: 0,
            item_options: Vec::new(),
            create_time: Utc::now(),
        }
    }

    /// Create item with template and quantity
    pub fn with_template(template: ItemTemplate, quantity: i32) -> Self {
        Self {
            template: Some(template),
            info: String::new(),
            content: String::new(),
            quantity,
            quantity_gd: 0,
            item_options: Vec::new(),
            create_time: Utc::now(),
        }
    }

    /// Check if item is not null (has template)
    pub fn is_not_null_item(&self) -> bool {
        self.template.is_some()
    }

    /// Check if item is null (no template)
    pub fn is_null_item(&self) -> bool {
        self.template.is_none()
    }

    /// Get item name
    pub fn get_name(&self) -> String {
        if let Some(ref template) = self.template {
            template.name.clone()
        } else {
            String::new()
        }
    }

    /// Get item template ID
    pub fn get_template_id(&self) -> Option<i32> {
        self.template.as_ref().map(|t| t.id)
    }

    /// Get item type
    pub fn get_type(&self) -> Option<i32> {
        self.template.as_ref().map(|t| t.r#type as i32)
    }

    /// Get option parameter by option ID
    pub fn get_option_param(&self, option_id: i32) -> i32 {
        for option in &self.item_options {
            if option.get_option_id() == option_id {
                return option.get_param();
            }
        }
        0
    }

    /// Check if item has specific option
    pub fn has_option(&self, option_id: i32) -> bool {
        for option in &self.item_options {
            if option.get_option_id() == option_id {
                return true;
            }
        }
        false
    }

    /// Add option to item
    pub fn add_option(&mut self, option: ItemOption) {
        self.item_options.push(option);
    }

    /// Add option parameter
    pub fn add_option_param(&mut self, option_id: i32, param: i32) {
        for option in &mut self.item_options {
            if option.get_option_id() == option_id {
                option.set_param(option.get_param() + param);
                return;
            }
        }
        // If option doesn't exist, create new one
        // This would need ItemOptionTemplate lookup
        // For now, just add with default template
    }

    /// Subtract option parameter
    pub fn sub_option_param(&mut self, option_id: i32, param: i32) {
        for option in &mut self.item_options {
            if option.get_option_id() == option_id {
                option.set_param((option.get_param() - param).max(0));
                return;
            }
        }
    }

    /// Get all option strings
    pub fn get_option_info(&self) -> String {
        let mut option_strings = Vec::new();

        for option in &self.item_options {
            // Skip certain option IDs (72, 73, 102, 107)
            let option_id = option.get_option_id();
            if option_id != 72 && option_id != 73 && option_id != 102 && option_id != 107 {
                option_strings.push(option.get_option_description());
            }
        }

        option_strings.join("\n")
    }

    /// Clone item
    pub fn clone_item(&self) -> Self {
        Self {
            template: self.template.clone(),
            info: self.info.clone(),
            content: self.content.clone(),
            quantity: self.quantity,
            quantity_gd: self.quantity_gd,
            item_options: self.item_options.clone(),
            create_time: Utc::now(),
        }
    }

    /// Get item info
    pub fn get_info(&self) -> String {
        if let Some(ref template) = self.template {
            format!("{} - {}", template.name, self.get_option_info())
        } else {
            "Empty Item".to_string()
        }
    }

    /// Get item content
    pub fn get_content(&self) -> String {
        self.content.clone()
    }

    /// Set item content
    pub fn set_content(&mut self, content: String) {
        self.content = content;
    }

    /// Check if item can be enhanced (Pha Le Hoa)
    pub fn can_pha_le_hoa(&self) -> bool {
        if let Some(ref template) = self.template {
            (template.r#type < 5 || template.r#type == 32)
                && (template.id == 1418 || template.id == 1429)
        } else {
            false
        }
    }

    /// Check if item is SKH (Special Item)
    pub fn is_skh(&self) -> bool {
        for option in &self.item_options {
            let option_id = option.get_option_id();
            if option_id >= 127 && option_id <= 135 {
                return true;
            }
        }
        false
    }

    /// Check if item is Trang Bi An (Hidden Equipment)
    pub fn is_trang_bi_an(&self) -> bool {
        for option in &self.item_options {
            let option_id = option.get_option_id();
            if option_id >= 34 && option_id <= 36 {
                return true;
            }
        }
        false
    }

    /// Check if item is DTS (Special Item Type)
    pub fn is_dts(&self) -> bool {
        if let Some(ref template) = self.template {
            template.id >= 1048 && template.id <= 1062
        } else {
            false
        }
    }

    /// Check if item is DTL (Special Item Type)
    pub fn is_dtl(&self) -> bool {
        if let Some(ref template) = self.template {
            template.id >= 555 && template.id <= 567
        } else {
            false
        }
    }

    /// Check if item is DHD (Special Item Type)
    pub fn is_dhd(&self) -> bool {
        if let Some(ref template) = self.template {
            template.id >= 650 && template.id <= 662
        } else {
            false
        }
    }

    /// Check if item is Manh TS (Special Item Type)
    pub fn is_manh_ts(&self) -> bool {
        if let Some(ref template) = self.template {
            template.id >= 1066 && template.id <= 1070
        } else {
            false
        }
    }

    /// Check if item is Cong Thuc VIP (VIP Recipe)
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

    /// Check if item is Da Nang Cap (Upgrade Stone)
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

    /// Check if item is Sach Tuyet Ky (Ultimate Book)
    pub fn is_sach_tuyet_ky(&self) -> bool {
        if let Some(ref template) = self.template {
            template.id == 1093
        } else {
            false
        }
    }

    /// Check if item is Sach Tuyet Ky 2 (Ultimate Book 2)
    pub fn is_sach_tuyet_ky2(&self) -> bool {
        if let Some(ref template) = self.template {
            template.id == 1094
        } else {
            false
        }
    }

    /// Get item type name
    pub fn type_name(&self) -> String {
        if let Some(ref template) = self.template {
            match template.r#type {
                0 => "Trang Bi".to_string(),
                1 => "Vu Khi".to_string(),
                2 => "Giap".to_string(),
                3 => "Quan".to_string(),
                4 => "Giay".to_string(),
                5 => "Rong".to_string(),
                6 => "Ngoc".to_string(),
                7 => "Thuoc".to_string(),
                8 => "Vat Pham".to_string(),
                9 => "Cong Thuc".to_string(),
                10 => "Sach".to_string(),
                11 => "Trang Suc".to_string(),
                12 => "Bao Ve".to_string(),
                13 => "Bao Ve".to_string(),
                14 => "Bao Ve".to_string(),
                15 => "Bao Ve".to_string(),
                16 => "Bao Ve".to_string(),
                17 => "Bao Ve".to_string(),
                18 => "Bao Ve".to_string(),
                19 => "Bao Ve".to_string(),
                20 => "Bao Ve".to_string(),
                21 => "Bao Ve".to_string(),
                22 => "Bao Ve".to_string(),
                23 => "Bao Ve".to_string(),
                24 => "Bao Ve".to_string(),
                25 => "Bao Ve".to_string(),
                26 => "Bao Ve".to_string(),
                27 => "Bao Ve".to_string(),
                28 => "Bao Ve".to_string(),
                29 => "Bao Ve".to_string(),
                30 => "Bao Ve".to_string(),
                31 => "Bao Ve".to_string(),
                32 => "Bao Ve".to_string(),
                33 => "Bao Ve".to_string(),
                34 => "Bao Ve".to_string(),
                35 => "Bao Ve".to_string(),
                _ => "Khac".to_string(),
            }
        } else {
            "Khac".to_string()
        }
    }

    /// Get item planet name (not available in template)
    pub fn type_hanh_tinh(&self) -> String {
        // Planet ID not available in item_template
        "Khac".to_string()
    }

    /// Get item level (not available in template)
    pub fn get_level(&self) -> Option<i32> {
        None // Level not available in item_template
    }

    /// Get item icon ID
    pub fn get_icon_id(&self) -> Option<i32> {
        self.template.as_ref().map(|t| t.icon_id as i32)
    }

    /// Get item part
    pub fn get_part(&self) -> Option<i32> {
        self.template.as_ref().map(|t| t.part as i32)
    }

    /// Get item gold cost
    pub fn get_gold(&self) -> Option<i64> {
        self.template.as_ref().map(|t| t.gold as i64)
    }

    /// Get item gem cost
    pub fn get_gem(&self) -> Option<i32> {
        self.template.as_ref().map(|t| t.gem as i32)
    }

    /// Get item ruby cost (not available in template)
    pub fn get_ruby(&self) -> Option<i32> {
        None // Ruby not available in item_template
    }

    /// Get item strength requirement
    pub fn get_str_require(&self) -> Option<i32> {
        self.template.as_ref().map(|t| t.power_require as i32)
    }

    /// Check if player can use this item based on strength requirement
    pub fn can_use(&self, player_str: i32) -> bool {
        if let Some(str_require) = self.get_str_require() {
            player_str >= str_require
        } else {
            true
        }
    }

    /// Get item description
    pub fn get_description(&self) -> String {
        if let Some(ref template) = self.template {
            template.description.clone()
        } else {
            String::new()
        }
    }

    /// Get item gender requirement
    pub fn get_gender(&self) -> Option<i32> {
        self.template.as_ref().map(|t| t.gender as i32)
    }

    /// Check if item matches gender requirement
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
