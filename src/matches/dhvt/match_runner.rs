use super::constants::*;
use super::manager::DhvtHandle;
use crate::map::{map_service, ChangeMapService, SpaceShipType};
use crate::matches::pvp::{change_type_pk, send_thong_bao};
use crate::player::player_actor::message::PlayerMessage;
use crate::player::player_actor::TypePk;
use crate::player::player_manager::PLAYER_MANAGER;
use crate::utils::Location;
use std::time::Duration;

#[derive(Debug)]
struct MatchResult {
    winner_id: i64,
    loser_id: i64,
    reson: EndReson,
}
#[derive(Debug)]
enum EndReson {
    Fallout,
    Die,
    Leave,
    Timeout,
}

struct CountdownEvent {
    at_tick: i32,
    message: &'static str,
}
const COUNDOWN_EVENTS: &[CountdownEvent] = &[
    CountdownEvent {
        at_tick: 6,
        message: "Trận đấu sắp bắt đầu, hãy chuẩn bị!",
    },
    CountdownEvent {
        at_tick: 13,
        message: "3...",
    },
    CountdownEvent {
        at_tick: 15,
        message: "2...",
    },
    CountdownEvent {
        at_tick: 17,
        message: "1...",
    },
    CountdownEvent {
        at_tick: 19,
        message: "Trận đấu bắt đầu!",
    },
];
const TICK_ENABLE: i32 = 22;

/// Chạy 1 trận đấu DHVT trên 1 zone riêng
pub async fn run_match(p1_id: i64, p2_id: i64, zone_id: i32, dhvt_handle: DhvtHandle) {
    tracing::info!(
        "[DHVT_MATCH] Starting match: {} vs {} on zone {}",
        p1_id,
        p2_id,
        zone_id
    );

    // ── Phase 1: Chuẩn bị ──
    // Di chuyển 2 player vào map võ đài
    teleport_player(
        p1_id,
        MAP_VO_DAI,
        zone_id,
        P1_SPAWN_X as i16,
        P1_SPAWN_Y as i16,
    );
    teleport_player(
        p2_id,
        MAP_VO_DAI,
        zone_id,
        P2_SPAWN_X as i16,
        P2_SPAWN_Y as i16,
    );

    // Chờ player load map
    tokio::time::sleep(Duration::from_millis(1000)).await;

    // Full HP/MP
    modify_player(p1_id, |player| {
        player.n_point.set_full_hp_mp();
    });
    modify_player(p2_id, |player| {
        player.n_point.set_full_hp_mp();
    });

    // Đếm ngược
    let tick_duration = Duration::from_millis(MATCH_TICK_MS);
    for tick in 0..MATCH_COUNTDOWN_TICKS {
        if let Some(event) = COUNDOWN_EVENTS.iter().find(|e| e.at_tick == tick) {
            send_thong_bao(p1_id, event.message);
            send_thong_bao(p2_id, event.message);
        }

        if tick == TICK_ENABLE {
            change_type_pk(p1_id, TypePk::PkPvp);
            change_type_pk(p2_id, TypePk::PkPvp);
        }
        tokio::time::sleep(tick_duration).await;
    }

    // ── Phase 2: Thi đấu ──
    let mut p1_total_damage: u64 = 0;
    let mut p2_total_damage: u64 = 0;
    let mut result: Option<MatchResult> = None;

    for _tick in 0..MATCH_FIGHT_TICKS {
        // Lấy snapshot cả 2 player
        let p1_snap = get_player_snapshot(p1_id).await;
        let p2_snap = get_player_snapshot(p2_id).await;

        match (p1_snap, p2_snap) {
            (Some(p1), Some(p2)) => {
                // Track damage
                let p1_max_hp = p1.n_point.hp_max as u64;
                let p1_current_hp = p1.n_point.hp_current.max(0) as u64;
                let p2_max_hp = p2.n_point.hp_max as u64;
                let p2_current_hp = p2.n_point.hp_current.max(0) as u64;
                p1_total_damage = p1_max_hp.saturating_sub(p1_current_hp);
                p2_total_damage = p2_max_hp.saturating_sub(p2_current_hp);

                if p1.map_id != MAP_VO_DAI {
                    result = Some(MatchResult {
                        winner_id: p2_id,
                        loser_id: p1_id,
                        reson: EndReson::Leave,
                    });
                    break;
                }
                if p2.map_id != MAP_VO_DAI {
                    result = Some(MatchResult {
                        winner_id: p1_id,
                        loser_id: p2_id,
                        reson: EndReson::Leave,
                    });
                    break;
                }
                if p1.is_die() {
                    result = Some(MatchResult {
                        winner_id: p2_id,
                        loser_id: p1_id,
                        reson: EndReson::Die,
                    });
                    break;
                }
                if p2.is_die() {
                    result = Some(MatchResult {
                        winner_id: p1_id,
                        loser_id: p2_id,
                        reson: EndReson::Die,
                    });
                    break;
                }

                if p1.location.x < ARENA_X_MIN
                    || p1.location.x > ARENA_X_MAX
                    || p1.location.y > ARENA_Y_MAX
                {
                    result = Some(MatchResult {
                        winner_id: p2_id,
                        loser_id: p1_id,
                        reson: EndReson::Fallout,
                    });
                    break;
                }
                if p2.location.x < ARENA_X_MIN
                    || p2.location.x > ARENA_X_MAX
                    || p2.location.y > ARENA_Y_MAX
                {
                    result = Some(MatchResult {
                        winner_id: p1_id,
                        loser_id: p2_id,
                        reson: EndReson::Fallout,
                    });
                    break;
                }
            }
            (None, Some(_)) => {
                result = Some(MatchResult {
                    winner_id: p2_id,
                    loser_id: p1_id,
                    reson: EndReson::Leave,
                });
                break;
            }
            (Some(_), None) => {
                result = Some(MatchResult {
                    winner_id: p1_id,
                    loser_id: p2_id,
                    reson: EndReson::Leave,
                });
                break;
            }
            (None, None) => {
                tracing::warn!("[DHVT_MATCH] Both players offline, aborting");
                return;
            }
        }

        tokio::time::sleep(tick_duration).await;
    }

    // Timeout: so sánh damage taken
    if result.is_none() {
        if p1_total_damage >= p2_total_damage {
            result = Some(MatchResult {
                winner_id: p2_id,
                loser_id: p1_id,
                reson: EndReson::Timeout,
            });
        } else {
            result = Some(MatchResult {
                winner_id: p1_id,
                loser_id: p2_id,
                reson: EndReson::Timeout,
            });
        };
    }

    // ── Phase 3: Kết thúc ──
    let match_result = result.unwrap();
    let winner_id = match_result.winner_id;
    let loser_id = match_result.loser_id;

    // Thông báo kết quả
    send_match_result(winner_id, loser_id, &match_result);

    // Reset PK mode
    change_type_pk(p1_id, TypePk::PkNon);
    change_type_pk(p2_id, TypePk::PkNon);

    // wait 5s
    tokio::time::sleep(Duration::from_millis(5000)).await;
    // Winner: full HP/MP rồi teleport về map 52 (phòng chờ)

    modify_player(winner_id, |player| {
        player.n_point.set_full_hp_mp();
    });

    // =========hanldep player win 2 round thi finish task ====

    teleport_player_spaceship(winner_id, MAP_PHONG_CHO, -1, 300, 336);

    // Loser: hồi sinh nếu đang chết, rồi teleport về nhà
    modify_player(loser_id, |player| {
        if player.is_die() {
            player.n_point.set_full_hp_mp();
        }
    });
    teleport_to_home(loser_id);

    // Thông báo Manager
    dhvt_handle.match_finished(winner_id, loser_id);

    tracing::info!(
        "[DHVT_MATCH] Match finished: winner={}, loser={}, reason={:?}",
        winner_id,
        loser_id,
        match_result.reson
    );
}

fn send_match_result(winner_id: i64, loser_id: i64, result: &MatchResult) {
    let winner_msg = match result.reson {
        EndReson::Die => TEXT_DOI_THU_KIET_SUC,
        EndReson::Fallout => TEXT_DOI_THU_BO_CUOC_ROI_MAP,
        EndReson::Leave => TEXT_DOI_THU_BO_CUOC_ROI_MAP,
        EndReson::Timeout => "Hết giờ thi đấu, bạn đã chiến thắng vì bị thương ít hơn",
    };
    let loser_msg = match result.reson {
        EndReson::Die => TEXT_CHIA_BUON,
        EndReson::Fallout => TEXT_CHIA_BUON,
        EndReson::Leave => TEXT_XU_THUA_BO_CHAY,
        EndReson::Timeout => TEXT_CHIA_BUON,
    };
    send_thong_bao(winner_id, winner_msg);
    send_thong_bao(loser_id, loser_msg);
}

// ── Helper functions ──

async fn get_player_snapshot(player_id: i64) -> Option<crate::player::player::Player> {
    let handle = PLAYER_MANAGER.get(player_id as u64)?;
    handle.get_snapshot().await
}

fn teleport_player(player_id: i64, map_id: i32, zone_id: i32, x: i16, y: i16) {
    if let Some(handle) = PLAYER_MANAGER.get(player_id as u64) {
        handle.send_forget(PlayerMessage::ChangeMap {
            map_id,
            zone_id,
            x,
            y,
            space_type: SpaceShipType::None,
        });
    }
}

fn teleport_player_spaceship(player_id: i64, map_id: i32, zone_id: i32, x: i16, y: i16) {
    if let Some(handle) = PLAYER_MANAGER.get(player_id as u64) {
        handle.send_forget(PlayerMessage::ChangeMap {
            map_id,
            zone_id,
            x,
            y,
            space_type: SpaceShipType::Default,
        });
    }
}

fn teleport_to_home(player_id: i64) {
    if let Some(handle) = PLAYER_MANAGER.get(player_id as u64) {
        tokio::spawn(async move {
            if let Some(player) = handle.get_snapshot().await {
                let is_mabu = map_service::is_mapa_mabu(player.map_id);
                let home_map = ChangeMapService::calculate_home_map(player.gender, is_mabu);
                handle.send_forget(PlayerMessage::ChangeMap {
                    map_id: home_map,
                    zone_id: -1,
                    x: 300,
                    y: 336,
                    space_type: SpaceShipType::Default,
                });
            }
        });
    }
}

fn modify_player(
    player_id: i64,
    f: impl FnOnce(&mut crate::player::player::Player) + Send + 'static,
) {
    if let Some(handle) = PLAYER_MANAGER.get(player_id as u64) {
        handle.send_forget(PlayerMessage::Modify(Box::new(f)));
    }
}
