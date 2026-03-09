use crate::constant::const_npc::NpcId;
use crate::constant::menu_enum::MenuId;
use crate::constant::task_type::TaskType;
use crate::network::message::Message;
use crate::network::session::{AsyncSession, SessionArc};
use crate::npc::handlers::bahatmit::BahatmitHandler;
use crate::npc::handlers::conmeo::ConMeoHandler;
use crate::npc::handlers::dau_than::DauThanHandler;
use crate::npc::handlers::dynamic_shop_handler::DynamicShopHandler;
use crate::npc::handlers::ong_gohan::NpcHomeHandler;
use crate::npc::handlers::quy_lao_kame::QuyLaoKameHandler;
use crate::npc::handlers::ruong_do::RuongDoHandler;
use crate::npc::handlers::santa::SantaHandler;
use crate::npc::handlers::{NpcContext, NpcHandler};
use crate::npc::{BaseMenu, RtNpc};
use crate::player::player_actor::message::PlayerMessage;
use crate::player::Player;
use crate::templates::npc_template_manager;

pub mod npc_service {
    use crate::dungoen::NpcLinhCanhHandler;
    use crate::map::map_manager;
    use crate::matches::dhvt::service::GhiDanhHandler;
    use crate::matches::pvp_service;
    use crate::npc::handlers::cargo::CargoHandler;
    use crate::npc::handlers::cui::CuiHandler;
    use crate::npc::handlers::dr_drief::DrDriefHandler;
    use crate::npc::handlers::than_meo::ThanMeoKarinHandler;
    use crate::services::task_service::TaskService;
    use crate::utils::MapUtils;

    use super::*;

    pub fn npc_chat(session: &SessionArc, message: &str, npc_id: i16) -> anyhow::Result<()> {
        let mut msg = Message::new(124);
        msg.write_short(npc_id)?;
        msg.write_utf(message)?;
        session.transmit(msg);
        Ok(())
    }

    pub fn hide_wait_dialog(session: &SessionArc) -> anyhow::Result<()> {
        let mut msg = Message::new(-99);
        msg.write_byte(-1)?;
        session.transmit(msg);
        Ok(())
    }

    pub async fn can_open_npc(session: &SessionArc, npc_id: i16) -> bool {
        let (map_id, player_loc) = if let Some(snapshot) = session.get_player_snapshot().await {
            (snapshot.map_id, snapshot.location.clone())
        } else {
            return false;
        };

        let npc = NpcId::from_i16(npc_id);

        if npc == Some(NpcId::DauThan) {
            if map_id == 21 || map_id == 22 || map_id == 23 {
                return true;
            } else {
                let _ = hide_wait_dialog(session);
                return false;
            }
        }

        if npc == Some(NpcId::LyTieuNuong) {
            return true;
        }

        let map_manage = &map_manager::MAP_MANAGER;
        if let Some(map) = map_manage.find_by_id(map_id) {
            if let Some(npc_spawnd) = map.info.npcs.iter().find(|n| n.temp_id == npc_id as i32) {
                let is_black_war = false;
                if !is_black_war {
                    return true;
                } else {
                    return MapUtils::is_position_in_range(&player_loc, &npc_spawnd.location, 60);
                }
            }
        }
        false
    }

    pub async fn open_menu_controller(session: &SessionArc, npc_id: i16) -> anyhow::Result<()> {
        let player_name = session
            .get_player_snapshot()
            .await
            .map(|p| p.name)
            .unwrap_or_else(|| "Unknown".to_string());

        tracing::debug!(
            "NPC: open_menu_controller: player={}, npc_id={}",
            player_name,
            npc_id
        );

        if session.get_player_handle().await.is_none() {
            return Ok(());
        }

        let mut is_talk_task = false;
        if let Some(snapshot) = session.get_player_snapshot().await {
            if let Some(sub_task) =
                crate::services::task_service::TaskService::get_current_sub_task(&snapshot)
            {
                if sub_task.task_type == TaskType::TalkNpc
                    && TaskService::is_match_npc(&snapshot, npc_id)
                {
                    is_talk_task = true;
                    tracing::debug!(
                        "NPC: Block menu (Talk task match): player={}, npc_id={}, task={}",
                        snapshot.name,
                        npc_id,
                        sub_task.name
                    );
                }
            }
        }

        if let Some(handle) = session.get_player_handle().await {
            handle.send_forget(PlayerMessage::TaskAction(
                TaskType::TalkNpc,
                npc_id.to_string(),
            ));

            if is_talk_task {
                hide_wait_dialog(session)?;
                return Ok(());
            }
        }

        if !can_open_npc(session, npc_id).await {
            hide_wait_dialog(session)?;
            npc_chat(session, "Xin lỗi, tôi không thể mở menu này.", npc_id)?;
            return Ok(());
        }

        let ctx = NpcContext::new(session, npc_id).await;

        if let Some(handler) = get_handler(npc_id) {
            handler.open_menu(&ctx).await?;
        } else {
            tracing::warn!("Unhandled NPC ID: {}", npc_id);
            ctx.npc_chat("Xin lỗi, tôi không thể mở menu này.")?;
            ctx.hide_wait_dialog()?;
        }

        Ok(())
    }

    fn get_handler(npc_id: i16) -> Option<Box<dyn NpcHandler + Send + Sync>> {
        let npc = NpcId::from_i16(npc_id)?;

        match npc {
            NpcId::BaHatMit => Some(Box::new(BahatmitHandler)),
            NpcId::Cui => Some(Box::new(CuiHandler)),
            NpcId::RuongDo => Some(Box::new(RuongDoHandler)),
            NpcId::ConMeo => Some(Box::new(ConMeoHandler)),
            NpcId::Santa => Some(Box::new(SantaHandler)),
            NpcId::DauThan => Some(Box::new(DauThanHandler)),
            NpcId::OngGohan | NpcId::OngMoori | NpcId::OngParagus => Some(Box::new(NpcHomeHandler)),
            NpcId::DrDrief => Some(Box::new(DrDriefHandler)),
            NpcId::Cargo => Some(Box::new(CargoHandler)),
            NpcId::ThanMeoKarin => Some(Box::new(ThanMeoKarinHandler)),
            NpcId::GhiDanh => Some(Box::new(GhiDanhHandler)),
            NpcId::QuyLaoKame => Some(Box::new(QuyLaoKameHandler)),
            NpcId::LinhCanh => Some(Box::new(NpcLinhCanhHandler)),

            // =================Handle Shop Dynamic===============
            NpcId::Bunma => Some(Box::new(DynamicShopHandler::new(
                MenuId::BunmaMenu,
                "Chào bạn! Tôi là Bunma, bạn cần gì?",
            ))),
            NpcId::Dende => Some(Box::new(DynamicShopHandler::new(
                MenuId::DendeMenu,
                "Chào bạn! Tôi là Dende, hôm nay bạn muốn mua gì?",
            ))),
            NpcId::Appule => Some(Box::new(DynamicShopHandler::new(
                MenuId::AppuleMenu,
                "Chào bạn! Tôi là Appule, hành tinh Xayda có nhiều đồ hay lắm!",
            ))),

            _ => None,
        }
    }

    pub async fn handle_menu_confirm(
        session: &SessionArc,
        npc_id: i16,
        select: i8,
    ) -> anyhow::Result<()> {
        let state = if let Some(snapshot) = session.get_player_snapshot().await {
            Some(snapshot.interaction_state.get_index_menu())
        } else {
            None
        };

        let state = match state {
            Some(s) => s,
            None => return Ok(()),
        };

        println!(
            "handle_menu_confirm npc_id={} select={} state={:?}",
            npc_id, select, state
        );

        if let Some(handle) = session.get_player_handle().await {
            handle.send_forget(PlayerMessage::TaskAction(
                TaskType::ConfirmMenu,
                npc_id.to_string(),
            ));
        }

        println!("handle_menu_confirm state={:?}", state);
        match state {
            MenuId::MakeMatchPvp => {
                pvp_service::send_invite_pvp_thachdau(session, select).await?;
                return Ok(());
            }
            MenuId::Revenge => {
                if select == 0 {
                    crate::matches::pvp_service::accept_revenge(session).await?;
                }
                return Ok(());
            }
            _ => {}
        }

        if !can_open_npc(session, npc_id).await {
            return Ok(());
        }

        let ctx = NpcContext::new(session, npc_id).await;

        if let Some(handler) = get_handler(npc_id) {
            handler.handle_menu(&ctx, state, select).await?;
        } else {
            tracing::warn!("Unhandled NPC ID: {}", npc_id);
        }

        Ok(())
    }

    pub async fn create_menu(
        session: &SessionArc,
        npc_id: i16,
        npc_say: &str,
        menu_options: Vec<&str>,
        state: MenuId,
    ) -> anyhow::Result<()> {
        if let Some(handle) = session.get_player_handle().await {
            let options: Vec<String> = menu_options.iter().map(|&s| s.to_string()).collect();
            handle.send_forget(crate::player::player_actor::PlayerMessage::CreateMenu {
                npc_id,
                npc_say: npc_say.to_string(),
                menu_options: options,
                state,
            });
        }
        Ok(())
    }

    pub fn create_menu_player(
        player: &mut Player,
        npc_id: i16,
        npc_say: &str,
        menu_options: Vec<&str>,
        state: MenuId,
    ) -> anyhow::Result<()> {
        player.interaction_state.set_index_menu(state);

        let mut msg = Message::new(32);
        msg.write_short(npc_id)?;
        msg.write_utf(npc_say)?;
        msg.write_byte(menu_options.len() as i8)?;

        for option in menu_options {
            msg.write_utf(option)?;
        }
        player.send_to_client(msg)?;
        Ok(())
    }

    /// Tạo NPC từ template
    pub fn create_npc(template_id: i32, map_id: i32, x: i32, y: i32) -> Option<RtNpc> {
        if let Some(template) = npc_template_manager::get(template_id as i16) {
            Some(RtNpc::from_template(&template, map_id, x, y))
        } else {
            tracing::warn!("NPC template not found for ID: {}", template_id);
            None
        }
    }

    /// Tạo base menu
    pub fn create_base_menu(npc_id: i32, npc_say: &str, menu_options: Vec<String>) -> BaseMenu {
        BaseMenu::new(npc_id, npc_say.to_string(), menu_options)
    }

    /// Lấy NPCs trong tầm
    pub fn get_npcs_in_range<'a>(
        npcs: &'a [RtNpc],
        player_x: i32,
        player_y: i32,
        range: i32,
    ) -> Vec<&'a RtNpc> {
        npcs.iter()
            .filter(|npc| npc.is_in_range(player_x, player_y, range))
            .collect()
    }
}
