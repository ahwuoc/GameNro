use super::constants::*;
use super::match_runner;
use crate::matches::pvp::{change_type_pk, send_thong_bao};
use crate::player::player_manager::PLAYER_MANAGER;
use chrono::{Datelike, Local, Timelike};
use std::sync::OnceLock;
use std::time::Duration;
use tokio::sync::{mpsc, oneshot};
use tokio::task::JoinHandle;

static DHVT_HANDLE: OnceLock<DhvtHandle> = OnceLock::new();

pub fn init_dhvt() {
    let (handle, actor) = DhvtActor::new();
    handle.spawn_tick_loop();
    tokio::spawn(actor.run());
    DHVT_HANDLE
        .set(handle)
        .expect("DHVT_HANDLE already initialized");
    tracing::info!("DhvtActor started");
}

pub fn get_dhvt_handle() -> &'static DhvtHandle {
    DHVT_HANDLE
        .get()
        .expect("DHVT_HANDLE not initialized, call init_dhvt() first")
}

// ── Messages ──

pub enum DhvtMessage {
    Register {
        player_id: i64,
    },
    Unregister {
        player_id: i64,
    },
    IsRegistered {
        player_id: i64,
        reply: oneshot::Sender<bool>,
    },
    CheckPlayer {
        player_id: i64,
        reply: oneshot::Sender<bool>,
    },
    GetInfo {
        player_id: i64,
        reply: oneshot::Sender<DhvtInfo>,
    },
    MatchFinished {
        winner_id: i64,
        loser_id: i64,
    },
    IsChampion {
        player_name: String,
        reply: oneshot::Sender<bool>,
    },
    Tick,
    ForceStart,
}

#[derive(Debug, Clone)]
pub struct DhvtInfo {
    pub can_reg: bool,
    pub round: i32,
    pub reg_count: usize,
    pub tournament: TournamentClass,
    pub cup_name: String,
    pub is_registered: bool,
    pub is_in_wait_list: bool,
    pub hour: u32,
}

// ── Handle ──

#[derive(Clone, Debug)]
pub struct DhvtHandle {
    sender: mpsc::Sender<DhvtMessage>,
}

impl DhvtHandle {
    pub fn register(&self, player_id: i64) {
        let _ = self.sender.try_send(DhvtMessage::Register { player_id });
    }

    pub fn unregister(&self, player_id: i64) {
        let _ = self.sender.try_send(DhvtMessage::Unregister { player_id });
    }

    pub async fn is_registered(&self, player_id: i64) -> bool {
        let (tx, rx) = oneshot::channel();
        let _ = self
            .sender
            .send(DhvtMessage::IsRegistered {
                player_id,
                reply: tx,
            })
            .await;
        rx.await.unwrap_or(false)
    }

    /// Kiểm tra player có đang chờ vòng sau không (đã vào vòng trong)
    pub async fn check_player(&self, player_id: i64) -> bool {
        let (tx, rx) = oneshot::channel();
        let _ = self
            .sender
            .send(DhvtMessage::CheckPlayer {
                player_id,
                reply: tx,
            })
            .await;
        rx.await.unwrap_or(false)
    }

    pub async fn get_info(&self, player_id: i64) -> DhvtInfo {
        let (tx, rx) = oneshot::channel();
        let _ = self
            .sender
            .send(DhvtMessage::GetInfo {
                player_id,
                reply: tx,
            })
            .await;
        rx.await.unwrap_or(DhvtInfo {
            can_reg: false,
            round: 0,
            reg_count: 0,
            tournament: TournamentClass::default(),
            cup_name: String::new(),
            is_registered: false,
            is_in_wait_list: false,
            hour: 0,
        })
    }

    pub fn match_finished(&self, winner_id: i64, loser_id: i64) {
        let _ = self.sender.try_send(DhvtMessage::MatchFinished {
            winner_id,
            loser_id,
        });
    }

    pub async fn is_champion(&self, player_name: &str) -> bool {
        let (tx, rx) = oneshot::channel();
        let _ = self
            .sender
            .send(DhvtMessage::IsChampion {
                player_name: player_name.to_string(),
                reply: tx,
            })
            .await;
        rx.await.unwrap_or(false)
    }

    fn spawn_tick_loop(&self) {
        let handle = self.clone();
        tokio::spawn(async move {
            let mut interval = tokio::time::interval(Duration::from_secs(1));
            loop {
                interval.tick().await;
                let _ = handle.sender.send(DhvtMessage::Tick).await;
            }
        });
    }

    /// Force bắt đầu ghép cặp (admin test cmd)
    pub fn force_start(&self) {
        let _ = self.sender.try_send(DhvtMessage::ForceStart);
    }
}

// ── Actor ──

pub struct DhvtActor {
    receiver: mpsc::Receiver<DhvtMessage>,
    list_reg: Vec<i64>,
    list_wait: Vec<i64>,
    list_champ: Vec<String>,
    active_matches: Vec<JoinHandle<()>>,
    round: i32,
    can_reg: bool,
    tournament: TournamentClass,
    last_mins: u32,
    last_date: u32,
}

impl DhvtActor {
    pub fn new() -> (DhvtHandle, Self) {
        let (tx, rx) = mpsc::channel(256);
        let handle = DhvtHandle { sender: tx };
        let actor = Self {
            receiver: rx,
            list_reg: Vec::new(),
            list_wait: Vec::new(),
            list_champ: Vec::new(),
            active_matches: Vec::new(),
            round: 0,
            can_reg: false,
            tournament: TournamentClass::default(),
            last_mins: 99,
            last_date: 0,
        };
        (handle, actor)
    }

    pub async fn run(mut self) {
        while let Some(msg) = self.receiver.recv().await {
            match msg {
                DhvtMessage::Register { player_id } => {
                    if !self.list_reg.contains(&player_id) {
                        self.list_reg.push(player_id);
                        tracing::info!(
                            "[DHVT] Player {} registered. Total: {}",
                            player_id,
                            self.list_reg.len()
                        );
                    }
                }
                DhvtMessage::Unregister { player_id } => {
                    self.list_reg.retain(|&id| id != player_id);
                    tracing::info!(
                        "[DHVT] Player {} unregistered. Total: {}",
                        player_id,
                        self.list_reg.len()
                    );
                }
                DhvtMessage::IsRegistered { player_id, reply } => {
                    let _ = reply.send(self.list_reg.contains(&player_id));
                }
                DhvtMessage::CheckPlayer { player_id, reply } => {
                    let in_wait = self.list_wait.contains(&player_id);
                    let in_reg = self.list_reg.contains(&player_id);
                    let _ = reply.send(in_wait || in_reg);
                }
                DhvtMessage::GetInfo { player_id, reply } => {
                    let now = Local::now();
                    let _ = reply.send(DhvtInfo {
                        can_reg: self.can_reg,
                        round: self.round,
                        reg_count: self.list_reg.len(),
                        tournament: self.tournament,
                        cup_name: self.tournament.get_name().to_string(),

                        is_registered: self.list_reg.contains(&player_id),
                        is_in_wait_list: self.list_wait.contains(&player_id),
                        hour: now.hour(),
                    });
                }
                DhvtMessage::MatchFinished {
                    winner_id,
                    loser_id,
                } => {
                    self.handle_match_finished(winner_id, loser_id);
                }
                DhvtMessage::IsChampion { player_name, reply } => {
                    let _ = reply.send(self.list_champ.contains(&player_name));
                }
                DhvtMessage::Tick => {
                    self.tick();
                }
                DhvtMessage::ForceStart => {
                    tracing::info!("[DHVT] ForceStart triggered by admin");
                    if !self.list_reg.is_empty() {
                        self.pair_and_start_matches();
                    } else {
                        tracing::warn!("[DHVT] ForceStart: no players registered");
                    }
                }
            }
        }
    }

    fn tick(&mut self) {
        let now = Local::now();
        let hour = now.hour();
        let min = now.minute();
        let day = now.ordinal();

        // Daily reset
        if day != self.last_date {
            self.list_champ.clear();
            self.last_date = day;
            tracing::info!("[DHVT] Daily reset - cleared champion list");
        }

        let tour = TournamentClass::from_hour(hour);

        if let Some(tour) = tour {
            self.tournament = tour;
            self.can_reg = min < MINS_MAX_CAN_REG;
            self.update_tournament(min);
        } else {
            if self.round != 0 || !self.list_reg.is_empty() {
                self.round = 0;
                self.list_reg.clear();
                self.list_wait.capacity();
                for handle in self.active_matches.drain(..) {
                    handle.abort();
                }
            }
        };
    }

    fn update_tournament(&mut self, min: u32) {
        if min >= MINS_END {
            // Force kết thúc tất cả
            if self.round != 0 || !self.list_reg.is_empty() || !self.list_wait.is_empty() {
                tracing::info!("[DHVT] Tournament ended (MINS_END reached)");
                self.round = 0;
                for handle in self.active_matches.drain(..) {
                    handle.abort();
                }
                self.list_reg.clear();
                self.list_wait.clear();
            }
        } else if min >= MINS_START {
            // Nếu chỉ còn 1 người chờ, không còn trận, không còn đăng ký → VÔ ĐỊCH
            if self.list_wait.len() == 1
                && self.list_reg.is_empty()
                && self.active_matches.is_empty()
            {
                let winner_id = self.list_wait[0];
                self.reward_champion(winner_id);
                return;
            }

            // Vòng sau: chuyển list_wait → list_reg khi tất cả trận kết thúc
            if self.round > 0
                && self.list_wait.len() > 1
                && self.list_reg.is_empty()
                && self.active_matches.is_empty()
            {
                tracing::info!(
                    "[DHVT] Round {} done. {} players advancing to next round",
                    self.round,
                    self.list_wait.len()
                );
                self.list_reg = self.list_wait.drain(..).collect();
            }

            // Ghép cặp nếu có người đăng ký và không có trận đang chạy
            if !self.list_reg.is_empty() && self.active_matches.is_empty() {
                self.pair_and_start_matches();
            }
        }
    }

    fn pair_and_start_matches(&mut self) {
        self.round += 1;
        tracing::info!(
            "[DHVT] Starting round {}. {} players registered",
            self.round,
            self.list_reg.len()
        );

        // Loại player không ở map 52 (check bằng PLAYER_MANAGER.contains)
        // Vì chúng ta không thể await get_snapshot trong sync context,
        // ta chỉ kiểm tra player còn online không. Map check sẽ ở match_runner.
        self.list_reg.retain(|&player_id| {
            let online = PLAYER_MANAGER.contains(player_id as u64);
            if !online {
                tracing::info!("[DHVT] Player {} eliminated (offline)", player_id);
            }
            online
        });

        if self.list_reg.is_empty() {
            tracing::info!("[DHVT] No players left after filter");
            return;
        }

        // Lẻ người → 1 người vào list_wait (bye)
        if self.list_reg.len() % 2 != 0 {
            if let Some(bye_id) = self.list_reg.pop() {
                self.list_wait.push(bye_id);
                send_thong_bao(bye_id, TEXT_DOI_THU_BO_CUOC);
                tracing::info!("[DHVT] Player {} gets a bye", bye_id);
            }
        }

        if self.list_reg.is_empty() {
            return;
        }

        // Ghép cặp
        let pairs: Vec<(i64, i64)> = self
            .list_reg
            .chunks(2)
            .filter_map(|chunk| {
                if chunk.len() == 2 {
                    Some((chunk[0], chunk[1]))
                } else {
                    None
                }
            })
            .collect();

        self.list_reg.clear();

        let dhvt_handle = get_dhvt_handle().clone();
        for (i, (p1, p2)) in pairs.into_iter().enumerate() {
            tracing::info!("[DHVT] Match: {} vs {} on zone {}", p1, p2, i);
            let handle = dhvt_handle.clone();
            let zone_id = i as i32;
            let jh = tokio::spawn(async move {
                match_runner::run_match(p1, p2, zone_id, handle).await;
            });
            self.active_matches.push(jh);
        }
    }

    fn handle_match_finished(&mut self, winner_id: i64, _loser_id: i64) {
        self.list_wait.push(winner_id);
        tracing::info!(
            "[DHVT] Match finished. Winner: {}. Wait list: {}, Active: {}",
            winner_id,
            self.list_wait.len(),
            self.active_matches
                .iter()
                .filter(|h| !h.is_finished())
                .count()
        );
    }

    fn reward_champion(&mut self, player_id: i64) {
        tracing::info!("[DHVT] CHAMPION: player {}", player_id);

        // Lấy tên player để ghi vào danh sách vô địch
        if let Some(handle) = PLAYER_MANAGER.get_ref(player_id as u64) {
            // Lưu ID player vào champion list (dùng id format string vì không có name trên handle)
            let player_id_str = player_id.to_string();
            self.list_champ.push(player_id_str);
        }

        send_thong_bao(player_id, TEXT_VO_DICH);

        // TODO: Thưởng vô địch
        // - drop item 77 x 50
        // - drop đá nâng cấp 220-224
        // - sendThongBaoToAll(TEXT_KHOE_VO_DICH)

        // Teleport winner về map 52
        if let Some(handle) = PLAYER_MANAGER.get(player_id as u64) {
            use crate::map::SpaceShipType;
            use crate::player::player_actor::message::PlayerMessage;
            handle.send_forget(PlayerMessage::ChangeMap {
                map_id: MAP_PHONG_CHO,
                zone_id: -1,
                x: 300,
                y: 336,
                space_type: SpaceShipType::None,
            });
        }

        self.list_reg.clear();
        self.list_wait.clear();
    }
}
