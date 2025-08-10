use crate::item::Item;
use crate::item::ItemOption;

pub struct ItemValidator;

impl ItemValidator {
    pub fn validate_item(item: &Item) -> ValidationResult {
        let mut errors = Vec::new();
        let mut warnings = Vec::new();

        // Check if item has template
        if item.template.is_none() {
            errors.push("Item has no template".to_string());
        }

        // Check quantity
        if item.quantity <= 0 {
            errors.push("Item quantity must be greater than 0".to_string());
        }

        // Check if quantity exceeds max stack
        if let Some(template) = &item.template {
            let max_stack = Self::get_max_stack_size_for_template(template.id);
            if item.quantity > max_stack {
                errors.push(format!("Item quantity {} exceeds max stack size {}", item.quantity, max_stack));
            }
        }

        // Validate options
        for (index, option) in item.item_options.iter().enumerate() {
            if let Some(option_error) = Self::validate_item_option(option) {
                errors.push(format!("Option {}: {}", index, option_error));
            }
        }

        // Check for duplicate options
        let mut option_ids: Vec<i32> = item.item_options.iter().map(|opt| opt.option_id).collect();
        option_ids.sort();
        option_ids.dedup();
        if option_ids.len() != item.item_options.len() {
            warnings.push("Item has duplicate options".to_string());
        }

        // Check option count limit
        if item.item_options.len() > 5 {
            errors.push("Item has too many options (max 5)".to_string());
        }

        ValidationResult {
            is_valid: errors.is_empty(),
            errors,
            warnings,
        }
    }

    pub fn validate_item_option(option: &ItemOption) -> Option<String> {
        if option.option_id < 0 {
            return Some("Option ID must be non-negative".to_string());
        }

        if option.param < 0 {
            return Some("Option parameter must be non-negative".to_string());
        }

        // Check if option ID is valid
        if !Self::is_valid_option_id(option.option_id) {
            return Some(format!("Invalid option ID: {}", option.option_id));
        }

        // Check if parameter is within valid range for this option
        if let Some(max_param) = Self::get_max_param_for_option(option.option_id) {
            if option.param > max_param {
                return Some(format!("Option parameter {} exceeds maximum {} for option ID {}", 
                                   option.param, max_param, option.option_id));
            }
        }

        None
    }

    pub fn is_valid_option_id(option_id: i32) -> bool {
        matches!(option_id, 0..=15)
    }

    pub fn get_max_param_for_option(option_id: i32) -> Option<i32> {
        match option_id {
            0 => Some(0), // None
            1..=2 => Some(1000), // HP/MP
            3..=4 => Some(100), // Attack/Defense
            5..=9 => Some(50), // Stats
            10..=15 => Some(100), // Skills
            _ => None,
        }
    }

    pub fn get_max_stack_size_for_template(template_id: i32) -> i32 {
        match template_id {
            457 => 9999999, // Special item
            590 => 9999999, // Special item
            610 => 9999999, // Special item
            933 => 9999999, // Special item
            934 => 9999999, // Special item
            537..=542 => 999, // Stackable items
            2069 => 999, // Stackable item
            540 => 999, // Stackable item
            1268..=1273 => 999, // Stackable items
            _ => 1, // Default non-stackable
        }
    }

    pub fn validate_item_name(name: &str) -> ValidationResult {
        let mut errors = Vec::new();
        let mut warnings = Vec::new();

        if name.is_empty() {
            errors.push("Item name cannot be empty".to_string());
        }

        if name.len() > 50 {
            errors.push("Item name too long (max 50 characters)".to_string());
        }

        if name.len() < 2 {
            warnings.push("Item name very short".to_string());
        }

        // Check for invalid characters
        if name.chars().any(|c| !c.is_alphanumeric() && c != ' ' && c != '-' && c != '_') {
            warnings.push("Item name contains special characters".to_string());
        }

        ValidationResult {
            is_valid: errors.is_empty(),
            errors,
            warnings,
        }
    }

    pub fn validate_item_price(price: i32) -> ValidationResult {
        let mut errors = Vec::new();
        let mut warnings = Vec::new();

        if price < 0 {
            errors.push("Item price cannot be negative".to_string());
        }

        if price > 999999999 {
            errors.push("Item price too high (max 999,999,999)".to_string());
        }

        if price == 0 {
            warnings.push("Item has zero price".to_string());
        }

        ValidationResult {
            is_valid: errors.is_empty(),
            errors,
            warnings,
        }
    }

    pub fn validate_item_level(level: i32) -> ValidationResult {
        let mut errors = Vec::new();
        let mut warnings = Vec::new();

        if level < 0 {
            errors.push("Item level cannot be negative".to_string());
        }

        if level > 100 {
            errors.push("Item level too high (max 100)".to_string());
        }

        if level > 50 {
            warnings.push("Item level very high".to_string());
        }

        ValidationResult {
            is_valid: errors.is_empty(),
            errors,
            warnings,
        }
    }

    pub fn can_item_be_equipped(item: &Item, player_level: i32, player_gender: i32, player_class: i32) -> ValidationResult {
        let mut errors = Vec::new();
        let mut warnings = Vec::new();

        if let Some(template) = &item.template {
            // Check gender requirement
            if template.gender != 0 && template.gender != player_gender as i16 {
                errors.push(format!("Item requires gender {} but player is gender {}", template.gender, player_gender));
            }
            if !Self::is_equipment_type(template.r#type as i32) {
                errors.push("Item is not equipment type".to_string());
            }
        } else {
            errors.push("Item has no template".to_string());
        }

        ValidationResult {
            is_valid: errors.is_empty(),
            errors,
            warnings,
        }
    }

    pub fn is_equipment_type(item_type: i32) -> bool {
        matches!(item_type, 0..=9)
    }

    pub fn is_consumable_type(item_type: i32) -> bool {
        matches!(item_type, 10..=15)
    }

    pub fn is_material_type(item_type: i32) -> bool {
        matches!(item_type, 20..=25)
    }
}

#[derive(Debug, Clone)]
pub struct ValidationResult {
    pub is_valid: bool,
    pub errors: Vec<String>,
    pub warnings: Vec<String>,
}

impl ValidationResult {
    pub fn new() -> Self {
        Self {
            is_valid: true,
            errors: Vec::new(),
            warnings: Vec::new(),
        }
    }

    pub fn add_error(&mut self, error: String) {
        self.errors.push(error);
        self.is_valid = false;
    }

    pub fn add_warning(&mut self, warning: String) {
        self.warnings.push(warning);
    }

    pub fn has_errors(&self) -> bool {
        !self.errors.is_empty()
    }

    pub fn has_warnings(&self) -> bool {
        !self.warnings.is_empty()
    }

    pub fn get_error_summary(&self) -> String {
        if self.errors.is_empty() {
            "No errors".to_string()
        } else {
            format!("{} errors: {}", self.errors.len(), self.errors.join(", "))
        }
    }

    pub fn get_warning_summary(&self) -> String {
        if self.warnings.is_empty() {
            "No warnings".to_string()
        } else {
            format!("{} warnings: {}", self.warnings.len(), self.warnings.join(", "))
        }
    }
}
