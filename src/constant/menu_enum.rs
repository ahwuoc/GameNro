#[repr(i16)]
#[derive(Debug, Clone, Copy)]
pub enum MenuId {
    BaseMenu = 0,
    Unknow = -1,
}

impl From<i16> for MenuId {
    fn from(value: i16) -> Self {
        match value {
            0 => MenuId::BaseMenu,
            _ => MenuId::Unknow,
        }
    }
}
impl From<MenuId> for i16 {
    fn from(value: MenuId) -> Self {
        value as i16
    }
}
