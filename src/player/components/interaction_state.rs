use crate::constant::menu_enum::MenuId;

#[derive(Debug, Clone, Default)]
pub struct InteractionState {
    pub index_menu: MenuId,
    pub id_item_up_top: i32,
    pub ott: i32,
    pub mbv: i32,
    pub type_change_map: i32,
    pub tag_shop: String,
    pub is_thachdau: bool,
    pub has_training_boss: bool,
    // PVP fields
    pub id_play_thach_dau: i64,
    pub gold_thach_dau: i64,
    pub id_enemy: i64,
    pub last_time_revenge: u64,
}

impl InteractionState {
    pub fn new() -> Self {
        Self {
            index_menu: MenuId::None,
            ..Default::default()
        }
    }

    pub fn get_index_menu(&self) -> MenuId {
        self.index_menu
    }
    pub fn set_tag_shop(&mut self, tab_shop: String) {
        self.tag_shop = tab_shop;
    }
    pub fn get_tag_shop(&self) -> &str {
        &self.tag_shop
    }
    pub fn get_is_thachdau(&self) -> bool {
        self.is_thachdau
    }

    pub fn set_is_thachdau(&mut self, is_training: bool) {
        self.is_thachdau = is_training;
    }

    pub fn set_index_menu(&mut self, index: MenuId) {
        self.index_menu = index;
    }

    pub fn get_id_item_up_top(&self) -> i32 {
        self.id_item_up_top
    }

    pub fn set_id_item_up_top(&mut self, id: i32) {
        self.id_item_up_top = id;
    }

    pub fn get_ott(&self) -> i32 {
        self.ott
    }

    pub fn set_ott(&mut self, ott: i32) {
        self.ott = ott;
    }

    pub fn get_mbv(&self) -> i32 {
        self.mbv
    }

    pub fn set_mbv(&mut self, mbv: i32) {
        self.mbv = mbv;
    }

    pub fn get_has_training_boss(&self) -> bool {
        self.has_training_boss
    }

    pub fn set_has_training_boss(&mut self, has: bool) {
        self.has_training_boss = has;
    }
}
