use crate::item::item::Item;
use crate::item::{item_manager, option_template_manager};
pub struct ItemService;

impl ItemService {
    pub fn create_item_null() -> Item {
        Item::new()
    }
    pub fn create_new_item(template_id: i16) -> Option<Item> {
        Self::create_new_item_with_quantity(template_id, 1)
    }

    pub fn create_new_item_with_quantity(template_id: i16, quantity: i32) -> Option<Item> {
        if let Some(item_template) = item_manager::get(template_id){
              Some(Item::with_template(item_template.clone(), quantity))
        }else{
            println!("Warning: Item template not found for ID: {}", template_id);
            None
        }
    }
    pub fn random_skh_id(gender: i32) -> i32 {
        let adjusted_gender = if gender == 3 { 2 } else { gender };

        let options = vec![
            vec![128, 129, 127], // Male
            vec![130, 131, 132], // Female
            vec![133, 135, 134], // Neutral
        ];

        let skh_v1 = 25; // 25% chance
        let skh_v2 = 35; // 35% chance
        let skh_c = 40; // 40% chance

        let random = (std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap()
            .as_millis()
            % 100) as i32;

        let skh_id = if random <= skh_v1 {
            0
        } else if random <= skh_v1 + skh_v2 {
            1
        } else if random <= skh_v1 + skh_v2 + skh_c {
            2
        } else {
            0
        };

        if adjusted_gender < options.len() as i32
            && skh_id < options[adjusted_gender as usize].len() as i32
        {
            options[adjusted_gender as usize][skh_id as usize]
        } else {
            127 // Default
        }
    }

    /// Get option ID for SKH
    pub fn option_id_skh(skh_id: i32) -> i32 {
        // Map SKH ID to option ID
        match skh_id {
            127 => 30, // SKH V1
            128 => 31, // SKH V1
            129 => 32, // SKH V1
            130 => 33, // SKH V2
            131 => 34, // SKH V2
            132 => 35, // SKH V2
            133 => 36, // SKH C
            134 => 37, // SKH C
            135 => 38, // SKH C
            _ => 30,   // Default
        }
    }

    pub fn is_item_activation(_item: &Item) -> bool {
        false
    }


    pub fn get_all_item_option_templates_count() -> usize {
        option_template_manager::get_all().len()
    }

    pub fn can_item_stack(template_id: i32, item_type: i32) -> bool {
        // Items that can be stacked
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
