use crate::item::item::Item;

#[derive(Debug, Clone)]
pub struct Inventory {
    pub gold: i64,
    pub gem: i64,
    pub items_bag: Vec<Item>,
    pub items_body: Vec<Item>,
}
impl Inventory {
    pub fn new() -> Self {
        Inventory {
            gold: 0,
            gem: 0,
            items_bag: Vec::new(),
            items_body: vec![Item::new(); 10],
        }
    }
}

