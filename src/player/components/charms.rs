use sea_orm::sea_query::value;

use crate::utils::time;

#[derive(Debug, Clone)]
pub struct Charms {
    pub td_tri_tue: u64,  // item 213
    pub td_manh_me: u64,  // item 214
    pub td_da_trau: u64,  // item 215
    pub td_oai_hung: u64, // item 216
    pub td_bat_tu: u64,   // item 217
    pub td_deo_dai: u64,  // item 218
    pub td_thu_hut: u64,  // item 219
    pub td_de_tu: u64,    // item 522
    pub td_tri_tue3: u64, // item 671
    pub td_tri_tue4: u64, // item 672
}
impl Charms {
    pub fn new() -> Self {
        let now = time::current_time_millis();
        Self {
            td_tri_tue: now,
            td_manh_me: now,
            td_da_trau: now,
            td_oai_hung: now,
            td_bat_tu: now,
            td_deo_dai: now,
            td_thu_hut: now,
            td_de_tu: now,
            td_tri_tue3: now,
            td_tri_tue4: now,
        }
    }
    pub fn add_time_bua(&mut self, item_id: i32, mins: i32) {
        let curr_time = time::current_time_millis();

        let add_ms = mins as u64 * 60 * 1000;

        let time_bua = match item_id {
            213 => &mut self.td_tri_tue,
            214 => &mut self.td_manh_me,
            215 => &mut self.td_da_trau,
            216 => &mut self.td_oai_hung,
            217 => &mut self.td_bat_tu,
            218 => &mut self.td_deo_dai,
            219 => &mut self.td_thu_hut,
            522 => &mut self.td_de_tu,
            671 => &mut self.td_tri_tue3,
            672 => &mut self.td_tri_tue4,
            _ => return,
        };
        if *time_bua < curr_time {
            *time_bua = curr_time;
        }
        *time_bua += add_ms;
    }

    pub fn get_remaining_minutes(&self, item_id: i32) -> i64 {
        let curr_time = time::current_time_millis();
        let expiry = match item_id {
            213 => self.td_tri_tue,
            214 => self.td_manh_me,
            215 => self.td_da_trau,
            216 => self.td_oai_hung,
            217 => self.td_bat_tu,
            218 => self.td_deo_dai,
            219 => self.td_thu_hut,
            522 => self.td_de_tu,
            671 => self.td_tri_tue3,
            672 => self.td_tri_tue4,
            _ => return 0,
        };
        let diff = expiry as i64 - curr_time as i64;
        if diff > 0 {
            diff / 60_000
        } else {
            0
        }
    }

    pub fn from_db(data: &str) -> Self {
        let curr_time = time::current_time_millis();
        let size = 10;
        if data.is_empty() || data == "[]" {
            return Self::new();
        }

        let parser: Vec<u64> = serde_json::from_str(data).unwrap_or_else(|_| vec![curr_time; size]);

        let get = |i: usize| -> u64 { parser.get(i).copied().unwrap_or(curr_time) };

        Self {
            td_tri_tue: get(0),
            td_manh_me: get(1),
            td_da_trau: get(2),
            td_oai_hung: get(3),
            td_bat_tu: get(4),
            td_deo_dai: get(5),
            td_thu_hut: get(6),
            td_de_tu: get(7),
            td_tri_tue3: get(8),
            td_tri_tue4: get(9),
        }
    }

    pub fn to_db(&self) -> String {
        let bua_ba_mit = vec![
            self.td_tri_tue,
            self.td_manh_me,
            self.td_da_trau,
            self.td_oai_hung,
            self.td_bat_tu,
            self.td_deo_dai,
            self.td_thu_hut,
            self.td_de_tu,
            self.td_tri_tue3,
            self.td_tri_tue4,
        ];
        serde_json::to_string(&bua_ba_mit).unwrap_or_else(|_| "[]".to_string())
    }
}
