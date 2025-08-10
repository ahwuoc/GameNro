#[derive(Debug, Clone)]
pub struct Item {
    pub template_id: i32,
    pub quantity: i32,
    pub info: String,
}
impl Item {
    pub fn new(template_id: i32, quantity: i32) -> Self {
        Item {
            template_id,
            quantity,
            info: String::new(),
        }
    }
    pub fn is_not_null_item(&self) -> bool {
        self.template_id > -1
    }
}
