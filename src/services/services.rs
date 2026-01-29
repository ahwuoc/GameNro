use crate::network::session::SessionArc;
use crate::services::player_info_service::sub_command_i30;
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
        Self::send_mess_another_not_me_in_map(pl, msg)?;
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
    pub fn send_speed_to_client(pl: &Player, speed_modify: i8) -> Result<()> {
        let mut msg = sub_command_i30(8)?;
        msg.write_int(pl.id as i32)?;
        msg.write_byte(if speed_modify != -1 {
            speed_modify
        } else {
            pl.n_point.speed
        })?;
        pl.send_to_client(msg);
        Ok(())
    }

    pub fn send_cai_trang(player: &Player) -> anyhow::Result<()> {
        println!(
            "[DEBUG BIENKHI] send_cai_trang called, is_monkey={}",
            player.effect_skill.is_monkey
        );
        let mut message = Message::new(-90);
        message.write_byte(1)?;
        message.write_int(player.id as i32)?;
        message.write_short(player.get_head())?;
        message.write_short(player.get_body())?;
        message.write_short(player.get_leg())?;
        message.write_byte(if player.effect_skill.is_monkey { 1 } else { 0 })?;
        Self::send_mess_all_player_in_map(player, message)?;
        Ok(())
    }

    pub fn send_item_time(player: &Player, item_id: i16, time: i16) -> Result<()> {
        let mut msg = Message::new(-106);
        msg.write_short(item_id)?;
        msg.write_short(time)?;
        player.send_to_client(msg)?;
        Ok(())
    }

    pub fn send_player_info(pl_receiver: &Player, pl_target: &Player) -> Result<()> {
        let mut msg = Message::new(-5);

        let id = pl_target.id as i32;
        let clan_id = -1;
        let level = 10;
        let is_invis = false;
        let type_pk = pl_target.type_pk;
        let gender = pl_target.gender;
        let class = pl_target.gender;
        let head = pl_target.get_head();
        let name = pl_target.get_name();
        let hp = pl_target.n_point.hp_current;
        let max_hp = pl_target.n_point.hp_max;
        let body = pl_target.get_body();
        let leg = pl_target.get_leg();
        let bag = 0;
        let unknown_byte = -1;
        let x = pl_target.location.x;
        let y = pl_target.location.y;
        let eff_buff_1 = 0;
        let eff_buff_2 = 0;
        let eff_buff_3 = 0;
        let spaceship_id = 0;
        let is_monkey = 0;
        let mount_id = 0;
        let c_flag = 0;
        let none = 0;

        let _ = msg.write_int(id);
        let _ = msg.write_int(clan_id);
        let _ = msg.write_byte(level);
        let _ = msg.write_bool(is_invis);
        let _ = msg.write_byte(type_pk);
        let _ = msg.write_byte(gender);
        let _ = msg.write_byte(class);
        let _ = msg.write_short(head);
        let _ = msg.write_utf(name);
        let _ = msg.write_int(hp);
        let _ = msg.write_int(max_hp);
        let _ = msg.write_short(body);
        let _ = msg.write_short(leg);
        let _ = msg.write_byte(bag);
        let _ = msg.write_byte(unknown_byte);
        let _ = msg.write_short(x);
        let _ = msg.write_short(y);
        let _ = msg.write_short(eff_buff_1);
        let _ = msg.write_short(eff_buff_2);
        let _ = msg.write_byte(eff_buff_3);
        let _ = msg.write_byte(spaceship_id);
        let _ = msg.write_byte(is_monkey);
        let _ = msg.write_short(mount_id);
        let _ = msg.write_byte(c_flag);
        let _ = msg.write_byte(none);

        if pl_target.is_pl() {
            let id_aura = 0;
            let aura = 0;
            let eff_front = 0;

            let _ = msg.write_short(id_aura);
            let _ = msg.write_short(aura);
            let _ = msg.write_byte(eff_front);
        }
        pl_receiver.send_to_client(msg)?;
        Ok(())
    }

    pub fn build_player_death_message(pl_info: &Player) -> Message {
        let mut msg = Message::new(-8);
        let _ = msg.write_short(pl_info.id as i16);
        let _ = msg.write_byte(0);
        let _ = msg.write_short(pl_info.location.x);
        let _ = msg.write_short(pl_info.location.y);
        msg
    }
}
