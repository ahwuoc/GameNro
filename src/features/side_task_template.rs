// Placeholder SideTaskTemplate module
#[derive(Debug, Clone)]
pub struct SideTaskTemplate {
    pub id: i32,
    pub name: String,
}

impl SideTaskTemplate {
    pub fn new(id: i32, name: String) -> Self {
        Self { id, name }
    }
}
