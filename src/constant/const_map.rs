use std::convert::TryFrom;

// ========================================
// Player Gender
// ========================================
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
#[repr(i8)]
pub enum Gender {
    TraiDat = 0,
    Namek = 1,
    Xayda = 2,
}

// ========================================
// Map Change Types
// ========================================
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
#[repr(i32)]
pub enum ChangeMapType {
    Capsule = 0,
    BlackBall = 1,
    MaBu = 2,
}

// ========================================
// Task Thresholds / IDs
// ========================================
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
#[repr(i32)]
pub enum TaskId {
    Task1_0 = 2048,
    Task2_0 = 4096,
    Task3_0 = 6144,
    Task4_0 = 8192,
    Task7_0 = 14336,
    Task13_0 = 26624,
    Task15_0 = 30720,
    Task16_0 = 32768,
    Task18_0 = 36864,
    Task19_0 = 38912,
    Task20_0 = 40960,
    Task21_0 = 43008,
    Task24_0 = 49152,
    Task27_0 = 55296,
}

// ========================================
// Map IDs - Centralized
// ========================================
// Map IDs - Sử dụng constants thay vì enum để hỗ trợ bitwise operations
// ========================================

// --- Trái Đất ---
pub const LANG_ARU: i32 = 0;
pub const DOI_HOA_CUC: i32 = 1;
pub const THUNG_LUNG_TRE: i32 = 2;
pub const RUNG_NAM_DAOU: i32 = 3;
pub const DAO_KAME: i32 = 5;
pub const DONG_KARIN: i32 = 6;
pub const LANG_ARU_2: i32 = 15;
pub const NHA_GOHAN: i32 = 21;
pub const TRAM_TAU_VU_TRU_TRAI_DAT: i32 = 24;
pub const TRUNG_TAM_BANG_THIET_GIAP: i32 = 27;
pub const VACH_NUI_ARU: i32 = 44;

// --- Namếc ---
pub const LANG_MORI: i32 = 7;
pub const DOI_HOANG: i32 = 8;
pub const THI_TRAN_MORI: i32 = 9;
pub const THUNG_LUNG_NAMEC: i32 = 10;
pub const THUNG_LUNG_MAIMA: i32 = 11;
pub const NHA_MOORI: i32 = 22;
pub const TRAM_TAU_VU_TRU_NAMEC: i32 = 25;
pub const VACH_NUI_NAMEC: i32 = 43;

// --- Xayda ---
pub const LANG_KAKALOT: i32 = 13;
pub const VOT_OC_SEN: i32 = 14;
pub const LANG_PLANE: i32 = 16;
pub const RUNG_NGUYEN_SINH: i32 = 17;
pub const THUNG_LUNG_NAPPA: i32 = 19;
pub const NHA_BROLY: i32 = 23;
pub const TRAM_TAU_VU_TRU_XAYDA: i32 = 26;
pub const VACH_NUI_KAKAROT: i32 = 42;
pub const THANH_PHO_VEGETA: i32 = 68;

// --- Đặc biệt / Event ---
pub const DAO_BULONG: i32 = 30;
pub const DONG_NAM_GURU: i32 = 34;
pub const NGHIA_DIA_KHU_MO: i32 = 52;
pub const TRAI_LINH_FIDE: i32 = 79;
pub const SIEU_THI: i32 = 84;
pub const VUC_CAM: i32 = 85; // Black Ball Start
pub const TRUNG_TAM_BKK: i32 = 154;
pub const NHA_MA_BU: i32 = 114;

pub const BLACK_BALL_WAR_MAP_START: i32 = VUC_CAM;
pub const BLACK_BALL_WAR_MAP_END: i32 = 91;

pub const MABU_HOME_MAP_ID: i32 = NHA_MA_BU;
pub const MAP_TRAM_TAU_VU_TRU_TRAI_DAT: i32 = TRAM_TAU_VU_TRU_TRAI_DAT;
pub const MAP_TRAM_TAU_VU_TRU_NAMEC: i32 = TRAM_TAU_VU_TRU_NAMEC;
pub const MAP_TRAM_TAU_VU_TRU_XAYDA: i32 = TRAM_TAU_VU_TRU_XAYDA;
pub const MAP_SIEU_THI: i32 = SIEU_THI;
pub const MAP_THANH_PHO_VEGETA: i32 = THANH_PHO_VEGETA;

pub const GENDER_TRAI_DAT: i8 = Gender::TraiDat as i8;
pub const GENDER_NAMEC: i8 = Gender::Namek as i8;
pub const GENDER_XAYDA: i8 = Gender::Xayda as i8;

pub const CHANGE_CAPSULE: i32 = ChangeMapType::Capsule as i32;
pub const CHANGE_BLACK_BALL: i32 = ChangeMapType::BlackBall as i32;
pub const CHANGE_CUP: i32 = 2;

pub const TASK_1_0: i32 = TaskId::Task1_0 as i32;
pub const TASK_2_0: i32 = TaskId::Task2_0 as i32;
pub const TASK_3_0: i32 = TaskId::Task3_0 as i32;
pub const TASK_4_0: i32 = TaskId::Task4_0 as i32;
pub const TASK_7_0: i32 = TaskId::Task7_0 as i32;
pub const TASK_13_0: i32 = TaskId::Task13_0 as i32;
pub const TASK_15_0: i32 = TaskId::Task15_0 as i32;
pub const TASK_16_0: i32 = TaskId::Task16_0 as i32;
pub const TASK_18_0: i32 = TaskId::Task18_0 as i32;
pub const TASK_19_0: i32 = TaskId::Task19_0 as i32;
pub const TASK_20_0: i32 = TaskId::Task20_0 as i32;
pub const TASK_21_0: i32 = TaskId::Task21_0 as i32;
pub const TASK_24_0: i32 = TaskId::Task24_0 as i32;
pub const TASK_27_0: i32 = TaskId::Task27_0 as i32;

