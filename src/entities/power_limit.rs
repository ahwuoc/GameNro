use sea_orm::entity::prelude::*;
#[derive(Clone, Debug, PartialEq, DeriveEntityModel, Eq)]
#[sea_orm(table_name = "power_limit")]
pub struct Model {
    #[sea_orm(primary_key)]
    pub id: i32,
    pub power: i64,
    pub hp: i64,
    pub mp: i64,
    pub damage: i64,
    pub defense: i32,
    pub critical: i32,
}
#[derive(Copy, Clone, Debug, EnumIter, DeriveRelation)]
pub enum Relation {}
impl ActiveModelBehavior for ActiveModel {}
