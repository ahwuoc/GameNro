use crate::item::inventory::Inventory;
use crate::item::inventory_service::InventoryService;
use crate::item::item_service::ItemService;
use crate::player::magic_tree::{
    HARVEST_GEM, MAX_LEVEL, PEA_PARAM, PEA_TEMP, PEA_UPGRADE, UPGRADE_GEM,
};
use crate::player::Player;
use crate::services::player_info_service;
use crate::services::services::ServiceHandles;
use crate::utils::time;
use tracing::info;

pub struct MagicTreeService;

impl MagicTreeService {
    pub fn harvest_pea(player: &mut Player) {
        let curr_peas = player.magic_tree.curr_peas;
        info!(
            "MagicTreeService::harvest_pea: player={}, curr_peas={}",
            player.name, curr_peas
        );
        if curr_peas > 0 {
            let level = player.magic_tree.level;
            let template_id = PEA_TEMP[(level - 1) as usize];
            let param = PEA_PARAM[(level - 1) as usize];
            let option_id = if level - 1 > 1 { 2 } else { 48 };

            let mut pea =
                match ItemService::create_new_item_with_quantity(template_id, curr_peas as i32) {
                    Some(p) => p,
                    None => {
                        info!(
                            "MagicTreeService::harvest_pea: Failed to create item for id={}",
                            template_id
                        );
                        return;
                    }
                };
            pea.add_option_param(option_id as i8, param as i16);

            if InventoryService::add_item_to_inventory(&mut player.inventory.items_bag, pea.clone())
            {
                info!(
                    "MagicTreeService::harvest_pea: Added {} peas to bag",
                    curr_peas
                );
                let _ = ServiceHandles::send_thong_bao(
                    player,
                    &format!(
                        "Bạn vừa thu hoạch được {} hạt Đậu thần cấp {}",
                        curr_peas, level
                    ),
                );
                player.magic_tree.curr_peas = 0;
                player.magic_tree.last_time_harvest = time::current_time_millis() as i64;
                let _ = InventoryService::send_item_bag(player);
            } else if InventoryService::add_item_to_inventory(&mut player.inventory.items_box, pea)
            {
                info!(
                    "MagicTreeService::harvest_pea: Bag full, added {} peas to box",
                    curr_peas
                );
                let _ = ServiceHandles::send_message_alert(
                    player,
                    &format!(
                        "Hành trang đầy, {} hạt Đậu thần cấp {} đã được chuyển vào rương",
                        curr_peas, level
                    ),
                );
                player.magic_tree.curr_peas = 0;
                player.magic_tree.last_time_harvest = time::current_time_millis() as i64;
            } else {
                info!("MagicTreeService::harvest_pea: Both bag and box are full");
                let _ = ServiceHandles::send_message_alert(
                    player,
                    "Hành trang và rương đều đầy, hãy làm trống để thu hoạch",
                );
            }

            // Refresh client tree state if harvested
            if player.magic_tree.curr_peas == 0 {
                if let Ok(msg) = player.magic_tree.create_load_message(player) {
                    let _ = player.send_to_client(msg);
                }
            }
        }
    }

    pub fn fast_respawn_pea(player: &mut Player) {
        let level_idx = (player.magic_tree.level - 1) as usize;
        let gem_cost = HARVEST_GEM[level_idx];

        if player.inventory.get_gem() >= gem_cost {
            player.inventory.sub_gem(gem_cost);
            let _ = ServiceHandles::send_gold_gem_ruby_to_client(player);

            player.magic_tree.curr_peas = player.magic_tree.get_max_pea() as u16;

            if let Ok(msg) = player.magic_tree.create_load_message(player) {
                let _ = player.send_to_client(msg);
            }
        } else {
            let _ = ServiceHandles::send_message_alert(
                player,
                &format!(
                    "Bạn không đủ gem để kết hạt nhanh, còn thiếu {} gem.",
                    gem_cost - player.inventory.get_gem()
                ),
            );
        }
    }

    pub fn upgrade_magic_tree(player: &mut Player) {
        let level_idx = (player.magic_tree.level - 1) as usize;
        let gold_base = PEA_UPGRADE[level_idx][3];
        let gold_required = gold_base as i64
            * (if player.magic_tree.level <= 3 {
                1000
            } else {
                1000000
            });

        if player.inventory.get_gold() >= gold_required {
            player.inventory.sub_gold(gold_required);
            player.magic_tree.is_upgrade = true;
            player.magic_tree.last_time_upgrade = time::current_time_millis() as i64;

            let _ = ServiceHandles::send_gold_gem_ruby_to_client(player);
            if let Ok(msg) = player.magic_tree.create_load_message(player) {
                let _ = player.send_to_client(msg);
            }
        } else {
            let _ = ServiceHandles::send_message_alert(
                player,
                &format!(
                    "Bạn không đủ vàng để nâng cấp, còn thiếu {} vàng nữa",
                    gold_required - player.inventory.get_gold()
                ),
            );
        }
    }

    pub fn fast_upgrade_magic_tree(player: &mut Player) {
        let level_idx = (player.magic_tree.level - 1) as usize;
        let gem_cost = UPGRADE_GEM[level_idx];

        if player.magic_tree.level < MAX_LEVEL && player.inventory.get_gem() >= gem_cost {
            player.inventory.sub_gem(gem_cost);
            player.magic_tree.level += 1;
            player.magic_tree.is_upgrade = false;

            let _ = ServiceHandles::send_gold_gem_ruby_to_client(player);
            let _ = ServiceHandles::send_message_alert(
                player,
                &format!("Nâng cấp nhanh thành công! Bạn đã mất {} ngọc.", gem_cost),
            );

            if let Ok(msg) = player.magic_tree.create_load_message(player) {
                let _ = player.send_to_client(msg);
            }
        } else {
            let _ = ServiceHandles::send_message_alert(
                player,
                "Bạn không đủ ngọc để nâng cấp nhanh hoặc cây đã đạt cấp tối đa.",
            );
        }
    }

    pub fn unupgrade_magic_tree(player: &mut Player) {
        let level_idx = (player.magic_tree.level - 1) as usize;
        let gold_base = PEA_UPGRADE[level_idx][3];
        let gold_return = gold_base as i64
            * (if player.magic_tree.level <= 3 {
                1000
            } else {
                1000000
            });

        player.inventory.add_gold(gold_return);
        player.magic_tree.is_upgrade = false;

        let _ = ServiceHandles::send_gold_gem_ruby_to_client(player);
        if let Ok(msg) = player.magic_tree.create_load_message(player) {
            let _ = player.send_to_client(msg);
        }
    }
}

pub fn harvest_pea(player: &mut Player) {
    MagicTreeService::harvest_pea(player);
}

pub fn fast_respawn_pea(player: &mut Player) {
    MagicTreeService::fast_respawn_pea(player);
}

pub fn upgrade_magic_tree(player: &mut Player) {
    MagicTreeService::upgrade_magic_tree(player);
}

pub fn fast_upgrade_magic_tree(player: &mut Player) {
    MagicTreeService::fast_upgrade_magic_tree(player);
}

pub fn unupgrade_magic_tree(player: &mut Player) {
    MagicTreeService::unupgrade_magic_tree(player);
}
