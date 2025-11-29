//! `SeaORM` Entity. Generated manually from `player` table

use chrono::Utc;
use sea_orm::entity::prelude::*;
use serde::{Deserialize, Serialize};

#[derive(Clone, Debug, PartialEq, DeriveEntityModel, Serialize, Deserialize)]
#[sea_orm(table_name = "player")]
pub struct Model {
    #[sea_orm(primary_key)]
    pub id: i32,
    
    #[sea_orm(unique)]
    pub account_id: Option<i32>,
    
    pub name: String,
    pub head: i16,
    pub gender: i32,
    pub have_tennis_space_ship: Option<bool>,
    pub clan_id: i32,
    pub data_inventory: String,
    pub data_location: String,
    pub data_point: String,
    pub data_magic_tree: String,
    pub items_body: String,
    pub items_bag: String,
    pub items_box: String,
    pub items_box_lucky_round: String,
    pub items_daban: String,
    pub friends: String,
    pub enemies: String,
    pub data_intrinsic: String,
    pub data_item_time: String,
    pub data_task: String,
    pub data_mabu_egg: String,
    pub data_charm: String,
    pub skills: String,
    pub skills_shortcut: String,
    pub pet: String,
    pub data_black_ball: String,
    pub data_side_task: String,
    pub create_time: chrono::DateTime<Utc>,
    pub notify: Option<String>,
    pub baovetaikhoan: String,
    pub captcha: String,
    pub data_card: String,
    pub lasttimepkcommeson: i64,
    pub bandokhobau: String,
    pub doanhtrai: i64,
    pub conduongrandoc: String,
    #[sea_orm(column_name = "masterDoesNotAttack")]
    pub master_does_not_attack: String,
    pub nhanthoivang: String,
    pub ruonggo: String,
    pub sieuthanthuy: String,
    pub vodaisinhtu: String,
    pub rongxuong: i64,
    pub data_item_event: String,
    pub data_luyentap: String,
    pub data_clan_task: String,
    pub data_vip: Option<String>,
    pub rank: i32,
    pub data_achievement: String,
    pub giftcode: String,
    pub event_point: i32,
    pub data_event: Option<String>,
    #[sea_orm(column_name = "dataBadges")]
    pub data_badges: Option<String>,
    #[sea_orm(column_name = "dataTaskBadges")]
    pub data_task_badges: Option<String>,
    #[sea_orm(column_name = "firstTimeLogin")]
    pub first_time_login: chrono::DateTime<Utc>,
    #[sea_orm(column_name = "BoughtSkill")]
    pub bought_skill: Option<String>,
    #[sea_orm(column_name = "LearnSkill")]
    pub learn_skill: Option<String>,
    #[sea_orm(column_name = "dailyGift")]
    pub daily_gift: Option<String>,
}

#[derive(Copy, Clone, Debug, EnumIter, DeriveRelation)]
pub enum Relation {
    #[sea_orm(
        belongs_to = "super::account::Entity",
        from = "Column::AccountId",
        to = "super::account::Column::Id"
    )]
    Account,
}

impl Related<super::account::Entity> for Entity {
    fn to() -> RelationDef {
        Relation::Account.def()
    }
}

impl ActiveModelBehavior for ActiveModel {}
