#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum MenuId {
    None,
    BaseMenu,
    SubMenuSanta,
    Intrinsic,
    SantaMenu,
    MenuCombine,
    ConfirmOpenIntrinsic,
    ConfirmOpenIntrinsicVip,
    Admin,
    OngGohanMenu,
    // Dynamic shop NPCs
    BunmaMenu,
    DendeMenu,
    AppuleMenu,
    // Magic Tree (Dau Than)
    MagicTreeNonUpgradeLeftPea,
    MagicTreeNonUpgradeFullPea,
    MagicTreeConfirmUpgrade,
    MagicTreeUpgrade,
    MagicTreeConfirmUnupgrade,
}

impl Default for MenuId {
    fn default() -> Self {
        MenuId::None
    }
}
