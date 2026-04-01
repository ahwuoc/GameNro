use crate::network::message::Message;
use crate::network::session::SessionArc;
use crate::player::player::Player;
use crate::player::player_actor::PlayerHandle;
use crate::player::command::CommandService;
use crate::services::player_tnsm_services::{self, TypeTNSM};
use crate::services::{player_info_service, ServiceHandles};

pub struct MiscHandler;

impl MiscHandler {
    pub async fn handle_chat(player: &mut Player, session: &SessionArc, text: String) {
        if let Ok(false) = CommandService::check(player, session, &text).await {
            let _ = ServiceHandles::chat(session, player.id, player.map_id, player.zone_id, &text);
        }
    }

    pub async fn handle_increase_point(player: &mut Player, type_increment: u8, point: i16) -> (i32, i32) {
        let old_task = (
            crate::services::task_utils::TaskUtils::get_id_task(player),
            crate::services::task_utils::TaskUtils::get_task_index(player),
        );
        
        player.n_point.increase_point(type_increment, point);
        player.n_point.cal_point();
        player_info_service::send_point_info_sync(player);

        let _ = crate::services::task_service::TaskService::check_done_task_scripts(player, "2");
        let _ = crate::services::task_service::TaskService::check_done_task_scripts(player, "1");
        
        old_task
    }

    pub fn handle_add_tnsm(player: &mut Player, type_tnsm: TypeTNSM, param: i64, is_ori: bool) {
        player_tnsm_services::tiemnang_sucmanh_add(player, type_tnsm, param, is_ori);
    }

    pub fn handle_create_menu(
        player: &mut Player,
        npc_id: i16,
        npc_say: String,
        menu_options: Vec<String>,
        state: crate::constant::menu_enum::MenuId,
    ) {
        let options: Vec<&str> = menu_options.iter().map(|s| s.as_str()).collect();
        let _ = crate::npc::npc_service::npc_service::create_menu_player(
            player,
            npc_id,
            &npc_say,
            options,
            state,
        );
    }

    pub fn handle_send_info_to(player: &Player, target_handle: PlayerHandle) {
        let _ = ServiceHandles::send_player_info_to_handle(&target_handle, player);
    }

    pub fn handle_send_info_to_all(player: &Player, targets: Vec<PlayerHandle>) {
        for target_handle in targets {
            let _ = ServiceHandles::send_player_info_to_handle(&target_handle, player);
        }
    }

    pub fn handle_call_training_boss(player: &mut Player, boss_id: String, is_thachdau: bool) {
        if let Err(e) = crate::map::services::training_services::call_boss_by_id(
            player,
            &boss_id,
            is_thachdau,
        ) {
            tracing::error!("Error calling training boss for player {}: {:?}", player.id, e);
        }
    }

    pub async fn handle_radar_action(
        player: &mut Player,
        action: i8,
        msg: &mut Message,
    ) -> anyhow::Result<()> {
        match action {
            0 => {
                crate::services::radar_service::RadarService::send_radar(player, &player.radar_cards)?;
            }
            1 => {
                let card_id = msg.read_short()?;
                let any_other_used = player
                    .radar_cards
                    .iter()
                    .any(|c| c.id != card_id && c.used == 1);
                let mut new_used = 0;
                let mut updated = false;

                if let Some(card) = player.radar_cards.iter_mut().find(|c| c.id == card_id) {
                    if card.level == 0 {
                        return Ok(());
                    }

                    if card.used == 0 {
                        if any_other_used {
                            ServiceHandles::send_message_alert(player, "Số thẻ sử dụng đã đạt tối đa")?;
                            return Ok(());
                        }
                        card.used = 1;
                    } else {
                        card.used = 0;
                    }
                    new_used = card.used;
                    updated = true;
                }

                if updated {
                    crate::services::radar_service::RadarService::send_radar_1(player, card_id, new_used)?;
                    player.n_point.cal_point();
                    player_info_service::send_point_info_sync(player)?;
                }
            }
            _ => {}
        }
        Ok(())
    }

    pub async fn handle_combine_open_tab(
        player: &mut Player,
        session: &SessionArc,
        type_combine: crate::combine::combine_type::CombineType,
        npc_id: i16,
    ) {
        let _ = crate::combine::combine_service::handle_open_tab_actor(
            player,
            session,
            type_combine,
            npc_id,
        );
    }

    pub async fn handle_combine_show_info(
        player: &mut Player,
        session: &SessionArc,
        index: Vec<i16>,
    ) {
        let _ = crate::combine::combine_service::handle_show_info_actor(player, session, index).await;
    }

    pub async fn handle_combine_confirm(player: &mut Player, session: &SessionArc) {
        let _ = crate::combine::combine_service::handle_confirm_actor(player, session).await;
    }
}
