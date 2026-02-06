use crate::network::message::Message;
use crate::player::Player;
use crate::utils::time;
use serde::{Deserialize, Serialize};

pub const MAX_LEVEL: u8 = 10;

pub const PEA_TEMP: [i16; 10] = [13, 60, 61, 62, 63, 64, 65, 352, 523, 595];
pub const PEA_PARAM: [i32; 10] = [100, 500, 2, 4, 8, 16, 32, 64, 128, 256];

pub const POS_PEAS: [&[[i8; 2]]; 10] = [
    &[[19, 22], [-1, 16], [3, 10], [19, 8], [9, 0]],
    &[
        [-1, 27],
        [22, 35],
        [15, 24],
        [0, 17],
        [-1, 7],
        [26, 5],
        [5, 0],
    ],
    &[
        [25, 41],
        [-1, 40],
        [25, 34],
        [3, 32],
        [25, 23],
        [10, 19],
        [2, 12],
        [17, 10],
        [4, 5],
    ],
    &[
        [3, 44],
        [21, 49],
        [25, 39],
        [4, 30],
        [29, 25],
        [0, 18],
        [21, 15],
        [14, 39],
        [18, 25],
        [4, 7],
        [15, 0],
    ],
    &[
        [21, 58],
        [0, 56],
        [18, 48],
        [10, 0],
        [25, 38],
        [0, 26],
        [14, 28],
        [25, 16],
        [1, 14],
        [22, 7],
        [10, 14],
        [28, 23],
        [15, 16],
    ],
    &[
        [25, 63],
        [0, 66],
        [21, 52],
        [3, 55],
        [14, 60],
        [3, 45],
        [22, 43],
        [10, 35],
        [22, 28],
        [3, 28],
        [18, 17],
        [3, 14],
        [17, 6],
        [11, 22],
        [6, 1],
    ],
    &[
        [32, 86],
        [5, 77],
        [25, 77],
        [8, 89],
        [29, 68],
        [4, 63],
        [18, 61],
        [33, 53],
        [8, 48],
        [26, 39],
        [11, 36],
        [33, 23],
        [18, 25],
        [4, 20],
        [26, 12],
        [12, 7],
        [19, 0],
    ],
    &[
        [32, 86],
        [5, 77],
        [25, 77],
        [8, 89],
        [29, 68],
        [4, 63],
        [18, 61],
        [33, 53],
        [8, 48],
        [26, 39],
        [11, 36],
        [33, 23],
        [18, 25],
        [4, 20],
        [26, 12],
        [12, 7],
        [19, 0],
        [19, 0],
        [19, 0],
    ],
    &[
        [32, 86],
        [5, 77],
        [25, 77],
        [8, 89],
        [29, 68],
        [4, 63],
        [18, 61],
        [33, 53],
        [8, 48],
        [26, 39],
        [11, 36],
        [33, 23],
        [18, 25],
        [4, 20],
        [26, 12],
        [12, 7],
        [19, 0],
        [19, 0],
        [19, 0],
        [19, 0],
        [19, 0],
    ],
    &[
        [32, 86],
        [5, 77],
        [25, 77],
        [8, 89],
        [29, 68],
        [4, 63],
        [18, 61],
        [33, 53],
        [8, 48],
        [26, 39],
        [11, 36],
        [33, 23],
        [18, 25],
        [4, 20],
        [26, 12],
        [12, 7],
        [19, 0],
        [19, 0],
        [19, 0],
        [19, 0],
        [19, 0],
        [19, 0],
        [19, 0],
    ],
];

pub const PEA_UPGRADE: [[i16; 4]; 10] = [
    [0, 0, 10, 5],
    [0, 1, 40, 10],
    [0, 16, 40, 100],
    [6, 22, 0, 1],
    [13, 21, 0, 10],
    [27, 18, 0, 20],
    [55, 13, 0, 50],
    [69, 10, 0, 100],
    [104, 4, 0, 300],
    [0, 0, 0, 0],
];

pub const ID_MAGIC_TREE: [[i16; 10]; 3] = [
    [84, 85, 86, 87, 88, 89, 90, 90, 90, 90],
    [371, 372, 373, 374, 375, 376, 377, 377, 377, 377],
    [378, 379, 380, 381, 382, 383, 384, 384, 384, 384],
];

pub const POS_MAGIC_TREE: [[i16; 2]; 3] = [[348, 336], [372, 336], [348, 336]];

pub const UPGRADE_GEM: [i32; 10] = [20, 50, 120, 300, 800, 1500, 3000, 6000, 7500, 10000];
pub const HARVEST_GEM: [i32; 10] = [1, 2, 5, 7, 9, 12, 15, 20, 25, 30];

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MagicTree {
    pub level: u8,
    pub curr_peas: u16,
    pub is_upgrade: bool,
    pub last_time_harvest: i64,
    pub last_time_upgrade: i64,
}

impl MagicTree {
    pub fn new() -> Self {
        Self {
            level: 1,
            curr_peas: 5,
            is_upgrade: false,
            last_time_harvest: time::current_time_millis() as i64,
            last_time_upgrade: 0,
        }
    }

    pub fn update(&mut self) {
        let now = time::current_time_millis() as i64;
        if !self.is_upgrade {
            let max = self.get_max_pea();
            if self.curr_peas < max as u16 {
                let second_per_pea = self.get_second_per_pea() as i64;
                let time_passed = (now - self.last_time_harvest) / 1000;
                let num_pea_release = (time_passed / second_per_pea) as u16;

                if num_pea_release > 0 {
                    self.curr_peas += num_pea_release;
                    if self.curr_peas >= max as u16 {
                        self.curr_peas = max as u16;
                        self.last_time_harvest = now;
                    } else {
                        self.last_time_harvest += (num_pea_release as i64 * second_per_pea) * 1000;
                    }
                }
            }
        } else {
            if self.can_do_with_time(self.last_time_upgrade, self.get_time_upgrade()) {
                if self.level < MAX_LEVEL {
                    self.level += 1;
                }
                self.is_upgrade = false;
            }
        }
    }

    fn can_do_with_time(&self, last_time: i64, time_delay: i64) -> bool {
        time::current_time_millis() as i64 > last_time + time_delay
    }

    pub fn get_max_pea(&self) -> u8 {
        (self.level - 1) * 2 + 5
    }

    pub fn get_second_per_pea(&self) -> u16 {
        self.level as u16 * 60
    }

    pub fn get_second_pea_left(&self) -> i32 {
        let now = time::current_time_millis() as i64;
        let second_per_pea = self.get_second_per_pea() as i64;
        let time_pea_release = self.last_time_harvest + second_per_pea * 1000;
        let second_left = (time_pea_release - now) / 1000;
        if second_left < 0 {
            0
        } else {
            second_left as i32
        }
    }

    pub fn get_second_upgrade_left(&self) -> i32 {
        let now = time::current_time_millis() as i64;
        let upgrade_time = self.get_time_upgrade();
        let second_left = (self.last_time_upgrade + upgrade_time - now) / 1000;
        if second_left < 0 {
            0
        } else {
            second_left as i32
        }
    }

    pub fn get_time_upgrade(&self) -> i64 {
        let idx = (self.level - 1) as usize;
        let d = PEA_UPGRADE[idx][0] as i64;
        let h = PEA_UPGRADE[idx][1] as i64;
        let m = PEA_UPGRADE[idx][2] as i64;
        d * 24 * 60 * 60 * 1000 + h * 60 * 60 * 1000 + m * 60 * 1000
    }

    pub fn get_menu_id(&self) -> crate::constant::menu_enum::MenuId {
        if !self.is_upgrade {
            if self.curr_peas < self.get_max_pea() as u16 {
                crate::constant::menu_enum::MenuId::MagicTreeNonUpgradeLeftPea
            } else {
                crate::constant::menu_enum::MenuId::MagicTreeNonUpgradeFullPea
            }
        } else {
            crate::constant::menu_enum::MenuId::MagicTreeUpgrade
        }
    }

    pub fn get_text_menu_upgrade(&self) -> String {
        let idx = (self.level - 1) as usize;
        let d = PEA_UPGRADE[idx][0];
        let h = PEA_UPGRADE[idx][1];
        let m = PEA_UPGRADE[idx][2];
        let gold = PEA_UPGRADE[idx][3];

        let mut text = String::from("Nâng cấp\n");
        if d != 0 {
            text.push_str(&format!("{}d", d));
        }
        if h != 0 {
            text.push_str(&format!("{}h", h));
        }
        if m != 0 {
            text.push_str(&format!("{}'", m));
        }

        let unit = if self.level <= 3 { " k" } else { " Tr" };
        text.push_str(&format!("\n{} {}\nvàng", gold, unit));
        text
    }

    pub fn create_load_message(&self, player: &Player) -> anyhow::Result<Message> {
        let gender_idx = player.gender as usize;
        let level_idx = (self.level - 1) as usize;

        let mut msg = Message::new(-34);
        msg.write_byte(0)?;
        msg.write_short(ID_MAGIC_TREE[gender_idx][level_idx])?;
        msg.write_utf(&format!("Đậu thần cấp {}", self.level))?;
        msg.write_short(POS_MAGIC_TREE[gender_idx][0])?;
        msg.write_short(POS_MAGIC_TREE[gender_idx][1])?;
        msg.write_byte(self.level as i8)?;
        msg.write_short(self.curr_peas as i16)?;
        msg.write_short(self.get_max_pea() as i16)?;
        msg.write_utf("Đang kết hạt\nCây lớn sinh nhiều hạt hơn")?;
        msg.write_int(if self.is_upgrade {
            self.get_second_upgrade_left()
        } else {
            self.get_second_pea_left()
        })?;

        let pos = POS_PEAS[level_idx];
        msg.write_byte(pos.len() as i8)?;
        for p in pos {
            msg.write_byte(p[0])?;
            msg.write_byte(p[1])?;
        }
        msg.write_boolean(self.is_upgrade)?;
        Ok(msg)
    }

    pub fn create_menu_message(&self, _player: &Player) -> anyhow::Result<Message> {
        let mut msg = Message::new(-34);
        msg.write_byte(1)?;
        if !self.is_upgrade {
            msg.write_utf("Thu\nhoạch")?;
            if self.level < MAX_LEVEL {
                msg.write_utf(&self.get_text_menu_upgrade())?;
            }
            if self.curr_peas < self.get_max_pea() as u16 {
                msg.write_utf(&format!(
                    "Kết hạt\nnhanh\n{} ngọc",
                    HARVEST_GEM[(self.level - 1) as usize]
                ))?;
            }
        } else {
            msg.write_utf(&format!(
                "Nâng cấp\nnhanh\n{} ngọc",
                UPGRADE_GEM[(self.level - 1) as usize]
            ))?;
            let idx = (self.level - 1) as usize;
            let gold = PEA_UPGRADE[idx][3];
            let unit = if self.level <= 3 { " k" } else { " Tr" };
            msg.write_utf(&format!("Hủy\nnâng cấp\nhồi {} {}\nvàng", gold / 2, unit))?;
        }
        Ok(msg)
    }
}
