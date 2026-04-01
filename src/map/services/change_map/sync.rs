//! Post-load-map sync: finish_load_map, reset_point, effects

use crate::map::{map_manager::MAP_MANAGER, zone_manager::ZONE_MANAGER};
use crate::network::{message::Message, session::SessionArc};
use crate::player::player::Player;
use crate::services::task_service::TaskService;

pub struct SyncService;

impl SyncService {
    pub async fn finish_load_map(player: &Player, session: Option<&SessionArc>) -> anyhow::Result<()> {
        if let Some(zone) = ZONE_MANAGER.get_zone(player.map_id, player.zone_id) {
            zone.load_another_to_me(player.id).await?;
            zone.load_me_to_another(player.id).await?;
        }
        Self::send_effect_map_to_me(player)?;
        Self::send_effect_me_to_map(player)?;
        TaskService::send_update_count_sub_task(player);
        Ok(())
    }

    pub fn send_effect_map_to_me(player: &Player) -> anyhow::Result<()> {
        let Some(_zone) = ZONE_MANAGER.get_zone(player.map_id, player.zone_id) else {
            return Ok(());
        };
        Ok(())
    }

    pub fn send_effect_me_to_map(_player: &Player) -> anyhow::Result<()> {
        Ok(())
    }

    pub fn send_reset_point(player: &mut Player, session: &SessionArc) {
        let mut x = player.location.x;
        let map_width = MAP_MANAGER
            .find_by_id(player.map_id)
            .map(|m| m.info.map_width as i16)
            .unwrap_or(800);
        if x >= map_width - 60 {
            x = map_width - 60;
        } else if x <= 60 {
            x = 60;
        }
        player.location.x = x;
        let mut msg = Message::new(46);
        let _ = msg.write_short(x);
        let _ = msg.write_short(player.location.y);
        session.transmit(msg);
    }
}
