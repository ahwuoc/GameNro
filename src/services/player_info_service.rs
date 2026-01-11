use crate::data::DataGame;
use crate::map::zone_manager::ZONE_MANAGER;
use crate::map::Zone;
use crate::network::message::Message;
use crate::network::session::AsyncSession;
use crate::player::Player as RtPlayer;
use crate::services::IntrinsicService;
use std::fs::OpenOptions;
use std::io::Write;

struct SubTaskInfo {
    name: String,
    npc_id: i8,
    map_id: i16,
    notify: String,
    count: i16,
    max_count: i16,
}

pub struct PlayerInfoService;

impl PlayerInfoService {
    pub async fn send_point_info(
        session: &mut AsyncSession,
        player: &RtPlayer,
    ) -> anyhow::Result<()> {
        let mut msg = Message::new(-42);
        msg.write_int(player.n_point.hpg)?; // hpg
        msg.write_int(player.n_point.mpg)?; // mpg
        msg.write_int(player.n_point.dameg)?; // dameg
        msg.write_int(player.n_point.hp_max)?; // hpMax
        msg.write_int(player.n_point.mp_max)?; // mpMax
        msg.write_int(player.n_point.hp)?; // hp
        msg.write_int(player.n_point.mp)?; // mp
        msg.write_byte(player.n_point.speed)?; // speed
        msg.write_byte(20)?; // reserved
        msg.write_byte(20)?; // reserved
        msg.write_byte(1)?; // reserved
        msg.write_int(player.n_point.dame)?; // dame
        msg.write_int(player.n_point.def)?; // def
        msg.write_byte(player.n_point.crit)?; // crit
        msg.write_long(player.n_point.tiem_nang)?; // tiemNang
        msg.write_short(100)?; // reserved
        msg.write_int(player.n_point.defg)?; // defg (reserved)
        msg.write_byte(player.n_point.critg)?; // critg (reserved)

        session.send_message(&msg).await?;
        Ok(())
    }

    pub async fn send_task_info(
        session: &mut AsyncSession,
        player: &RtPlayer,
    ) -> anyhow::Result<()> {
        println!(
            "Sending task main info for player {} (task_id={})",
            player.name, player.task_id
        );
        let task_main_id = player.task_id >> 10;
        let task_index = (player.task_id >> 1) & 0x1FF;

        let mut msg = Message::new(40);
        msg.write_short(task_main_id as i16)?;
        msg.write_byte(task_index as i8)?;

        // taskMain.name
        let task_name = Self::get_task_name(task_main_id);
        msg.write_utf(&format!("{}[{}]", task_name, task_main_id))?;

        // taskMain.detail
        msg.write_utf(&Self::get_task_detail(task_main_id))?;

        // subTasks.size() - typically each main task has multiple sub-tasks
        let sub_tasks = Self::get_sub_tasks(task_main_id, player.gender);
        let sub_task_count = sub_tasks.len() as i8;
        msg.write_byte(sub_task_count)?;

        // Loop through ALL subTasks to send their info
        for sub_task in &sub_tasks {
            msg.write_utf(&sub_task.name)?; // stm.name
            msg.write_byte(sub_task.npc_id)?; // stm.npcId
            msg.write_short(sub_task.map_id)?; // stm.mapId
            msg.write_utf(&sub_task.notify)?; // stm.notify
        }

        // Current count of the CURRENT subTask (at task_index)
        let current_count = if task_index < sub_tasks.len() as i32 {
            sub_tasks[task_index as usize].count
        } else {
            0
        };
        msg.write_short(current_count)?;

        // Loop through ALL subTasks to send their maxCount
        for sub_task in &sub_tasks {
            msg.write_short(sub_task.max_count)?;
        }

        session.send_message(&msg).await?;
        Ok(())
    }

    /// Get task name based on task_main_id
    fn get_task_name(task_main_id: i32) -> String {
        match task_main_id {
            0 => "Bắt đầu hành trình".to_string(),
            1 => "Tiêu diệt quái vật".to_string(),
            2 => "Khám phá thế giới".to_string(),
            3 => "Rèn luyện sức mạnh".to_string(),
            7 => "Nhiệm vụ nâng cao".to_string(),
            _ => format!("Nhiệm vụ #{}", task_main_id),
        }
    }

    /// Get task detail based on task_main_id
    fn get_task_detail(task_main_id: i32) -> String {
        match task_main_id {
            0 => "Hãy nói chuyện với người hướng dẫn".to_string(),
            1 => "Tiêu diệt các quái vật xung quanh".to_string(),
            2 => "Khám phá các vùng đất mới".to_string(),
            3 => "Luyện tập để trở nên mạnh mẽ hơn".to_string(),
            _ => "Hoàn thành nhiệm vụ được giao".to_string(),
        }
    }

    fn get_sub_tasks(task_main_id: i32, gender: i8) -> Vec<SubTaskInfo> {
        match task_main_id {
            0 => vec![SubTaskInfo {
                name: "Nói chuyện với người hướng dẫn".to_string(),
                npc_id: if gender == 0 {
                    1
                } else if gender == 1 {
                    2
                } else {
                    3
                },
                map_id: (gender as i16) + 21,
                notify: "Hãy tìm gặp người hướng dẫn".to_string(),
                count: 0,
                max_count: 1,
            }],
            _ => vec![SubTaskInfo {
                name: format!("Nhiệm vụ phụ #{}", task_main_id),
                npc_id: 1,
                map_id: 0,
                notify: "Hoàn thành nhiệm vụ".to_string(),
                count: 0,
                max_count: 10,
            }],
        }
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
        msg.write_byte(0)?; // ctaskId
        msg.write_byte(player.gender)?; // cgender
        msg.write_short(player.head)?; // head
        msg.write_utf(&player.name)?; // cName
        msg.write_byte(0)?; // cPk

        msg.write_byte(player.type_pk)?; // cTypePk

        msg.write_long(player.n_point.power)?; // cPower
        msg.write_short(0)?; // eff5BuffHp
        msg.write_short(0)?; // eff5BuffMp

        msg.write_byte(0)?; // nClass index (GameScr.nClasss[...])

        // Skills - client expects to read skills here
        msg.write_byte(0)?; // number of skills (sbyte b2)

        // Currency - client always reads xu as long
        msg.write_long(player.inventory.get_gold())?; // xu
        msg.write_int(player.inventory.get_ruby())?; // luongKhoa
        msg.write_int(player.inventory.get_gem())?; // luong

        // Body items
        let body_len = (player.inventory.items_body.len().min(255)) as i8;
        msg.write_byte(body_len)?;
        for item in player.inventory.items_body.iter().take(body_len as usize) {
            if item.is_null_item() {
                msg.write_short(-1)?;
            } else {
                if let Some(tpl) = &item.template {
                    msg.write_short(tpl.id)?;
                    msg.write_int(item.quantity)?;
                    msg.write_utf(&item.get_info())?;
                    msg.write_utf(&item.get_content())?;
                    if item.item_options.is_empty() {
                        msg.write_byte(1)?;
                        msg.write_byte(73)?;
                        msg.write_short(1)?;
                    } else {
                        let opts_len = item.item_options.len() as i8;
                        msg.write_byte(opts_len)?;
                        for opt in item.item_options.iter() {
                            if opt.get_option_id() == 47 {
                                println!("send client {}", opt.get_name());
                            }
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
        DataGame::send_head_to_client(&mut msg).await?;
        msg.write_short(player.get_head())?; // num17 - number of head/avatar pairs
        msg.write_short(514)?; // charId[gender][0]
        msg.write_short(515)?; // charId[gender][1]
        msg.write_short(537)?; // charId[gender][2]
        msg.write_byte(0)?; // isNhapThe (0 = false, 1 = true)
        msg.write_int(333)?; // deltaTime (server time)
        msg.write_byte(if player.is_new_member { 1 } else { 0 })?; // isNewMember
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
        Self::send_task_info(session, &player).await?;

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
        let zone_manager = ZONE_MANAGER.read().await;
        zone_manager
            .load_player_to_best_zone(player.clone(), session)
            .await?;

        Self::send_cai_trang(session, &player).await?;

        Self::send_time_skill(session).await?;

        // clear vt sk
        Self::clear_vtsk(session).await?;

        println!("All player info sent successfully");
        Ok(())
    }
}
