use crate::constant::const_map;
use crate::network::message::Message;
use crate::player::Player;
use crate::services::ServiceHandles;

pub fn is_map_black_ball_war(map_id: i32) -> bool {
    map_id >= const_map::BLACK_BALL_WAR_MAP_START && map_id <= const_map::BLACK_BALL_WAR_MAP_END
}
pub fn is_future_map(map_id: i32) -> bool {
    (92..=100).contains(&map_id)
}
pub fn is_mapa_mabu(map_id: i32) -> bool {
    matches!(map_id, 114 | 115 | 117 | 118 | 119 | 120)
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

pub fn is_map_doanh_trai(map_id: i32) -> bool {
    (53..=62).contains(&map_id)
}

pub fn is_map_pho_ban(map_id: i32) -> bool {
    is_map_doanh_trai(map_id)
        || (135..=138).contains(&map_id)
        || (141..=144).contains(&map_id)
        || (147..=152).contains(&map_id) && map_id != 150
}

pub fn is_map_offline(map_id: i32) -> bool {
    if let Some(template) = crate::templates::map_template_manager::get(map_id) {
        return template.r#type == 1;
    }
    false
}

pub fn is_map_tap_luyen(map_id: i32) -> bool {
    (191..=193).contains(&map_id)
}

pub fn build_player_teleport_message(player: &Player) -> Message {
    let mut msg = Message::new(123);
    let _ = msg.write_int(player.id as i32);
    let _ = msg.write_short(player.location.x);
    let _ = msg.write_short(player.location.y);
    let _ = msg.write_byte(1);
    msg
}
