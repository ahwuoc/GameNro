#[derive(Debug, Clone, serde::Serialize, serde::Deserialize)]
pub struct Skill {
    pub template_id: i32,
    pub point: i8,
    pub power_require: i64,
    pub cool_down: i32,
    pub last_time_use: u64,
    #[serde(default)]
    pub dx: i16,
    #[serde(default)]
    pub dy: i16,
    #[serde(default)]
    pub max_fight: i16,
    #[serde(default)]
    pub mana_use: i16,
    #[serde(default)]
    pub damage: i16,
    #[serde(default)]
    pub more_info: String,
    #[serde(default)]
    pub price: i16,
    #[serde(default)]
    pub curr_level: i16,
}

impl Skill {
    pub const RANGE_ATTACK_CHIEU_DAM: i32 = 100;
    pub const RANGE_ATTACK_CHIEU_CHUONG: i32 = 300;

    pub const DRAGON: i32 = 0;
    pub const KAMEJOKO: i32 = 1;
    pub const DEMON: i32 = 2;
    pub const MASENKO: i32 = 3;
    pub const GALICK: i32 = 4;
    pub const ANTOMIC: i32 = 5;
    pub const THAI_DUONG_HA_SAN: i32 = 6;
    pub const TRI_THUONG: i32 = 7;
    pub const TAI_TAO_NANG_LUONG: i32 = 8;
    pub const KAIOKEN: i32 = 9;
    pub const QUA_CAU_KENH_KHI: i32 = 10;
    pub const MAKANKOSAPPO: i32 = 11;
    pub const DE_TRUNG: i32 = 12;
    pub const BIEN_KHI: i32 = 13;
    pub const TU_SAT: i32 = 14;
    pub const LIEN_HOAN: i32 = 17;
    pub const SOCOLA: i32 = 18;
    pub const KHIEN_NANG_LUONG: i32 = 19;
    pub const DICH_CHUYEN_TUC_THOI: i32 = 20;
    pub const HUYT_SAO: i32 = 21;
    pub const THOI_MIEN: i32 = 22;
    pub const TROI: i32 = 23;
    pub const SUPER_KAME: i32 = 24;
    pub const LIEN_HOAN_CHUONG: i32 = 25;
    pub const MA_PHONG_BA: i32 = 26;

    pub fn new(id: i32) -> Self {
        Skill {
            template_id: id,
            point: 0,
            power_require: 0,
            cool_down: 0,
            last_time_use: 0,
            dx: 0,
            dy: 0,
            max_fight: 0,
            mana_use: 0,
            damage: 0,
            more_info: String::new(),
            price: 0,
            curr_level: 0,
        }
    }
}
