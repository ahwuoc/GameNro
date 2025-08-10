use crate::item::Item;
use crate::item::ItemOption;

pub struct ItemUtils;

impl ItemUtils {
    pub fn calculate_item_value(item: &Item) -> i64 {
        if let Some(template) = &item.template {
            let base_value = template.gold as i64; // Use gold field instead of price
            let quantity_multiplier = item.quantity as i64;
            let option_bonus = Self::calculate_option_value(&item.item_options);
            
            base_value * quantity_multiplier + option_bonus
        } else {
            0
        }
    }

    pub fn calculate_option_value(options: &[ItemOption]) -> i64 {
        let mut total_value = 0i64;
        
        for option in options {
            let option_value = match option.option_id {
                1..=2 => option.param as i64 * 10, // HP/MP
                3..=4 => option.param as i64 * 50, // Attack/Defense
                5..=9 => option.param as i64 * 25, // Stats
                10..=15 => option.param as i64 * 100, // Skills
                _ => option.param as i64,
            };
            total_value += option_value;
        }
        
        total_value
    }

    pub fn get_item_rarity(item: &Item) -> ItemRarity {
        if let Some(template) = &item.template {
            match template.id % 10 {
                0..=2 => ItemRarity::Common,
                3..=4 => ItemRarity::Uncommon,
                5..=6 => ItemRarity::Rare,
                7..=8 => ItemRarity::Epic,
                _ => ItemRarity::Legendary,
            }
        } else {
            ItemRarity::Common
        }
    }

    pub fn is_equipment(item: &Item) -> bool {
        if let Some(template) = &item.template {
            let equipment_types = vec![0, 1, 2, 3, 4, 5, 6, 7, 8, 9];
            equipment_types.contains(&(template.r#type as i32))
        } else {
            false
        }
    }

    pub fn is_consumable(item: &Item) -> bool {
        if let Some(template) = &item.template {
            let consumable_types = vec![10, 11, 12, 13, 14, 15];
            consumable_types.contains(&(template.r#type as i32))
        } else {
            false
        }
    }

    pub fn is_material(item: &Item) -> bool {
        if let Some(template) = &item.template {
            // Material types: crafting materials, etc.
            let material_types = vec![20, 21, 22, 23, 24, 25];
            material_types.contains(&(template.r#type as i32))
        } else {
            false
        }
    }

    pub fn can_stack(item: &Item) -> bool {
        if let Some(template) = &item.template {
            // Items that can be stacked
            template.id == 457 || template.id == 590 || template.id == 610 ||
            template.r#type == 14 || template.id == 933 || template.id == 934 ||
            template.id == 537 || template.id == 538 || template.id == 539 ||
            template.id == 541 || template.id == 542 || template.id == 2069 ||
            template.id == 540 || (template.id >= 1268 && template.id <= 1273)
        } else {
            false
        }
    }

    pub fn get_max_stack_size(item: &Item) -> i32 {
        if Self::can_stack(item) {
            if let Some(template) = &item.template {
                match template.id {
                    457 => 9999999, // Special item
                    590 => 9999999, // Special item
                    610 => 9999999, // Special item
                    _ => 999, // Default stack size
                }
            } else {
                999
            }
        } else {
            1
        }
    }

    pub fn can_combine_items(item1: &Item, item2: &Item) -> bool {
        if let (Some(template1), Some(template2)) = (&item1.template, &item2.template) {
            // Check if items are the same type and can be combined
            template1.id == template2.id && Self::can_stack(item1)
        } else {
            false
        }
    }

    pub fn combine_items(item1: &Item, item2: &Item) -> Option<Item> {
        if Self::can_combine_items(item1, item2) {
            let mut combined = item1.clone();
            let max_stack = Self::get_max_stack_size(&combined);
            let new_quantity = (combined.quantity + item2.quantity).min(max_stack);
            combined.quantity = new_quantity;
            Some(combined)
        } else {
            None
        }
    }

    pub fn split_item(item: &Item, split_quantity: i32) -> Option<(Item, Item)> {
        if item.quantity > split_quantity && split_quantity > 0 {
            let mut original = item.clone();
            let mut split = item.clone();
            
            original.quantity = item.quantity - split_quantity;
            split.quantity = split_quantity;
            
            Some((original, split))
        } else {
            None
        }
    }

    pub fn get_item_requirements(item: &Item) -> ItemRequirements {
        if let Some(template) = &item.template {
            ItemRequirements {
                level: 0, // TODO: Use proper level field when available
                gender: template.gender as i32,
                class: 0, // TODO: Use proper class field when available
            }
        } else {
            ItemRequirements {
                level: 0,
                gender: 0,
                class: 0,
            }
        }
    }

    pub fn can_use_item(item: &Item, player_level: i32, player_gender: i32, player_class: i32) -> bool {
        let requirements = Self::get_item_requirements(item);
        
        player_level >= requirements.level &&
        (requirements.gender == 0 || requirements.gender == player_gender) &&
        (requirements.class == 0 || requirements.class == player_class)
    }

    pub fn get_item_effect_description(item: &Item) -> String {
        if let Some(template) = &item.template {
            match template.r#type {
                0..=9 => "Equipment item".to_string(),
                10..=15 => "Consumable item".to_string(),
                20..=25 => "Material item".to_string(),
                _ => "Unknown item type".to_string(),
            }
        } else {
            "No effect".to_string()
        }
    }

    pub fn get_item_trade_info(item: &Item) -> ItemTradeInfo {
        if let Some(template) = &item.template {
            ItemTradeInfo {
                can_trade: true, 
                can_sell: true, 
                can_drop: true, 
                price: template.gold,
                sell_price: template.gold / 2,
            }
        } else {
            ItemTradeInfo {
                can_trade: false,
                can_sell: false,
                can_drop: false,
                price: 0,
                sell_price: 0,
            }
        }
    }
}

#[derive(Debug, Clone, PartialEq)]
pub enum ItemRarity {
    Common,
    Uncommon,
    Rare,
    Epic,
    Legendary,
}

#[derive(Debug, Clone)]
pub struct ItemRequirements {
    pub level: i32,
    pub gender: i32,
    pub class: i32,
}

#[derive(Debug, Clone)]
pub struct ItemTradeInfo {
    pub can_trade: bool,
    pub can_sell: bool,
    pub can_drop: bool,
    pub price: i32,
    pub sell_price: i32,
}
