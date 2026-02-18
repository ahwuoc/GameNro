#[derive(Debug, Clone, Copy, PartialEq)]
pub enum TypePvp {
    ThachDau,
    LuyenTap,
    TraThu,
    CuuSat,
    DaiHoiVoThuat,
}

#[derive(Debug, Clone, Copy, PartialEq)]
pub enum TypeLosePvp {
    RunsAway,
    Dead,
}

pub mod dhvt;
pub mod luyen_tap;
pub mod pvp;
pub mod pvp_manager;
pub mod pvp_service;
pub mod thach_dau;
pub mod tra_thu;
