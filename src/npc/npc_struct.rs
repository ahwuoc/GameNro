use crate::network::session::SessionArc;
use crate::{
    entities::npc_template::Model as NpcTemplate,
    network::{message::Message, session::AsyncSession},
    utils::Location,
};
use chrono::{DateTime, Utc};

#[derive(Debug, Clone)]
pub struct BaseMenu {
    pub npc_id: i32,
    pub npc_say: String,
    pub menu_select: Vec<String>,
}

impl BaseMenu {
    pub fn new(npc_id: i32, npc_say: String, menu_select: Vec<String>) -> Self {
        Self {
            npc_id,
            npc_say,
            menu_select,
        }
    }

    pub fn get_menu_count(&self) -> usize {
        self.menu_select.len()
    }

    pub fn get_menu_option(&self, index: usize) -> Option<&String> {
        self.menu_select.get(index)
    }
}

#[derive(Debug, Clone)]
pub struct RtNpc {
    pub map_id: i32,
    pub status: i32,
    pub location: Location,
    pub temp_id: i32,
    pub avatar: i32,
    pub base_menu: Option<BaseMenu>,
    pub create_time: DateTime<Utc>,
}

impl RtNpc {
    pub fn new(map_id: i32, status: i32, x: i32, y: i32, temp_id: i32, avatar: i32) -> Self {
        Self {
            map_id,
            status,
            location: {
                let mut loc = Location::new();
                loc.set_position(x as i16, y as i16);
                loc
            },
            temp_id,
            avatar,
            base_menu: None,
            create_time: Utc::now(),
        }
    }

    pub fn from_template(template: &NpcTemplate, map_id: i32, x: i32, y: i32) -> Self {
        Self {
            map_id,
            status: 1,
            location: {
                let mut loc = Location::new();
                loc.set_position(x as i16, y as i16);
                loc
            },
            temp_id: template.id,
            avatar: template.avatar.unwrap_or(0),
            base_menu: None,
            create_time: Utc::now(),
        }
    }
    pub fn init_base_menu(&mut self, text: &str) {
        if text.is_empty() {
            return;
        }

        let text = &text[1..];
        let parts: Vec<&str> = text.split('|').collect();

        if parts.is_empty() {
            return;
        }

        let npc_say = parts[0].replace("<>", "\n");
        let menu_select: Vec<String> = parts[1..].iter().map(|s| s.replace("<>", "\n")).collect();

        self.base_menu = Some(BaseMenu::new(self.temp_id, npc_say, menu_select));
    }

    pub fn get_name(&self) -> String {
        format!("NPC_{}", self.temp_id)
    }

    pub fn get_position(&self) -> (i16, i16) {
        self.location.get_position()
    }

    pub fn set_position(&mut self, x: i32, y: i32) {
        self.location.set_position(x as i16, y as i16);
    }

    pub fn has_menu(&self) -> bool {
        self.base_menu.is_some()
    }

    pub fn get_base_menu(&self) -> Option<&BaseMenu> {
        self.base_menu.as_ref()
    }

    pub async fn open_base_menu(session: &SessionArc) -> anyhow::Result<()> {
        let mut msg = Message::new(-32);
        msg.write_short(14)?;
        msg.write_utf("Ta co the giup gi cho nguoi")?;
        msg.write_byte(1)?;
        msg.write_utf("Tu choi")?;
        session.transmit(msg);
        Ok(())
    }
    pub fn update(&mut self) {}

    pub fn is_in_range(&self, player_x: i32, player_y: i32, range: i32) -> bool {
        use crate::map::MapUtils;
        let mut target = crate::utils::Location::new();
        target.x = player_x as i16;
        target.y = player_y as i16;
        MapUtils::is_position_in_range(&self.location, &target, range as i16)
    }
}
