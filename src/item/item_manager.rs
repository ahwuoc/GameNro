use crate::entities::item_option_template::Model as ItemOptionTemplate;
use crate::entities::item_template::Model as ItemTemplate;
use crate::item;
use crate::item::Item;
use crate::item::ItemDao;
use crate::item::ItemService;
use once_cell::sync::Lazy;
use sea_orm::DatabaseConnection;
use std::collections::HashMap;
use std::sync::Arc;
use tokio::sync::RwLock;

pub struct ItemManager {
    item_templates: Arc<RwLock<HashMap<i32, ItemTemplate>>>,
    item_service: ItemService,
}

impl ItemManager {
    pub fn new() -> Self {
        Self {
            item_templates: Arc::new(RwLock::new(HashMap::new())),
            item_service: ItemService::new(),
        }
    }
    
    pub async fn load_from_db(
        &self,
        db: &DatabaseConnection,
    ) -> Result<(), Box<dyn std::error::Error>> {
        let templates = ItemDao::get_all_item_templates(db).await?;
        let mut templates_lock = self.item_templates.write().await;
        
        for template in templates {
            templates_lock.insert(template.id, template);
        }
        Ok(())
    }

    pub async fn get_all_templates(&self) -> Vec<crate::entities::item_template::Model> {
        let templates = self.item_templates.read().await;
        templates.values().cloned().collect()
    }
}
   
pub static ITEM_MANAGER: Lazy<RwLock<ItemManager>> = Lazy::new(|| RwLock::new(ItemManager::new()));

impl Clone for ItemManager {
    fn clone(&self) -> Self {
        Self {
            item_templates: Arc::clone(&self.item_templates),
            item_service: ItemService::new(),
        }
    }
}
