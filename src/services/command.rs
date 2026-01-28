use crate::constant::const_npc;
use crate::constant::menu_enum::MenuId;
use crate::item::InventoryService;
use crate::item::ItemService;
use crate::map::change_map_service::{ChangeMapService, SpaceShipType};
use crate::network::session::SessionArc;
use crate::network::{session::AsyncSession, SESSION_MANAGER};
use crate::npc::npc_service;
use crate::player::Player;
use crate::services::skill_service;
use crate::services::ServiceHandles;
use sysinfo::System;

pub struct CommandService;

impl CommandService {
    pub async fn check(
        player: &mut Player,
        session: &SessionArc,
        text: &str,
    ) -> anyhow::Result<bool> {
        let is_admin = player.is_admin;

        if is_admin {
            if text == "menu" {
                let online_players = SESSION_MANAGER.get_online_count();
                let online_sessions = online_players; // Simplified for now
                let threads = System::new_all().cpus().len();

                let menu_text = format!(
                    "Admin Menu\nTotal players: {}\nTotal sessions: {}\nTotal threads: {}",
                    online_players, online_sessions, threads
                );

                npc_service::npc_service::create_menu_player(
                    player,
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
                )?;
                return Ok(true);
            } else if text == "r" {
                skill_service::send_release_cooldown(player);
                ServiceHandles::send_message_alert(player, "Đã reset cooldown tất cả kỹ năng!")?;
                return Ok(true);
            } else if text == "full_skill" {
                skill_service::learn_full_skill(player).await?;
                ServiceHandles::send_message_alert(player, "Đã học toàn bộ kỹ năng hành tinh!")?;
                return Ok(true);
            } else if text.starts_with("i_") {
                let item_id = text.strip_prefix("i_").unwrap_or("");
                if let Ok(item_id) = item_id.trim().parse::<i16>() {
                    let item = match ItemService::create_new_item(item_id) {
                        Some(mut it) => it,
                        None => {
                            ServiceHandles::send_message_alert(player, "Item not found")?;
                            return Ok(true);
                        }
                    };
                    InventoryService::add_item_bag(player, item)?;
                }
                return Ok(true);
            } else if text.starts_with("m ") {
                let map_id_str = text.strip_prefix("m ").unwrap_or("");
                if let Ok(map_id) = map_id_str.trim().parse::<i32>() {
                    if let Some(zone) = ChangeMapService::get_available_zone(map_id) {
                        ChangeMapService::change_map_to_zone(
                            player,
                            &zone,
                            -1,
                            -1,
                            SpaceShipType::TeleportYardrat,
                            session,
                        )?;
                    }
                }
                return Ok(true);
            }
        }

        Ok(false)
    }
}
