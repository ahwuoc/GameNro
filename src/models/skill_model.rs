#[derive(Debug, Clone)]
pub struct Skill {
    pub id: i16,
    pub point: i8,
    pub power_require: i64,
    pub cool_down: i32,
    pub last_time_use: u64,
}

impl Skill {
    pub fn new(id: i16) -> Self {
        Skill {
            id,
            point: 0,
            power_require: 0,
            cool_down: 1000,
            last_time_use: 0,
        }
    }
}
