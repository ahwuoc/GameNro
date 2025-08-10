use sea_orm::*;
use crate::entities::item_template;
use crate::entities::item_option_template;
use crate::item::Item;
use crate::item::ItemOption;

pub struct ItemDao;

impl ItemDao {
    pub async fn load_item_template(
        database: &DatabaseConnection,
        template_id: i32,
    ) -> Result<Option<item_template::Model>, DbErr> {
        let template = item_template::Entity::find_by_id(template_id)
            .one(database)
            .await?;

        Ok(template)
    }

    pub async fn load_all_item_templates(
        database: &DatabaseConnection,
    ) -> Result<Vec<item_template::Model>, DbErr> {
        let templates = item_template::Entity::find().all(database).await?;
        Ok(templates)
    }

    pub async fn load_item_option_template(
        database: &DatabaseConnection,
        template_id: i32,
    ) -> Result<Option<item_option_template::Model>, DbErr> {
        let template = item_option_template::Entity::find_by_id(template_id)
            .one(database)
            .await?;

        Ok(template)
    }

    pub async fn load_all_item_option_templates(
        database: &DatabaseConnection,
    ) -> Result<Vec<item_option_template::Model>, DbErr> {
        let templates = item_option_template::Entity::find().all(database).await?;
        Ok(templates)
    }

    pub async fn create_item_from_template(
        database: &DatabaseConnection,
        template: &item_template::Model,
        quantity: i32,
    ) -> Result<Item, Box<dyn std::error::Error>> {
        let item = Item::with_template(template.clone(), quantity);
        Ok(item)
    }

    pub async fn save_item_state(
        database: &DatabaseConnection,
        item: &Item,
    ) -> Result<(), Box<dyn std::error::Error>> {
        // TODO: Implement saving item state to database if needed
        // This could be used for persistent item data
        Ok(())
    }

    pub async fn load_item_state(
        database: &DatabaseConnection,
        item_id: i32,
    ) -> Result<Option<Item>, Box<dyn std::error::Error>> {
        // TODO: Implement loading item state from database if needed
        Ok(None)
    }

    pub async fn get_items_by_type(
        database: &DatabaseConnection,
        item_type: i32,
    ) -> Result<Vec<item_template::Model>, DbErr> {
        let templates = item_template::Entity::find()
            .filter(item_template::Column::Type.eq(item_type))
            .all(database)
            .await?;
        Ok(templates)
    }

    pub async fn get_items_by_rarity(
        database: &DatabaseConnection,
        rarity: i32,
    ) -> Result<Vec<item_template::Model>, DbErr> {
        let templates = item_template::Entity::find().all(database).await?;
        Ok(templates)
    }

    pub async fn search_items_by_name(
        database: &DatabaseConnection,
        name_pattern: &str,
    ) -> Result<Vec<item_template::Model>, DbErr> {
        let templates = item_template::Entity::find()
            .filter(item_template::Column::Name.like(format!("%{}%", name_pattern)))
            .all(database)
            .await?;
        Ok(templates)
    }

    pub async fn get_item_option_templates_by_item_id(
        database: &DatabaseConnection,
        item_id: i32,
    ) -> Result<Vec<item_option_template::Model>, DbErr> {
        // TODO: Implement item_id filtering when the column is available
        // For now, return all option templates
        let templates = item_option_template::Entity::find().all(database).await?;
        Ok(templates)
    }

    pub async fn create_item_option(
        database: &DatabaseConnection,
        option_template: &item_option_template::Model,
        param: i32,
    ) -> Result<ItemOption, Box<dyn std::error::Error>> {
        let option = ItemOption::new(option_template.id, param);
        Ok(option)
    }

    pub async fn save_item_options(
        database: &DatabaseConnection,
        item_id: i32,
        options: &[ItemOption],
    ) -> Result<(), Box<dyn std::error::Error>> {
        // TODO: Implement saving item options to database
        Ok(())
    }

    pub async fn load_item_options(
        database: &DatabaseConnection,
        item_id: i32,
    ) -> Result<Vec<ItemOption>, Box<dyn std::error::Error>> {
        // TODO: Implement loading item options from database
        Ok(Vec::new())
    }
}
