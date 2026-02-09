//! `SeaORM` Entity. Generated manually from `account` table

use chrono::Utc;
use sea_orm::entity::prelude::*;
use serde::{Deserialize, Serialize};

#[derive(Clone, Debug, PartialEq, DeriveEntityModel, Serialize, Deserialize)]
#[sea_orm(table_name = "account")]
pub struct Model {
    #[sea_orm(primary_key)]
    pub id: i32,

    #[sea_orm(unique)]
    pub username: String,

    pub password: String,
    pub email: String,
    pub create_time: Option<chrono::DateTime<Utc>>,
    pub update_time: Option<chrono::DateTime<Utc>>,
    pub ban: bool,
    pub is_admin: bool,
    pub last_time_login: chrono::DateTime<Utc>,
    pub last_time_logout: chrono::DateTime<Utc>,
    pub ip_address: Option<String>,
    pub active: i32,
    pub server_login: i32,
    pub bd_player: Option<f64>,
    pub is_gift_box: Option<bool>,
    pub gift_time: Option<String>,
    pub vnd: i32,
    pub tongnap: i32,
}

#[derive(Copy, Clone, Debug, EnumIter, DeriveRelation)]
pub enum Relation {
    #[sea_orm(has_many = "super::player::Entity")]
    Player,
}

impl Related<super::player::Entity> for Entity {
    fn to() -> RelationDef {
        Relation::Player.def()
    }
}

impl ActiveModelBehavior for ActiveModel {}
