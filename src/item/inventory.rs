#![allow(dead_code)]
use crate::{constant, item::item::Item};

#[derive(Debug, Clone)]
pub struct Inventory {
    pub gold: i64,
    pub gem: i32,
    pub ruby: i32,
    pub coupon: i32,
    pub event: i32,

    // Items
    pub items_body: Vec<Item>,
    pub items_bag: Vec<Item>,
    pub items_box: Vec<Item>,
    pub items_box_crack_ball: Vec<Item>,
    pub train_armor: Option<Item>,
}

impl Inventory {
    pub fn new() -> Self {
        Self {
            gold: 0,
            gem: 0,
            ruby: 0,
            coupon: 0,
            event: 0,
            items_body: Vec::new(),
            items_bag: Vec::new(),
            items_box: Vec::new(),
            items_box_crack_ball: Vec::new(),
            train_armor: None,
        }
    }

    // ============Set==========
    pub fn set_gem(&mut self, amount: i32) {
        self.gem = amount.min(constant::limit::LIMIT_GEM);
    }

    pub fn set_ruby(&mut self, amount: i32) {
        self.ruby = amount.min(constant::limit::LIMIT_RUBY);
    }

    pub fn set_gold(&mut self, amount: i64) {
        self.gold = amount.min(constant::limit::LIMIT_GOLD);
    }

    pub fn get_gem(&self) -> i32 {
        self.gem
    }

    pub fn get_ruby(&self) -> i32 {
        self.ruby
    }

    pub fn get_gold(&self) -> i64 {
        self.gold
    }

    pub fn add_gold(&mut self, amount: i64) {
        self.gold = self
            .gold
            .saturating_add(amount)
            .min(crate::constant::limit::LIMIT_GOLD);
    }

    pub fn sub_gold(&mut self, amount: i64) -> bool {
        if self.gold < amount {
            return false;
        }
        self.gold = self.gold.saturating_sub(amount).max(0);
        true
    }

    pub fn add_gem(&mut self, amount: i32) {
        self.gem = self
            .gem
            .saturating_add(amount)
            .min(constant::limit::LIMIT_GEM);
    }

    pub fn sub_gem(&mut self, amount: i32) -> bool {
        if self.gem < amount {
            return false;
        }
        self.gem = self.gem.saturating_sub(amount).max(0);
        true
    }

    pub fn add_ruby(&mut self, amount: i32) {
        self.ruby = self
            .ruby
            .saturating_add(amount)
            .min(constant::limit::LIMIT_RUBY);
    }

    pub fn sub_ruby(&mut self, amount: i32) {
        self.ruby = self.ruby.saturating_sub(amount).max(0);
    }

    pub fn clear_all_items(&mut self) {
        self.items_body.clear();
        self.items_bag.clear();
        self.items_box.clear();
        self.items_box_crack_ball.clear();
        self.train_armor = None;
    }
}

impl Default for Inventory {
    fn default() -> Self {
        Self::new()
    }
}
