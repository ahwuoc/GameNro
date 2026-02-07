use serde::{Deserialize, Serialize};

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct OptionCard {
    pub id: i32,
    pub param: i32,
    pub active_card: i8,
}

#[derive(Clone, Debug)]
pub struct RadarCardTemplate {
    pub id: i16,
    pub icon_id: i16,
    pub rank: i8,
    pub max: i8,
    pub type_radar: i8,
    pub template: i16, // mob_id
    pub name: String,
    pub info: String,
    pub head: i16,
    pub body: i16,
    pub leg: i16,
    pub bag: i16,
    pub options: Vec<OptionCard>,
    pub require: i16,
    pub require_level: i16,
    pub aura_id: i16,
}

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct Card {
    pub id: i16,
    pub amount: i8,
    pub max_amount: i8,
    pub level: i8,
    pub used: i8,
    pub options: Vec<OptionCard>,
}
