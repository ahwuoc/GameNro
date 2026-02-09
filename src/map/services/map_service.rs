use crate::constant::const_map;
use crate::network::message::Message;
use crate::player::Player;
use crate::services::ServiceHandles;

pub fn is_map_black_ball_war(map_id: i32) -> bool {
    map_id >= const_map::BLACK_BALL_WAR_MAP_START && map_id <= const_map::BLACK_BALL_WAR_MAP_END
}
pub fn is_map_tanthu(map_id: i32) -> bool {
    map_id == 1 || map_id == 8 || map_id == 15
}
pub fn is_map_ma_bu(map_id: i32) -> bool {
    map_id >= 114 && map_id <= 120
}

pub fn is_map_yardart(map_id: i32) -> bool {
    map_id >= 131 && map_id <= 133
}

pub fn is_map_boss_final(map_id: i32) -> bool {
    map_id == 111
}

pub fn is_map_huy_diet(map_id: i32) -> bool {
    map_id >= 169 && map_id <= 171
}

pub fn build_player_teleport_message(player: &Player) -> Message {
    let mut msg = Message::new(123);
    let _ = msg.write_int(player.id as i32);
    let _ = msg.write_short(player.location.x);
    let _ = msg.write_short(player.location.y);
    let _ = msg.write_byte(1);
    msg
}
