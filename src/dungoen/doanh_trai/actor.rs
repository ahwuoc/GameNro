use super::handle::DoanhTraiHandle;
use super::message::DoanhTraiMessage;
use crate::boss;
use crate::boss::{boss_id::BOSS_TRUNG_UY_TRANG, manager::BossManager};
use crate::clan::clan_manager::CLAN_MANAGER;
use crate::clan::message::ClanMessage;
use crate::map::managers::map_manager::MAP_MANAGER;
use crate::map::services::change_map_models::SpaceShipType;
use crate::map::zone_manager::ZONE_MANAGER;
use crate::mob::RtMob;
use crate::player::player_actor::message::PlayerMessage;
use crate::player::player_actor::PlayerHandle;
use crate::services::ServiceHandles;
use crate::templates::mob_template_manager;
use crate::utils::time::current_time_millis;
use std::time::Duration;
use tokio::sync::mpsc;
use tracing::{error, info};

/// 30 phút (ms)
const TIME_DOANH_TRAI_MS: i64 = 1_800_000;
/// 5 phút (ms) sau khi đánh xong trùm cuối hoặc hoàn thành
const TIME_PICK_MS: i64 = 300_000;
/// Tick interval (ms)
const TICK_INTERVAL_MS: u64 = 150;
/// Map IDs của doanh trại
const MAP_IDS: [i32; 10] = [53, 54, 55, 56, 57, 58, 59, 60, 61, 62];
/// Map doanh trại đầu tiên
const MAP_DOANH_TRAI_ENTRY: i32 = 53;

pub struct DoanhTraiActor {
    pub id: i32,
    pub sender: mpsc::Sender<DoanhTraiMessage>,
    pub receiver: mpsc::Receiver<DoanhTraiMessage>,

    // State
    pub clan_id: Option<i32>,
    pub is_opened: bool,
    pub is_time_picking: bool,
    pub win_dt: bool,
    pub last_time_open: i64,
    pub last_time_pick: i64,

    // Players đang trong doanh trại
    pub player_handles: Vec<PlayerHandle>,
}

impl DoanhTraiActor {
    pub fn new(
        id: i32,
        sender: mpsc::Sender<DoanhTraiMessage>,
        receiver: mpsc::Receiver<DoanhTraiMessage>,
    ) -> Self {
        Self {
            id,
            sender,
            receiver,
            clan_id: None,
            is_opened: false,
            is_time_picking: false,
            win_dt: false,
            last_time_open: 0,
            last_time_pick: 0,
            player_handles: Vec::new(),
        }
    }

    pub fn get_handle(&self) -> DoanhTraiHandle {
        DoanhTraiHandle::new(self.id, self.sender.clone())
    }

    pub async fn run(mut self) {
        let mut interval = tokio::time::interval(Duration::from_millis(TICK_INTERVAL_MS));

        loop {
            tokio::select! {
                Some(msg) = self.receiver.recv() => {
                    match msg {
                        DoanhTraiMessage::Shutdown => {
                            self.handle_shutdown().await;
                            break;
                        }
                        m => self.handle_message(m).await,
                    }
                }
                _ = interval.tick() => {
                    if self.is_opened {
                        self.update().await;
                    }
                }
            }
        }
    }

    async fn handle_message(&mut self, msg: DoanhTraiMessage) {
        match msg {
            DoanhTraiMessage::Open {
                clan_id,
                opener_handle,
                teammate_handles,
            } => {
                self.handle_open(clan_id, opener_handle, teammate_handles)
                    .await;
            }
            DoanhTraiMessage::Join { player_handle } => {
                self.handle_join(player_handle).await;
            }
            DoanhTraiMessage::IsActive(tx) => {
                let _ = tx.send(self.is_opened);
            }
            DoanhTraiMessage::GetClanId(tx) => {
                let _ = tx.send(self.clan_id);
            }
            DoanhTraiMessage::GetTimeLeft(tx) => {
                let elapsed = current_time_millis() as i64 - self.last_time_open;
                let left = ((TIME_DOANH_TRAI_MS - elapsed) / 1000).max(0);
                let _ = tx.send(left);
            }
            DoanhTraiMessage::Shutdown => {}
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Handlers
    // ─────────────────────────────────────────────────────────

    fn get_instance_zone_id(&self) -> i32 {
        100 + self.id
    }

    async fn handle_open(
        &mut self,
        clan_id: i32,
        opener: PlayerHandle,
        teammates: Vec<PlayerHandle>,
    ) {
        if self.is_opened {
            return;
        }

        let zone_id = self.get_instance_zone_id();
        let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
        for map_id in MAP_IDS {
            if let Err(e) = zone_manager.create_zone(map_id, zone_id, 15) {
                tracing::error!("Failed to create zone {}_{}: {:?}", map_id, zone_id, e);
            }
        }

        self.clan_id = Some(clan_id);
        self.is_opened = true;
        self.win_dt = false;
        self.last_time_open = current_time_millis() as i64;

        // Update clan state
        if let Some(clan_handle) = CLAN_MANAGER.get_clan(clan_id) {
            clan_handle.set_dungeon(self.get_handle());
            if let Some(clan) = clan_handle.get_snapshot().await {
                let clan_power = clan.power_point;
                self.init_mobs_dungeon(clan_power).await;
                self.init_bosses_dungoen_doanhtrai(clan_power).await;
            }
        }

        // Teleport opener vào map doanh trại
        self.teleport_player(&opener).await;
        self.player_handles.push(opener);

        // Teleport teammates
        for ph in teammates {
            self.teleport_player(&ph).await;
            self.player_handles.push(ph);
        }

        info!("DoanhTrai[{}] opened for clan {}", self.id, clan_id);
    }

    async fn handle_join(&mut self, ph: PlayerHandle) {
        if !self.is_opened {
            return;
        }

        // Teleport player vào map doanh trại
        self.teleport_player(&ph).await;
        self.player_handles.push(ph);
    }

    async fn update(&mut self) {
        let now = current_time_millis() as i64;

        // Check hết giờ
        if !self.is_time_picking && (now - self.last_time_open) > TIME_DOANH_TRAI_MS {
            self.finish().await;
            self.dispose().await;
            return;
        }
        if self.is_time_picking && (now - self.last_time_pick) > TIME_PICK_MS {
            self.finish().await;
            self.dispose().await;
            return;
        }

        // Cleanup: remove disconnected player handles
        self.player_handles.retain(|ph| !ph.tx.is_closed());
    }

    async fn finish(&mut self) {
        for ph in &self.player_handles {
            let msg = ServiceHandles::build_thong_bao("Đã hết thời gian, bạn sẽ được đưa về nhà");
            ph.send_forget(PlayerMessage::SendPacket(msg));
            if let Some(snapshot) = ph.get_snapshot().await {
                let home_map = 21 + snapshot.gender as i32;
                ph.send_forget(PlayerMessage::ChangeMap {
                    map_id: home_map,
                    zone_id: -1,
                    x: -1,
                    y: -1,
                    space_type: SpaceShipType::Auto,
                });
            }
        }
    }

    async fn dispose(&mut self) {
        // Xóa 10 zone riêng
        let zone_id = self.get_instance_zone_id();
        let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
        for map_id in MAP_IDS {
            zone_manager.remove_zone(map_id, zone_id);
        }

        // Update clan state
        if let Some(clan_id) = self.clan_id {
            if let Some(clan_handle) = CLAN_MANAGER.get_clan(clan_id) {
                clan_handle.send_forget(ClanMessage::SetGoneDungeon(true));
                clan_handle.send_forget(ClanMessage::ClearDungeon);
            }
        }

        self.player_handles.clear();
        self.clan_id = None;
        self.is_opened = false;
        self.is_time_picking = false;
        self.win_dt = false;

        info!("DoanhTrai[{}] disposed", self.id);
    }

    async fn handle_shutdown(&mut self) {
        if self.is_opened {
            self.finish().await;
            self.dispose().await;
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────

    async fn teleport_player(&self, ph: &PlayerHandle) {
        ph.send_forget(PlayerMessage::ChangeMap {
            map_id: MAP_DOANH_TRAI_ENTRY,
            zone_id: self.get_instance_zone_id(),
            x: -1,
            y: 60,
            space_type: SpaceShipType::Default,
        });
    }

    #[allow(dead_code)]
    pub async fn notify_all(&self, text: &str) {
        let msg = ServiceHandles::build_thong_bao(text);
        for ph in &self.player_handles {
            ph.send_forget(PlayerMessage::SendPacket(msg.clone()));
        }
    }

    async fn init_mobs_dungeon(&self, clan_power: i64) {
        let zone_id = self.get_instance_zone_id();
        let hp_quai = (clan_power / 2000).max(1000) as i32;
        let dame_quai = 5;

        for &map_id in &MAP_IDS {
            let Some(map) = MAP_MANAGER.find_by_id(map_id) else {
                continue;
            };
            let Some(zone) = ZONE_MANAGER.get_zone(map_id, zone_id) else {
                continue;
            };

            for (idx, mob) in map.info.mobs.iter().enumerate() {
                if let Some(template) = mob_template_manager::get(mob.temp_id as i8) {
                    let mut rt_mob = RtMob::from_template(template.clone(), idx as u64);
                    rt_mob.set_location(map_id, zone_id, mob.x, mob.y);
                    rt_mob.max_hp = hp_quai;
                    rt_mob.hp = hp_quai;
                    rt_mob.percent_dame = dame_quai;
                    rt_mob.status = 5;
                    rt_mob.spawn_status = 5;
                    if let Err(e) = zone.add_mob(rt_mob).await {
                        error!("Failed to add mob to zone {}_{}: {:?}", map_id, zone_id, e);
                    }
                }
            }
        }
    }

    async fn init_bosses_dungoen_doanhtrai(&self, clan_power: i64) {
        let zone_id = self.get_instance_zone_id();
        let mob_hp = (clan_power / 2000).max(1000) as i32;
        let mob_dame = mob_hp / 20;

        let base_hp = mob_hp * 50;
        let base_dame = mob_dame * 5;

        // Map 54: Ninja Áo Tím (hệ số x1.2)
        BossManager::spawn_boss_dungeon_async(
            boss::boss_id::BOSS_NINJA_AO_TIM,
            54,
            zone_id,
            868,
            336,
            Some(crate::boss::manager::Overrider {
                hp: Some((base_hp as f64 * 1.2) as i32),
                dame: Some((base_dame as f64 * 1.2) as i32),
                name: None,
            }),
        );

        BossManager::spawn_boss_dungeon_async(
            boss::boss_id::BOSS_TRUNG_UY_THEP,
            55,
            zone_id,
            444,
            288,
            Some(crate::boss::manager::Overrider {
                hp: Some((base_hp as f64 * 1.15) as i32),
                dame: Some((base_dame as f64 * 1.15) as i32),
                name: None,
            }),
        );

        // Map 57: Robot Vệ Sĩ x4 (hệ số x1.3)
        let robot_hp = (base_hp as f64 * 1.3) as i32;
        let robot_dame = (base_dame as f64 * 1.3) as i32;
        for i in 1..=4 {
            let x = 618 + rand::random_range(-100i16..100);
            BossManager::spawn_boss_dungeon_async(
                boss::boss_id::BOSS_ROBOT_VE_SI,
                57,
                zone_id,
                x,
                336,
                Some(crate::boss::manager::Overrider {
                    hp: Some(robot_hp),
                    dame: Some(robot_dame),
                    name: Some(format!("Rôbốt Vệ Sĩ 0{}", i)),
                }),
            );
        }

        // Map 59: Trung Úy Trắng (hệ số x1.0)
        BossManager::spawn_boss_dungeon_async(
            boss::boss_id::BOSS_TRUNG_UY_TRANG,
            59,
            zone_id,
            686,
            312,
            Some(crate::boss::manager::Overrider {
                hp: Some(base_hp),
                dame: Some(base_dame),
                name: None,
            }),
        );

        // Map 62: Trung Úy Xanh Lơ (hệ số x1.1)
        BossManager::spawn_boss_dungeon_async(
            boss::boss_id::BOSS_TRUNG_UY_XANH_LO,
            62,
            zone_id,
            750,
            264,
            Some(crate::boss::manager::Overrider {
                hp: Some((base_hp as f64 * 1.1) as i32),
                dame: Some((base_dame as f64 * 1.1) as i32),
                name: None,
            }),
        );
    }
}
