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

    /// Thay thế các placeholder trong chuỗi (%1, %2, ...) bằng tên tương ứng theo hành tinh của người chơi.
    pub fn transform_name(player: &Player, text: &str) -> String {
        let mut result = text.to_string();
        let gender = player.gender;

        // %1: Làng
        result = result.replace(
            "%1",
            match gender {
                0 => "Làng Aru",
                1 => "Làng Mori",
                2 => "Làng Kakarot",
                _ => "Làng Aru",
            },
        );

        // %2: Ông nội
        result = result.replace(
            "%2",
            match gender {
                0 => "ông Gôhan",
                1 => "ông Moori",
                2 => "ông Paragus",
                _ => "ông Gôhan",
            },
        );

        // %3: Đồi / Rừng
        result = result.replace(
            "%3",
            match gender {
                0 => "Đồi hoa cúc",
                1 => "Đồi nấm tím",
                2 => "Đồi hoang",
                _ => "Đồi hoa cúc",
            },
        );

        // %4: Quái sơ cấp
        result = result.replace(
            "%4",
            match gender {
                0 => "khủng long",
                1 => "lợn lòi",
                2 => "quỷ đất",
                _ => "khủng long",
            },
        );

        // %5: Vách núi
        result = result.replace(
            "%5",
            match gender {
                0 => "Vách núi Aru",
                1 => "Vách núi Moori",
                2 => "Vách núi Kakarot",
                _ => "Vách núi Aru",
            },
        );

        // %6: Thị trấn / Làng Plant
        result = result.replace(
            "%6",
            match gender {
                0 => "Thung lũng tre",
                1 => "Thị trấn Moori",
                2 => "Làng Plant",
                _ => "Thung lũng tre",
            },
        );

        // %7: NPC Trạm tàu vũ trụ
        result = result.replace(
            "%7",
            match gender {
                0 => "Dr. Brief",
                1 => "Cargo",
                2 => "Cui",
                _ => "Dr. Brief",
            },
        );

        // %8: NPC Shop làng
        result = result.replace(
            "%8",
            match gender {
                0 => "Bunma",
                1 => "Dende",
                2 => "Appule",
                _ => "Bunma",
            },
        );

        // %9: Quái bay 1
        result = result.replace(
            "%9",
            match gender {
                0 => "thằn lằn bay",
                1 => "phi long",
                2 => "quỷ bay",
                _ => "thằn lằn bay",
            },
        );

        // %10: NPC Tổng quản (Quy Lão)
        result = result.replace(
            "%10",
            match gender {
                0 => "Quy Lão Kame",
                1 => "Trưởng lão Guru",
                2 => "Vua Vegeta",
                _ => "Quy Lão Kame",
            },
        );

        // %11: Bản đồ đặc biệt (Đảo Kame...)
        result = result.replace(
            "%11",
            match gender {
                0 => "Đảo Kamê",
                1 => "Đảo Guru",
                2 => "Vách núi đen",
                _ => "Đảo Kamê",
            },
        );

        // %12: Quái 3000 HP
        result = result.replace(
            "%12",
            match gender {
                0 => "ốc mượn hồn",
                1 => "ốc sên",
                2 => "heo Xayda mẹ",
                _ => "ốc mượn hồn",
            },
        );

        // %13: Bản đồ trung cấp
        result = result.replace(
            "%13",
            match gender {
                0 => "Rừng nấm",
                1 => "Thung lũng Namếc",
                2 => "Rừng nguyên sinh",
                _ => "Rừng nấm",
            },
        );

        // %14: Quái mẹ sơ cấp
        result = result.replace(
            "%14",
            match gender {
                0 => "phi long mẹ",
                1 => "quỷ bay mẹ",
                2 => "thằn lằn mẹ",
                _ => "phi long mẹ",
            },
        );

        result
    }
}
