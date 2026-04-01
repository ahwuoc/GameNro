//! Validation: map access checks and task requirements

use crate::constant::const_map::*;
use crate::map::services::change_map_models::*;
use crate::player::player::Player;

pub struct ValidationService;

impl ValidationService {
    pub fn check_map_access(player: &Player, map_id: i32) -> MapAccessResult {
        if map_id == -1 {
            return MapAccessResult::InvalidZone;
        }
        if player.is_boss || player.is_admin {
            return MapAccessResult::Allowed;
        }
        let required_task_id = Self::get_required_task_id_for_map(map_id);
        if required_task_id > 0 && player.get_task_id() < required_task_id {
            return MapAccessResult::TaskRequirementNotMet { required_task_id };
        }
        match player.gender {
            GENDER_TRAI_DAT if matches!(map_id, 22 | 23) => MapAccessResult::GenderRestricted {
                player_gender: player.gender,
                allowed_gender: (map_id - 21) as i8,
            },
            GENDER_NAMEC if matches!(map_id, 21 | 23) => MapAccessResult::GenderRestricted {
                player_gender: player.gender,
                allowed_gender: (map_id - 21) as i8,
            },
            GENDER_XAYDA if matches!(map_id, 21 | 22) => MapAccessResult::GenderRestricted {
                player_gender: player.gender,
                allowed_gender: (map_id - 21) as i8,
            },
            _ => MapAccessResult::Allowed,
        }
    }

    pub fn get_required_task_id_for_map(map_id: i32) -> i32 {
        match map_id {
            1 | 8 | 15             => TASK_1_0,
            42 | 43 | 44           => TASK_2_0,
            2 | 9 | 16             => TASK_3_0,
            24 | 25 | 26           => TASK_4_0,
            3 | 11 | 17            => TASK_7_0,
            27 | 28 | 31 | 32 | 35 | 36 => TASK_13_0,
            30 | 34 | 38           => TASK_15_0,
            6 | 10 | 19            => TASK_16_0,
            68 | 69 | 70 | 71 | 72 | 64 | 65 => TASK_18_0,
            63 | 66 | 67 | 73 | 74 | 75 | 76 | 77 | 81 | 82 | 83 | 79 => TASK_19_0,
            80                     => TASK_20_0,
            102 | 92 | 93 | 94 | 96 => TASK_21_0,
            97 | 98 | 99 | 100     => TASK_24_0,
            105 | 106 | 107 | 108 | 109 | 110 | 103 | 154 => TASK_27_0,
            _                      => 0,
        }
    }
}
