#[derive(Debug, Clone)]
pub struct NPoint {
    pub hpg: i32,
    pub mpg: i32,
    pub dameg: i32,
    pub defg: i32,
    pub critg: i8,
    pub stamina: i16,

    pub hp: i32,
    pub mp: i32,
    pub dame: i32,
    pub def: i32,
    pub crit: i8,

    pub hp_max: i32,
    pub mp_max: i32,
    pub max_stamina: i16,

    pub speed: i8,
    pub power: i64,
    pub tiem_nang: i64,
    pub limit_power: i8,
}

impl NPoint {
    pub fn new() -> Self {
        NPoint {
            hpg: 100,
            mpg: 100,
            dameg: 10,
            defg: 5,
            critg: 1,
            crit: 1,
            dame: 1,
            def: 1,
            hp: 1,
            mp: 1,
            hp_max: 1,
            mp_max: 1,
            speed: 5,
            power: 1,
            tiem_nang: 1,
            limit_power: 1,
            stamina: 1,
            max_stamina: 1,
        }
    }
    pub fn update(&mut self) {
        if self.hp > self.hp_max {
            self.hp = self.hp_max;
        }
        if self.mp > self.mp_max {
            self.mp = self.mp_max;
        }
    }
}
