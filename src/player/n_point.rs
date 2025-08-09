#[derive(Debug, Clone)]
pub struct NPoint {
    pub hp: u64,
    pub hp_max: u64,
    pub mp: u64,
    pub mp_max: u64,
    pub damage: u64,
    pub defense: u64,
    pub crit: u32,
    pub power: u64,
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
    
    pub fn set_hp(&mut self, hp: u64) {
        self.hp = if hp > self.hp_max { self.hp_max } else { hp };
    }
    
    pub fn set_mp(&mut self, mp: u64) {
        self.mp = if mp > self.mp_max { self.mp_max } else { mp };
    }
    
    pub fn sub_hp(&mut self, damage: u64) {
        if damage >= self.hp {
            self.hp = 0;
        } else {
            self.hp -= damage;
        }
    }
    
    pub fn add_hp(&mut self, amount: u64) {
        self.hp = std::cmp::min(self.hp + amount, self.hp_max);
    }
    
    pub fn add_mp(&mut self, amount: u64) {
        self.mp = std::cmp::min(self.mp + amount, self.mp_max);
    }
    
    pub fn update(&mut self) {
        if self.hp > self.hp_max {
            self.hp = self.hp_max;
        }
        if self.mp > self.mp_max {
            self.mp = self.mp_max;
        }
    }
    
    pub fn is_die(&self) -> bool {
        self.hp <= 0
    }
    
    pub fn cal_point(&mut self) {
        self.hp_max = 100;
        self.mp_max = 100;
        self.damage = 10;
        self.defense = 5;
        self.crit = 0;
        self.power = 0;
    }
}
