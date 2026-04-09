use crate::player::player::Player;
use crate::player::player_actor::pet::message::PetMessage;
use crate::player::player_actor::pet::PetHandle;
use crate::services::player_info_service;

pub struct PetHandler;

impl PetHandler {
    pub async fn handle_show_info_pet(player: &Player, pet_handle: &Option<PetHandle>) {
        if let Some(ref pet_handle) = pet_handle {
            let (tx, rx) = tokio::sync::oneshot::channel();
            if let Ok(_) = pet_handle.send(PetMessage::GetSnapshot(tx)).await {
                let player_clone = player.clone();
                tokio::spawn(async move {
                    if let Ok(pet_snapshot) = rx.await {
                        let _ = player_info_service::send_info_pet(&player_clone, &pet_snapshot);
                    }
                });
            }
        }
    }

    pub fn handle_pet_forward(player: &Player, pet_handle: &Option<PetHandle>, pet_msg: PetMessage) {
        if player.fusion.type_fusion != 0 {
            if matches!(pet_msg, PetMessage::ChangeStatus(_)) {
                tracing::info!(
                    "[PET] Blocked status change while player {} is fused",
                    player.id
                );
                return;
            }
        }
        
        if let Some(handle) = pet_handle {
            handle.send_forget(pet_msg);
        }
    }
    pub fn handle_update_pet_ui(player: &Player, pet_snapshot: &crate::player::player_actor::pet::Pet, chat: Option<String>) {
        let _ = player_info_service::send_info_pet(player, pet_snapshot);
        if let Some(text) = chat {
            let _ = crate::services::ServiceHandles::send_message_chat_just_for_me(
                player,
                &pet_snapshot.player,
                &text,
            );
        }
    }
}
