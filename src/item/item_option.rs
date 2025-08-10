#[derive(Debug, Clone)]
pub struct ItemOption {
    pub option_id: i32,
    pub param: i32,
}

impl ItemOption {
    pub fn new(option_id: i32, param: i32) -> Self {
        Self {
            option_id,
            param,
        }
    }

    pub fn get_option_id(&self) -> i32 {
        self.option_id
    }

    pub fn get_param(&self) -> i32 {
        self.param
    }

    pub fn set_param(&mut self, param: i32) {
        self.param = param;
    }

    pub fn is_valid(&self) -> bool {
        self.option_id > 0 && self.param >= 0
    }

    pub fn get_option_name(&self) -> String {
        match self.option_id {
            0 => "None".to_string(),
            1 => "HP".to_string(),
            2 => "MP".to_string(),
            3 => "Attack".to_string(),
            4 => "Defense".to_string(),
            5 => "Speed".to_string(),
            6 => "Critical".to_string(),
            7 => "Dodge".to_string(),
            8 => "Accuracy".to_string(),
            9 => "Luck".to_string(),
            10 => "Power".to_string(),
            11 => "Skill".to_string(),
            12 => "Magic".to_string(),
            13 => "Physical".to_string(),
            14 => "Elemental".to_string(),
            15 => "Resistance".to_string(),
            _ => format!("Unknown Option {}", self.option_id),
        }
    }

    pub fn get_option_description(&self) -> String {
        match self.option_id {
            0 => "No effect".to_string(),
            1 => format!("+{} HP", self.param),
            2 => format!("+{} MP", self.param),
            3 => format!("+{} Attack", self.param),
            4 => format!("+{} Defense", self.param),
            5 => format!("+{} Speed", self.param),
            6 => format!("+{}% Critical", self.param),
            7 => format!("+{}% Dodge", self.param),
            8 => format!("+{}% Accuracy", self.param),
            9 => format!("+{}% Luck", self.param),
            10 => format!("+{} Power", self.param),
            11 => format!("+{} Skill", self.param),
            12 => format!("+{} Magic", self.param),
            13 => format!("+{} Physical", self.param),
            14 => format!("+{} Elemental", self.param),
            15 => format!("+{} Resistance", self.param),
            _ => format!("Unknown effect: +{}", self.param),
        }
    }

    pub fn is_combat_option(&self) -> bool {
        matches!(self.option_id, 1..=15)
    }

    pub fn is_stat_option(&self) -> bool {
        matches!(self.option_id, 1..=10)
    }

    pub fn is_skill_option(&self) -> bool {
        matches!(self.option_id, 11..=15)
    }

    pub fn get_option_type(&self) -> OptionType {
        match self.option_id {
            0 => OptionType::None,
            1..=2 => OptionType::Resource,
            3..=4 => OptionType::Combat,
            5..=9 => OptionType::Stat,
            10..=15 => OptionType::Skill,
            _ => OptionType::Unknown,
        }
    }

    pub fn get_option_rarity(&self) -> OptionRarity {
        match self.param {
            0..=10 => OptionRarity::Common,
            11..=25 => OptionRarity::Uncommon,
            26..=50 => OptionRarity::Rare,
            51..=100 => OptionRarity::Epic,
            _ => OptionRarity::Legendary,
        }
    }

    pub fn is_legendary_option(&self) -> bool {
        self.get_option_rarity() == OptionRarity::Legendary
    }

    pub fn is_epic_option(&self) -> bool {
        self.get_option_rarity() == OptionRarity::Epic
    }

    pub fn is_rare_option(&self) -> bool {
        self.get_option_rarity() == OptionRarity::Rare
    }

    pub fn is_uncommon_option(&self) -> bool {
        self.get_option_rarity() == OptionRarity::Uncommon
    }

    pub fn is_common_option(&self) -> bool {
        self.get_option_rarity() == OptionRarity::Common
    }
}

#[derive(Debug, Clone, PartialEq)]
pub enum OptionType {
    None,
    Resource,
    Combat,
    Stat,
    Skill,
    Unknown,
}

#[derive(Debug, Clone, PartialEq)]
pub enum OptionRarity {
    Common,
    Uncommon,
    Rare,
    Epic,
    Legendary,
}
