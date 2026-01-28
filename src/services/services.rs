use crate::network::session::SessionArc;
use crate::{
    constant::cmd::cmd,
    network::{
        message::{self, Message},
        session::{self, AsyncSession},
    },
    player::Player,
};
use anyhow::Result;

pub struct ServiceHandles {}
impl ServiceHandles {
    pub fn send_gold_gem_ruby_to_client(player: &Player) -> Result<()> {
        let mut msg = Message::new(6);
        msg.write_long(player.inventory.get_gold())?;
        msg.write_int(player.inventory.get_gem())?;
        msg.write_int(player.inventory.get_ruby())?;
        player.send_to_client(msg)?;
        Ok(())
    }

    pub fn send_message_eat_dauthan(pl: &Player) -> Result<()> {
        let mut msg = Self::sub_command_30(14)?;

        msg.write_int(pl.id as i32)?;
        msg.write_int(pl.n_point.hp_current)?;
        msg.write_byte(1)?;
        msg.write_int(pl.n_point.hp_max)?;

        let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
        if let Some(zone) = zone_manager.get_zone(pl.map_id, pl.zone_id) {
            let _ = zone.send_message_to_other_players(pl.id, msg);
        }
        Ok(())
    }
    pub fn sub_command_30(byte: i8) -> Result<Message> {
        let mut msg = Message::new(30);
        msg.write_byte(byte)?;
        Ok(msg)
    }

    pub fn send_player_die(player: &Player) -> Result<()> {
        let mut msg = Message::new(-17);
        msg.write_byte(player.id as i8)?;
        msg.write_short(player.location.x)?;
        msg.write_short(player.location.y)?;
        player.send_to_client(msg)?;
        Ok(())
    }
    pub fn send_message_alert(player: &Player, text: &str) -> Result<()> {
        let mut response = Message::new(cmd::SEND_ALTER_MESSAGE);
        response.write_utf(text);
        player.send_to_client(response)?;
        Ok(())
    }
    pub fn send_message_alert_session(session: &SessionArc, text: &str) -> Result<()> {
        let mut response = Message::new(cmd::SEND_ALTER_MESSAGE);
        response.write_utf(text);
        session.transmit(response);
        Ok(())
    }
    pub async fn chat(session: &SessionArc, text: &str) -> Result<()> {
        let (player_id, map_id, zone_id) = session
            .get_player_ref(|player| {
                if let Some(player) = player {
                    Some((player.id, player.map_id, player.zone_id))
                } else {
                    None
                }
            })
            .await
            .unwrap_or_else(|| (0, 0, 0));

        if player_id == 0 {
            return Ok(());
        }

        let mut response = Message::new(cmd::CHAT);
        response.write_int(player_id as i32)?;
        response.write_utf(text)?;
        session.transmit(response.clone());
        let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
        if let Some(zone) = zone_manager.get_zone(map_id, zone_id) {
            zone.send_message_to_other_players(player_id, response)?;
        }
        Ok(())
    }
    pub fn send_mess_all_player_in_map(player: &Player, msg: Message) -> Result<()> {
        let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
        if let Some(zone) = zone_manager.get_zone(player.map_id, player.zone_id) {
            zone.send_message_to_all_players(msg)?;
        }
        Ok(())
    }

    pub fn send_mess_another_not_me_in_map(player: &Player, msg: Message) -> Result<()> {
        let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
        if let Some(zone) = zone_manager.get_zone(player.map_id, player.zone_id) {
            zone.send_message_to_other_players(player.id, msg)?;
        }
        Ok(())
    }

    pub fn send_item_time(player: &Player, item_id: i16, time: i16) -> Result<()> {
        let mut msg = Message::new(-106);
        msg.write_short(item_id)?;
        msg.write_short(time)?;
        player.send_to_client(msg)?;
        Ok(())
    }
}
