use crate::network::session::SessionArc;
use crate::player::player::Player;
use tracing::info;

pub struct MagicTreeHandler;

impl MagicTreeHandler {
    pub fn handle_action(player: &mut Player, session: &SessionArc, action: u8) {
        info!("MagicTreeAction({}) for player {}", action, player.id);
        
        match action {
            1 => {
                let menu_id = player.magic_tree.get_menu_id();
                player.interaction_state.set_index_menu(menu_id);
                if let Ok(msg) = player.magic_tree.create_menu_message(player) {
                    session.transmit(msg);
                }
            }
            2 => {
                if let Ok(msg) = player.magic_tree.create_load_message(player) {
                    session.transmit(msg);
                }
            }
            _ => {}
        }
    }

    pub fn handle_harvest(player: &mut Player) {
        info!("MagicTreeHarvest for player {}", player.id);
        crate::services::magic_tree_service::harvest_pea(player);
    }

    pub fn handle_fast_respawn(player: &mut Player) {
        info!("MagicTreeFastRespawn for player {}", player.id);
        crate::services::magic_tree_service::fast_respawn_pea(player);
    }

    pub fn handle_upgrade(player: &mut Player) {
        info!("MagicTreeUpgrade for player {}", player.id);
        crate::services::magic_tree_service::upgrade_magic_tree(player);
    }

    pub fn handle_fast_upgrade(player: &mut Player) {
        info!("MagicTreeFastUpgrade for player {}", player.id);
        crate::services::magic_tree_service::fast_upgrade_magic_tree(player);
    }

    pub fn handle_unupgrade(player: &mut Player) {
        info!("MagicTreeUnupgrade for player {}", player.id);
        crate::services::magic_tree_service::unupgrade_magic_tree(player);
    }
}
