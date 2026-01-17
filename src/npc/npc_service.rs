use crate::constant::const_menu;
use crate::constant::const_menu::*;
use crate::constant::const_npc;
use crate::constant::const_npc::*;
use crate::constant::menu_enum::MenuId;
use crate::entities::npc_template;
use crate::entities::npc_template::Model as NpcTemplate;
use crate::entities::player;
use crate::network::message::Message;
use crate::network::session::AsyncSession;
use crate::npc;
use crate::npc::handlers::bahatmit::BahatmitHandler;
use crate::npc::handlers::conmeo::ConMeoHandler;
use crate::npc::handlers::ruong_do::RuongDoHandler;
use crate::npc::handlers::santa::SantaHandler;
use crate::npc::handlers::NpcHandler;
use crate::npc::npc_manager;
use crate::npc::npc_template_manager;
use crate::npc::{BaseMenu, RtNpc};
use std::collections::HashMap;

pub mod npc_service {
    use super::*;

    pub async fn open_menu_controller(
        session: &mut AsyncSession,
        npc_id: i16,
    ) -> anyhow::Result<()> {
        if session.get_player().is_none() {
            return Ok(());
        }

        if let Some(handler) = get_handler(npc_id) {
            handler.open_menu(session, npc_id as i16).await?;
        } else {
            println!("Unhandled NPC ID: {}", npc_id);
        }
        Ok(())
    }

    fn get_handler(npc_id: i16) -> Option<Box<dyn NpcHandler + Send + Sync>> {
        match npc_id {
            BA_HAT_MIT => Some(Box::new(BahatmitHandler)),
            RUONG_DO => Some(Box::new(RuongDoHandler)),
            CON_MEO => Some(Box::new(ConMeoHandler)),
            SANTA => Some(Box::new(SantaHandler)),
            _ => None,
        }
    }

    pub async fn handle_menu_confirm(
        session: &mut AsyncSession,
        npc_id: i16,
        select: i8,
    ) -> anyhow::Result<()> {
        let state = match session.get_player() {
            Some(p) => p.id_mark.get_index_menu(),
            None => return Ok(()),
        };

        if let Some(handler) = get_handler(npc_id) {
            handler.handle_menu(session, state, select).await?;
        } else {
            println!("Unhandled NPC ID: {}", npc_id);
        }

        Ok(())
    }

    pub async fn create_menu(
        session: &mut AsyncSession,
        npc_id: i16,
        npc_say: &str,
        menu_options: Vec<&str>,
        state: MenuId,
    ) -> anyhow::Result<()> {
        let Some(player) = session.get_player_mut() else {
            return Ok(());
        };

        player.id_mark.set_index_menu(state);

        let mut msg = Message::new(32);
        msg.write_short(npc_id)?;
        msg.write_utf(npc_say)?;
        msg.write_byte(menu_options.len() as i8)?;

        for option in menu_options {
            msg.write_utf(option)?;
        }
        session.send_message(&msg).await?;
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
