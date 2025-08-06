#[derive(Debug, Clone)]
pub struct SideTaskTemplate {
    pub id: i8,
    pub count: i16,
    pub max_count: i16,
    pub level: i8,
}

impl SideTaskTemplate {
    pub fn new() -> Self {
        SideTaskTemplate {
            id: -1,
            count: -1,
            max_count: -1,
            level: -1,
        }
    }
}
