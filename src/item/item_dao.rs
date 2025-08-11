use crate::entities::item_option_template;
use crate::entities::item_template;
use crate::item::item;
use crate::item::Item;
use crate::item::ItemOption;
use sea_orm::*;

pub struct ItemDao;

impl ItemDao {
    pub async fn get_all_item_templates(
        db: &DatabaseConnection,
    ) -> Result<Vec<item_template::Model>, DbErr> {
        let templates = item_template::Entity::find().all(db).await?;
        Ok(templates)
    }
    pub async fn get_all_item_option_templates(
        database: &DatabaseConnection,
    ) -> Result<Vec<item_option_template::Model>, DbErr> {
        let templates_options = item_option_template::Entity::find().all(database).await?;
        Ok(templates_options)
    }
}
