use crate::constant::const_menu;
use crate::constant::const_menu::*;
use crate::constant::const_npc;
use crate::constant::const_npc::*;
use crate::constant::menu_enum::MenuId;
use crate::entities::npc_template::Model as NpcTemplate;
use crate::entities::player;
use crate::network::message::Message;
use crate::network::session::AsyncSession;
use crate::npc;
use crate::npc::handlers::santa::SantaHandler;
use crate::npc::handlers::NpcHandler;
use crate::npc::{BaseMenu, Npc};
use std::collections::HashMap;

pub struct NpcService {
    npc_templates: HashMap<i32, NpcTemplate>,
}

impl NpcService {
    pub fn new() -> Self {
        Self {
            npc_templates: HashMap::new(),
        }
    }
    pub async fn default_menu(session: &mut AsyncSession, npc_id: i16) -> anyhow::Result<()> {
        let mut msg = Message::new(-32);
        msg.write_short(npc_id)?;
        msg.write_utf("Chức năng đang phát triển")?;
        msg.write_byte(1)?;
        msg.write_utf("Đóng")?;
        session.send_message(&msg).await?;

        Ok(())
    }
    pub async fn open_base_menu(session: &mut AsyncSession, npc_id: i16) -> anyhow::Result<()> {
        let _player = match session.get_player() {
            Some(p) => p,
            None => return Ok(()),
        };

        if let Some(handler) = Self::get_handler(npc_id) {
            handler.open_menu(session).await?;
        } else {
            Self::default_menu(session, npc_id).await?;
        }
        Ok(())
    }

    fn get_handler(npc_id: i16) -> Option<Box<dyn NpcHandler + Send + Sync>> {
        match npc_id {
            SANTA => Some(Box::new(SantaHandler)),
            _ => None,
        }
    }

    pub async fn handle_menu_confirm(
        session: &mut AsyncSession,
        npc_id: i16,
        select: i8,
    ) -> anyhow::Result<()> {
        let state = if let Some(player) = session.get_player() {
            MenuId::from(player.id_mark.get_index_menu())
        } else {
            return Ok(());
        };

        if let Some(handler) = Self::get_handler(npc_id) {
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
    ) -> anyhow::Result<()> {
        let mut msg = crate::network::message::Message::new(32);
        msg.write_short(npc_id)?;
        msg.write_utf(npc_say)?;
        msg.write_byte(menu_options.len() as i8)?;
        for option in menu_options {
            msg.write_utf(option)?;
        }
        session.send_message(&msg).await?;
        Ok(())
    }

    pub fn init(&mut self, npc_templates: Vec<NpcTemplate>) {
        for template in npc_templates {
            self.npc_templates.insert(template.id, template);
        }

        println!(
            "NpcService initialized with {} NPC templates",
            self.npc_templates.len()
        );
    }

    pub fn get_template(&self, id: i32) -> Option<&NpcTemplate> {
        self.npc_templates.get(&id)
    }

    pub fn create_npc(&self, template_id: i32, map_id: i32, x: i32, y: i32) -> Option<Npc> {
        if let Some(template) = self.get_template(template_id) {
            Some(Npc::from_template(template, map_id, x, y))
        } else {
            println!("Warning: NPC template not found for ID: {}", template_id);
            None
        }
    }

    pub fn create_base_menu(
        &self,
        npc_id: i32,
        npc_say: &str,
        menu_options: Vec<String>,
    ) -> BaseMenu {
        BaseMenu::new(npc_id, npc_say.to_string(), menu_options)
    }

    pub fn create_simple_menu(
        &self,
        npc_id: i32,
        npc_say: &str,
        menu_options: &[&str],
    ) -> BaseMenu {
        let menu_select: Vec<String> = menu_options.iter().map(|s| s.to_string()).collect();
        self.create_base_menu(npc_id, npc_say, menu_select)
    }

    pub fn get_npcs_in_range<'a>(
        &self,
        npcs: &'a [Npc],
        player_x: i32,
        player_y: i32,
        range: i32,
    ) -> Vec<&'a Npc> {
        npcs.iter()
            .filter(|npc| npc.is_in_range(player_x, player_y, range))
            .collect()
    }

    pub fn get_all_templates(&self) -> &HashMap<i32, NpcTemplate> {
        &self.npc_templates
    }

    pub fn get_template_count(&self) -> usize {
        self.npc_templates.len()
    }
}
