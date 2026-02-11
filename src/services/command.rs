use crate::boss::manager::BossManager;
use crate::constant::const_npc;
use crate::constant::menu_enum::MenuId;
use crate::item::InventoryService;
use crate::item::ItemService;
use crate::map::change_map_service::ChangeMapService;
use crate::map::services::change_map_models::SpaceShipType;
use crate::network::session::SessionArc;
use crate::network::{session::AsyncSession, SESSION_MANAGER};
use crate::npc::npc_service;
use crate::player::player_actor::message::PlayerMessage;
use crate::player::player_actor::pet::message::PetMessage;
use crate::player::player_actor::pet::PetService;
use crate::player::player_actor::pet::PetStatus;
use crate::player::player_manager::PLAYER_MANAGER;
use crate::player::Player;
use crate::services::skill_service;
use crate::services::ServiceHandles;

pub struct CommandService;

impl CommandService {
    pub async fn check(
        player: &mut Player,
        session: &SessionArc,
        text: &str,
    ) -> anyhow::Result<bool> {
        let is_admin = player.is_admin;
        if is_admin {
            if text == "b" {
                let _ = BossManager::show_list_boss(player.id, session.get_version().await).await;
                return Ok(true);
            }
            if text == "pet" {
                if player.pet_id.is_none() {
                    let handle = PetService::spawn_pet(player).await?;
                    if let Some(player_handle) = PLAYER_MANAGER.get(player.id) {
                        let _ = player_handle
                            .send(PlayerMessage::SetPetHandle(handle))
                            .await;
                    }
                    ServiceHandles::send_message_alert(player, "Đã gọi đệ tử!")?;
                } else {
                    ServiceHandles::send_message_alert(player, "Bạn đã có đệ tử rồi!")?;
                }
                return Ok(true);
            } else if text == "boss" || text.starts_with("sb ") {
                let boss_id = if text == "boss" {
                    "boss_kuku"
                } else {
                    text.strip_prefix("sb ").unwrap_or("").trim()
                };

                tracing::info!(
                    "COMMAND: Spawning boss '{}' at map {}, zone {}, x {}, y {}",
                    boss_id,
                    player.map_id,
                    player.zone_id,
                    player.location.x,
                    player.location.y
                );
                match BossManager::spawn_boss(
                    boss_id,
                    player.map_id,
                    player.zone_id,
                    player.location.x,
                    player.location.y,
                    None,
                    -1,
                    Vec::new(),
                )
                .await
                {
                    Ok(_) => {
                        tracing::info!("COMMAND: BossManager::spawn_boss successful");
                        ServiceHandles::send_message_alert(
                            player,
                            &format!("Đã gọi boss {} thành công!", boss_id),
                        )?;
                    }
                    Err(e) => {
                        tracing::error!("COMMAND: BossManager::spawn_boss failed: {:?}", e);
                        ServiceHandles::send_message_alert(
                            player,
                            &format!("Lỗi gọi boss {}: {:?}", boss_id, e),
                        )?;
                    }
                }
                return Ok(true);
            } else if text == "menu" {
                let online_players = SESSION_MANAGER.get_online_count();
                let online_sessions = online_players;
                let threads = std::thread::available_parallelism()
                    .map(|n| n.get())
                    .unwrap_or(1);

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
            } else if text == "lb" {
                let templates = crate::templates::boss_template_manager::get_all();
                let list = templates
                    .iter()
                    .map(|t| format!("{} ({})", t.id, t.r#type))
                    .collect::<Vec<_>>()
                    .join(", ");
                tracing::info!("COMMAND: Loaded Boss Templates: {}", list);
                ServiceHandles::send_message_alert(
                    player,
                    &format!(
                        "Xem danh sách boss tại console! Số lượng: {}",
                        templates.len()
                    ),
                )?;
                return Ok(true);
            } else if text.starts_with("m ") {
                let map_id_str = text.strip_prefix("m ").unwrap_or("");
                if let Ok(map_id) = map_id_str.trim().parse::<i32>() {
                    if let Some(zone) = ChangeMapService::get_available_zone(map_id) {
                        ChangeMapService::change_map_to_zone(
                            player,
                            &zone,
                            -1,
                            5,
                            SpaceShipType::TeleportYardrat,
                            Some(session),
                        )
                        .await?;
                    }
                }
                return Ok(true);
            } else if text.starts_with("sm ") {
                let amount_str = text.strip_prefix("sm ").unwrap_or("");
                if let Ok(amount) = amount_str.trim().parse::<i64>() {
                    if let Some(handle) = PLAYER_MANAGER.get(player.id) {
                        let _ = handle
                            .send(PlayerMessage::AddTNSM {
                                type_tnsm: 2,
                                param: amount,
                                is_ori: true,
                            })
                            .await;
                    }
                }
                return Ok(true);
            }
        }

        match text {
            "follow" | "di-theo" => {
                if player.pet_id.is_some() {
                    if let Some(handle) = PLAYER_MANAGER.get(player.id) {
                        let _ = handle
                            .send(PlayerMessage::Pet(PetMessage::ChangeStatus(
                                PetStatus::Follow,
                            )))
                            .await;
                        ServiceHandles::send_message_alert(
                            player,
                            "Đệ tử: Sư phụ đi đâu con theo đó!",
                        )?;
                        return Ok(true);
                    }
                }
            }
            "attack" | "tan-cong" => {
                if player.pet_id.is_some() {
                    if let Some(handle) = PLAYER_MANAGER.get(player.id) {
                        let _ = handle
                            .send(PlayerMessage::Pet(PetMessage::ChangeStatus(
                                PetStatus::Attack,
                            )))
                            .await;
                        ServiceHandles::send_message_alert(
                            player,
                            "Đệ tử: Đại ca đợi đó, con xử nó cho!",
                        )?;
                        return Ok(true);
                    }
                }
            }
            "protect" | "bao-ve" => {
                if player.pet_id.is_some() {
                    if let Some(handle) = PLAYER_MANAGER.get(player.id) {
                        let _ = handle
                            .send(PlayerMessage::Pet(PetMessage::ChangeStatus(
                                PetStatus::Protect,
                            )))
                            .await;
                        ServiceHandles::send_message_alert(
                            player,
                            "Đệ tử: Con sẽ không để ai đụng vào sư phụ!",
                        )?;
                        return Ok(true);
                    }
                }
            }
            "ve-nha" | "gohome" => {
                if player.pet_id.is_some() {
                    if let Some(handle) = PLAYER_MANAGER.get(player.id) {
                        let _ = handle
                            .send(PlayerMessage::Pet(PetMessage::ChangeStatus(
                                PetStatus::GoHome,
                            )))
                            .await;
                        ServiceHandles::send_message_alert(
                            player,
                            "Đệ tử: Con về nhà tắm rửa đây!",
                        )?;
                        return Ok(true);
                    }
                }
            }
            "hop-the" | "fusion" => {
                if player.pet_id.is_some() {
                    if let Some(handle) = PLAYER_MANAGER.get(player.id) {
                        if !player.fusion.is_timed_fusion() {
                            let _ = handle
                                .send(PlayerMessage::Fusion {
                                    type_fusion: 4,
                                    template_id: 1,
                                })
                                .await;
                        }
                        return Ok(true);
                    }
                }
            }
            _ => {}
        }

        Ok(false)
    }
}
