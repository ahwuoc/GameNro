use crate::combine::combine_service;
use crate::item::{item_controller, type_item_inventory};
use crate::map::zone_manager::ZONE_MANAGER;
use crate::network::message::Message;
use crate::network::session::SessionArc;
use crate::npc::npc_service;
use crate::player::player_actor::PlayerMessage;
use crate::services::{self, ServiceHandles};
use crate::shop::shop_services::shop_service;
use anyhow::Result;
use tracing::{debug, error, warn};

pub struct PlayerHandler;

impl PlayerHandler {
    pub async fn get_item(session: &SessionArc, mut msg: Message) -> Result<()> {
        let type_byte = msg.read_byte()?;
        let type_inventory = type_item_inventory::TypeItemInventory::try_from(type_byte)?;
        let index = msg.read_byte()?;
        item_controller::ItemController::get_item(session, type_inventory, index).await?;
        Ok(())
    }

    pub async fn do_item(session: &SessionArc, mut msg: Message) -> Result<()> {
        let type_byte = msg.read_byte()?;
        let type_action = type_item_inventory::TypeItemAction::try_from(type_byte)?;
        let where_item = msg.read_byte()?;
        let index = msg.read_byte()?;
        item_controller::ItemController::handle_item_action(
            session,
            type_action,
            where_item,
            index,
        )
        .await?;
        Ok(())
    }

    pub async fn buy_item(session: &SessionArc, mut msg: Message) -> Result<()> {
        let type_shop = msg.read_byte()?;
        let temp_id = msg.read_short()?;
        if let Err(e) = shop_service::take_item_shop(session, type_shop, temp_id).await {
            error!("Shop Error: {:?}", e);
        }
        Ok(())
    }

    pub async fn npc_select(session: &SessionArc, mut msg: Message) -> Result<()> {
        let npc_id = msg.read_short()?;
        let select = msg.read_byte()?;
        npc_service::npc_service::handle_menu_confirm(session, npc_id, select).await?;
        Ok(())
    }

    pub async fn npc_menu(session: &SessionArc, mut msg: Message) -> Result<()> {
        let npc_id = msg.read_short()?;
        npc_service::npc_service::open_menu_controller(session, npc_id).await?;
        Ok(())
    }

    pub async fn dau_than_confirm(session: &SessionArc, mut msg: Message) -> Result<()> {
        let _ = msg.read_byte()?;
        let select = msg.read_byte()?;
        npc_service::npc_service::handle_menu_confirm(session, 4, select).await?;
        Ok(())
    }

    pub async fn chat(session: &SessionArc, mut msg: Message) -> Result<()> {
        let text = msg.read_utf()?;
        if let Some(handle) = session.get_player_handle().await {
            handle.send_forget(PlayerMessage::Chat { text });
        }
        Ok(())
    }

    pub async fn get_player_menu(session: &SessionArc, mut msg: Message) -> Result<()> {
        let target_id = msg.read_int()?;
        if let Some(snapshot) = session.get_player_snapshot().await {
            if let Some(zone) = ZONE_MANAGER.get_zone(snapshot.map_id, snapshot.zone_id) {
                if let Some(target_handle) = zone.get_player(target_id as u64).await? {
                    if let Some(target_snapshot) = target_handle.get_snapshot().await {
                        ServiceHandles::send_player_menu(&snapshot, &target_snapshot)?;
                    }
                }
            }
        }
        Ok(())
    }

    pub async fn show_info_pet(session: &SessionArc) -> Result<()> {
        if let Some(handle) = session.get_player_handle().await {
            handle.send_forget(PlayerMessage::ShowInfoPet);
        }
        Ok(())
    }

    pub async fn change_type_pk(session: &SessionArc, mut msg: Message) -> Result<()> {
        let type_byte = msg.read_byte()?;
        match type_byte {
            16 => {
                let type_increment = msg.read_byte()?;
                let point = msg.read_short()?;
                if let Some(handle) = session.get_player_handle().await {
                    handle.send_forget(PlayerMessage::IncreasePoint {
                        type_increment: type_increment as u8,
                        point,
                    });
                }
            }
            64 => {}
            _ => {
                warn!("Unknown type for -30 command: {}", type_byte);
            }
        }
        Ok(())
    }

    pub async fn magic_tree(session: &SessionArc, mut msg: Message) -> Result<()> {
        let action = msg.read_byte()?;
        debug!("MagicTree action: {}", action);
        match action {
            1 | 2 => {
                if let Some(handle) = session.get_player_handle().await {
                    handle.send_forget(PlayerMessage::MagicTree(
                        crate::player::player_actor::MagicTreeMsg::OpenOrLoad(action as u8),
                    ));
                }
            }
            _ => {}
        }
        Ok(())
    }

    pub async fn radar(session: &SessionArc, mut msg: Message) -> Result<()> {
        let action = msg.read_byte()?;
        if let Some(handle) = session.get_player_handle().await {
            handle.send_forget(PlayerMessage::RadarAction(action, msg));
        }
        Ok(())
    }

    pub async fn pick_item(session: &SessionArc, mut msg: Message) -> Result<()> {
        let item_map_id = msg.read_short()? as i32;
        if let Some(handle) = session.get_player_handle().await {
            handle.send_forget(PlayerMessage::PickItem { item_map_id });
        }
        Ok(())
    }

    pub async fn combine_info(session: &SessionArc, mut msg: Message) -> Result<()> {
        let _ = msg.read_byte()?;
        let len = msg.read_byte()?;
        let mut index_item = Vec::new();
        for _ in 0..len {
            index_item.push(msg.read_byte()? as i16);
        }
        combine_service::show_info_combine(session, index_item).await?;
        Ok(())
    }

    pub async fn skill_shortcut_update(session: &SessionArc, mut msg: Message) -> Result<()> {
        let mut shortcuts = Vec::new();
        for _ in 0..10 {
            shortcuts.push(msg.read_byte()?);
        }
        if let Some(handle) = session.get_player_handle().await {
            handle.send_forget(PlayerMessage::UpdateSkillShortcuts { shortcuts });
        }
        Ok(())
    }

    pub async fn intrinsic_menu(session: &SessionArc) -> Result<()> {
        if let Some(_handle) = session.get_player_handle().await {
            services::IntrinsicService::show_menu(session).await?;
        }
        Ok(())
    }
}
