use std::collections::HashMap;
use std::sync::Arc;
use tokio::sync::RwLock;
use once_cell::sync::Lazy;
use crate::item::Item;
use crate::item::ItemService;
use crate::entities::item_template::Model as ItemTemplate;
use crate::entities::item_option_template::Model as ItemOptionTemplate;

pub struct ItemManager {
    items: Arc<RwLock<HashMap<i32, Item>>>,
    item_service: ItemService,
}

impl ItemManager {
    pub fn new() -> Self {
        Self {
            items: Arc::new(RwLock::new(HashMap::new())),
            item_service: ItemService::new(),
        }
    }

    pub async fn create_item(&self, template_id: i32, quantity: i32) -> Result<Option<Item>, Box<dyn std::error::Error>> {
        if let Some(item) = self.item_service.create_new_item_with_quantity(template_id, quantity) {
            let mut items = self.items.write().await;
            items.insert(item.get_template_id().unwrap_or(0), item.clone());
            Ok(Some(item))
        } else {
            Ok(None)
        }
    }

    pub async fn get_item(&self, template_id: i32) -> Option<Item> {
        let items = self.items.read().await;
        items.get(&template_id).cloned()
    }

    pub async fn get_all_items(&self) -> Vec<Item> {
        let items = self.items.read().await;
        items.values().cloned().collect()
    }

    pub fn get_item_service(&self) -> &ItemService {
        &self.item_service
    }

    pub async fn update_all_items(&self) -> Result<(), Box<dyn std::error::Error>> {
        let items = self.items.read().await;
        
        for item in items.values() {
            // TODO: Implement item update logic
            // item.update();
        }
        
        Ok(())
    }

    pub async fn get_items_by_type(&self, item_type: i32) -> Vec<Item> {
        let items = self.items.read().await;
        items.values()
            .filter(|item| item.get_type() == Some(item_type))
            .cloned()
            .collect()
    }

    pub async fn get_items_by_rarity(&self, rarity: i32) -> Vec<Item> {
        let items = self.items.read().await;
        items.values()
            .filter(|item| {
                if let Some(template) = &item.template {
                    // TODO: Use proper rarity field when available
                    template.id % 10 == rarity // Temporary workaround
                } else {
                    false
                }
            })
            .cloned()
            .collect()
    }

    pub async fn search_items_by_name(&self, name_pattern: &str) -> Vec<Item> {
        let items = self.items.read().await;
        items.values()
            .filter(|item| {
                if let Some(template) = &item.template {
                    template.name.to_lowercase().contains(&name_pattern.to_lowercase())
                } else {
                    false
                }
            })
            .cloned()
            .collect()
    }

    pub async fn remove_item(&self, template_id: i32) -> bool {
        let mut items = self.items.write().await;
        items.remove(&template_id).is_some()
    }

    pub async fn get_item_count(&self) -> usize {
        let items = self.items.read().await;
        items.len()
    }

    pub async fn clear_all_items(&self) {
        let mut items = self.items.write().await;
        items.clear();
    }

    pub async fn is_item_exists(&self, template_id: i32) -> bool {
        let items = self.items.read().await;
        items.contains_key(&template_id)
    }

    pub async fn get_valuable_items(&self) -> Vec<Item> {
        let items = self.items.read().await;
        items.values()
            .filter(|item| {
                if let Some(template) = &item.template {
                    template.id >= 1000 // Temporary workaround for valuable items
                } else {
                    false
                }
            })
            .cloned()
            .collect()
    }

    pub async fn get_stackable_items(&self) -> Vec<Item> {
        let items = self.items.read().await;
        items.values()
            .filter(|item| {
                if let Some(template) = &item.template {
                    self.item_service.can_item_stack(template.id, template.r#type as i32)
                } else {
                    false
                }
            })
            .cloned()
            .collect()
    }

    pub async fn get_equipment_items(&self) -> Vec<Item> {
        let items = self.items.read().await;
        items.values()
            .filter(|item| {
                if let Some(template) = &item.template {
                    // Equipment types: weapon, armor, accessory, etc.
                    let equipment_types = vec![0, 1, 2, 3, 4, 5, 6, 7, 8, 9];
                    equipment_types.contains(&(template.r#type as i32))
                } else {
                    false
                }
            })
            .cloned()
            .collect()
    }

    pub async fn get_consumable_items(&self) -> Vec<Item> {
        let items = self.items.read().await;
        items.values()
            .filter(|item| {
                if let Some(template) = &item.template {
                    // Consumable types: potion, food, etc.
                    let consumable_types = vec![10, 11, 12, 13, 14, 15];
                    consumable_types.contains(&(template.r#type as i32))
                } else {
                    false
                }
            })
            .cloned()
            .collect()
    }
}

// Global item manager
pub static ITEM_MANAGER: Lazy<RwLock<ItemManager>> = Lazy::new(|| RwLock::new(ItemManager::new()));

impl Clone for ItemManager {
    fn clone(&self) -> Self {
        Self {
            items: Arc::clone(&self.items),
            item_service: ItemService::new(),
        }
    }
}
