use crate::models::zone::Zone;
use crate::services::map_service::MapService;
use crate::player::Player;

pub struct ChangeMapService {
    initialized: bool,
}

impl ChangeMapService {
    pub fn new() -> Self {
        Self {
            initialized: false,
        }
    }

    /// Get singleton instance
    pub fn get_instance() -> &'static mut ChangeMapService {
        static mut INSTANCE: Option<ChangeMapService> = None;
        unsafe {
            if INSTANCE.is_none() {
                INSTANCE = Some(ChangeMapService::new());
            }
            INSTANCE.as_mut().unwrap()
        }
    }

    /// Initialize service
    pub fn init(&mut self) {
        self.initialized = true;
        println!("ChangeMapService initialized");
    }

    /// Check if service is initialized
    pub fn is_initialized(&self) -> bool {
        self.initialized
    }

    /// Change player's zone within the same map
    pub fn change_zone(&self, player: &mut Player, zone_id: i32) -> bool {
        if player.zone_id == 0 {
            return false;
        }
        false
    }
    pub fn change_map(&self, player: &mut Player, map_id: i32, zone_id: i32, x: i32, y: i32) -> bool {
        let map_service = MapService::get_instance();
        
        if let Some(zone) = map_service.get_map_can_join(player, map_id, zone_id) {
            self.change_map_to_zone(player, zone, x, y);
            true
        } else {
            false
        }
    }
    pub fn change_map_to_zone(&self, player: &mut Player, zone: &Zone, x: i32, y: i32) {
        self.exit_map(player);
        let nx: i16 = if x != -1 { x as i16 } else { player.location.x };
        let ny: i16 = if y != -1 { y as i16 } else { player.location.y };
        player.location.set_position(nx, ny);
        self.go_to_map(player, zone);
    }
    pub fn change_map_by_spaceship(&self, player: &mut Player, map_id: i32, zone_id: i32, x: i32) -> bool {
        let map_service = MapService::get_instance();
        
        if let Some(zone) = map_service.get_map_can_join(player, map_id, zone_id) {
            self.space_ship_arrive(player, 1, 1); // DEFAULT_SPACE_SHIP
            
            self.change_map_to_zone(player, zone, x, 100);
            true
        } else {
            false
        }
    }
    pub fn change_map_in_yard(&self, player: &mut Player, map_id: i32, zone_id: i32, x: i32) -> bool {
        let map_service = MapService::get_instance();
        
        if let Some(zone) = map_service.get_map_can_join(player, map_id, zone_id) {
            let map_width: i32 = MapService::get_instance()
                .get_map_by_id(zone.map_id)
                .map(|m| m.map_width)
                .unwrap_or(2000);
            let usable: u32 = (map_width.max(200) - 200) as u32;
            let final_x = if x != -1 { x } else {
                use std::time::SystemTime;
                let seed = SystemTime::now().duration_since(SystemTime::UNIX_EPOCH).unwrap().as_millis() as u32;
                100 + (seed % usable) as i32
            };
            
            self.change_map_to_zone(player, zone, final_x, 100); 
            true
        } else {
            false
        }
    }

    /// Change map using waypoint
    pub fn change_map_waypoint(&self, player: &mut Player) -> bool {
        let map_service = MapService::get_instance();
        
        // Check if player is in waypoint area
        if let Some(waypoint) = map_service.get_waypoint_player_in(player) {
            if let Some(zone) = map_service.get_map_can_join(player, waypoint.go_map, -1) {
                self.change_map_to_zone(player, zone, waypoint.go_x as i32, waypoint.go_y as i32);
                return true;
            }
        }
        false
    }

    /// Go to map (internal method)
    pub fn go_to_map(&self, player: &mut Player, zone: &Zone) {
        // Set player's zone
        player.zone_id = zone.zone_id as u32;
        player.map_id = zone.map_id as u32;
        player.location.set_map(player.map_id, player.zone_id);

        println!("🗺️ Player {} moved to zone {}", player.name, zone.zone_id);
    }

    /// Exit current map
    pub fn exit_map(&self, player: &mut Player) {
        if player.zone_id != 0 {
            println!("🚪 Player {} exited zone {}", player.name, player.zone_id);
            player.zone_id = 0;
        }
    }

    /// Spaceship arrive effect
    pub fn space_ship_arrive(&self, player: &Player, _type_send_msg: u8, type_space: u8) {
        // TODO: Send message to client about spaceship arrival
        println!(" Spaceship arrived for player: {} (type: {})", player.name, type_space);
    }

    /// Check if map can be joined
    pub fn check_map_can_join<'a>(&self, player: &Player, zone: &'a Zone) -> Option<&'a Zone> {
        // TODO: Implement async zone checks
        // For now, return Some(zone) as basic check
        Some(zone)
    }

    /// Get zone by map ID and zone ID
    pub fn get_zone_join_by_map_id_and_zone_id(&self, player: &Player, map_id: i32, zone_id: i32) -> Option<&Zone> {
        // TODO: Implement async zone access
        None
    }

    /// Get map can join
    pub fn get_map_can_join(&self, player: &Player, map_id: i32) -> Option<&Zone> {
        // TODO: Implement async map access
        None
    }

    /// Change map without spaceship
    pub fn change_map_non_spaceship(&self, player: &mut Player, map_id: i32, x: i32, y: i32) -> bool {
        if let Some(zone) = self.get_map_can_join(player, map_id) {
            self.change_map_to_zone(player, zone, x, y);
            true
        } else {
            false
        }
    }

    /// Reset player position
    pub fn reset_point(&self, player: &mut Player) {
        // Reset player to safe position based on map width
        let map_width: i32 = MapService::get_instance()
            .get_map_by_id(player.map_id as i32)
            .map(|m| m.map_width)
            .unwrap_or(2000);
        let max_x = (map_width - 60).max(60);
        let x_i32 = player.location.x as i32;
        if x_i32 >= max_x {
            player.location.x = max_x as i16;
        } else if x_i32 <= 60 {
            player.location.x = 60;
        }
    }

    /// Finish loading map
    pub fn finish_load_map(&self, player: &mut Player) {
        // TODO: Send map info to player
        println!("🗺️ Map loaded for player: {}", player.name);
    }
}
