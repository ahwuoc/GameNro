#![allow(dead_code)]
use std::time::{SystemTime, UNIX_EPOCH};

use crate::utils;

pub struct AntiLogin {
    pub wrong_count: u32,
    pub last_wrong_time: u64,
    pub can_login: bool,
}

impl AntiLogin {
    pub fn new() -> Self {
        Self {
            wrong_count: 0,
            last_wrong_time: 0,
            can_login: true,
        }
    }

    pub fn can_login(&self) -> bool {
        self.can_login
    }

    pub fn wrong(&mut self) {
        self.wrong_count += 1;
        self.last_wrong_time = utils::time::current_time_millis();
        if self.wrong_count >= 5 {
            self.can_login = false;
        }
    }

    pub fn reset(&mut self) {
        self.wrong_count = 0;
        self.can_login = true;
    }

    pub fn get_notify_cannot_login(&self) -> String {
        if self.wrong_count >= 5 {
            "Bạn đã nhập sai quá nhiều lần, vui lòng thử lại sau!".to_string()
        } else {
            format!("Vui lòng chờ {}s", 5 - self.wrong_count)
        }
    }
}
