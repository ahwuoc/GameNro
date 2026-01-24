#![allow(dead_code)]
use crate::templates::{item_template_manager, option_template_manager};

#[derive(Debug, Clone)]
pub struct ItemOption {
    pub option_id: i8,
    pub param: i16,
}

impl ItemOption {
    pub fn new(option_id: i8, param: i16) -> Self {
        Self { option_id, param }
    }
    pub fn get_option_id(&self) -> i8 {
        self.option_id
    }

    pub fn get_param(&self) -> i16 {
        self.param
    }

    pub fn new_null() -> Self {
        Self {
            option_id: 73,
            param: 0,
        }
    }
    pub fn set_param(&mut self, param: i16) {
        self.param = param;
    }

    pub fn is_valid(&self) -> bool {
        self.option_id > 0 && self.param >= 0
    }

    pub fn get_name(&self) -> String {
        match option_template_manager::get(self.get_option_id()) {
            Some(opt) => opt.name.to_string(),
            None => format!("Error name {}", self.get_option_id()),
        }
    }
}
