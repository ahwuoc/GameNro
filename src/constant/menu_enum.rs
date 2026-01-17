#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum MenuId {
    None,
    BaseMenu,
    SubMenuSanta,
    Intrinsic,
    SantaMenu,
    ConfirmOpenIntrinsic,
    ConfirmOpenIntrinsicVip,
    Admin,
    OngGohanMenu,
}

impl Default for MenuId {
    fn default() -> Self {
        MenuId::None
    }
}
