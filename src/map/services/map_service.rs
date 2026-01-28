use crate::constant::const_map;
use crate::network::message::Message;
use crate::player::Player;
use crate::services::ServiceHandles;

pub fn is_ma_black_ball_war(pl: &Player) -> bool {
    pl.map_id >= const_map::BLACK_BALL_WAR_MAP_START
        && pl.map_id <= const_map::BLACK_BALL_WAR_MAP_END
}

pub fn send_player_teleport(player: &Player) -> anyhow::Result<()> {
    let mut msg = Message::new(123);
    msg.write_int(player.id as i32)?;
    msg.write_short(player.location.x)?;
    msg.write_short(player.location.y)?;
    msg.write_byte(1)?; // Java writes 1 here
    ServiceHandles::send_mess_all_player_in_map(player, msg)?;
    Ok(())
}
