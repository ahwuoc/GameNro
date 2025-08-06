#[derive(Debug, Clone)]
pub struct NPoint {
    pub hp: i64,
    pub hp_max: i64,
    pub mp: i64,
    pub mp_max: i64,
    pub damage: i64,
    pub defense: i64,
    pub crit: i32,
    pub power: i64,
}
impl NPoint {
    pub fn new() -> Self {
        NPoint {
            hp: 100,
            hp_max: 100,
            mp: 100,
            mp_max: 100,
            damage: 10,
            defense: 5,
            crit: 0,
            power: 0,
        }
    }
    pub fn set_hp(&mut self, hp: i64) {
        self.hp = if hp > self.hp_max { self.hp_max } else { hp };
    }
    pub fn set_mp(&mut self, mp: i64) {
        self.mp = if mp > self.mp { self.mp_max } else { mp };
    }
    pub fn sub_hp(&mut self, damege: f64) {
        self.hp -= damege as i64;
        if self.hp < 0 {
            self.hp = 0;
        }
    }
}
