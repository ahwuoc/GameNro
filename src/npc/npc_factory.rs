use crate::npc::Npc;

pub struct NpcFactory;

impl NpcFactory {
    pub fn new() -> Self {
        Self {}
    }
    /// Create basic NPC with default menu
    pub fn create_basic_npc(&self, template_id: i32, map_id: i32, x: i32, y: i32) -> Option<Npc> {
        let mut npc = Npc::new(map_id, 1, x, y, template_id, 0);
        
        // Initialize basic menu
        let menu_text = format!("|Chào bạn!<>Tôi có thể giúp gì cho bạn?<>1. Mua bán<>2. Nhiệm vụ<>3. Thoát");
        npc.init_base_menu(&menu_text);
        
        Some(npc)
    }

    /// Create shop NPC with shop menu
    pub fn create_shop_npc(&self, template_id: i32, map_id: i32, x: i32, y: i32) -> Option<Npc> {
        let mut npc = Npc::new(map_id, 1, x, y, template_id, 0);
        
        // Initialize shop menu
        let menu_text = format!("|Chào mừng đến cửa hàng!<>Bạn muốn mua gì?<>1. Vũ khí<>2. Giáp<>3. Thuốc<>4. Thoát");
        npc.init_base_menu(&menu_text);
        
        Some(npc)
    }

    /// Create quest NPC with quest menu
    pub fn create_quest_npc(&self, template_id: i32, map_id: i32, x: i32, y: i32) -> Option<Npc> {
        let mut npc = Npc::new(map_id, 1, x, y, template_id, 0);
        
        // Initialize quest menu
        let menu_text = format!("|Bạn có nhiệm vụ nào không?<>Tôi có thể giúp bạn!<>1. Nhận nhiệm vụ<>2. Nộp nhiệm vụ<>3. Thoát");
        npc.init_base_menu(&menu_text);
        
        Some(npc)
    }

    /// Create NPC with custom menu
    pub fn create_npc_with_menu(&self, template_id: i32, map_id: i32, x: i32, y: i32, menu_text: &str) -> Option<Npc> {
        let mut npc = Npc::new(map_id, 1, x, y, template_id, 0);
        npc.init_base_menu(menu_text);
        Some(npc)
    }
}
