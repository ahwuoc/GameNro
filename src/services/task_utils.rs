use crate::constant::task_type::TaskType;
use crate::entities::task_sub_template;
use crate::map::managers::map_manager::MAP_MANAGER;
use crate::player::Player;

/// Các hàm tiện ích dùng chung cho Task System.
pub struct TaskUtils;

impl TaskUtils {
    /// Lấy NPC ID tương ứng với hành tinh của người chơi (Trái đất: 0, Namếc: 2, Xayda: 1).
    pub fn get_planet_npc_id(gender: i8) -> i16 {
        match gender {
            0 => 0,
            1 => 2,
            2 => 1,
            _ => 0,
        }
    }

    pub fn transform_name(player: &Player, text: &str) -> String {
        let mut result = text.to_string();
        let gender = player.gender;

        // {village} / %1: Làng
        let village = match gender {
            0 => "Làng Aru",
            1 => "Làng Mori",
            2 => "Làng Kakarot",
            _ => "Làng Aru",
        };
        result = result.replace("{village}", village).replace("%1", village);

        // {elder} / %2: Ông nội
        let elder = match gender {
            0 => "ông Gôhan",
            1 => "ông Moori",
            2 => "ông Paragus",
            _ => "ông Gôhan",
        };
        result = result.replace("{elder}", elder).replace("%2", elder);

        // {map_0} / %3: Đồi / Rừng
        let map_0 = match gender {
            0 => "Đồi hoa cúc",
            1 => "Đồi nấm tím",
            2 => "Đồi hoang",
            _ => "Đồi hoa cúc",
        };
        result = result.replace("{map_0}", map_0).replace("%3", map_0);

        // {mob_0} / %4: Quái sơ cấp
        let mob_0 = match gender {
            0 => "khủng long",
            1 => "lợn lòi",
            2 => "quỷ đất",
            _ => "khủng long",
        };
        result = result.replace("{mob_0}", mob_0).replace("%4", mob_0);

        // {map_1} / %5: Vách núi
        let map_1 = match gender {
            0 => "Vách núi Aru",
            1 => "Vách núi Moori",
            2 => "Vách núi Kakarot",
            _ => "Vách núi Aru",
        };
        result = result.replace("{map_1}", map_1).replace("%5", map_1);

        // {map_2} / %6: Thị trấn / Làng Plant
        let map_2 = match gender {
            0 => "Thung lũng tre",
            1 => "Thị trấn Moori",
            2 => "Làng Plant",
            _ => "Thung lũng tre",
        };
        result = result.replace("{map_2}", map_2).replace("%6", map_2);

        // {station_npc} / %7: NPC Trạm tàu vũ trụ
        let station_npc = match gender {
            0 => "Dr. Brief",
            1 => "Cargo",
            2 => "Cui",
            _ => "Dr. Brief",
        };
        result = result
            .replace("{station_npc}", station_npc)
            .replace("%7", station_npc);

        // {village_shop} / %8: NPC Shop làng
        let village_shop = match gender {
            0 => "Bunma",
            1 => "Dende",
            2 => "Appule",
            _ => "Bunma",
        };
        result = result
            .replace("{village_shop}", village_shop)
            .replace("%8", village_shop);

        // {mob_1} / %9: Quái bay 1
        let mob_1 = match gender {
            0 => "thằn lằn bay",
            1 => "phi long",
            2 => "quỷ bay",
            _ => "thằn lằn bay",
        };
        result = result.replace("{mob_1}", mob_1).replace("%9", mob_1);

        // {master} / %10: NPC Tổng quản (Quy Lão)
        let master = match gender {
            0 => "Quy Lão Kame",
            1 => "Trưởng lão Guru",
            2 => "Vua Vegeta",
            _ => "Quy Lão Kame",
        };
        result = result.replace("{master}", master).replace("%10", master);

        // {map_master} / %11: Bản đồ đặc biệt (Đảo Kame...)
        let map_master = match gender {
            0 => "Đảo Kamê",
            1 => "Đảo Guru",
            2 => "Vách núi đen",
            _ => "Đảo Kamê",
        };
        result = result
            .replace("{map_master}", map_master)
            .replace("%11", map_master);

        // {mob_2} / %12: Quái 3000 HP
        let mob_2 = match gender {
            0 => "ốc mượn hồn",
            1 => "ốc sên",
            2 => "heo Xayda mẹ",
            _ => "ốc mượn hồn",
        };
        result = result.replace("{mob_2}", mob_2).replace("%12", mob_2);

        // {map_3} / %13: Bản đồ trung cấp
        let map_3 = match gender {
            0 => "Rừng nấm",
            1 => "Thung lũng Namếc",
            2 => "Rừng nguyên sinh",
            _ => "Rừng nấm",
        };
        result = result.replace("{map_3}", map_3).replace("%13", map_3);

        // {mob_mother} / %14: Quái mẹ sơ cấp
        let mob_mother = match gender {
            0 => "phi long mẹ",
            1 => "quỷ bay mẹ",
            2 => "thằn lằn mẹ",
            _ => "phi long mẹ",
        };
        result = result
            .replace("{mob_mother}", mob_mother)
            .replace("%14", mob_mother);

        // {station_npc_map}: Bản đồ Trạm tàu vũ trụ
        let station_npc_map = match gender {
            0 => "Trạm tàu vũ trụ Trái Đất",
            1 => "Trạm tàu vũ trụ Namếc",
            2 => "Trạm tàu vũ trụ Xayda",
            _ => "Trạm tàu vũ trụ",
        };
        result = result.replace("{station_npc_map}", station_npc_map);

        result
    }
}
