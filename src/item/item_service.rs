use crate::item::item::Item;
use crate::templates::item_template_manager;
pub struct ItemService;

impl ItemService {
    pub fn create_item_null() -> Item {
        Item::new()
    }
    pub fn create_new_item(template_id: i16) -> Option<Item> {
        Self::create_new_item_with_quantity(template_id, 1)
    }

    pub fn create_new_item_with_quantity(template_id: i16, quantity: i32) -> Option<Item> {
        if let Some(item_template) = item_template_manager::get(template_id) {
            Some(Item::with_template(item_template.clone(), quantity))
        } else {
            tracing::warn!("Warning: Item template not found for ID: {}", template_id);
            None
        }
    }

    pub fn can_item_stack(template_id: i32, item_type: i32) -> bool {
        template_id == 457
            || template_id == 590
            || template_id == 610
            || item_type == 14
            || item_type == 933
            || item_type == 934
            || template_id == 537
            || template_id == 538
            || item_type == 539
            || item_type == 541
            || item_type == 542
            || template_id == 2069
            || item_type == 540
            || (template_id >= 1268 && template_id <= 1273)
    }
}
