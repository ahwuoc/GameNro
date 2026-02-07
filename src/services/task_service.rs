use crate::constant::task_id;
use crate::constant::task_type::TaskType;
use crate::entities::task_sub_template;
use crate::network::message::Message;
use crate::player::Player;
use crate::services::ServiceHandles;
use crate::templates::task_template_manager::TASK_TEMPLATE_MANAGER;
use anyhow::Result;

pub struct TaskService;

impl TaskService {
    /// Lấy thông tin template của sub-task hiện tại mà người chơi đang thực hiện.
    pub fn get_current_sub_task(player: &Player) -> Option<task_sub_template::Model> {
        let sub_tasks = TASK_TEMPLATE_MANAGER.get_sub_tasks(player.task_player.task_main.id);
        sub_tasks
            .get(player.task_player.task_main.index as usize)
            .cloned()
    }

    /// Kiểm tra và cập nhật tiến trình nhiệm vụ tổng quát.
    /// Đây là hàm trung tâm thay thế cho hàng loạt switch-case trong \.
    pub fn check_done_task(
        player: &mut Player,
        task_type: TaskType,
        target_id: &str,
    ) -> Result<()> {
        if let Some(sub_task) = Self::get_current_sub_task(player) {
            let current_type = sub_task.task_type;
            // Nếu đúng loại nhiệm vụ
            if current_type == task_type {
                let mut is_match = false;

                if sub_task.target_id == "-1" || sub_task.target_id == target_id {
                    is_match = true;
                } else if task_type == TaskType::TalkNpc {
                    if sub_task.npc_id == target_id.parse::<i32>().unwrap_or(-1) {
                        is_match = true;
                    } else if sub_task.npc_id == -2 {
                        if target_id == player.gender.to_string() {
                            is_match = true;
                        }
                    }
                }

                if is_match {
                    tracing::debug!(
                        target: "task",
                        "Match: player={}, type={:?}, target_id={}, sub_task_name={}",
                        player.name,
                        task_type,
                        target_id,
                        sub_task.name
                    );
                    Self::add_done_sub_task(player, 1)?;
                }
            } else if current_type == TaskType::TaskScripts {
                // target_id trong TaskScripts:
                // "1" = PowerReach (kiểm tra sức mạnh)
                // "2" = UseTiemNang (dùng tiềm năng)
                // "3" = OpenRuongDo (mở rương đồ - TASK_0_3)
                // "4" = JoinClan (gia nhập bang - TASK_13_0)
                // "5" = Find7Stars (tìm 7 ngọc rồng - TASK_8_1)
                // "6" = DameReach (đạt sát thương - TASK_27_0)
                // "-1" = Script trigger bất kỳ (auto-complete khi được gọi)
                match sub_task.target_id.as_str() {
                    "1" => {
                        // PowerReach - Kiểm tra sức mạnh theo task ID
                        let power_required = match player.task_player.task_main.id {
                            task_id::TASK_7 => 16000,
                            task_id::TASK_8 => 40000,
                            task_id::TASK_10 => 200000,
                            task_id::TASK_11 => 500000,
                            task_id::TASK_20 => 600_000_000,
                            task_id::TASK_21 => 2_000_000_000,
                            task_id::TASK_27 => 35000,
                            _ => 0,
                        };
                        // Xử lý đặc biệt cho Task 11 có nhiều mốc sức mạnh
                        if player.task_player.task_main.id == task_id::TASK_11 {
                            let power_by_index = match player.task_player.task_main.index {
                                0 => 500000,
                                1 => 550000,
                                2 => 600000,
                                _ => 0,
                            };
                            if player.n_point.power >= power_by_index {
                                tracing::debug!(
                                    target: "task",
                                    "Script PowerReach OK (Task 11): player={}, power={}",
                                    player.name,
                                    player.n_point.power
                                );
                                Self::add_done_sub_task(player, 1)?;
                            }
                        } else if player.n_point.power >= power_required {
                            tracing::debug!(
                                target: "task",
                                "Script PowerReach OK: player={}, power={}",
                                player.name,
                                player.n_point.power
                            );
                            Self::add_done_sub_task(player, 1)?;
                        }
                    }
                    "2" => {
                        if task_type == TaskType::TaskScripts && target_id == "2" {
                            tracing::debug!(target: "task", "Script UseTiemNang OK: player={}", player.name);
                            Self::add_done_sub_task(player, 1)?;
                        }
                    }
                    "3" => {
                        if (task_type == TaskType::TaskScripts || task_type == TaskType::TalkNpc)
                            && target_id == "3"
                        {
                            tracing::debug!(target: "task", "Script OpenRuongDo OK: player={}", player.name);
                            Self::add_done_sub_task(player, 1)?;
                        }
                    }
                    "4" => {
                        if task_type == TaskType::TaskScripts && target_id == "4" {
                            tracing::debug!(target: "task", "Script JoinClan OK: player={}", player.name);
                            Self::add_done_sub_task(player, 1)?;
                        }
                    }
                    "5" => {
                        if task_type == TaskType::TaskScripts && target_id == "5" {
                            tracing::debug!(target: "task", "Script Find7Stars OK: player={}", player.name);
                            Self::add_done_sub_task(player, 1)?;
                        }
                    }
                    "6" => {
                        if player.n_point.dame >= 35000 {
                            tracing::debug!(
                                target: "task",
                                "Script DameReach OK: player={}, dame={}",
                                player.name,
                                player.n_point.dame
                            );
                            Self::add_done_sub_task(player, 1)?;
                        }
                    }
                    "-1" => {
                        if task_type == TaskType::TaskScripts {
                            tracing::debug!(
                                target: "task",
                                "Script Generic OK: player={}, target_id={}",
                                player.name,
                                target_id
                            );
                            Self::add_done_sub_task(player, 1)?;
                        }
                    }
                    _ => {}
                }
            } else {
                if player.task_player.task_main.id < 5 {
                    tracing::debug!(
                        target: "task",
                        "No Match: player={}, current={:?}({}), input={:?}({})",
                        player.name,
                        current_type,
                        sub_task.target_id,
                        task_type,
                        target_id
                    );
                }
            }
        }
        Ok(())
    }
    pub fn add_done_sub_task(player: &mut Player, num: i32) -> Result<()> {
        if let Some(sub_task) = Self::get_current_sub_task(player) {
            player.task_player.task_main.count += num;

            tracing::debug!(
                target: "task",
                "Update Progress: player={}, count={}/{}",
                player.name,
                player.task_player.task_main.count,
                sub_task.max_count
            );

            if player.task_player.task_main.count >= sub_task.max_count {
                tracing::debug!(
                    target: "task",
                    "Sub-task Completed: player={}, sub_task={}",
                    player.name,
                    sub_task.name
                );
                Self::send_next_sub_task(player)?;
            } else {
                Self::send_update_count_sub_task(player)?;
            }
        }
        Ok(())
    }

    /// Chuyển sang sub-task tiếp theo hoặc task chính tiếp theo.
    pub fn send_next_sub_task(player: &mut Player) -> Result<()> {
        let sub_tasks = TASK_TEMPLATE_MANAGER.get_sub_tasks(player.task_player.task_main.id);
        player.task_player.task_main.index += 1;
        player.task_player.task_main.count = 0;

        if player.task_player.task_main.index as usize >= sub_tasks.len() {
            Self::send_next_task_main(player)?;
        } else {
            // Gửi message 41 (Next SubTask)
            let msg = Message::new(41);
            player.send_to_client(msg)?;

            // Gửi thông báo NPC Say giống Java
            if let Some(next_st) = sub_tasks.get(player.task_player.task_main.index as usize) {
                let npc_id = if next_st.npc_id == -2 {
                    match player.gender {
                        0 => 0,
                        1 => 2,
                        2 => 1,
                        _ => 0,
                    }
                } else {
                    next_st.npc_id as i16
                };

                // Gửi khung thoại NPC giống Java (createTutorial/npcSay)
                if let Some(session) = &player.session {
                    let _ = crate::npc::npc_service::npc_service::npc_chat(
                        session,
                        &next_st.notify,
                        npc_id,
                    );
                }
            }

            // Gửi lại thông tin task mới (Message 40)
            Self::send_task_main(player)?;
        }
        Ok(())
    }

    pub fn send_next_task_main(player: &mut Player) -> Result<()> {
        let old_id = player.task_player.task_main.id;
        player.task_player.task_main.id += 1;
        player.task_player.task_main.index = 0;
        player.task_player.task_main.count = 0;

        tracing::debug!(
            target: "task",
            "Main Task Transition: player={}, from={}, to={}",
            player.name,
            old_id,
            player.task_player.task_main.id
        );

        Self::send_task_main(player)?;

        let next_task_name = TASK_TEMPLATE_MANAGER
            .get_main_task(player.task_player.task_main.id)
            .map(|t| t.name)
            .unwrap_or_else(|| "Nhiệm vụ mới".to_string());

        ServiceHandles::send_message_alert(
            player,
            &format!("Nhiệm vụ tiếp theo của bạn là: {}", next_task_name),
        )?;

        if let Some(first_st) = Self::get_current_sub_task(player) {
            let npc_id = if first_st.npc_id == -2 {
                match player.gender {
                    0 => 0,
                    1 => 2,
                    2 => 1,
                    _ => 0,
                }
            } else {
                first_st.npc_id as i16
            };

            if let Some(session) = &player.session {
                let _ = crate::npc::npc_service::npc_service::npc_chat(
                    session,
                    &first_st.notify,
                    npc_id,
                );
            }
        }
        Ok(())
    }

    pub fn send_task_main(player: &Player) -> Result<()> {
        let Some(main_task) = TASK_TEMPLATE_MANAGER.get_main_task(player.task_player.task_main.id)
        else {
            return Ok(());
        };
        let sub_tasks = TASK_TEMPLATE_MANAGER.get_sub_tasks(player.task_player.task_main.id);

        let mut msg = Message::new(40);
        msg.write_short(main_task.id as i16)?;
        msg.write_byte(player.task_player.task_main.index as i8)?;
        msg.write_utf(&Self::transform_name(player, &main_task.name))?;
        msg.write_utf(&Self::transform_name(player, &main_task.detail))?;
        msg.write_byte(sub_tasks.len() as i8)?;

        for stm in &sub_tasks {
            msg.write_utf(&Self::transform_name(player, &stm.name))?;
            msg.write_byte(stm.npc_id as i8)?;
            msg.write_short(stm.map as i16)?;
            msg.write_utf(&Self::transform_name(player, &stm.notify))?;
        }

        msg.write_short(player.task_player.task_main.count as i16)?;
        for stm in &sub_tasks {
            msg.write_short(stm.max_count as i16)?;
        }

        player.send_to_client(msg)?;
        Ok(())
    }

    pub fn send_update_count_sub_task(player: &Player) -> Result<()> {
        let mut msg = Message::new(43);
        msg.write_short(player.task_player.task_main.count as i16)?;
        player.send_to_client(msg)?;
        Ok(())
    }
}

impl TaskService {
    pub fn check_done_task_talk_npc(player: &mut Player, npc_id: &str) -> Result<()> {
        Self::check_done_task(player, TaskType::TalkNpc, npc_id)
    }

    pub fn check_done_task_kill_mob(player: &mut Player, mob_id: &str) -> Result<()> {
        Self::check_done_task(player, TaskType::KillMob, mob_id)
    }

    pub fn check_done_task_kill_boss(player: &mut Player, boss_id: &str) -> Result<()> {
        Self::check_done_task(player, TaskType::KillBoss, boss_id)
    }

    pub fn check_done_task_pick_item(player: &mut Player, item_id: &str) -> Result<()> {
        Self::check_done_task(player, TaskType::PickItem, item_id)
    }

    pub fn check_done_task_use_item(player: &mut Player, item_id: &str) -> Result<()> {
        Self::check_done_task(player, TaskType::UseItem, item_id)
    }

    pub fn check_done_task_go_to_map(player: &mut Player, map_id: &str) -> Result<()> {
        Self::check_done_task(player, TaskType::GoToMap, map_id)
    }

    pub fn check_done_task_confirm_menu(player: &mut Player, npc_id: &str) -> Result<()> {
        Self::check_done_task(player, TaskType::ConfirmMenu, npc_id)
    }

    pub fn check_done_task_scripts(player: &mut Player, script_id: &str) -> Result<()> {
        Self::check_done_task(player, TaskType::TaskScripts, script_id)
    }
}

impl TaskService {
    /// Lấy ID task hiện tại của player
    pub fn get_id_task(player: &Player) -> i32 {
        player.task_player.task_main.id
    }

    /// Kiểm tra hoàn thành task khi player di chuyển trên map
    /// Java: checkDoneTaskGoToMap
    pub fn check_done_task_go_to_map_position(
        player: &mut Player,
        map_id: i32,
        x: i16,
    ) -> Result<()> {
        match map_id {
            // Map làng (TraiDat=39, Namec=40, Xayda=41)
            39 | 40 | 41 => {
                if x >= 635 {
                    // Player đã di chuyển tới vị trí mũi tên (x >= 635)
                    Self::done_task_by_id(player, task_id::TASK_0, 0)?;
                }
            }
            // Map nhà (TraiDat=21, Namec=22, Xayda=23)
            21 | 22 | 23 => {
                Self::done_task_by_id(player, task_id::TASK_0, 1)?;
                Self::done_task_by_id(player, task_id::TASK_12, 0)?;
            }
            // Map Vũ trụ
            0 | 7 | 14 => {
                Self::done_task_by_id(player, task_id::TASK_8, 0)?;
            }
            // Map Tinh cầu
            5 | 13 | 20 => {
                Self::done_task_by_id(player, task_id::TASK_9, 0)?;
            }
            19 => {
                Self::done_task_by_id(player, task_id::TASK_17, 0)?;
            }
            93 => {
                Self::done_task_by_id(player, task_id::TASK_23, 0)?;
            }
            104 => {
                Self::done_task_by_id(player, task_id::TASK_24, 0)?;
            }
            97 => {
                Self::done_task_by_id(player, task_id::TASK_25, 0)?;
            }
            100 => {
                Self::done_task_by_id(player, task_id::TASK_26, 0)?;
            }
            103 => {
                Self::done_task_by_id(player, task_id::TASK_27, 2)?;
            }
            _ => {}
        }
        Ok(())
    }

    /// Hoàn thành task với ID và index cụ thể
    fn done_task_by_id(player: &mut Player, task_id: i32, task_index: i32) -> Result<()> {
        if player.task_player.task_main.id == task_id
            && player.task_player.task_main.index == task_index
        {
            Self::add_done_sub_task(player, 1)?;
        }
        Ok(())
    }

    pub fn send_tutorial_task_0(player: &Player, server_name: &str) -> Result<()> {
        let task_id = Self::get_id_task(player);
        let task_index = player.task_player.task_main.index;
        tracing::debug!(
            target: "task",
            "send_tutorial_task_0: player={}, task_id={}, task_index={}",
            player.name,
            task_id,
            task_index
        );

        if task_id == task_id::TASK_0 && task_index == 0 {
            tracing::debug!(target: "task", "Sending tutorial message for player {}", player.name);
            let text = format!(
                "Chào Mừng {} Đến Với: {}\n\
                Nhiệm vụ đầu tiên của bạn là di chuyển\n\
                Bạn hãy di chuyển nhân vật theo mũi tên chỉ hướng",
                player.name, server_name
            );
            const CON_MEO: i16 = 5;
            let mut msg = Message::new(38);
            msg.write_short(CON_MEO)?;
            msg.write_utf(&text)?;
            player.send_to_client(msg)?;
        }
        Ok(())
    }

    pub fn check_auto_skip_task_home(player: &mut Player) -> Result<()> {
        let home_map = (player.gender as i32) + 21; // 21, 22, 23
        let task_id = Self::get_id_task(player);
        let task_index = player.task_player.task_main.index;

        tracing::debug!(
            "[TASK] check_auto_skip_task_home: player={}, map_id={}, home_map={}, task_id={}, task_index={}",
            player.name, player.map_id, home_map, task_id, task_index
        );

        if player.map_id == home_map {
            if task_id == task_id::TASK_0 && (task_index == 0 || task_index == 1) {
                tracing::debug!("[TASK] Auto-skipping to index 2 for player {}", player.name);
                player.task_player.task_main.index = 2;
                player.task_player.task_main.count = 0;
                Self::send_task_main(player)?;
            }
        }
        Ok(())
    }

    /// Gửi thông tin task hiện tại (gọi khi client OK)
    pub fn send_info_current_task(player: &Player) -> Result<()> {
        tracing::debug!(
            "[TASK] send_info_current_task: player={}, task_id={}, task_index={}, task_count={}",
            player.name,
            player.task_player.task_main.id,
            player.task_player.task_main.index,
            player.task_player.task_main.count
        );
        Self::send_task_main(player)
    }
}

impl TaskService {
    fn transform_name(player: &Player, text: &str) -> String {
        let mut result = text.to_string();
        let gender = player.gender;

        // %1: Làng
        result = result.replace(
            "%1",
            match gender {
                0 => "Làng Aru",
                1 => "Làng Mori",
                2 => "Làng Kakarot",
                _ => "Làng Aru",
            },
        );

        // %2: Ông nội
        result = result.replace(
            "%2",
            match gender {
                0 => "ông Gôhan",
                1 => "ông Moori",
                2 => "ông Paragus",
                _ => "ông Gôhan",
            },
        );

        // %3: Đồi / Rừng
        result = result.replace(
            "%3",
            match gender {
                0 => "Đồi hoa cúc",
                1 => "Đồi nấm tím",
                2 => "Đồi hoang",
                _ => "Đồi hoa cúc",
            },
        );

        // %4: Quái sơ cấp
        result = result.replace(
            "%4",
            match gender {
                0 => "khủng long",
                1 => "lợn lòi",
                2 => "quỷ đất",
                _ => "khủng long",
            },
        );

        // %5: Vách núi
        result = result.replace(
            "%5",
            match gender {
                0 => "Vách núi Aru",
                1 => "Vách núi Moori",
                2 => "Vách núi Kakarot",
                _ => "Vách núi Aru",
            },
        );

        // %6: Thị trấn / Làng Plant
        result = result.replace(
            "%6",
            match gender {
                0 => "Thung lũng tre",
                1 => "Thị trấn Moori",
                2 => "Làng Plant",
                _ => "Thung lũng tre",
            },
        );

        // %7: NPC Trạm tàu vũ trụ
        result = result.replace(
            "%7",
            match gender {
                0 => "Dr. Brief",
                1 => "Cargo",
                2 => "Cui",
                _ => "Dr. Brief",
            },
        );

        // %8: NPC Shop làng
        result = result.replace(
            "%8",
            match gender {
                0 => "Bunma",
                1 => "Dende",
                2 => "Appule",
                _ => "Bunma",
            },
        );

        // %9: Quái bay 1
        result = result.replace(
            "%9",
            match gender {
                0 => "thằn lằn bay",
                1 => "phi long",
                2 => "quỷ bay",
                _ => "thằn lằn bay",
            },
        );

        // %10: NPC Tổng quản (Quy Lão)
        result = result.replace(
            "%10",
            match gender {
                0 => "Quy Lão Kame",
                1 => "Trưởng lão Guru",
                2 => "Vua Vegeta",
                _ => "Quy Lão Kame",
            },
        );

        // %11: Bản đồ đặc biệt (Đảo Kame...)
        result = result.replace(
            "%11",
            match gender {
                0 => "Đảo Kamê",
                1 => "Đảo Guru",
                2 => "Vách núi đen",
                _ => "Đảo Kamê",
            },
        );

        // %12: Quái 3000 HP
        result = result.replace(
            "%12",
            match gender {
                0 => "ốc mượn hồn",
                1 => "ốc sên",
                2 => "heo Xayda mẹ",
                _ => "ốc mượn hồn",
            },
        );

        // %13: Bản đồ trung cấp
        result = result.replace(
            "%13",
            match gender {
                0 => "Rừng nấm",
                1 => "Thung lũng Namếc",
                2 => "Rừng nguyên sinh",
                _ => "Rừng nấm",
            },
        );

        // %14: Quái mẹ sơ cấp
        result = result.replace(
            "%14",
            match gender {
                0 => "phi long mẹ",
                1 => "quỷ bay mẹ",
                2 => "thằn lằn mẹ",
                _ => "phi long mẹ",
            },
        );

        result
    }
}
