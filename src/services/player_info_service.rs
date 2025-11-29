use crate::data::DataGame;
use crate::network::message::Message;
use crate::network::session::AsyncSession;
use crate::player::Player as RtPlayer;
use crate::services::{IntrinsicService, ZoneService};

pub struct PlayerInfoService;

impl PlayerInfoService {
    pub async fn send_point_info(
        session: &mut AsyncSession,
        player: &RtPlayer,
    ) -> anyhow::Result<()> {
        let mut msg = Message::new(-42);
        msg.write_int(player.n_point.base_hp)?; // hpg
        msg.write_int(player.n_point.base_mp)?; // mpg
        msg.write_int(player.n_point.base_dame)?; // dameg
        msg.write_int(player.n_point.max_hp)?; // hpMax
        msg.write_int(player.n_point.max_mp)?; // mpMax
        msg.write_int(player.n_point.final_hp)?; // hp
        msg.write_int(player.n_point.final_mp)?; // mp
        msg.write_byte(player.n_point.speed)?; // speed
        msg.write_byte(20)?; // reserved
        msg.write_byte(20)?; // reserved
        msg.write_byte(1)?; // reserved
        msg.write_int(player.n_point.final_dame)?; // dame
        msg.write_int(player.n_point.final_def)?; // def
        msg.write_byte(player.n_point.final_crit)?; // crit
        msg.write_long(player.n_point.tiem_nang)?; // tiemNang
        msg.write_short(100)?; // reserved
        msg.write_int(player.n_point.base_def)?; // defg (reserved)
        msg.write_byte(player.n_point.base_crit)?; // critg (reserved)

        session.send_message(&msg).await?;
        Ok(())
    }

    pub async fn send_task_info(session: &mut AsyncSession) -> anyhow::Result<()> {
        println!("Sending task main info");

        let mut msg = Message::new(40);
        msg.write_short(1)?; // taskMain.id
        msg.write_byte(0)?; // taskMain.index
        msg.write_utf("Nhiệm vụ chính[1]")?; // taskMain.name + "[" + taskMain.id + "]"
        msg.write_utf("Chi tiết nhiệm vụ chính")?; // taskMain.detail
        msg.write_byte(1)?; // subTasks.size()

        msg.write_utf("Nhiệm vụ phụ")?; // stm.name
        msg.write_byte(1)?; // stm.npcId
        msg.write_short(1)?; // stm.mapId
        msg.write_utf("Thông báo nhiệm vụ")?; // stm.notify

        msg.write_short(0)?; // current subTask count
        msg.write_short(10)?; // stm.maxCount

        session.send_message(&msg).await?;
        Ok(())
    }

    /// Clear map (-22)
    pub async fn clear_map(session: &mut AsyncSession) -> anyhow::Result<()> {
        let mut msg = Message::new(-22);
        session.send_message(&msg).await?;
        Ok(())
    }

    pub async fn send_clan_info(session: &mut AsyncSession) -> anyhow::Result<()> {
        println!("Sending clan info");

        let mut msg = Message::new(-53);
        msg.write_int(-1)?; // clan.id or -1 if no clan

        session.send_message(&msg).await?;
        Ok(())
    }

    /// Send max stamina (-69)
    pub async fn send_max_stamina(session: &mut AsyncSession) -> anyhow::Result<()> {
        println!("Sending max stamina");

        let mut msg = Message::new(-69);
        msg.write_int(100)?; // max stamina

        session.send_message(&msg).await?;
        Ok(())
    }

    /// Send current stamina (-68)
    pub async fn send_current_stamina(session: &mut AsyncSession) -> anyhow::Result<()> {
        println!("Sending current stamina");

        let mut msg = Message::new(-68);
        msg.write_int(100)?; // current stamina

        session.send_message(&msg).await?;
        Ok(())
    }

    pub async fn send_pet_info(session: &mut AsyncSession) -> anyhow::Result<()> {
        println!("Sending pet info");
        let mut msg = Message::new(-107);
        msg.write_byte(0)?;
        session.send_message(&msg).await?;
        Ok(())
    }

    pub async fn send_top_rank_info(session: &mut AsyncSession) -> anyhow::Result<()> {
        println!("Sending top rank info");

        let mut msg = Message::new(-119);
        msg.write_utf("1630679754740_-119_r")?;

        session.send_message(&msg).await?;
        Ok(())
    }

    /// Send notification tab (-50)
    pub async fn send_notification_tab(session: &mut AsyncSession) -> anyhow::Result<()> {
        let mut msg = Message::new(-50);
        msg.write_byte(0)?; // notification count

        session.send_message(&msg).await?;
        Ok(())
    }

    pub async fn send_time_skill(session: &mut AsyncSession) -> anyhow::Result<()> {
        println!("Sending time skill info");

        let mut msg = Message::new(-30);
        msg.write_byte(62)?; // sub command

        session.send_message(&msg).await?;
        Ok(())
    }

   pub async fn sub_command_30(sub_command: i8) -> anyhow::Result<Message> {
        let mut msg = Message::new(-30);  
        msg.write_byte(sub_command)?;
        Ok(msg)
   }

   pub async fn send_player_blob_internal(
        session: &mut AsyncSession,
        player: &RtPlayer,
    ) -> anyhow::Result<()> {
        let mut msg = Self::sub_command_30(0).await?;
        
        // Basic player info
        msg.write_int(player.id as i32)?; // charID
        msg.write_byte(0)?; // ctaskId
        msg.write_byte(player.gender)?; // cgender
        msg.write_short(player.head)?; // head
        msg.write_utf(&player.name)?; // cName
        msg.write_byte(0)?; // cPk
        msg.write_byte(player.type_pk)?; // cTypePk
        msg.write_long(player.n_point.power)?; // cPower
        // applyCharLevelPercent() - client method call, no data read
        msg.write_short(0)?; // eff5BuffHp
        msg.write_short(0)?; // eff5BuffMp
        msg.write_byte(0)?; // nClass index (GameScr.nClasss[...])
        
        // Skills - client expects to read skills here
        msg.write_byte(0)?; // number of skills (sbyte b2)
        // No skills to write since we wrote 0
        
        // Currency - client always reads xu as long
        msg.write_long(player.inventory.get_gold())?; // xu 
        msg.write_int(player.inventory.get_ruby())?; // luongKhoa
        msg.write_int(player.inventory.get_gem())?; // luong

        // Body items
        let body_len = player.inventory.items_body.len() as i8;
        msg.write_byte(body_len)?;

        for item in player.inventory.items_body.iter().take(body_len as usize) {
            if !item.is_not_null_item() {
                msg.write_short(-1)?;
            } else {
                if let Some(tpl) = &item.template {
                    msg.write_short(tpl.id)?;
                } else {
                    msg.write_short(-1)?;
                }
                msg.write_int(item.quantity)?;
                msg.write_utf(&item.get_info())?;
                msg.write_utf(&item.get_content())?;

                let opts_len = (item.item_options.len().min(255)) as u8;
                msg.write_byte(opts_len as i8)?;
                for opt in item.item_options.iter().take(opts_len as usize) {
                    msg.write_byte(opt.get_option_id() as i8)?;
                    msg.write_short(opt.get_param() as i16)?;
                }
            }
        }

        // Bag items
        let bag_len = (player.inventory.items_bag.len().min(255)) as i8;
        msg.write_byte(bag_len)?;

        for item in player.inventory.items_bag.iter().take(bag_len as usize) {
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

        // Box items
        let box_len = (player.inventory.items_box.len().min(255)) as i8;
        msg.write_byte(box_len)?;

        for item in player.inventory.items_box.iter().take(box_len as usize) {
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

        // Head/Avatar data
        msg.write_short(0)?; // num17 - number of head/avatar pairs
        // No head/avatar data since we wrote 0
        
        // Character info IDs for gender
        msg.write_short(514)?; // charId[gender][0]
        msg.write_short(515)?; // charId[gender][1] 
        msg.write_short(537)?; // charId[gender][2]
        
        msg.write_byte(0)?; // isNhapThe (0 = false, 1 = true)
        msg.write_int(333)?; // deltaTime (server time)
        msg.write_byte(if player.is_new_member { 1 } else { 0 })?; // isNewMember
        
        // Additional effects (version dependent)
        msg.write_short(0)?; // idAuraEff
        msg.write_byte(0)?; // idEff_Set_Item
        msg.write_short(0)?; // idHat

        session.send_message(&msg).await?;
        Ok(())
    }

    pub async fn send_cai_trang(
        session: &mut AsyncSession,
        _player: &RtPlayer,
    ) -> anyhow::Result<()> {
        let mut message = Message::new(-90);
        message.write_byte(1)?;
        message.write_int(_player.id as i32)?;

        message.write_short(_player.get_head())?;
        message.write_short(_player.get_body())?;
        message.write_short(_player.get_leg())?;
        message.write_byte(0)?;

        let player = session
            .get_player()
            .cloned()
            .ok_or_else(|| anyhow::anyhow!("Player not set"))?;
        if let Some(zone) = &player.zone {
            zone.send_message_to_all_players(message.clone()).await?;
        }
        session.send_message(&message).await?;
        Ok(())
    }

    pub async fn clear_vtsk(_session: &mut AsyncSession) -> anyhow::Result<()> {
        Ok(())
    }

    pub async fn send_all_player_info(session: &mut AsyncSession) -> anyhow::Result<()> {
        println!("Sending all player info");

        let player = session
            .get_player()
            .cloned()
            .ok_or_else(|| anyhow::anyhow!("Player not set"))?;
        DataGame::send_data_item_bg(session).await?;

        //-82
        DataGame::send_tile_set_info(session).await?;

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
        Self::send_player_blob_internal(session, &player).await?;

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
        ZoneService::load_player_to_best_zone(player.clone(), session).await?;

        Self::send_cai_trang(session, &player).await?;

        Self::send_time_skill(session).await?;

        // clear vt sk
        Self::clear_vtsk(session).await?;

        println!("All player info sent successfully");
        Ok(())
    }
}
