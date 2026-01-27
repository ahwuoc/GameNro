use crate::constant::const_menu;
use crate::constant::const_menu::*;
use crate::constant::const_npc;
use crate::constant::const_npc::*;
use crate::constant::menu_enum::MenuId;
use crate::entities::npc_template;
use crate::entities::npc_template::Model as NpcTemplate;
use crate::entities::player;
use crate::network::message::Message;
use crate::network::session::{AsyncSession, SessionArc};
use crate::npc;
use crate::npc::handlers::bahatmit::BahatmitHandler;
use crate::npc::handlers::conmeo::ConMeoHandler;
use crate::npc::handlers::ong_gohan::NpcHomeHandler;
use crate::npc::handlers::ruong_do::RuongDoHandler;
use crate::npc::handlers::santa::SantaHandler;
use crate::npc::handlers::NpcHandler;
use crate::npc::npc_manager;
use crate::npc::{BaseMenu, RtNpc};
use crate::templates::npc_template_manager;
use std::collections::HashMap;

pub mod npc_service {
    use crate::map::map_manager;

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
        msg.write_byte(-1);
        session.transmit(msg);
        Ok(())
    }
    pub async fn can_open_npc(session: &SessionArc, npc_id: i16) -> bool {
        let (map_id, loc_x, loc_y) = session
            .get_player_ref(|player| {
                if let Some(player) = player {
                    Some((player.map_id, player.location.x, player.location.y))
                } else {
                    None
                }
            })
            .await
            .unwrap_or((0, 0, 0));

        if map_id == 0 {
            if session.get_player_ref(|p| p.is_none()).await {
                return false;
            }
        }

        if npc_id == DAU_THAN as i16 {
            if map_id == 21 || map_id == 22 || map_id == 23 {
                return true;
            } else {
                if let Err(e) = hide_wait_dialog(session) {
                    println!("Error sending hide_wait_dialog: {:?}", e);
                }
                return false;
            }
        }

        if npc_id == const_npc::LY_TIEU_NUONG {
            return true;
        }
        let map_manage = &map_manager::MAP_MANAGER;
        if let Some(map) = map_manage.find_by_id(map_id) {
            if let Some(npc_spawnd) = map.info.npcs.iter().find(|n| n.temp_id == npc_id as i32) {
                let is_black_war = false;
                if !is_black_war {
                    return true;
                } else {
                    let dx = (npc_spawnd.x as i32 - loc_x as i32).abs();
                    let dy = (npc_spawnd.y as i32 - loc_y as i32).abs();
                    if dx * dx + dy * dy <= 60_i32.pow(2) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        }
        false
    }

    pub async fn open_menu_controller(session: &SessionArc, npc_id: i16) -> anyhow::Result<()> {
        if session.get_player_ref(|p| p.is_none()).await {
            return Ok(());
        }
        if !can_open_npc(session, npc_id).await {
            npc_chat(session, "Xin lỗi, tôi không thể mở menu này.", npc_id)?;
            hide_wait_dialog(session)?;
            return Ok(());
        }

        if let Some(handler) = get_handler(npc_id) {
            handler.open_menu(session, npc_id as i16).await?;
        } else {
            println!("Unhandled NPC ID: {}", npc_id);
            npc_chat(session, "Xin lỗi, tôi không thể mở menu này.", npc_id)?;
            hide_wait_dialog(session)?;
        }

        Ok(())
    }

    fn get_handler(npc_id: i16) -> Option<Box<dyn NpcHandler + Send + Sync>> {
        match npc_id {
            BA_HAT_MIT => Some(Box::new(BahatmitHandler)),
            RUONG_DO => Some(Box::new(RuongDoHandler)),
            CON_MEO => Some(Box::new(ConMeoHandler)),
            SANTA => Some(Box::new(SantaHandler)),
            ONG_GOHAN | ONG_MOORI | ONG_PARAGUS => Some(Box::new(NpcHomeHandler)),
            _ => None,
        }
    }

    pub async fn handle_menu_confirm(
        session: &SessionArc,
        npc_id: i16,
        select: i8,
    ) -> anyhow::Result<()> {
        let state = session
            .get_player_ref(|player| player.map(|p| p.interaction_state.get_index_menu()))
            .await;

        let state = match state {
            Some(s) => s,
            None => return Ok(()),
        };

        if !can_open_npc(session, npc_id).await {
            return Ok(());
        }

        if let Some(handler) = get_handler(npc_id) {
            handler.handle_menu(session, npc_id, state, select).await?;
        } else {
            println!("Unhandled NPC ID: {}", npc_id);
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
        let Some(mut player) = session.take_player().await else {
            return Ok(());
        };

        player.interaction_state.set_index_menu(state);

        let mut msg = Message::new(32);
        msg.write_short(npc_id)?;
        msg.write_utf(npc_say)?;
        msg.write_byte(menu_options.len() as i8)?;

        for option in menu_options {
            msg.write_utf(option)?;
        }
        session.transmit(msg);
        session.set_player(player).await;
        Ok(())
    }
    pub fn create_npc(template_id: i32, map_id: i32, x: i32, y: i32) -> Option<RtNpc> {
        if let Some(template) = npc_template_manager::get(template_id as i16) {
            Some(RtNpc::from_template(&template, map_id, x, y))
        } else {
            println!("Warning: NPC template not found for ID: {}", template_id);
            None
        }
    }

    pub fn create_base_menu(npc_id: i32, npc_say: &str, menu_options: Vec<String>) -> BaseMenu {
        BaseMenu::new(npc_id, npc_say.to_string(), menu_options)
    }
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
