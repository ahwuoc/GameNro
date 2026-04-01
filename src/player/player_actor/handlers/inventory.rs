use crate::constant::const_item::{ITEM_DUI_GA_NUONG, ITEM_EM_BE};
use crate::item::item_controller::ItemController;
use crate::item::use_item_service::UseItemResult;
use crate::item::{InventoryService, Item};
use crate::map::zone_manager::ZONE_MANAGER;
use crate::map::ItemMapService;
use crate::services::ServiceHandles;
use crate::network::session::SessionArc;
use crate::player::player::Player;
use crate::player::player_actor::pet::message::PetMessage;
use crate::player::player_actor::pet::PetHandle;
use crate::services::task_service::TaskService;
use crate::services::{player_info_service};

pub struct InventoryHandler;

impl InventoryHandler {
    pub async fn handle_item_action(
        session: &SessionArc,
        player: &mut Player,
        pet_handle: &Option<PetHandle>,
        type_action: crate::item::type_item_inventory::TypeItemAction,
        where_item: i8,
        index: i8,
    ) {
        if let Ok(Some(use_result)) = ItemController::handle_item_action_actor(
            session,
            player,
            type_action,
            where_item,
            index,
        )
        .await
        {
            if let UseItemResult::RecoveredHpMp { hp_ki, stamina, .. } = use_result {
                if let Some(ref pet_handle) = pet_handle {
                    let _ = pet_handle
                        .send(PetMessage::HealPet {
                            hp: hp_ki,
                            mp: hp_ki,
                            stamina,
                        })
                        .await;
                }
            }
        }
    }

    pub async fn handle_get_item(
        session: &SessionArc,
        player: &mut Player,
        type_item_inventory: crate::item::type_item_inventory::TypeItemInventory,
        index: i8,
    ) {
        let _ = ItemController::handle_get_item_actor(session, player, type_item_inventory, index).await;
    }

    pub async fn handle_pick_item(
        player: &mut Player,
        session: &SessionArc,
        item_map_id: i32,
    ) {
        let zone_handle = match ZONE_MANAGER.get_zone(player.map_id, player.zone_id) {
            Some(zh) => zh,
            None => return,
        };
        
        let item_map_peek = match zone_handle.get_item(item_map_id).await {
            Ok(Some(it)) => it,
            _ => return,
        };
        
        if !item_map_peek.can_pickup(player.id, Some(player.clan_id)) {
            let _ = ServiceHandles::send_thong_bao_to_player(
                player,
                "Không thể nhặt vật phẩm của người khác",
            );
            return;
        }

        match zone_handle.remove_item(item_map_id).await {
            Ok(Some(item_map)) => {
                let item_id = item_map.get_item_id();
                let item_type = item_map.get_item_type();

                // Special item: Đùi gà nướng
                if item_id == ITEM_DUI_GA_NUONG && matches!(player.map_id, 21 | 22 | 23) {
                    Self::handle_special_chicken(player, session, &zone_handle, item_map_id).await;
                    return;
                }
                
                // Special item: Em bé
                if item_id == ITEM_EM_BE && matches!(player.map_id, 42 | 43 | 44) {
                    Self::handle_special_baby(player, session, &zone_handle, item_map_id, &item_id).await;
                    return;
                }
                
                // Normal item pickup
                Self::handle_normal_pickup(player, session, &zone_handle, item_map, item_map_id).await;
            }
            _ => {}
        }
    }

    async fn handle_special_chicken(
        player: &mut Player,
        session: &SessionArc,
        zone_handle: &crate::map::models::zone::ZoneHandle,
        item_map_id: i32,
    ) {
        player.n_point.set_hp(player.n_point.hp_max);
        player.n_point.set_mp(player.n_point.mp_max);
        player_info_service::send_point_info_sync(player);
        player_info_service::send_info_hp_mp_money(player);

        let mut msg = crate::network::message::Message::new(-20);
        let _ = msg.write_short(item_map_id as i16);
        let _ = msg.write_utf("Bạn vừa ăn đùi gà nướng, HP và KI đã được hồi phục hoàn toàn");
        session.transmit(msg);

        let pickup_msg = ItemMapService::build_pickup_notification_message(item_map_id, player.id);
        let _ = ServiceHandles::send_to_other_in_zone(zone_handle, pickup_msg, player.id);
        
        let disappear_msg = ItemMapService::build_item_disappear_message(item_map_id);
        let _ = ServiceHandles::send_to_all_in_zone(zone_handle, disappear_msg);
    }

    async fn handle_special_baby(
        player: &mut Player,
        session: &SessionArc,
        zone_handle: &crate::map::models::zone::ZoneHandle,
        item_map_id: i32,
        item_id: &i16,
    ) {
        let mut msg = crate::network::message::Message::new(-20);
        let _ = msg.write_short(item_map_id as i16);
        let _ = msg.write_utf("Wow, một em bé dễ thương!");
        session.transmit(msg);

        TaskService::check_done_task_pick_item(player, &item_id.to_string());

        let pickup_msg = ItemMapService::build_pickup_notification_message(item_map_id, player.id);
        let _ = ServiceHandles::send_to_other_in_zone(zone_handle, pickup_msg, player.id);
        
        let disappear_msg = ItemMapService::build_item_disappear_message(item_map_id);
        let _ = ServiceHandles::send_to_all_in_zone(zone_handle, disappear_msg);
    }

    async fn handle_normal_pickup(
        player: &mut Player,
        session: &SessionArc,
        zone_handle: &crate::map::models::zone::ZoneHandle,
        item_map: crate::map::models::item_map::ItemMap,
        item_map_id: i32,
    ) {
        let item_type = item_map.get_item_type();
        let quantity = item_map.quantity;
        
        if let Some(template) = item_map.item_template.clone() {
            let mut item = Item::with_template(template, quantity);
            item.item_options = item_map.options.clone();
            let item_template_id = item.template.as_ref().map(|t| t.id as i32).unwrap_or(0);

            match InventoryService::add_item_bag(player, item) {
                Ok(_) => {
                    let msg = ItemMapService::build_pickup_notification_message(item_map_id, player.id);
                    ServiceHandles::send_to_other_in_zone(zone_handle, msg, player.id);

                    let disappearing_msg = ItemMapService::build_item_disappear_message(item_map_id);
                    ServiceHandles::send_to_all_in_zone(zone_handle, disappearing_msg);

                    // Show notification for equipment
                    if item_type >= 0 && item_type < 5 {
                        let mut msg = crate::network::message::Message::new(-20);
                        let _ = msg.write_short(item_map_id as i16);
                        let _ = msg.write_utf(&format!(
                            "Bạn nhận được {}",
                            item_map.item_template.as_ref().map(|t| t.name.clone()).unwrap_or_default()
                        ));
                        session.transmit(msg);
                    } else if matches!(item_type, 9 | 10 | 34) && quantity > 30000 {
                        let mut msg = crate::network::message::Message::new(-20);
                        let _ = msg.write_short(item_map_id as i16);
                        let _ = msg.write_utf(&format!(
                            "Bạn vừa nhận được {} {}",
                            quantity,
                            item_map.item_template.as_ref().map(|t| t.name.clone()).unwrap_or_default()
                        ));
                        session.transmit(msg);
                    }

                    TaskService::check_done_task_pick_item(player, &item_template_id.to_string());
                }
                Err(_) => {
                    let _ = zone_handle.add_item(item_map).await;
                }
            }
        }
    }

    pub async fn handle_pet_ask_pea(
        player: &mut Player,
        pet_handle: &Option<PetHandle>,
        _pet_id: u64,
    ) {
        if let Some(index) = player
            .inventory
            .items_bag
            .iter()
            .position(|it| it.is_not_null_item() && it.get_type() == 6)
        {
            if let Some(recovery) =
                crate::item::use_item_service::UseItemService::eat_pea(player, index)
            {
                let _ = player_info_service::send_point_info_sync(player);
                let _ = player_info_service::send_current_stamina(player);
                let _ = InventoryService::send_item_bag(player);
                
                if let Some(ref pet_handle) = pet_handle {
                    let _ = pet_handle
                        .send(PetMessage::HealPet {
                            hp: recovery.hp_ki,
                            mp: recovery.hp_ki,
                            stamina: recovery.stamina,
                        })
                        .await;
                }
            }
        }
    }
}
