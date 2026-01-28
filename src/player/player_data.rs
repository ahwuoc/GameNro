//! Data structures for player serialization/deserialization
#![allow(dead_code)]

use serde::{Deserialize, Serialize};

// ============================================
// Skill Data
// ============================================

#[derive(Deserialize, Serialize, Debug)]
#[serde(rename_all = "camelCase")]
pub struct SkillData {
    pub template_id: i32,
    #[serde(default)]
    pub skill_id: i16,
    pub point: i32,
    pub last_time_use: u64,
    pub curr_level: i16,
}

// ============================================
// Intrinsic Data
// ============================================

#[derive(Debug, Deserialize, Serialize, Default)]
pub struct IntrinsicData {
    #[serde(default)]
    pub intrinsic_id: i32,
    #[serde(default)]
    pub param1: i16,
    #[serde(default)]
    pub param2: i16,
    #[serde(default)]
    pub count_open: i8,
}

// ============================================
// Point Data (Player Stats)
// ============================================

#[derive(Debug, Deserialize, Serialize, Default)]
pub struct PointData {
    #[serde(default, rename = "limitPower")]
    pub limit_power: i8,
    #[serde(default)]
    pub power: i64,
    #[serde(default, rename = "tiemNang")]
    pub tiem_nang: i64,
    #[serde(default)]
    pub stamina: i16,
    #[serde(default, rename = "maxStamina")]
    pub max_stamina: i16,
    #[serde(default, rename = "hpg")]
    pub hp_goc: i32,
    #[serde(default, rename = "mpg")]
    pub mp_goc: i32,
    #[serde(default, rename = "dameg")]
    pub damege_goc: i32,
    #[serde(default, rename = "defg")]
    pub defen_goc: i32,
    #[serde(default, rename = "critg")]
    pub crit_goc: i8,
    #[serde(default)]
    pub crit_max: i8,
    #[serde(default, rename = "nangDong")]
    pub nang_dong: i32,
    #[serde(default, rename = "plHp")]
    pub pl_hp: i32,
    #[serde(default, rename = "plMp")]
    pub pl_mp: i32,
}

// ============================================
// Inventory Data
// ============================================

#[derive(Debug, Deserialize, Serialize, Default)]
pub struct InventoryData {
    #[serde(default)]
    pub gold: i64,
    #[serde(default)]
    pub gem: i32,
    #[serde(default)]
    pub ruby: i32,
}

// ============================================
// Item Data (for JSON serialization)
// ============================================

#[derive(Deserialize, Serialize)]
pub struct ItemOptionJson {
    pub id: i32,
    pub value: i32,
}

#[derive(Deserialize, Serialize)]
pub struct ItemDataJson {
    pub id: i32,
    #[serde(default)]
    pub quantity: i32,
    #[serde(default)]
    pub options: Vec<ItemOptionJson>,
    #[serde(default, rename = "createTime")]
    pub create_time: i64,
}

// ============================================
// Item Data (internal parsed format)
// ============================================

pub struct ItemDataParsed {
    pub template_id: i16,
    pub quantity: i32,
    pub options: Vec<(i8, i16)>,
    pub created: i64,
}
