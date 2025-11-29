use crate::{
    map::{MapService, Zone},
    player::Player,
};

pub struct ChangeMapService {}

impl ChangeMapService {
    pub fn change_zone(&self, player: &mut Player, zone_id: i32) -> bool {
        if player.zone_id == 0 {
            return false;
        }
        false
    }
    pub fn change_map(
        &self,
        player: &mut Player,
        map_id: i32,
        zone_id: i32,
        x: i16,
        y: i16,
    ) -> bool {
        let map_service = MapService::get_instance();

        if let Some(zone) = map_service.get_map_can_join(player, map_id, zone_id) {
            self.change_map_to_zone(player, zone, x, y);
            true
        } else {
            false
        }
    }
    pub fn change_map_to_zone(&self, player: &mut Player, zone: &Zone, x: i16, y: i16) {
        self.exit_map(player);
        let nx = if x != -1 { x } else { player.location.x };
        let ny  = if y != -1 { y } else { player.location.y };
        player.location.set_position(nx, ny);
        self.go_to_map(player, zone);
    }

    pub fn change_map_in_yard(
        &self,
        player: &mut Player,
        map_id: i32,
        zone_id: i32,
        x: i32,
    ) -> bool {
        let map_service = MapService::get_instance();

        if let Some(zone) = map_service.get_map_can_join(player, map_id, zone_id) {
            let map_width: i32 = MapService::get_instance()
                .get_map_by_id(zone.map_id)
                .map(|m| m.map_width)
                .unwrap_or(2000);
            let usable: u32 = (map_width.max(200) - 200) as u32;
            let final_x: i32 = if x != -1 {
                x
            } else {
                use std::time::SystemTime;
                let seed = SystemTime::now()
                    .duration_since(SystemTime::UNIX_EPOCH)
                    .unwrap()
                    .as_millis() as u32;
                100 + (seed % usable) as i32
            };

            self.change_map_to_zone(player, zone, final_x as i16, 100);
            true
        } else {
            false
        }
    }

    pub fn change_map_waypoint(&self, player: &mut Player) -> bool {
        let map_service = MapService::get_instance();
        if let Some(waypoint) = map_service.get_waypoint_player_in(player) {
            if let Some(zone) = map_service.get_map_can_join(player, waypoint.go_map, -1) {
                self.change_map_to_zone(player, zone, waypoint.go_x , waypoint.go_y );
                return true;
            }
        }
        false
    }

    pub fn go_to_map(&self, player: &mut Player, zone: &Zone) {
        player.zone_id = zone.zone_id;
        player.map_id = zone.map_id;
        player.location.set_map(player.map_id, player.zone_id);

        println!("🗺️ Player {} moved to zone {}", player.name, zone.zone_id);
    }

    pub fn exit_map(&self, player: &mut Player) {
        if player.zone_id != 0 {
            println!("🚪 Player {} exited zone {}", player.name, player.zone_id);
            player.zone_id = 0;
        }
    }

    pub async fn finish_load_map(player: &Player) -> anyhow::Result<()> {
        if let Some(zone) = &player.zone {
            zone.load_another_to_me(player.id).await?;
        }
        Ok(())
    }
}
