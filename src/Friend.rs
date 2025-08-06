#[derive(Debug, Clone)]
pub struct Friend {
    pub id: u32,
    pub name: String,
    pub head: i16,
    pub body: i16,
    pub leg: i16,
    pub power: i64,
}

#[derive(Debug, Clone)]
pub struct Enemy {
    pub id: u32,
    pub name: String,
    pub head: i16,
    pub body: i16,
    pub leg: i16,
    pub power: i64,
}
