#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum MenuId {
    None,
    BaseMenu,
    SubMenuSanta,
    Intrinsic,
    ConfirmOpenIntrinsic,
    ConfirmOpenIntrinsicVip,
}

impl Default for MenuId {
    fn default() -> Self {
        MenuId::None
    }
}
