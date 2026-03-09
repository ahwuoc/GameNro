//! `SeaORM` Entity.
use sea_orm::entity::prelude::*;

#[derive(Clone, Debug, PartialEq, DeriveEntityModel, Eq)]
#[sea_orm(table_name = "clan")]
pub struct Model {
    #[sea_orm(primary_key)]
    pub id: i32,
    #[sea_orm(column_name = "NAME")]
    pub name: String,
    #[sea_orm(column_name = "NAME_2")]
    pub name_2: String,
    pub slogan: String,
    pub img_id: i32,
    pub power_point: i64,
    pub max_member: i16,
    pub clan_point: i32,
    #[sea_orm(column_name = "LEVEL")]
    pub level: i32,
    #[sea_orm(column_type = "Text")]
    pub members: String,
    #[sea_orm(column_type = "Text")]
    pub tops: String,
    pub create_time: DateTimeLocal,
}

#[derive(Copy, Clone, Debug, EnumIter, DeriveRelation)]
pub enum Relation {}

impl ActiveModelBehavior for ActiveModel {}
