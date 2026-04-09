use crate::constant::const_npc::NpcId;
use crate::network::session::SessionArc;
use crate::player::player_actor::PlayerHandle;
use crate::services::player_info_service::sub_command_i30;
use crate::services::player_tnsm_services::TypeTNSM;
use crate::{
    constant::cmd::cmd,
    network::{
        message::{self, Message},
        session::{self, AsyncSession},
    },
    player::Player,
};
use anyhow::Result;
use tracing::debug;

pub struct ServiceHandles {}
impl ServiceHandles {
    /// Gửi thông báo (chữ chạy ở trên) cho toàn bộ người chơi trong server.
    pub fn broadcast_server(text: &str) {
        let mut msg = Message::new(-25);
        let _ = msg.write_utf(text);

        for entry in crate::player::player_manager::PLAYER_MANAGER.iter() {
            let handle = entry.value();
            if !handle.is_pet && handle.boss_info.is_none() {
                handle.send_forget(
                    crate::player::player_actor::message::PlayerMessage::SendPacket(msg.clone()),
                );
            }
        }
    }

    pub fn send_fusion_effect(pl: &Player, type_fusion: i8) -> Result<()> {
        let mut msg = Message::new(125);
        msg.write_byte(type_fusion)?;
        msg.write_int(pl.id as i32)?;
        Self::send_mess_all_player_in_map(pl, msg)?;
        Ok(())
    }
    pub fn send_hidden_npc(pl: &Player, npc_id: NpcId, is_hidden: bool) -> Result<()> {
        let mut msg = Message::new(-73);
        msg.write_byte(npc_id as i8)?;
        msg.write_byte(if is_hidden { 0 } else { 1 })?;
        pl.send_to_client(msg)?;
        Ok(())
    }
    pub fn send_item_time_client(pl: &Player, item_id: i16, time_seconds: i16) -> Result<()> {
        let mut msg = Message::new(-106);
        msg.write_short(item_id)?;
        msg.write_short(time_seconds)?;
        pl.send_to_client(msg)?;
        Ok(())
    }
    pub fn hide_wait_dialog_client(pl: &Player) -> Result<()> {
        let mut msg = Message::new(-99);
        msg.write_byte(-1)?;
        pl.send_to_client(msg)?;
        Ok(())
    }

    pub fn build_gold_gem_ruby_packet(player: &Player) -> Result<Message> {
        let mut msg = Message::new(6);
        msg.write_long(player.inventory.get_gold())?;
        msg.write_int(player.inventory.get_gem())?;
        msg.write_int(player.inventory.get_ruby())?;
        Ok(msg)
    }

    pub fn send_gold_gem_ruby_to_client(player: &Player) -> Result<()> {
        let msg = Self::build_gold_gem_ruby_packet(player)?;
        let _ = player.send_to_client(msg);
        Ok(())
    }

    #[deprecated(note = "Use build_gold_gem_ruby_packet instead")]
    pub fn send_gold_gem_ruby_to_client_actor(player: &Player) -> Result<Message> {
        Self::build_gold_gem_ruby_packet(player)
    }

    pub fn send_message_eat_dauthan(pl: &Player) -> Result<()> {
        let mut msg = Self::sub_command_30(14)?;

        msg.write_int(pl.id as i32)?;
        msg.write_int(pl.n_point.hp_current)?;
        msg.write_byte(1)?;
        msg.write_int(pl.n_point.hp_max)?;
        Self::send_mess_all_player_in_map(pl, msg)?;
        Ok(())
    }
    pub fn sub_command_30(byte: i8) -> Result<Message> {
        let mut msg = Message::new(-30);
        msg.write_byte(byte)?;
        Ok(msg)
    }

    pub fn send_hp_sync(pl: &Player) -> Result<()> {
        let mut msg = Self::sub_command_30(9)?;
        msg.write_int(pl.id as i32)?;
        msg.write_int(pl.n_point.hp_current)?;
        msg.write_int(pl.n_point.hp_max)?;
        Self::send_mess_another_not_me_in_map(pl, msg)?;
        Ok(())
    }

    pub fn send_player_injured(
        pl: &Player,
        damage: i32,
        is_crit: bool,
        effect_id: u8,
    ) -> Result<()> {
        let mut msg = Message::new(56);
        msg.write_int(pl.id as i32)?;
        msg.write_int(pl.n_point.hp_current)?;
        msg.write_int(damage)?;
        msg.write_bool(is_crit)?;
        msg.write_byte(-1)?;
        Self::send_mess_all_player_in_map(pl, msg)?;
        Ok(())
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
    pub fn send_thong_bao_to_player(player: &Player, text: &str) -> Result<()> {
        let mut response = Message::new(cmd::THONG_BAO);
        response.write_utf(text)?;
        player.send_to_client(response)?;
        Ok(())
    }
    pub fn build_thong_bao(text: &str) -> Message {
        let mut msg = Message::new(cmd::THONG_BAO);
        let _ = msg.write_utf(text);
        msg
    }
    pub fn send_message_alert_session(session: &SessionArc, text: &str) -> Result<()> {
        let mut response = Message::new(cmd::SEND_ALTER_MESSAGE);
        response.write_utf(text);
        session.transmit(response);
        Ok(())
    }
    pub fn chat(
        session: &SessionArc,
        player_id: u64,
        map_id: i32,
        zone_id: i32,
        text: &str,
    ) -> Result<()> {
        let mut response = Message::new(cmd::CHAT);
        response.write_int(player_id as i32)?;
        response.write_utf(text)?;
        session.transmit(response.clone());
        let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
        if let Some(zone) = zone_manager.get_zone(map_id, zone_id) {
            zone.broadcast_except(response, player_id);
        }
        Ok(())
    }
    pub fn send_message_chat_just_for_me(
        target: &Player,
        chat_player: &Player,
        text: &str,
    ) -> Result<()> {
        let mut msg = Message::new(cmd::CHAT);
        msg.write_int(chat_player.id as i32)?;
        msg.write_utf(text)?;
        target.send_to_client(msg)?;
        Ok(())
    }
    /// Broadcast a message to ALL players in the same zone (including self).
    pub fn send_mess_all_player_in_map(player: &Player, msg: Message) -> Result<()> {
        let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
        if let Some(zone) = zone_manager.get_zone(player.map_id, player.zone_id) {
            zone.broadcast(msg);
        }
        Ok(())
    }

    /// Broadcast a message to all players in the same zone EXCEPT self.
    pub fn send_mess_another_not_me_in_map(player: &Player, msg: Message) -> Result<()> {
        let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
        if let Some(zone) = zone_manager.get_zone(player.map_id, player.zone_id) {
            zone.broadcast_except(msg, player.id);
        }
        Ok(())
    }

    /// Broadcast to zone when you already have a ZoneHandle (used by actors).
    pub fn send_to_all_in_zone(
        zone: &crate::map::models::zone::ZoneHandle,
        msg: Message,
    ) -> Result<()> {
        zone.broadcast(msg);
        Ok(())
    }

    /// Broadcast to zone except one player, when you already have a ZoneHandle.
    pub fn send_to_other_in_zone(
        zone: &crate::map::models::zone::ZoneHandle,
        msg: Message,
        except_id: u64,
    ) -> Result<()> {
        zone.broadcast_except(msg, except_id);
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

    pub fn build_player_info_packet(target: &Player) -> Message {
        let mut msg = Message::new(-5);
        let is_monkey: i8 = if target.effect_skill.is_monkey { 1 } else { 0 };

        let _ = msg.write_int(target.id as i32);
        let _ = msg.write_int(target.clan_id);
        let _ = msg.write_byte(10); // level
        let _ = msg.write_bool(false); // is_invis
        let _ = msg.write_byte(target.type_pk as i8);
        let _ = msg.write_byte(target.gender);
        let _ = msg.write_byte(target.gender); // class
        let _ = msg.write_short(target.get_head());
        let _ = msg.write_utf(target.get_name());
        let _ = msg.write_int(target.n_point.hp_current);
        let _ = msg.write_int(target.n_point.hp_max);
        let _ = msg.write_short(target.get_body());
        let _ = msg.write_short(target.get_leg());
        let _ = msg.write_byte(0); // bag
        let _ = msg.write_byte(-1); // unknown_byte
        let _ = msg.write_short(target.location.x);
        let _ = msg.write_short(target.location.y);
        let _ = msg.write_short(0); // eff_buff_1
        let _ = msg.write_short(0); // eff_buff_2
        let _ = msg.write_byte(0); // eff_buff_3
        let _ = msg.write_byte(target.spaceship_id);
        let _ = msg.write_byte(is_monkey);
        let _ = msg.write_short(0); // mount_id
        let _ = msg.write_byte(0); // c_flag
        let _ = msg.write_byte(0); // none
        let _ = msg.write_short(target.get_aura());
        let _ = msg.write_byte(target.get_eff_front() as i8);
        let _ = msg.write_short(target.get_hat());
        msg
    }

    pub fn send_player_info_to_handle(
        handle: &PlayerHandle,
        target_snapshot: &Player,
    ) -> Result<()> {
        let msg = Self::build_player_info_packet(target_snapshot);
        handle.send_forget(crate::player::player_actor::PlayerMessage::SendPacket(msg));

        if target_snapshot.is_die() {
            let death_msg = Self::build_player_death_message(target_snapshot);
            handle.send_forget(crate::player::player_actor::PlayerMessage::SendPacket(
                death_msg,
            ));
        }

        Ok(())
    }

    pub fn send_player_info(pl_receiver: &Player, pl_target: &Player) -> Result<()> {
        let msg = Self::build_player_info_packet(pl_target);
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

    pub fn send_player_attack_mob(player: &Player, mob_id: u8) -> Result<()> {
        let mut msg = Message::new(54);
        msg.write_int(player.id as i32)?;
        let Some(skill) = player.player_skill.skill_select.as_ref() else {
            return Ok(());
        };
        debug!(
            "[DEBUG ATTACK] Player {} attack mob {} with skill {}",
            player.id, mob_id, skill.skill_id
        );
        msg.write_byte(skill.skill_id as i8)?;
        msg.write_byte(mob_id as i8)?;
        Self::send_mess_all_player_in_map(player, msg)?;
        Ok(())
    }

    pub fn send_player_attack_player(
        player: &Player,
        target_id: u64,
        damage: i32,
        is_die: bool,
        is_crit: bool,
    ) -> Result<()> {
        let mut msg = Message::new(-60);
        msg.write_int(player.id as i32)?; // id attacker
        let skill_id = player
            .player_skill
            .skill_select
            .as_ref()
            .map(|s| s.skill_id)
            .unwrap_or(0);
        msg.write_byte(skill_id as i8)?; // skill id
        msg.write_byte(1)?; // number of targets
        msg.write_int(target_id as i32)?; // target id
        msg.write_byte(1)?; // read continue
        msg.write_byte(0)?; // type skill (0: attack)
        msg.write_int(damage)?; // damage
        msg.write_bool(is_die)?; // is die
        msg.write_bool(is_crit)?; // is crit
        Self::send_mess_all_player_in_map(player, msg)?;
        Ok(())
    }

    // Removed: send_mess_another_not_me_in_map_handle was an identical duplicate of send_mess_another_not_me_in_map

    pub fn send_type_pk(player: &Player) -> Result<()> {
        let mut msg = Message::new(crate::constant::cmd::cmd::CHANGE_TYPE_PK);
        msg.write_byte(35)?;
        msg.write_int(player.id as i64 as i32)?;
        msg.write_byte(player.type_pk as i8)?;
        Self::send_mess_all_player_in_map(player, msg)?;
        Ok(())
    }

    pub fn send_revive_player(player: &Player) -> Result<()> {
        let mut msg = Self::sub_command_30(15)?;
        msg.write_int(player.id as i32)?;
        msg.write_int(player.n_point.hp_current)?;
        msg.write_int(player.n_point.mp_current)?;
        msg.write_short(player.location.x)?;
        msg.write_short(player.location.y)?;
        Self::send_mess_all_player_in_map(player, msg)?;
        Ok(())
    }

    pub fn send_player_menu(player: &Player, target: &Player) -> Result<()> {
        let mut msg = Message::new(-79);
        msg.write_int(target.id as i32)?;
        msg.write_long(target.n_point.power)?;
        msg.write_utf(&target.get_caption())?;
        player.send_to_client(msg)?;
        Ok(())
    }

    pub fn send_tnsm(player: &Player, type_tnsm: TypeTNSM, param: i64) -> Result<()> {
        let mut msg = Message::new(-3);
        msg.write_byte(type_tnsm as i8)?;
        msg.write_int(param as i32)?;
        player.send_to_client(msg)?;
        Ok(())
    }
}
