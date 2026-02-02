use crate::constant::const_map;
use crate::network::message::Message;
use crate::player::Player;
use crate::services::ServiceHandles;

pub fn is_ma_black_ball_war(map_id: i32) -> bool {
    map_id >= const_map::BLACK_BALL_WAR_MAP_START && map_id <= const_map::BLACK_BALL_WAR_MAP_END
}

pub fn build_player_teleport_message(player: &Player) -> Message {
    let mut msg = Message::new(123);
    let _ = msg.write_int(player.id as i32);
    let _ = msg.write_short(player.location.x);
    let _ = msg.write_short(player.location.y);
    let _ = msg.write_byte(1);
    msg
}
