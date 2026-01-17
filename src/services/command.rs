use crate::constant::const_npc;
use crate::constant::menu_enum::MenuId;
use crate::map::change_map_service::{ChangeMapService, SpaceShipType};
use crate::network::{session::AsyncSession, SESSION_MANAGER};
use crate::npc::npc_service;
use sysinfo::System;

pub struct CommandService;

impl CommandService {
    pub async fn check(session: &mut AsyncSession, text: &str) -> anyhow::Result<bool> {
        let is_admin = if let Some(player) = session.get_player() {
            player.is_admin
        } else {
            return Ok(false);
        };
        println!("is_admin: {}", is_admin);
        println!("chat text: {}", text);

        if is_admin {
            if text == "menu" {
                let online_players = SESSION_MANAGER.get_online_count().await;
                let online_sessions = online_players;
                let threads = System::new_all().cpus().len();

                let menu_text = format!(
                    "Admin Menu\nTotal players: {}\nTotal sessions: {}\nTotal threads: {}",
                    online_players, online_sessions, threads
                );

                npc_service::npc_service::create_menu(
                    session,
                    const_npc::CON_MEO,
                    &menu_text,
                    vec![
                        "Ngọc rồng",
                        "Đệ tử",
                        "Bảo trì",
                        "Tìm kiếm\nngười chơi",
                        "Boss",
                        "Thông tin Server",
                        "Đóng",
                    ],
                    MenuId::Admin,
                )
                .await?;
                return Ok(true);
            } else if text.starts_with("m ") {
                let map_id_str = text.strip_prefix("m ").unwrap_or("");
                if let Ok(map_id) = map_id_str.trim().parse::<i32>() {
                    if let Some(mut player) = session.take_player() {
                        let change_map_service = ChangeMapService::new();
                        if let Some(zone) = change_map_service.get_available_zone(map_id).await {
                            change_map_service
                                .change_map_to_zone_async(
                                    &mut player,
                                    &zone,
                                    -1,
                                    -1,
                                    SpaceShipType::TeleportYardrat,
                                    session,
                                )
                                .await?;
                        }
                        session.set_player(player);
                    }
                }
                return Ok(true);
            }
        }

        Ok(false)
    }
}
