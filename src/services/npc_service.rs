use std::collections::HashMap;
use crate::models::npc::{Npc, BaseMenu};
use crate::entities::npc_template::Model as NpcTemplate;

pub struct NpcService {
    npc_templates: HashMap<i32, NpcTemplate>,
}

impl NpcService {
    pub fn new() -> Self {
        Self {
            npc_templates: HashMap::new(),
        }
    }

    pub fn get_instance() -> &'static mut NpcService {
        static mut INSTANCE: Option<NpcService> = None;
        unsafe {
            if INSTANCE.is_none() {
                INSTANCE = Some(NpcService::new());
            }
            INSTANCE.as_mut().unwrap()
        }
    }

    pub fn init(&mut self, npc_templates: Vec<NpcTemplate>) {
        for template in npc_templates {
            self.npc_templates.insert(template.id, template);
        }
        
        println!("NpcService initialized with {} NPC templates", self.npc_templates.len());
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

    /// Create base menu
    pub fn create_base_menu(&self, npc_id: i32, npc_say: &str, menu_options: Vec<String>) -> BaseMenu {
        BaseMenu::new(npc_id, npc_say.to_string(), menu_options)
    }

    /// Create simple menu
    pub fn create_simple_menu(&self, npc_id: i32, npc_say: &str, menu_options: &[&str]) -> BaseMenu {
        let menu_select: Vec<String> = menu_options.iter().map(|s| s.to_string()).collect();
        self.create_base_menu(npc_id, npc_say, menu_select)
    }

    /// Check if NPC can be opened
    pub fn can_open_npc(&self, npc: &Npc, player_id: i64) -> bool {
        npc.can_open_npc(player_id)
    }

    /// Get NPCs in range of player
    pub fn get_npcs_in_range<'a>(&self, npcs: &'a [Npc], player_x: i32, player_y: i32, range: i32) -> Vec<&'a Npc> {
        npcs.iter().filter(|npc| npc.is_in_range(player_x, player_y, range)).collect()
    }

    /// Get all NPC templates
    pub fn get_all_templates(&self) -> &HashMap<i32, NpcTemplate> {
        &self.npc_templates
    }

    /// Get NPC template count
    pub fn get_template_count(&self) -> usize {
        self.npc_templates.len()
    }
}
