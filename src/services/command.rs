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
use crate::services::player_tnsm_services::TypeTNSM;
use crate::services::skill_service;
use crate::services::task_service::TaskService;
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
            } else if text == "pet" {
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
            } else if text.starts_with("nv") {
                let parts: Vec<&str> = text.split("_").collect();
                if parts.len() == 3 {
                    let main = parts[1].parse().unwrap_or(0);
                    let sub = parts[2].parse().unwrap_or(0);
                    TaskService::force_set_task(player, main, sub)?;
                    ServiceHandles::send_thong_bao_to_player(player, "Đã chuyển nhiệm vụ!")?;
                } else if parts.len() == 2 {
                    let main = parts[1].parse().unwrap_or(0);
                    TaskService::force_set_task(player, main, 0)?;
                    ServiceHandles::send_thong_bao_to_player(player, "Đã chuyển nhiệm vụ!")?;
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
                    None,
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
                                type_tnsm: TypeTNSM::All,
                                param: amount,
                                is_ori: true,
                            })
                            .await;
                    }
                }
                return Ok(true);
            }
            // ── DHVT Test Commands ──
            else if text.starts_with("dhvt") {
                let sub = text.strip_prefix("dhvt").unwrap_or("").trim();
                match sub {
                    "info" | "i" => {
                        // Hiện thông tin DHVT hiện tại
                        let dhvt = crate::matches::dhvt::manager::get_dhvt_handle();
                        let info = dhvt.get_info(player.id as i64).await;
                        let msg = format!(
                            "[DHVT Info]\n\
                             Tournament: {} ({})\n\
                             Can register: {}\n\
                             Round: {}\n\
                             Registered: {} players\n\
                             You registered: {}\n\
                             You in wait list: {}\n\
                             Hour: {}",
                            info.cup_name,
                            info.tournament.get_name(),
                            info.can_reg,
                            info.round,
                            info.reg_count,
                            info.is_registered,
                            info.is_in_wait_list,
                            info.hour,
                        );
                        ServiceHandles::send_message_alert(player, &msg)?;
                    }
                    "reg" | "r" => {
                        // Force đăng ký (miễn phí)
                        let dhvt = crate::matches::dhvt::manager::get_dhvt_handle();
                        dhvt.register(player.id as i64);
                        ServiceHandles::send_message_alert(
                            player,
                            "DHVT: Đã đăng ký thành công (admin bypass)",
                        )?;
                    }
                    "unreg" | "u" => {
                        // Hủy đăng ký
                        let dhvt = crate::matches::dhvt::manager::get_dhvt_handle();
                        dhvt.unregister(player.id as i64);
                        ServiceHandles::send_message_alert(player, "DHVT: Đã hủy đăng ký")?;
                    }
                    "start" | "s" => {
                        // Force bắt đầu ghép cặp (gửi Tick liên tục)
                        let dhvt = crate::matches::dhvt::manager::get_dhvt_handle();
                        dhvt.force_start();
                        ServiceHandles::send_message_alert(
                            player,
                            "DHVT: Force start - ghép cặp ngay!",
                        )?;
                    }
                    "tp" => {
                        // Teleport đến map 52 (phòng chờ)
                        if let Some(zone) = ChangeMapService::get_available_zone(52) {
                            ChangeMapService::change_map_to_zone(
                                player,
                                &zone,
                                -1,
                                336,
                                SpaceShipType::TeleportYardrat,
                                Some(session),
                            )
                            .await?;
                        }
                        ServiceHandles::send_message_alert(player, "DHVT: Teleport đến map 52")?;
                    }
                    "tp129" => {
                        // Teleport đến map 129 (DHVT23)
                        if let Some(zone) = ChangeMapService::get_available_zone(129) {
                            ChangeMapService::change_map_to_zone(
                                player,
                                &zone,
                                -1,
                                360,
                                SpaceShipType::TeleportYardrat,
                                Some(session),
                            )
                            .await?;
                        }
                        ServiceHandles::send_message_alert(player, "DHVT: Teleport đến map 129")?;
                    }
                    "check" | "c" => {
                        // Check player có đang trong list_wait/list_reg
                        let dhvt = crate::matches::dhvt::manager::get_dhvt_handle();
                        let in_list = dhvt.check_player(player.id as i64).await;
                        let is_reg = dhvt.is_registered(player.id as i64).await;
                        ServiceHandles::send_message_alert(
                            player,
                            &format!(
                                "DHVT Check:\nRegistered: {}\nIn wait/reg list: {}",
                                is_reg, in_list
                            ),
                        )?;
                    }
                    _ => {
                        ServiceHandles::send_message_alert(
                            player,
                            "DHVT Commands:\n\
                             dhvt info - Thông tin\n\
                             dhvt reg - Đăng ký (free)\n\
                             dhvt unreg - Hủy\n\
                             dhvt start - Force start\n\
                             dhvt tp - Đến map 52\n\
                             dhvt tp129 - Đến map 129\n\
                             dhvt check - Check status",
                        )?;
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
