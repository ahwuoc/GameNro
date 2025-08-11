use crate::data::DataGame;
use crate::network::async_net::message::Message;
use crate::network::async_net::session::AsyncSession;
use crate::player::Player as RtPlayer;
use crate::services::IntrinsicService;
use crate::msg_write;

pub struct PlayerInfoService;

impl PlayerInfoService {
    pub async fn player(
        session: &mut AsyncSession,
        player: &RtPlayer,
    ) -> Result<(), Box<dyn std::error::Error>> {
        Ok(())
    }
    pub async fn send_point_info(
        session: &mut AsyncSession,
        player: &RtPlayer,
    ) -> Result<(), Box<dyn std::error::Error>> {
        println!("Sending point info");
        let n = &player.n_point;
        let hp_max = n.hp_max as i64;
        let mp_max = n.mp_max as i64;
        let hp = n.hp as i64;
        let mp = n.mp as i64;
        let damage = n.damage as i64;
        let defense = n.defense as i64;
        let crit = (n.crit.min(255)) as u8;
        let power = n.power as i64;
        let speed: u8 = 10;

        let mut msg = Message::new_for_writing(-42);
        msg_write!(msg, write_long(hp_max)); // hpg
        msg_write!(msg, write_long(mp_max)); // mpg
        msg_write!(msg, write_long(damage)); // dameg
        msg_write!(msg, write_long(hp_max)); // hpMax
        msg_write!(msg, write_long(mp_max)); // mpMax
        msg_write!(msg, write_long(hp)); // hp
        msg_write!(msg, write_long(mp)); // mp
        msg_write!(msg, write_byte(speed as i8)); // speed
        msg_write!(msg, write_byte(20)); // reserved
        msg_write!(msg, write_byte(20)); // reserved
        msg_write!(msg, write_byte(1)); // reserved
        msg_write!(msg, write_long(damage)); // dame
        msg_write!(msg, write_long(defense)); // def
        msg_write!(msg, write_byte(crit as i8)); // crit
        msg_write!(msg, write_long(power)); // tiemNang
        msg_write!(msg, write_short(0)); // reserved
        msg_write!(msg, write_long(0)); // defg (reserved)
        msg_write!(msg, write_byte(0)); // critg (reserved)
        msg.finalize_write();
        session.send_message(&msg).await?;

        Ok(())
    }

    pub async fn send_task_info(
        session: &mut AsyncSession,
    ) -> Result<(), Box<dyn std::error::Error>> {
        println!("Sending task main info");

        let mut msg = Message::new_for_writing(40);
        msg_write!(msg, write_short(1)); // taskMain.id
        msg_write!(msg, write_byte(0)); // taskMain.index
        msg_write!(msg, write_utf("Nhiệm vụ chính[1]")); // taskMain.name + "[" + taskMain.id + "]"
        msg_write!(msg, write_utf("Chi tiết nhiệm vụ chính")); // taskMain.detail
        msg_write!(msg, write_byte(1)); // subTasks.size()

        msg_write!(msg, write_utf("Nhiệm vụ phụ")); // stm.name
        msg_write!(msg, write_byte(1)); // stm.npcId
        msg_write!(msg, write_short(1)); // stm.mapId
        msg_write!(msg, write_utf("Thông báo nhiệm vụ")); // stm.notify

        msg_write!(msg, write_short(0)); // current subTask count
        msg_write!(msg, write_short(10)); // stm.maxCount

        msg.finalize_write();
        session.send_message(&msg).await?;
        Ok(())
    }
    /// Clear map (-22)
    pub async fn clear_map(session: &mut AsyncSession) -> Result<(), Box<dyn std::error::Error>> {
        let mut msg = Message::new_for_writing(-22);
        msg.finalize_write();
        session.send_message(&msg).await?;
        Ok(())
    }

    pub async fn send_clan_info(
        session: &mut AsyncSession,
    ) -> Result<(), Box<dyn std::error::Error>> {
        println!("Sending clan info");

        let mut msg = Message::new_for_writing(-53);
        msg_write!(msg, write_int(-1)); // clan.id or -1 if no clan

        msg.finalize_write();
        session.send_message(&msg).await?;

        Ok(())
    }

    /// Send max stamina (-69)
    pub async fn send_max_stamina(
        session: &mut AsyncSession,
    ) -> Result<(), Box<dyn std::error::Error>> {
        println!("Sending max stamina");

        let mut msg = Message::new_for_writing(-69);
        msg_write!(msg, write_int(100)); // max stamina
        msg.finalize_write();
        session.send_message(&msg).await?;

        Ok(())
    }

    /// Send current stamina (-68)
    pub async fn send_current_stamina(
        session: &mut AsyncSession,
    ) -> Result<(), Box<dyn std::error::Error>> {
        println!("Sending current stamina");

        let mut msg = Message::new_for_writing(-68);
        msg_write!(msg, write_int(100)); // current stamina
        msg.finalize_write();
        session.send_message(&msg).await?;

        Ok(())
    }

    pub async fn send_pet_info(
        session: &mut AsyncSession,
    ) -> Result<(), Box<dyn std::error::Error>> {
        println!("Sending pet info");
        let mut msg = Message::new_for_writing(-107);
        msg_write!(msg, write_byte(0)); 
        msg.finalize_write();
        session.send_message(&msg).await?;

        Ok(())
    }

    pub async fn send_top_rank_info(
        session: &mut AsyncSession,
    ) -> Result<(), Box<dyn std::error::Error>> {
        println!("Sending top rank info");

        let mut msg = Message::new_for_writing(-119);
        msg_write!(msg, write_utf("1630679754740_-119_r"));
        msg.finalize_write();
        session.send_message(&msg).await?;

        Ok(())
    }

    /// Send notification tab (-50)
    pub async fn send_notification_tab(
        session: &mut AsyncSession,
    ) -> Result<(), Box<dyn std::error::Error>> {
        let mut msg = Message::new_for_writing(-50);
        msg_write!(msg, write_byte(0)); // notification count
        msg.finalize_write();
        session.send_message(&msg).await?;

        Ok(())
    }
    /// Send big message (-70)
    pub async fn send_big_message(
        session: &mut AsyncSession,
    ) -> Result<(), Box<dyn std::error::Error>> {
        let mut msg = Message::new_for_writing(-70);
        msg_write!(msg, write_utf("Chào mừng đến với GameNro!"));
        msg.finalize_write();
        session.send_message(&msg).await?;

        Ok(())
    }

    pub async fn send_time_skill(
        session: &mut AsyncSession,
    ) -> Result<(), Box<dyn std::error::Error>> {
        println!("Sending time skill info");

        let mut msg = Message::new_for_writing(-30);
        msg_write!(msg, write_byte(62)); // sub command
        msg.finalize_write();
        session.send_message(&msg).await?;

        Ok(())
    }

    pub async fn send_player_blob(
        session: &mut AsyncSession,
        player: &RtPlayer,
    ) -> Result<(), Box<dyn std::error::Error>> {
        Self::send_player_blob_internal(session, player).await?;
        Ok(())
    }

    async fn send_player_blob_internal(
        session: &mut AsyncSession,
        player: &RtPlayer,
    ) -> Result<(), Box<dyn std::error::Error>> {
        let mut msg = Message::new_for_writing(-30);
        msg.write_byte(0)?;
        msg.write_int(player.id as i32)?;
        msg.write_byte(1)?; 
        msg.write_byte(player.gender as i8)?; 
        msg.write_short(player.head)?;
        msg.write_utf(&player.name)?; 
        msg.write_byte(0)?; 
        msg.write_byte(player.type_pk as i8)?; // typePk
        msg.write_long(player.n_point.power as i64)?; // power
        msg.write_short(0)?; 
        msg.write_short(0)?; 
        msg.write_byte(player.gender as i8)?;
        msg.write_byte(0)?;
        if session.get_version() >= 214 && session.get_version() < 231 {
            msg.write_long(player.inventory.get_gold())?;
        } else {
            msg.write_int(player.inventory.get_gold() as i32)?;
        }
        msg.write_int(player.inventory.get_ruby())?;
        msg.write_int(player.inventory.get_gem())?;
        
        let body_len = (player.inventory.items_body.len().min(255)) as i8;
        msg.write_byte(body_len)?;
        
        let mut body_items_sent = 0;
        for (index, item) in player.inventory.items_body.iter().take(body_len as usize).enumerate() {
            if !item.is_not_null_item() {
                msg.write_short(-1)?;
            } else {
                if let Some(tpl) = &item.template {
                    msg.write_short(tpl.id as i16)?;
                } else {
                    msg.write_short(-1)?;
                }
                msg.write_int(item.quantity)?;
                msg.write_utf(&item.get_info())?;
                msg.write_utf(&item.get_content())?;
                let opts_len = (item.item_options.len().min(255)) as i8;
                msg.write_byte(opts_len)?;
                for opt in item.item_options.iter().take(opts_len as usize) {
                    msg.write_byte(opt.get_option_id() as i8)?;
                    msg.write_short(opt.get_param() as i16)?;
                }
            }
        }
        let bag_len = (player.inventory.items_bag.len().min(255)) as i8;
        msg.write_byte(bag_len)?;
        
        let mut bag_items_sent = 0;
        for (index, item) in player.inventory.items_bag.iter().take(bag_len as usize).enumerate() {
            if !item.is_not_null_item() {
                msg.write_short(-1)?;
            } else {
                if let Some(tpl) = &item.template {
                    msg.write_short(tpl.id as i16)?;
                    bag_items_sent += 1;
                } else {
                    msg.write_short(-1)?;
                }
                msg.write_int(item.quantity)?;
                msg.write_utf(&item.get_info())?;
                msg.write_utf(&item.get_content())?;
                let opts_len = (item.item_options.len().min(255)) as i8;
                msg.write_byte(opts_len)?;
                for opt in item.item_options.iter().take(opts_len as usize) {
                    msg.write_byte(opt.get_option_id() as i8)?;
                    msg.write_short(opt.get_param() as i16)?;
                }
            }
        }
        let box_len = (player.inventory.items_box.len().min(255)) as i8;
        msg.write_byte(box_len)?;
        
        let mut box_items_sent = 0;
        for (index, item) in player.inventory.items_box.iter().take(box_len as usize).enumerate() {
            if !item.is_not_null_item() {
                msg.write_short(-1)?;
            } else {
                if let Some(tpl) = &item.template {
                    msg.write_short(tpl.id as i16)?;
                } else {
                    msg.write_short(-1)?;
                }
                msg.write_int(item.quantity)?;
                msg.write_utf(&item.get_info())?;
                msg.write_utf(&item.get_content())?;
                let opts_len = (item.item_options.len().min(255)) as i8;
                msg.write_byte(opts_len)?;
                for opt in item.item_options.iter().take(opts_len as usize) {
                    msg.write_byte(opt.get_option_id() as i8)?;
                    msg.write_short(opt.get_param() as i16)?;
                }
            }
        }
        msg.write_short(0)?;
        msg.write_short(514)?;
        msg.write_short(515)?;
        msg.write_short(537)?;
        msg.write_byte(0)?; // fusion flag
        msg.write_int(333)?; // delta time
        msg.write_byte(if player.is_new_member { 1 } else { 0 })?;
        // Add missing data like Java server
        msg.write_short(514)?; // char info id - con chim thông báo
        msg.write_short(515)?; // char info id  
        msg.write_short(537)?; // char info id
        msg.write_byte(0)?; // fusion type (0 = non-fusion)
        msg.write_int(333)?; // deltatime
        msg.write_byte(0)?; // isNewMember
        msg.write_short(0)?; // aura
        msg.write_byte(0)?; // eff front
        msg.finalize_write();
        session.send_message(&msg).await?;
   
        Ok(())
    }


    
    pub async fn send_cai_trang(
        session: &mut AsyncSession,
        _player: &RtPlayer,
    ) -> Result<(), Box<dyn std::error::Error>> {
        let mut message = Message::new_for_writing(-90);
        msg_write!(message, write_byte(1));
        msg_write!(message, write_int(_player.id as i32));
        let head = _player.get_head();
        let body = _player.get_body();
        let leg = _player.get_leg();
        msg_write!(message, write_short(head));
        msg_write!(message, write_short(body));
        msg_write!(message, write_short(leg));
        msg_write!(message, write_byte(0));
        let player = session.get_player().cloned().ok_or("Player not set")?;
        if let Some(zone) = &player.zone {
            zone.send_message_to_all_players(message.clone()).await?;
        }
        session.send_message(&message).await?;
        Ok(())
    }

    pub async fn clear_vtsk(_session: &mut AsyncSession) -> Result<(), Box<dyn std::error::Error>> {
        Ok(())
    }

    pub async fn send_player_info(
        _session: &mut AsyncSession,
        _player: &RtPlayer,
    ) -> Result<(), Box<dyn std::error::Error>> {
        let mut msg = Message::new_for_writing(-30);
        msg.write_byte(0)?;
        msg.write_int(_player.id as i32)?;
        msg.write_long(_player.n_point.hp as i64)?;
        msg.write_byte(0)?;
        msg.write_long(_player.n_point.hp_max as i64)?;
        if let Some(zone) = &_player.zone {
            zone.send_message_to_all_players(msg).await?;
        }
        Ok(())
    }
    pub async fn send_all_player_info(
        session: &mut AsyncSession,
    ) -> Result<(), Box<dyn std::error::Error>> {
        println!("Sending all player info");
        let player = session.get_player().cloned().ok_or("Player not set")?;
        DataGame::send_tile_set_info(session).await?;

        // 112 my info intrinsic
        let intrinsic_service: IntrinsicService = IntrinsicService;
        intrinsic_service
            .send_info_intrinsic(session, &player)
            .await?;

        // -42 my point
        Self::send_point_info(session, &player).await?;

        // 40 task
        Self::send_task_info(session).await?;

        // -22 reset all
        Self::clear_map(session).await?;

        // -30 sub 0 player blob
        Self::send_player_blob(session, &player).await?;

        // -53 my clan
        Self::send_clan_info(session).await?;

        // -69 max stamina
        Self::send_max_stamina(session).await?;

        // -68 cur stamina
        Self::send_current_stamina(session).await?;

        // -107 have pet
        Self::send_pet_info(session).await?;

        // -119 top rank
        Self::send_top_rank_info(session).await?;

        // -50 thông tin bảng thông báo
        Self::send_notification_tab(session).await?;
        crate::services::ZoneService::load_player_to_best_zone(player.clone(), session).await?;
        Self::send_cai_trang(session, &player).await?;
        Self::send_big_message(session).await?;

        // last time use skill
        Self::send_time_skill(session).await?;

        // clear vt sk
        Self::clear_vtsk(session).await?;

        println!("All player info sent successfully");
        Ok(())
    }
 
}
