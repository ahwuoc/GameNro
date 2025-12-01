use crate::data::DataGame;
use crate::network::message::Message;
use crate::network::session::AsyncSession;
use crate::player::Player as RtPlayer;
use crate::services::{IntrinsicService, ZoneService};
use std::fs::OpenOptions;
use std::io::Write;

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

    fn debug_write_to_file(content: &str) {
        if let Ok(mut file) = OpenOptions::new()
            .create(true)
            .append(true)
            .open("debug_player_blob.txt")
        {
            let _ = writeln!(file, "{}", content);
        }
    }

    pub async fn send_player_blob_internal(
        session: &mut AsyncSession,
        player: &RtPlayer,
    ) -> anyhow::Result<()> {
        let mut msg = Self::sub_command_30(0).await?;

        Self::debug_write_to_file("=== SEND_PLAYER_BLOB_INTERNAL DEBUG ===");
        Self::debug_write_to_file(&format!("Message ID: -30, Sub command: 0"));

        // Basic player info
        msg.write_int(player.id as i32)?; // charID
        Self::debug_write_to_file(&format!("1. charID (int): {}", player.id));
        msg.write_byte(0)?; // ctaskId
        Self::debug_write_to_file(&format!("2. ctaskId (byte): 0"));

        msg.write_byte(player.gender)?; // cgender
        Self::debug_write_to_file(&format!("3. cgender (byte): {}", player.gender));

        msg.write_short(player.head)?; // head
        Self::debug_write_to_file(&format!("4. head (short): {}", player.head));

        msg.write_utf(&player.name)?; // cName
        Self::debug_write_to_file(&format!("5. cName (UTF): '{}'", player.name));

        msg.write_byte(0)?; // cPk
        Self::debug_write_to_file(&format!("6. cPk (byte): 0"));

        msg.write_byte(player.type_pk)?; // cTypePk
        Self::debug_write_to_file(&format!("7. cTypePk (byte): {}", player.type_pk));

        msg.write_long(player.n_point.power)?; // cPower
        Self::debug_write_to_file(&format!("8. cPower (long): {}", player.n_point.power));
        // applyCharLevelPercent() - client method call, no data read
        msg.write_short(0)?; // eff5BuffHp
        Self::debug_write_to_file(&format!("9. eff5BuffHp (short): 0"));

        msg.write_short(0)?; // eff5BuffMp
        Self::debug_write_to_file(&format!("10. eff5BuffMp (short): 0"));

        msg.write_byte(0)?; // nClass index (GameScr.nClasss[...])
        Self::debug_write_to_file(&format!("11. nClass (byte): 0"));

        // Skills - client expects to read skills here
        msg.write_byte(0)?; // number of skills (sbyte b2)
        Self::debug_write_to_file(&format!("12. skills_count (byte): 0"));
        // No skills to write since we wrote 0

        // Currency - client always reads xu as long
        msg.write_long(player.inventory.get_gold())?; // xu
        Self::debug_write_to_file(&format!("13. xu (long): {}", player.inventory.get_gold()));

        msg.write_int(player.inventory.get_ruby())?; // luongKhoa
        Self::debug_write_to_file(&format!(
            "14. luongKhoa (int): {}",
            player.inventory.get_ruby()
        ));
        msg.write_int(player.inventory.get_gem())?; // luong

        // Body items
        let body_len = (player.inventory.items_body.len().min(255)) as i8;
        Self::debug_write_to_file(&format!("15. body_items_count (byte): {}", body_len));
        msg.write_byte(body_len)?;

        for (index, item) in player
            .inventory
            .items_body
            .iter()
            .enumerate()
            .take(body_len as usize)
        {
            if item.is_null_item() {
                msg.write_short(-1)?;
            } else {
                if let Some(tpl) = &item.template {
                    msg.write_short(tpl.id)?;
                    msg.write_int(item.quantity)?;
                    msg.write_utf(&item.get_info())?;
                    msg.write_utf(&item.get_content())?;
                    if item.item_options.is_empty() {
                        msg.write_byte(1);
                        msg.write_byte(1);
                        msg.write_short(0);
                    } else {
                        let opts_len = item.item_options.len() as i8;
                        msg.write_byte(opts_len)?;
                        for (opt_index, opt) in item.item_options.iter().enumerate() {
                            msg.write_byte(opt.get_option_id())?;
                            msg.write_short(opt.get_param())?;
                        }
                    }
                } else {
                    msg.write_short(-1)?;
                }
            }
        }

        // Bag items
        let bag_len = (player.inventory.items_bag.len().min(255)) as i8;
        msg.write_byte(bag_len)?;
        for (index, item) in player
            .inventory
            .items_bag
            .iter()
            .enumerate()
            .take(bag_len as usize)
        {
            if item.is_null_item() {
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
                if item.item_options.is_empty() {
                    msg.write_byte(1)?;
                    msg.write_byte(1)?;
                    msg.write_short(1)?;
                } else {
                    msg.write_byte(item.item_options.len() as i8)?;

                    for opt in item.item_options.iter() {
                        msg.write_byte(opt.get_option_id())?;
                        msg.write_short(opt.get_param())?;
                    }
                }
            }
        }
        let box_len = (player.inventory.items_box.len().min(255)) as i8;
        msg.write_byte(box_len)?;
        for (index, item) in player
            .inventory
            .items_box
            .iter()
            .enumerate()
            .take(box_len as usize)
        {
            if !item.is_not_null_item() {
                msg.write_short(-1)?;
            } else {
                if let Some(tpl) = &item.template {
                    msg.write_short(tpl.id as i16)?;
                } else {
                    Self::debug_write_to_file(&format!("    -> No template, writing -1"));
                    msg.write_short(-1)?;
                }
                msg.write_int(item.quantity)?;
                msg.write_utf(&item.get_info())?;
                msg.write_utf(&item.get_content())?;
                if item.item_options.is_empty() {
                    msg.write_byte(1)?;
                    msg.write_byte(1)?;
                    msg.write_short(1)?;
                } else {
                    msg.write_byte(item.item_options.len() as i8)?;

                    for opt in item.item_options.iter() {
                        msg.write_byte(opt.get_option_id())?;
                        msg.write_short(opt.get_param())?;
                    }
                }
            }
        }
        msg.write_short(player.get_head())?; // num17 - number of head/avatar pairs
        Self::debug_write_to_file(&format!("18. head_avatar_count (short): 0"));
        // Character info IDs for gender
        msg.write_short(514)?; // charId[gender][0]
        Self::debug_write_to_file(&format!("19. charId[gender][0] (short): 514"));

        msg.write_short(515)?; // charId[gender][1]
        Self::debug_write_to_file(&format!("20. charId[gender][1] (short): 515"));

        msg.write_short(537)?; // charId[gender][2]
        Self::debug_write_to_file(&format!("21. charId[gender][2] (short): 537"));

        msg.write_byte(0)?; // isNhapThe (0 = false, 1 = true)
        Self::debug_write_to_file(&format!("22. isNhapThe (byte): 0"));

        msg.write_int(333)?; // deltaTime (server time)
        Self::debug_write_to_file(&format!("23. deltaTime (int): 333"));

        msg.write_byte(if player.is_new_member { 1 } else { 0 })?; // isNewMember
        Self::debug_write_to_file(&format!(
            "24. isNewMember (byte): {}",
            if player.is_new_member { 1 } else { 0 }
        ));

        // Additional effects (version dependent)
        msg.write_short(0)?; // idAuraEff
        Self::debug_write_to_file(&format!("25. idAuraEff (short): 0"));

        msg.write_byte(0)?; // idEff_Set_Item
        Self::debug_write_to_file(&format!("26. idEff_Set_Item (byte): 0"));

        msg.write_short(0)?; // idHat
        Self::debug_write_to_file(&format!("27. idHat (short): 0"));

        Self::debug_write_to_file("=== END SEND_PLAYER_BLOB_INTERNAL DEBUG ===\n");

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
