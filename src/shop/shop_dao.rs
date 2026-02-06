use sea_orm::{ColumnTrait, EntityTrait, QueryFilter, QueryOrder};

use crate::database;
use crate::entities::shop;
pub struct ShopDao;

impl ShopDao {
    pub async fn get_shop_by_npc_id(npc_id: i32) -> anyhow::Result<Vec<shop::Model>> {
        let db = database::DbManager::get_pool();
        let shops = shop::Entity::find()
            .filter(shop::Column::NpcId.eq(npc_id))
            .filter(shop::Column::Status.eq(true))
            .order_by_asc(shop::Column::SortOrder)
            .all(db)
            .await?;
        Ok(shops)
    }
}
