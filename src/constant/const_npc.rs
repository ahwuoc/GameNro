#![allow(dead_code)]

/// NPC IDs as an enum for type safety
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
#[repr(i16)]
pub enum NpcId {
    OngGohan = 0,
    OngParagus = 1,
    OngMoori = 2,
    RuongDo = 3,
    DauThan = 4,
    ConMeo = 5,
    KhuVuc = 6,
    // ====================================================
    // sua lai id vi client no hardcode hien thi cho npc Bunma,Dende,Appule
    Bunma = 60,
    Dende = 61,
    Appule = 62,
    // ====================================================
    DrDrief = 10,
    Cargo = 11,
    Cui = 12,
    QuyLaoKame = 13,
    TruongLaoGuru = 14,
    VuaVegeta = 15,
    Uron = 16,
    BoMong = 17,
    ThanMeoKarin = 18,
    ThuongDe = 19,
    ThanVuTru = 20,
    BaHatMit = 21,
    TrongTai = 22,
    GhiDanh = 23,
    RongThieng = 24,
    LinhCanh = 25,
    DocNhan = 26,
    RongThiengNamec = 27,
    CuaHangKyGui = 28,
    RongOmega = 29,
    Rong2S = 30,
    Rong3S = 31,
    Rong4S = 32,
    Rong5S = 33,
    Rong6S = 34,
    Rong7S = 35,
    Rong1S = 36,
    BunmaTl = 37,
    Calick = 38,
    Santa = 39,
    MabuMap = 40,
    TrungThu = 41,
    QuocVuong = 42,
    ToSuKaio = 43,
    Osin = 44,
    Kibit = 45,
    Babiday = 46,
    GiumaDauBo = 47,
    NgoKhong = 48,
    DuongTang = 49,
    QuaTrung = 50,
    DuaHau = 51,
    HungVuong = 52,
    Tapion = 53,
    LyTieuNuong = 54,
    Bill = 55,
    Whis = 56,
    Champa = 57,
    Vados = 58,
    TrongTai2 = 59,
    Jaco = 63,
    DaiThienSu = 64,
    Yarirobe = 65,
    NoiBanh = 66,
    MrPopo = 67,
    Panchy = 68,
    ThoDaiCa = 69,
    Bardock = 70,
    Toribot = 71,
    CayNeu = 72,
    Event = 75,
    Intrinsic = 100,
}

impl NpcId {
    /// Convert from i16 to NpcId
    pub fn from_i16(value: i16) -> Option<Self> {
        match value {
            0 => Some(Self::OngGohan),
            1 => Some(Self::OngParagus),
            2 => Some(Self::OngMoori),
            3 => Some(Self::RuongDo),
            4 => Some(Self::DauThan),
            5 => Some(Self::ConMeo),
            6 => Some(Self::KhuVuc),
            60 => Some(Self::Bunma),
            61 => Some(Self::Dende),
            62 => Some(Self::Appule),
            10 => Some(Self::DrDrief),
            11 => Some(Self::Cargo),
            12 => Some(Self::Cui),
            13 => Some(Self::QuyLaoKame),
            14 => Some(Self::TruongLaoGuru),
            15 => Some(Self::VuaVegeta),
            16 => Some(Self::Uron),
            17 => Some(Self::BoMong),
            18 => Some(Self::ThanMeoKarin),
            19 => Some(Self::ThuongDe),
            20 => Some(Self::ThanVuTru),
            21 => Some(Self::BaHatMit),
            22 => Some(Self::TrongTai),
            23 => Some(Self::GhiDanh),
            24 => Some(Self::RongThieng),
            25 => Some(Self::LinhCanh),
            26 => Some(Self::DocNhan),
            27 => Some(Self::RongThiengNamec),
            28 => Some(Self::CuaHangKyGui),
            29 => Some(Self::RongOmega),
            30 => Some(Self::Rong2S),
            31 => Some(Self::Rong3S),
            32 => Some(Self::Rong4S),
            33 => Some(Self::Rong5S),
            34 => Some(Self::Rong6S),
            35 => Some(Self::Rong7S),
            36 => Some(Self::Rong1S),
            37 => Some(Self::BunmaTl),
            38 => Some(Self::Calick),
            39 => Some(Self::Santa),
            40 => Some(Self::MabuMap),
            41 => Some(Self::TrungThu),
            42 => Some(Self::QuocVuong),
            43 => Some(Self::ToSuKaio),
            44 => Some(Self::Osin),
            45 => Some(Self::Kibit),
            46 => Some(Self::Babiday),
            47 => Some(Self::GiumaDauBo),
            48 => Some(Self::NgoKhong),
            49 => Some(Self::DuongTang),
            50 => Some(Self::QuaTrung),
            51 => Some(Self::DuaHau),
            52 => Some(Self::HungVuong),
            53 => Some(Self::Tapion),
            54 => Some(Self::LyTieuNuong),
            55 => Some(Self::Bill),
            56 => Some(Self::Whis),
            57 => Some(Self::Champa),
            58 => Some(Self::Vados),
            59 => Some(Self::TrongTai2),
            63 => Some(Self::Jaco),
            64 => Some(Self::DaiThienSu),
            65 => Some(Self::Yarirobe),
            66 => Some(Self::NoiBanh),
            67 => Some(Self::MrPopo),
            68 => Some(Self::Panchy),
            69 => Some(Self::ThoDaiCa),
            70 => Some(Self::Bardock),
            71 => Some(Self::Toribot),
            72 => Some(Self::CayNeu),
            75 => Some(Self::Event),
            100 => Some(Self::Intrinsic),
            _ => None,
        }
    }

    /// Convert to i16
    pub fn as_i16(self) -> i16 {
        self as i16
    }
}

impl From<NpcId> for i16 {
    fn from(npc: NpcId) -> Self {
        npc as i16
    }
}

// Backward compatibility - keep old constants pointing to enum values
pub const ONG_GOHAN: i16 = NpcId::OngGohan as i16;
pub const ONG_PARAGUS: i16 = NpcId::OngParagus as i16;
pub const ONG_MOORI: i16 = NpcId::OngMoori as i16;
pub const RUONG_DO: i16 = NpcId::RuongDo as i16;
pub const DAU_THAN: i16 = NpcId::DauThan as i16;
pub const CON_MEO: i16 = NpcId::ConMeo as i16;
pub const KHU_VUC: i16 = NpcId::KhuVuc as i16;
pub const BUNMA: i16 = NpcId::Bunma as i16;
pub const DENDE: i16 = NpcId::Dende as i16;
pub const APPULE: i16 = NpcId::Appule as i16;
pub const DR_DRIEF: i16 = NpcId::DrDrief as i16;
pub const CARGO: i16 = NpcId::Cargo as i16;
pub const CUI: i16 = NpcId::Cui as i16;
pub const QUY_LAO_KAME: i16 = NpcId::QuyLaoKame as i16;
pub const TRUONG_LAO_GURU: i16 = NpcId::TruongLaoGuru as i16;
pub const VUA_VEGETA: i16 = NpcId::VuaVegeta as i16;
pub const URON: i16 = NpcId::Uron as i16;
pub const BO_MONG: i16 = NpcId::BoMong as i16;
pub const THAN_MEO_KARIN: i16 = NpcId::ThanMeoKarin as i16;
pub const THUONG_DE: i16 = NpcId::ThuongDe as i16;
pub const THAN_VU_TRU: i16 = NpcId::ThanVuTru as i16;
pub const BA_HAT_MIT: i16 = NpcId::BaHatMit as i16;
pub const TRONG_TAI: i16 = NpcId::TrongTai as i16;
pub const GHI_DANH: i16 = NpcId::GhiDanh as i16;
pub const RONG_THIENG: i16 = NpcId::RongThieng as i16;
pub const LINH_CANH: i16 = NpcId::LinhCanh as i16;
pub const DOC_NHAN: i16 = NpcId::DocNhan as i16;
pub const RONG_THIENG_NAMEC: i16 = NpcId::RongThiengNamec as i16;
pub const CUA_HANG_KY_GUI: i16 = NpcId::CuaHangKyGui as i16;
pub const RONG_OMEGA: i16 = NpcId::RongOmega as i16;
pub const RONG_2S: i16 = NpcId::Rong2S as i16;
pub const RONG_3S: i16 = NpcId::Rong3S as i16;
pub const RONG_4S: i16 = NpcId::Rong4S as i16;
pub const RONG_5S: i16 = NpcId::Rong5S as i16;
pub const RONG_6S: i16 = NpcId::Rong6S as i16;
pub const RONG_7S: i16 = NpcId::Rong7S as i16;
pub const RONG_1S: i16 = NpcId::Rong1S as i16;
pub const BUNMA_TL: i16 = NpcId::BunmaTl as i16;
pub const CALICK: i16 = NpcId::Calick as i16;
pub const SANTA: i16 = NpcId::Santa as i16;
pub const MABU_MAP: i16 = NpcId::MabuMap as i16;
pub const TRUNG_THU: i16 = NpcId::TrungThu as i16;
pub const QUOC_VUONG: i16 = NpcId::QuocVuong as i16;
pub const TO_SU_KAIO: i16 = NpcId::ToSuKaio as i16;
pub const OSIN: i16 = NpcId::Osin as i16;
pub const KIBIT: i16 = NpcId::Kibit as i16;
pub const BABIDAY: i16 = NpcId::Babiday as i16;
pub const GIUMA_DAU_BO: i16 = NpcId::GiumaDauBo as i16;
pub const NGO_KHONG: i16 = NpcId::NgoKhong as i16;
pub const DUONG_TANG: i16 = NpcId::DuongTang as i16;
pub const QUA_TRUNG: i16 = NpcId::QuaTrung as i16;
pub const DUA_HAU: i16 = NpcId::DuaHau as i16;
pub const HUNG_VUONG: i16 = NpcId::HungVuong as i16;
pub const TAPION: i16 = NpcId::Tapion as i16;
pub const LY_TIEU_NUONG: i16 = NpcId::LyTieuNuong as i16;
pub const BILL: i16 = NpcId::Bill as i16;
pub const WHIS: i16 = NpcId::Whis as i16;
pub const CHAMPA: i16 = NpcId::Champa as i16;
pub const VADOS: i16 = NpcId::Vados as i16;
pub const TRONG_TAI_2: i16 = NpcId::TrongTai2 as i16;
pub const JACO: i16 = NpcId::Jaco as i16;
pub const DAI_THIEN_SU: i16 = NpcId::DaiThienSu as i16;
pub const YARIROBE: i16 = NpcId::Yarirobe as i16;
pub const NOI_BANH: i16 = NpcId::NoiBanh as i16;
pub const MR_POPO: i16 = NpcId::MrPopo as i16;
pub const PANCHY: i16 = NpcId::Panchy as i16;
pub const THO_DAI_CA: i16 = NpcId::ThoDaiCa as i16;
pub const BARDOCK: i16 = NpcId::Bardock as i16;
pub const TORIBOT: i16 = NpcId::Toribot as i16;
pub const CAY_NEU: i16 = NpcId::CayNeu as i16;
pub const EVENT: i16 = NpcId::Event as i16;
pub const INTRINSIC: i16 = NpcId::Intrinsic as i16;
