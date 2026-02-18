use crate::constant::const_npc::CON_MEO;
use crate::constant::menu_enum::MenuId;
use crate::map::SpaceShipType;
use crate::matches::luyen_tap::LuyenTap;
use crate::matches::pvp_manager::get_pvp_handle;
use crate::matches::thach_dau::{ThachDau, GOLD_CHALLENGE};
use crate::matches::tra_thu::TraThu;
use crate::network::message::Message;
use crate::network::session::{AsyncSession, SessionArc};
use crate::npc::npc_service::npc_service;
use crate::player::player_actor::message::PlayerMessage;
use crate::player::player_manager::PLAYER_MANAGER;
use crate::services::ServiceHandles;
use crate::utils::number_util::number_to_money;
use anyhow::Result;

const OPEN_GOLD_SELECT: i8 = 0;
const ACCEPT_PVP: i8 = 1;

fn gold_options() -> Vec<String> {
    GOLD_CHALLENGE
        .iter()
        .map(|g| format!("{} vàng", number_to_money(*g)))
        .collect()
}

pub async fn controller_thach_dau(session: &SessionArc, mut msg: Message) -> Result<()> {
    let action = msg.read_byte()?;
    let pvp_type = msg.read_byte()?;
    let player_id = msg.read_int()?;

    match pvp_type {
        // ThachDau
        3 => match action {
            OPEN_GOLD_SELECT => {
                open_select_gold(session, player_id as i64).await?;
            }
            ACCEPT_PVP => {
                accept_pvp(session).await?;
            }
            _ => {}
        },
        // LuyenTap
        4 => match action {
            OPEN_GOLD_SELECT => {
                send_invite_pvp_luyentap(session, player_id as i64).await?;
            }
            ACCEPT_PVP => {
                accept_pvp_luyentap(session).await?;
            }
            _ => {}
        },
        _ => {}
    }

    Ok(())
}

async fn open_select_gold(session: &SessionArc, target_id: i64) -> Result<()> {
    let snapshot = match session.get_player_snapshot().await {
        Some(s) => s,
        None => return Ok(()),
    };

    if !PLAYER_MANAGER.contains(target_id as u64) {
        ServiceHandles::send_thong_bao_to_player(&snapshot, "Đối thủ đã thoát game")?;
        return Ok(());
    }
    let pvp_handle = get_pvp_handle();
    if pvp_handle.has_pvp(snapshot.id as i64).await || pvp_handle.has_pvp(target_id).await {
        npc_service::hide_wait_dialog(session)?;
        ServiceHandles::send_thong_bao_to_player(&snapshot, "Đang giao đấu không thể mời.")?;
        return Ok(());
    }

    if let Some(handle) = session.get_player_handle().await {
        let target = target_id;
        handle.send_forget(PlayerMessage::Modify(Box::new(move |player| {
            player.interaction_state.id_play_thach_dau = target;
        })));
    }

    let target_name = if let Some(target_handle) = PLAYER_MANAGER.get(target_id as u64) {
        if let Some(target_snap) = target_handle.get_snapshot().await {
            format!(
                "{} (sức mạnh {})",
                target_snap.name,
                number_to_money(target_snap.n_point.power)
            )
        } else {
            "Đối thủ".to_string()
        }
    } else {
        "Đối thủ".to_string()
    };

    let options: Vec<String> = gold_options();
    let options_ref: Vec<&str> = options.iter().map(|s| s.as_str()).collect();

    npc_service::create_menu(
        session,
        CON_MEO,
        &format!("{}\nBạn muốn cược bao nhiêu vàng?", target_name),
        options_ref,
        MenuId::MakeMatchPvp,
    )
    .await?;

    Ok(())
}

pub async fn send_invite_pvp_thachdau(session: &SessionArc, select_gold: i8) -> Result<()> {
    let snapshot = match session.get_player_snapshot().await {
        Some(s) => s,
        None => return Ok(()),
    };

    let target_id = snapshot.interaction_state.id_play_thach_dau;
    if target_id == 0 {
        return Ok(());
    }

    let target_handle = match PLAYER_MANAGER.get(target_id as u64) {
        Some(h) => h,
        None => {
            npc_service::hide_wait_dialog(session)?;
            ServiceHandles::send_thong_bao_to_player(&snapshot, "Đối thủ đã thoát game")?;
            return Ok(());
        }
    };

    let gold_thach_dau = GOLD_CHALLENGE[select_gold as usize];

    if snapshot.inventory.get_gold() < gold_thach_dau {
        ServiceHandles::send_thong_bao_to_player(
            &snapshot,
            &format!(
                "Bạn chỉ có {} vàng, không đủ tiền cược",
                snapshot.inventory.get_gold()
            ),
        )?;
        return Ok(());
    }

    if let Some(target_snap) = target_handle.get_snapshot().await {
        if target_snap.inventory.get_gold() < gold_thach_dau {
            ServiceHandles::send_thong_bao_to_player(
                &snapshot,
                &format!(
                    "Đối thủ chỉ có {} vàng, không đủ tiền cược",
                    target_snap.inventory.get_gold()
                ),
            )?;
            return Ok(());
        }

        let my_id = snapshot.id as i64;
        let gold = gold_thach_dau;
        target_handle.send_forget(PlayerMessage::Modify(Box::new(move |player| {
            player.interaction_state.id_play_thach_dau = my_id;
            player.interaction_state.gold_thach_dau = gold;
        })));

        let mut invite_msg = Message::new(-59);
        invite_msg.write_byte(3)?;
        invite_msg.write_int(snapshot.id as i32)?;
        invite_msg.write_int(gold_thach_dau as i32)?;
        invite_msg.write_utf(&format!(
            "{} (sức mạnh {}) muốn thách đấu bạn với mức cược {}",
            snapshot.name,
            number_to_money(snapshot.n_point.power),
            gold_thach_dau
        ))?;
        target_snap.send_to_client(invite_msg)?;
    }
    Ok(())
}

async fn accept_pvp(session: &SessionArc) -> Result<()> {
    let snapshot = match session.get_player_snapshot().await {
        Some(s) => s,
        None => return Ok(()),
    };

    let target_id = snapshot.interaction_state.id_play_thach_dau;
    if target_id == 0 {
        return Ok(());
    }

    let target_handle = match PLAYER_MANAGER.get(target_id as u64) {
        Some(h) => h,
        None => {
            npc_service::hide_wait_dialog(session)?;
            ServiceHandles::send_thong_bao_to_player(&snapshot, "Đối thủ đã thoát game")?;
            return Ok(());
        }
    };

    let pvp_handle = get_pvp_handle();
    if pvp_handle.has_pvp(snapshot.id as i64).await || pvp_handle.has_pvp(target_id).await {
        npc_service::hide_wait_dialog(session)?;
        ServiceHandles::send_thong_bao_to_player(&snapshot, "Đang giao đấu không thể mời.")?;
        return Ok(());
    }

    let gold_thach_dau = snapshot.interaction_state.gold_thach_dau;

    if snapshot.inventory.get_gold() < gold_thach_dau {
        ServiceHandles::hide_wait_dialog_client(&snapshot);
        let gold_player = snapshot.inventory.get_gold();
        ServiceHandles::send_thong_bao_to_player(
            &snapshot,
            &format!("Bạn chỉ có {} vàng, không đủ tiền cược", gold_player),
        );
        return Ok(());
    }

    if let Some(target_snap) = target_handle.get_snapshot().await {
        if target_snap.inventory.get_gold() < gold_thach_dau {
            ServiceHandles::hide_wait_dialog_client(&snapshot);
            let gold_player = target_snap.inventory.get_gold();
            ServiceHandles::send_thong_bao_to_player(
                &snapshot,
                &format!("Đối thủ chỉ có {} vàng, không đủ tiền cược", gold_player),
            );
            return Ok(());
        }
    }

    // Create Thachdau ===>> Hehe
    let thach_dau = ThachDau::new(snapshot.id as i64, target_id, gold_thach_dau);
    pvp_handle.create_pvp(Box::new(thach_dau));

    Ok(())
}

/// Gửi lời mời luyện tập
async fn send_invite_pvp_luyentap(session: &SessionArc, target_id: i64) -> Result<()> {
    let snapshot = match session.get_player_snapshot().await {
        Some(s) => s,
        None => return Ok(()),
    };

    // Kiểm tra target online
    let target_handle = match PLAYER_MANAGER.get(target_id as u64) {
        Some(h) => h,
        None => {
            ServiceHandles::send_thong_bao_to_player(&snapshot, "Đối thủ đã rời map")?;
            return Ok(());
        }
    };

    let target_snap = match target_handle.get_snapshot().await {
        Some(s) => s,
        None => {
            ServiceHandles::send_thong_bao_to_player(&snapshot, "Đối thủ đã rời map")?;
            return Ok(());
        }
    };

    // Set thông tin cho đối thủ
    let my_id = snapshot.id as i64;
    target_handle.send_forget(PlayerMessage::Modify(Box::new(move |player| {
        player.interaction_state.id_play_thach_dau = my_id;
    })));

    // Gửi message mời luyện tập cho đối thủ
    let power = number_to_money(snapshot.n_point.power);
    let mut invite_msg = Message::new(-59);
    invite_msg.write_byte(4)?;
    invite_msg.write_int(snapshot.id as i32)?;
    invite_msg.write_int(0)?;
    invite_msg.write_utf(&format!(
        "{} (sức mạnh {}) muốn luyện tập với bạn",
        snapshot.name, power
    ))?;
    target_snap.send_to_client(invite_msg)?;

    Ok(())
}

/// Accept tap luyen
async fn accept_pvp_luyentap(session: &SessionArc) -> Result<()> {
    let snapshot = match session.get_player_snapshot().await {
        Some(s) => s,
        None => return Ok(()),
    };

    let target_id = snapshot.interaction_state.id_play_thach_dau;
    if target_id == 0 {
        return Ok(());
    }

    if !PLAYER_MANAGER.contains(target_id as u64) {
        npc_service::hide_wait_dialog(session)?;
        ServiceHandles::send_thong_bao_to_player(&snapshot, "Đối thủ đã rời map")?;
        return Ok(());
    }

    // Kiểm tra đang PVP
    let pvp_handle = get_pvp_handle();
    if pvp_handle.has_pvp(snapshot.id as i64).await || pvp_handle.has_pvp(target_id).await {
        npc_service::hide_wait_dialog(session)?;
        ServiceHandles::send_thong_bao_to_player(&snapshot, "Đang giao đấu không thể mời.")?;
        return Ok(());
    }

    // Tạo LuyenTap
    let luyen_tap = LuyenTap::new(snapshot.id as i64, target_id);
    pvp_handle.create_pvp(Box::new(luyen_tap));

    Ok(())
}

// ===== TRẢ THÙ =====

/// Mở menu trả thù
pub async fn open_select_revenge(session: &SessionArc, id_enemy: i64) -> Result<()> {
    let snapshot = match session.get_player_snapshot().await {
        Some(s) => s,
        None => return Ok(()),
    };

    // Kiểm tra kẻ thù online
    if !PLAYER_MANAGER.contains(id_enemy as u64) {
        npc_service::hide_wait_dialog(session)?;
        ServiceHandles::send_thong_bao_to_player(&snapshot, "Đang offline")?;
        return Ok(());
    }

    // Lưu id_enemy
    if let Some(handle) = session.get_player_handle().await {
        handle.send_forget(PlayerMessage::Modify(Box::new(move |player| {
            player.interaction_state.id_enemy = id_enemy;
        })));
    }

    let now = crate::utils::time::current_time_millis();
    let last_revenge = snapshot.interaction_state.last_time_revenge;
    let can_do = now.saturating_sub(last_revenge) >= 300_000;

    if !can_do {
        // Trong 5 phút → đi trực tiếp không tốn ngọc
        accept_revenge(session).await?;
        return Ok(());
    }

    // Hỏi xác nhận trả thù
    npc_service::create_menu(
        session,
        -1,
        "Bạn muốn đến ngay chỗ hắn, phí là 1 ngọc\nvà được tìm thoải mái trong 5 phút nhé",
        vec!["OK", "Từ chối"],
        crate::constant::menu_enum::MenuId::Revenge,
    )
    .await?;

    Ok(())
}

/// Chấp nhận trả thù → teleport + tạo TraThu
pub async fn accept_revenge(session: &SessionArc) -> Result<()> {
    let snapshot = match session.get_player_snapshot().await {
        Some(s) => s,
        None => return Ok(()),
    };

    let now = crate::utils::time::current_time_millis();
    let last_revenge = snapshot.interaction_state.last_time_revenge;
    let can_do = now.saturating_sub(last_revenge) >= 300_000;

    if can_do {
        // Tốn 1 ngọc
        let total_gem = snapshot.inventory.get_gem() + snapshot.inventory.get_ruby();
        if total_gem < 1 {
            ServiceHandles::send_thong_bao_to_player(
                &snapshot,
                "Bạn không đủ ngọc, còn thiếu 1 ngọc nữa",
            )?;
            return Ok(());
        }

        // Trừ ngọc và set timer
        if let Some(handle) = session.get_player_handle().await {
            let now_ts = now;
            handle.send_forget(PlayerMessage::Modify(Box::new(move |player| {
                if !player.inventory.sub_gem(1) {
                    player.inventory.sub_ruby(1);
                }
                player.interaction_state.last_time_revenge = now_ts;
                let _ = ServiceHandles::send_gold_gem_ruby_to_client(player);
            })));
        }
    }

    let id_enemy = snapshot.interaction_state.id_enemy;

    let enemy_handle = match PLAYER_MANAGER.get(id_enemy as u64) {
        Some(h) => h,
        None => {
            npc_service::hide_wait_dialog(session)?;
            ServiceHandles::send_thong_bao_to_player(&snapshot, "Đang offline")?;
            return Ok(());
        }
    };

    let pvp_handle = get_pvp_handle();
    if pvp_handle.has_pvp(snapshot.id as i64).await || pvp_handle.has_pvp(id_enemy).await {
        npc_service::hide_wait_dialog(session)?;
        ServiceHandles::send_thong_bao_to_player(
            &snapshot,
            "Chưa thể đến lúc này, vui lòng thử lại sau ít phút",
        )?;
        return Ok(());
    }

    // Lấy vị trí kẻ thù, teleport player đến
    if let Some(enemy_snap) = enemy_handle.get_snapshot().await {
        if let Some(handle) = session.get_player_handle().await {
            handle.send_forget(PlayerMessage::ChangeMap {
                map_id: enemy_snap.map_id,
                zone_id: enemy_snap.zone_id,
                x: enemy_snap.location.x,
                y: enemy_snap.location.y,
                space_type: SpaceShipType::None,
            });
        }

        // Tạo TraThu PVP
        let tra_thu = TraThu::new(snapshot.id as i64, id_enemy);
        pvp_handle.create_pvp(Box::new(tra_thu));
    }

    Ok(())
}
