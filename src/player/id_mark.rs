use crate::constant::menu_enum::MenuId;

#[derive(Debug, Clone, Default)]
pub struct IdMark {
    pub index_menu: MenuId,
    pub id_item_up_top: i32,
    pub ott: i32,
    pub mbv: i32,
    pub type_change_map: i32,
    pub tag_shop: String,
}

impl IdMark {
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
}
