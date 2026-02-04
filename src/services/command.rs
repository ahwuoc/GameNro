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
            }
            if text == "menu" {
                let online_players = SESSION_MANAGER.get_online_count();
                let online_sessions = online_players;
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
                            Some(session),
                        )
                        .await?;
                    }
                }
                return Ok(true);
            }
        }

        // Pet commands for everyone
        match text {
            "follow" | "di-theo" => {
                if let Some(pet_id) = player.pet_id {
                    if let Some(handle) = PLAYER_MANAGER.get(pet_id) {
                        let _ = handle
                            .send(PlayerMessage::Pet(PetMessage::ChangeStatus(
                                PetStatus::Follow,
                            )))
                            .await;
                        if let Some(pet_snapshot) = handle.get_snapshot().await {
                            if pet_snapshot.map_id != player.map_id
                                || pet_snapshot.zone_id != player.zone_id
                            {
                                let _ = handle
                                    .tx
                                    .send(PlayerMessage::ChangeMap {
                                        map_id: player.map_id,
                                        zone_id: player.zone_id,
                                        x: player.location.x,
                                        y: player.location.y,
                                        space_type: SpaceShipType::None,
                                    })
                                    .await;
                            }
                        }

                        ServiceHandles::send_message_alert(
                            player,
                            "Đệ tử: Sư phụ đi đâu con theo đó!",
                        )?;
                        return Ok(true);
                    }
                }
            }
            "attack" | "tan-cong" => {
                if let Some(pet_id) = player.pet_id {
                    if let Some(handle) = PLAYER_MANAGER.get(pet_id) {
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
                if let Some(pet_id) = player.pet_id {
                    if let Some(handle) = PLAYER_MANAGER.get(pet_id) {
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
                if let Some(pet_id) = player.pet_id {
                    if let Some(handle) = PLAYER_MANAGER.get(pet_id) {
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
                        if player.fusion.type_fusion == 0 {
                            let _ = handle.send(PlayerMessage::Fusion(4)).await;
                        } else {
                            let _ = handle.send(PlayerMessage::Unfusion).await;
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
