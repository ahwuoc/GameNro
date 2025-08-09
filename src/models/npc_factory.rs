use std::collections::HashMap;
use crate::models::npc::Npc;
use crate::services::npc_service::NpcService;

pub struct NpcFactory {
    npc_service: NpcService,
}

impl NpcFactory {
    pub fn new() -> Self {
        Self {
            npc_service: NpcService::new(),
        }
    }

    pub fn get_instance() -> &'static mut NpcFactory {
        static mut INSTANCE: Option<NpcFactory> = None;
        unsafe {
            if INSTANCE.is_none() {
                INSTANCE = Some(NpcFactory::new());
            }
            INSTANCE.as_mut().unwrap()
        }
    }

    /// Create NPC by template ID
    pub fn create_npc(&self, template_id: i32, map_id: i32, x: i32, y: i32) -> Option<Npc> {
        self.npc_service.create_npc(template_id, map_id, x, y)
    }

    /// Create basic NPC
    pub fn create_basic_npc(&self, template_id: i32, map_id: i32, x: i32, y: i32) -> Option<Npc> {
        let mut npc = self.create_npc(template_id, map_id, x, y)?;
        
        // Initialize basic menu
        let menu_text = format!("|Chào bạn!<>Tôi có thể giúp gì cho bạn?<>1. Mua bán<>2. Nhiệm vụ<>3. Thoát");
        npc.init_base_menu(&menu_text);
        
        Some(npc)
    }

    /// Create shop NPC
    pub fn create_shop_npc(&self, template_id: i32, map_id: i32, x: i32, y: i32) -> Option<Npc> {
        let mut npc = self.create_npc(template_id, map_id, x, y)?;
        
        // Initialize shop menu
        let menu_text = format!("|Chào mừng đến cửa hàng!<>Bạn muốn mua gì?<>1. Vũ khí<>2. Giáp<>3. Thuốc<>4. Thoát");
        npc.init_base_menu(&menu_text);
        
        Some(npc)
    }

    /// Create quest NPC
    pub fn create_quest_npc(&self, template_id: i32, map_id: i32, x: i32, y: i32) -> Option<Npc> {
        let mut npc = self.create_npc(template_id, map_id, x, y)?;
        
        // Initialize quest menu
        let menu_text = format!("|Bạn có nhiệm vụ nào không?<>Tôi có thể giúp bạn!<>1. Nhận nhiệm vụ<>2. Nộp nhiệm vụ<>3. Thoát");
        npc.init_base_menu(&menu_text);
        
        Some(npc)
    }

    /// Create NPC with custom menu
    pub fn create_npc_with_menu(&self, template_id: i32, map_id: i32, x: i32, y: i32, menu_text: &str) -> Option<Npc> {
        let mut npc = self.create_npc(template_id, map_id, x, y)?;
        npc.init_base_menu(menu_text);
        Some(npc)
    }

    /// Get NPC service
    pub fn get_npc_service(&self) -> &NpcService {
        &self.npc_service
    }

    /// Get mutable NPC service
    pub fn get_npc_service_mut(&mut self) -> &mut NpcService {
        &mut self.npc_service
    }
}
