use crate::map::item_map::ItemMap;
use crate::map::map_manager::MAP_MANAGER;
use crate::map::models::zone_actor::ZoneActor;
use crate::network::message::Message;
use crate::network::session::SessionArc;
use crate::templates::npc_template_manager;
use anyhow::Result;

pub struct MapPacketService;

impl MapPacketService {
    pub fn send_map_info(
        zone: &ZoneActor,
        session: &SessionArc,
        player_id: u64,
        x: i16,
        y: i16,
        player_task_info: Option<(i32, i32)>,
        spaceship_id: i8,
    ) -> Result<()> {
        let map_id = zone.map_id;
        let map_opt = MAP_MANAGER.find_by_id(map_id);

        let mut msg = Message::new(-24);
        msg.write_byte((map_id as u8) as i8)?;

        if let Some(ref map) = map_opt {
            msg.write_byte(map.info.planet_id as i8)?;
            msg.write_byte(map.info.tile_id as i8)?;
            msg.write_byte(map.info.bg_id as i8)?;
            msg.write_byte(map.info.r#type as i8)?;
            msg.write_utf(&map.info.name)?;
        } else {
            msg.write_byte(0)?;
            msg.write_byte(0)?;
            msg.write_byte(0)?;
            msg.write_byte(0)?;
            msg.write_utf(&format!("Map {}", map_id))?;
        }

        msg.write_byte(zone.zone_id as i8)?;
        msg.write_short(x)?;
        msg.write_short(y)?;

        // Waypoints
        if let Some(ref map) = map_opt {
            let wps = &map.info.waypoints;
            let count = (wps.len().min(127)) as i8;
            msg.write_byte(count)?;
            for wp in wps.iter().take(count as usize) {
                msg.write_short(wp.min_x)?;
                msg.write_short(wp.min_y)?;
                msg.write_short(wp.max_x)?;
                msg.write_short(wp.max_y)?;
                msg.write_boolean(wp.is_enter)?;
                msg.write_boolean(wp.is_offline)?;
                msg.write_utf(&wp.name)?;
            }
        } else {
            msg.write_byte(0)?;
        }

        // Mobs
        let mob_count: i8 = (zone.active_mobs.len().min(127)) as i8;
        msg.write_byte(mob_count)?;
        for mob in zone.active_mobs.iter().take(mob_count as usize) {
            msg.write_boolean(false)?;
            msg.write_boolean(false)?;
            msg.write_boolean(false)?;
            msg.write_boolean(false)?;
            msg.write_boolean(false)?;
            msg.write_byte(mob.template_id as i8)?;
            msg.write_byte(0)?;
            msg.write_int(mob.hp)?;
            msg.write_byte(mob.level)?;
            msg.write_int(mob.max_hp)?;
            msg.write_short(mob.location.x)?;
            msg.write_short(mob.location.y)?;
            msg.write_byte(mob.status)?;
            msg.write_byte(mob.lv_mob)?;
            msg.write_boolean(false)?;
        }
        msg.write_byte(0)?;

        // NPCs
        if let Some(ref map) = map_opt {
            let count: i8 = (map.info.npcs.len().min(127)) as i8;
            msg.write_byte(count)?;
            for npc in map.info.npcs.iter().take(count as usize) {
                let status: i8 = 1;
                let avatar: i16 = npc_template_manager::get(npc.temp_id as i16)
                    .and_then(|t| t.avatar)
                    .unwrap_or(0) as i16;
                msg.write_byte(status)?;
                msg.write_short(npc.x)?;
                msg.write_short(npc.y)?;
                msg.write_byte(npc.temp_id as i8)?;
                msg.write_short(avatar)?;
            }
        } else {
            msg.write_byte(0)?;
        }

        // Items
        let filtered_items: Vec<ItemMap> = zone.get_filtered_items_with_task(player_task_info);
        let item_count = filtered_items.len().min(127) as i8;
        msg.write_byte(item_count)?;
        for item in filtered_items.iter().take(item_count as usize) {
            msg.write_short(item.item_map_id as i16)?;
            msg.write_short(item.get_item_id())?;
            msg.write_short(item.x as i16)?;
            msg.write_short(item.y as i16)?;
            msg.write_int(item.player_id as i32)?;
        }

        // BG Items & Effects
        let bg_item_path = format!("data/arc/map/item_bg_map_data/{}", map_id);
        if let Ok(data) = std::fs::read(&bg_item_path) {
            msg.write(&data)?;
        } else {
            msg.write_short(0)?;
        }

        let eff_item_path = format!("data/arc/map/eff_map/{}", map_id);
        if let Ok(data) = std::fs::read(&eff_item_path) {
            msg.write(&data)?;
        } else {
            msg.write_short(0)?;
        }

        let bg_type = map_opt.as_ref().map(|m| m.info.bg_type as i8).unwrap_or(0);
        msg.write_byte(bg_type)?;
        msg.write_byte(spaceship_id)?;
        msg.write_byte(if map_id == 148 { 1 } else { 0 })?;

        session.transmit(msg);
        Ok(())
    }
}
