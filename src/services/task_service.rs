use std::clone;
use std::panic::panic_any;

use crate::constant::cmd::cmd::GET_MOB_TEMPLATE;
use crate::constant::const_npc::CON_MEO;
use crate::constant::task_type::TaskType;
use crate::constant::{const_npc, task_id};
use crate::entities::task_sub_template;
use crate::network::message::Message;
use crate::network::session;
use crate::player::Player;
use crate::services::task_utils::TaskUtils;
use crate::services::ServiceHandles;
use crate::templates::task_template_manager::TASK_TEMPLATE_MANAGER;
use crate::templates::{boss_template_manager, mob_template_manager, npc_template_manager};
use anyhow::Result;

pub struct TaskService;

impl TaskService {
    pub fn get_current_sub_task(player: &Player) -> Option<task_sub_template::Model> {
        let sub_tasks = TASK_TEMPLATE_MANAGER.get_sub_tasks(player.task_player.task_main.id);
        let index = player.task_player.task_main.index as usize;

        if index >= sub_tasks.len() && !sub_tasks.is_empty() {
            tracing::warn!(
                "[TASK] Player {} stuck at index {} for task {}. Auto-fixing...",
                player.name,
                index,
                player.task_player.task_main.id
            );
            return sub_tasks.last().cloned();
        }

        sub_tasks.get(index).cloned()
    }
    pub fn resolve_id(input: &str, gender: i8) -> i32 {
        let parts: Vec<&str> = input.split(',').collect();
        if parts.len() == 3 {
            parts
                .get(gender as usize)
                .and_then(|&s| s.parse::<i32>().ok())
                .unwrap_or(-1)
        } else {
            input.parse::<i32>().unwrap_or(-1)
        }
    }

    /// Kiểm tra xem NPC hiện tại có khớp với NPC yêu cầu của Task không
    pub fn is_match_npc(player: &Player, target_npc_id: i16) -> bool {
        if let Some(sub_task) = Self::get_current_sub_task(player) {
            if let Some(npc_list) = &sub_task.npc_id {
                let npc_id = Self::resolve_id(npc_list, player.gender);
                return target_npc_id as i32 == npc_id || npc_list == "-1";
            }
        }
        false
    }

    pub fn check_done_task(
        player: &mut Player,
        task_type: TaskType,
        target_id: &str,
    ) -> Result<()> {
        if let Some(sub_task) = Self::get_current_sub_task(player) {
            let current_type = sub_task.task_type;

            // Logic xử lý TaskScripts trước
            if current_type == TaskType::TaskScripts {
                return Self::handle_task_scripts(player, task_type, target_id, &sub_task);
            }

            if current_type == task_type {
                let mut is_match = false;

                match task_type {
                    TaskType::TalkNpc | TaskType::ConfirmMenu => {
                        if let Some(npc_list) = &sub_task.npc_id {
                            let npc_id = Self::resolve_id(npc_list, player.gender);
                            if target_id == npc_id.to_string() || npc_list == "-1" {
                                is_match = true;
                            }
                        }
                    }
                    TaskType::KillMob => {
                        if let Some(mob_list) = &sub_task.mob_id {
                            let sub_targets: Vec<&str> = mob_list.split(',').collect();
                            if mob_list == "-1" || sub_targets.contains(&target_id) {
                                is_match = true;
                            }
                        }
                    }
                    TaskType::KillBoss => {
                        if let Some(boss_list) = &sub_task.boss_id {
                            let sub_targets: Vec<&str> = boss_list.split(',').collect();
                            if boss_list == "-1" || sub_targets.contains(&target_id) {
                                is_match = true;
                            }
                        }
                    }
                    TaskType::PickItem | TaskType::UseItem => {
                        if let Some(item_list) = &sub_task.pick_item_id {
                            let sub_targets: Vec<&str> = item_list.split(',').collect();
                            if item_list == "-1" || sub_targets.contains(&target_id) {
                                is_match = true;
                            }
                        }
                    }
                    TaskType::GoToMap => {
                        if let Some(map_list) = &sub_task.map_id {
                            let map_id = Self::resolve_id(map_list, player.gender);
                            if target_id == map_id.to_string() || map_list == "-1" {
                                is_match = true;
                            }
                        }
                    }
                    _ => {}
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
            }
        }
        Ok(())
    }
    pub fn add_done_sub_task(player: &mut Player, num: i32) -> Result<()> {
        if let Some(sub_task) = Self::get_current_sub_task(player) {
            player.task_player.task_main.count += num;

            let count = player.task_player.task_main.count;
            let max_count = sub_task.max_count;
            let notify = sub_task.notify.clone();
            let task_type = sub_task.task_type;

            tracing::debug!(
                target: "task",
                "Update Progress: player={}, count={}/{}",
                player.name,
                count,
                max_count
            );

            if count >= max_count {
                tracing::debug!(
                    target: "task",
                    "Sub-task Completed: player={}, sub_task={}",
                    player.name,
                    sub_task.name
                );
                Self::send_next_sub_task(player)?;
            } else {
                Self::send_update_count_sub_task(player)?;

                if !notify.is_empty() {
                    let prefix_text = match task_type {
                        TaskType::KillMob | TaskType::KillBoss => "đánh",
                        TaskType::PickItem => "nhặt",
                        TaskType::UseItem => "dùng",
                        _ => "",
                    };
                    let target_name = match task_type {
                        TaskType::KillBoss => {
                            let boss_id = sub_task.boss_id.as_deref().unwrap_or("0");
                            boss_template_manager::get(&boss_id).map(|x| x.name.clone())
                        }
                        TaskType::KillMob => {
                            let mob_id = sub_task.mob_id.as_deref().unwrap_or("0");
                            let split_ids = mob_id.split(",").collect::<Vec<&str>>();
                            if split_ids.len() > 1 {
                                let first_id = split_ids[player.gender as usize];
                                mob_template_manager::get(first_id.parse::<i8>().unwrap_or(0))
                                    .map(|x| x.name.clone())
                            } else if split_ids.len() == 1 {
                                mob_template_manager::get(split_ids[0].parse::<i8>().unwrap_or(0))
                                    .map(|x| x.name.clone())
                            } else {
                                None
                            }
                        }
                        _ => None,
                    };
                    let target_name = target_name.unwrap_or("".to_string()).to_lowercase();
                    if !prefix_text.is_empty() {
                        let text = format!(
                            "Bạn {} được {}/{} {}",
                            prefix_text, count, max_count, target_name,
                        );

                        println!("=============TEXT============ {}", &text);
                        ServiceHandles::send_thong_bao_to_player(player, &text)?;
                    }
                }
            }
        }
        Ok(())
    }

    pub fn send_next_sub_task(player: &mut Player) -> Result<()> {
        let sub_tasks = TASK_TEMPLATE_MANAGER.get_sub_tasks(player.task_player.task_main.id);
        player.task_player.task_main.index += 1;
        player.task_player.task_main.count = 0;

        if player.task_player.task_main.index as usize >= sub_tasks.len() {
            Self::send_next_task_main(player)?;
        } else {
            let msg = Message::new(41);
            player.send_to_client(msg)?;

            if let Some(next_st) = sub_tasks.get(player.task_player.task_main.index as usize) {
                Self::send_npc_chat_sub_task(player, next_st)?;
            }
            Self::send_task_main(player)?;
        }
        Ok(())
    }

    fn send_npc_chat_sub_task(player: &Player, sub_task: &task_sub_template::Model) -> Result<()> {
        let npc_id = if let Some(npc_list) = &sub_task.npc_id {
            Self::resolve_id(npc_list, player.gender)
        } else {
            const_npc::CON_MEO as i32
        };

        let Some(chattext) = sub_task.npc_say.as_ref() else {
            return Ok(());
        };

        let text = TaskUtils::transform_name(player, chattext);
        let mut msg = Message::new(38);
        msg.write_short(npc_id as i16)?;
        msg.write_utf(&text)?;
        if let Some(npc) = npc_template_manager::get(npc_id as i16) {
            msg.write_short(npc.avatar.unwrap_or(0) as i16)?;
        }
        player.send_to_client(msg)?;
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

        ServiceHandles::send_thong_bao_to_player(
            player,
            &format!("Nhiệm vụ tiếp theo của bạn là: {}", next_task_name),
        )?;

        if let Some(first_st) = Self::get_current_sub_task(player) {
            Self::send_npc_chat_sub_task(player, &first_st)?;
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
        msg.write_utf(&TaskUtils::transform_name(player, &main_task.name))?;
        msg.write_utf(&TaskUtils::transform_name(player, &main_task.detail))?;
        msg.write_byte(sub_tasks.len() as i8)?;

        for stm in &sub_tasks {
            msg.write_utf(&TaskUtils::transform_name(player, &stm.name))?;

            let npc_id = if let Some(npc_list) = &stm.npc_id {
                Self::resolve_id(npc_list, player.gender)
            } else {
                -1
            };

            let map_id = if let Some(map_list) = &stm.map_id {
                Self::resolve_id(map_list, player.gender)
            } else {
                -1
            };

            msg.write_byte(npc_id as i8)?;
            msg.write_short(map_id as i16)?;
            msg.write_utf(&TaskUtils::transform_name(player, &stm.notify))?;
        }

        msg.write_short(player.task_player.task_main.count as i16)?;
        for stm in &sub_tasks {
            msg.write_short(stm.max_count as i16)?;
        }

        player.send_to_client(msg)?;
        let _ = Self::send_update_count_sub_task(player);
        Ok(())
    }

    pub fn send_update_count_sub_task(player: &Player) -> Result<()> {
        let mut msg = Message::new(43);
        msg.write_short(player.task_player.task_main.count as i16)?;
        player.send_to_client(msg)?;
        Ok(())
    }

    fn handle_task_scripts(
        player: &mut Player,
        task_type: TaskType,
        target_id: &str,
        sub_task: &task_sub_template::Model,
    ) -> Result<()> {
        let main_id = player.task_player.task_main.id;
        let index = player.task_player.task_main.index;

        match main_id {
            task_id::TASK_0_0 => {
                if index == 0 && task_type == TaskType::TaskScripts {
                    tracing::debug!(target: "task", "Script Task 0_0 OK: player={}", player.name);
                    Self::add_done_sub_task(player, 1)?;
                } else if index == 3
                    && (task_type == TaskType::TaskScripts || task_type == TaskType::TalkNpc)
                    && target_id == "3"
                {
                    tracing::debug!(target: "task", "Script OpenRuongDo OK: player={}", player.name);
                    Self::add_done_sub_task(player, 1)?;
                }
            }
            task_id::TASK_7
            | task_id::TASK_8
            | task_id::TASK_10
            | task_id::TASK_11
            | task_id::TASK_20
            | task_id::TASK_21
            | task_id::TASK_27 => {
                if player.n_point.power >= sub_task.power_require {
                    tracing::debug!(
                        target: "task",
                        "Script PowerReach OK: player={}, power={}, require={}",
                        player.name,
                        player.n_point.power,
                        sub_task.power_require
                    );
                    Self::add_done_sub_task(player, 1)?;
                }
            }
            task_id::TASK_3 => {
                if index == 0 && task_type == TaskType::TaskScripts && target_id == "2" {
                    tracing::debug!(target: "task", "Script UseTiemNang OK: player={}", player.name);
                    Self::add_done_sub_task(player, 1)?;
                }
            }
            // Gia nhập bang hội
            task_id::TASK_13 => {
                if index == 0 && task_type == TaskType::TaskScripts {
                    tracing::debug!(target: "task", "Script JoinClan OK: player={}", player.name);
                    Self::add_done_sub_task(player, 1)?;
                }
            }
            _ => {
                // Auto-complete các task generic script
                if task_type == TaskType::TaskScripts {
                    tracing::debug!(target: "task", "Script Generic OK: player={}, main_id={}", player.name, main_id);
                    Self::add_done_sub_task(player, 1)?;
                }
            }
        }
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
    pub fn get_id_task(player: &Player) -> i32 {
        player.task_player.task_main.id
    }
    // Task go map
    pub fn check_done_task_go_to_map_position(
        player: &mut Player,
        map_id: i32,
        _x: i16,
    ) -> Result<()> {
        Self::check_done_task(player, TaskType::GoToMap, &map_id.to_string())?;

        match map_id {
            39 | 40 | 41 => {
                Self::done_task_by_id(player, task_id::TASK_0_0, 0)?;
            }
            21 | 22 | 23 => {
                Self::done_task_by_id(player, task_id::TASK_0_0, 1)?;
                Self::done_task_by_id(player, task_id::TASK_12, 0)?;
            }
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

    fn done_task_by_id(player: &mut Player, task_id: i32, task_index: i32) -> Result<()> {
        if player.task_player.task_main.id == task_id
            && player.task_player.task_main.index == task_index
        {
            tracing::debug!(
                target: "task",
                "done_task_by_id: Match! player={}, task_id={}, index={}",
                player.name, task_id, task_index
            );
            Self::add_done_sub_task(player, 1)?;
        }
        Ok(())
    }

    pub fn send_tutorial_task_0_0_0(player: &Player, server_name: &str) -> Result<()> {
        let task_id = Self::get_id_task(player);
        let task_index = player.task_player.task_main.index;
        tracing::debug!(
            target: "task",
            "send_tutorial_task_0: player={}, task_id={}, task_index={}",
            player.name,
            task_id,
            task_index
        );

        if task_id == task_id::TASK_0_0 && task_index == 0 {
            tracing::debug!(target: "task", "Sending tutorial message for player {}", player.name);
            let text = format!(
                "Chào Mừng {} Đến Với: {}\n\
                Nhiệm vụ đầu tiên của bạn là di chuyển\n\
                Bạn hãy di chuyển nhân vật theo mũi tên chỉ hướng",
                player.name, server_name
            );
            let mut msg = Message::new(38);
            msg.write_short(const_npc::CON_MEO as i16)?;
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
            if task_id == task_id::TASK_0_0 && (task_index == 0 || task_index == 1) {
                tracing::debug!("[TASK] Auto-skipping to index 2 for player {}", player.name);
                player.task_player.task_main.index = 2;
                player.task_player.task_main.count = 0;
                Self::send_task_main(player)?;
            }
        }
        Ok(())
    }

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
