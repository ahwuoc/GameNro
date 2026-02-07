//! `SeaORM` Entity.
use sea_orm::entity::prelude::*;

#[derive(Clone, Debug, PartialEq, DeriveEntityModel, Eq)]
#[sea_orm(table_name = "fusion_template")]
pub struct Model {
    #[sea_orm(primary_key, auto_increment = false)]
    pub id: i32,
    pub name: String,
    pub fusion_type: i8,
    pub data_0: Option<String>,
    pub data_1: Option<String>,
    pub data_2: Option<String>,
    pub hp_percent: i8,
    pub mp_percent: i8,
    pub dame_percent: i8,
    pub crit_bonus: i8,
    pub is_permanent: bool,
}

#[derive(Copy, Clone, Debug, EnumIter, DeriveRelation)]
pub enum Relation {}

impl ActiveModelBehavior for ActiveModel {}
