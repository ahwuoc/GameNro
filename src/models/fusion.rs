use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FusionAvatarData {
    pub head: i16,
    pub body: i16,
    pub leg: i16,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FusionTemplate {
    pub id: i32,
    pub name: String,
    pub fusion_type: i8,
    pub data_0: Option<FusionAvatarData>,
    pub data_1: Option<FusionAvatarData>,
    pub data_2: Option<FusionAvatarData>,
    pub hp_percent: i8,
    pub mp_percent: i8,
    pub dame_percent: i8,
    pub crit_bonus: i8,
    pub is_permanent: bool,
}
