use crate::constant::const_npc::NpcId;
use crate::map::zone_manager::ZONE_MANAGER;
use crate::network::message::Message;
use crate::player::player_actor::PlayerHandle;
use crate::player::player_manager::PLAYER_MANAGER;
use crate::player::Player;
use crate::utils::skill_util;
use crate::{item::ItemService, player::player_data::PetData};

use super::{handle::PetHandle, pet_actor::PetActor, Pet, PetStatus};

pub struct PetService;

impl PetService {
    pub async fn spawn_pet(master: &mut Player) -> anyhow::Result<PetHandle> {
        let mut pet_player = Player::new(
            master.id + 1000000,
            format!("$ Đệ tử {}", master.name),
            master.gender as u8,
        );
        master.pet_id = Some(pet_player.id);
        pet_player.is_pet = true;
        pet_player.master_id = Some(master.id);
        pet_player.location = master.location.clone();
        pet_player.map_id = master.map_id;
        pet_player.zone_id = master.zone_id;
        for _ in 0..6 {
            pet_player
                .inventory
                .items_body
                .push(ItemService::create_item_null());
        }

        let skill_id = (pet_player.gender * 2) as i32;
        if let Some(skill) = skill_util::create_skill(skill_id, 1).await {
            pet_player.player_skill.skills.push(skill);
        }

        let (tx, rx) = tokio::sync::mpsc::channel(100);
        let handle = PetHandle::new(pet_player.id, tx.clone());

        let pet = Pet {
            player: pet_player,
            master_id: master.id,
            status: PetStatus::Follow,
            type_pet: 0,
            is_tranform: false,
            last_time_die: 0,
            last_time_unfusion: 0,
            is_gohome: false,
            master_location: Some((master.location.x, master.location.y)),
            target_mob_id: None,
            target_player_id: None,
            last_time_chat: 0,
            chat_index: 0,
            last_time_idle_move: 0,
            last_time_ask_pea: 0,
            last_time_stamina_update: 0,
            last_time_gohome: 0,
        };

        if let Some(zone) = ZONE_MANAGER.get_zone(pet.player.map_id, pet.player.zone_id) {
            let handle = PlayerHandle::new(pet.player.id, true, tx, pet.player.public_state.clone());
            PLAYER_MANAGER.add(pet.player.id, handle.clone());
            let _ = zone.add_player(handle).await;
            let _ = zone.load_me_to_another(pet.player.id).await;
        }

        let actor = PetActor::new(pet, rx);
        tokio::spawn(actor.run());

        Ok(handle)
    }

    pub async fn load_pet(master: &mut Player, data: PetData) -> anyhow::Result<PetHandle> {
        let mut pet_player = Player::new(master.id + 1000000, data.name, data.gender as u8);
        master.pet_id = Some(pet_player.id);
        pet_player.is_pet = true;
        pet_player.master_id = Some(master.id);
        pet_player.head = data.head;
        pet_player.location = master.location.clone();
        pet_player.map_id = master.map_id;
        pet_player.zone_id = master.zone_id;

        // Load points
        pet_player.n_point.set_hp_chiso(data.n_point.hp_goc);
        pet_player.n_point.set_mp_chiso(data.n_point.mp_goc);
        pet_player.n_point.set_dame_chiso(data.n_point.damege_goc);
        pet_player.n_point.set_def_chiso(data.n_point.defen_goc);
        pet_player.n_point.set_crit_chiso(data.n_point.crit_goc);
        pet_player.n_point.set_power(data.n_point.power);
        pet_player.n_point.set_tiem_nang(data.n_point.tiem_nang);
        pet_player.n_point.set_limit_power(data.n_point.limit_power);
        pet_player.n_point.max_stamina = data.n_point.max_stamina;

        pet_player.n_point.cal_point();

        pet_player.n_point.set_hp_current(data.n_point.pl_hp);
        pet_player.n_point.set_mp_current(data.n_point.pl_mp);
        pet_player.n_point.set_stamina(data.n_point.stamina);

        // Load items body
        for item_data in data.items_body {
            if item_data.id != -1 {
                if let Some(mut item) =
                    crate::item::item_service::ItemService::create_new_item_with_quantity(
                        item_data.id as i16,
                        item_data.quantity,
                    )
                {
                    for opt in item_data.options {
                        item.add_option_param(opt.id as i8, opt.value as i16);
                    }
                    pet_player.inventory.items_body.push(item);
                }
            } else {
                pet_player
                    .inventory
                    .items_body
                    .push(crate::item::item_service::ItemService::create_item_null());
            }
        }

        // Load skills
        for skill_data in data.skills {
            if let Some(mut skill) =
                crate::utils::skill_util::create_skill(skill_data.template_id, skill_data.point)
                    .await
            {
                skill.start_time_use = skill_data.last_time_use;
                skill.curr_level = skill_data.curr_level;
                pet_player.player_skill.skills.push(skill);
            }
        }

        if let Some(first_skill) = pet_player.player_skill.skills.first() {
            pet_player.player_skill.skill_select = Some(first_skill.clone());
        }

        let (tx, rx) = tokio::sync::mpsc::channel(100);
        let handle = PetHandle::new(pet_player.id, tx.clone());

        let pet = Pet {
            player: pet_player,
            master_id: master.id,
            status: match data.status {
                0 => PetStatus::Follow,
                1 => PetStatus::Protect,
                2 => PetStatus::Attack,
                3 => PetStatus::GoHome,
                4 => PetStatus::Fusion,
                5 => PetStatus::HTVV,
                _ => PetStatus::Follow,
            },
            type_pet: data.type_pet,
            is_tranform: false,
            last_time_die: 0,
            last_time_unfusion: 0,
            is_gohome: false,
            master_location: Some((master.location.x, master.location.y)),
            target_mob_id: None,
            target_player_id: None,
            last_time_chat: 0,
            chat_index: 0,
            last_time_idle_move: 0,
            last_time_ask_pea: 0,
            last_time_stamina_update: 0,
            last_time_gohome: 0,
        };

        if let Some(zone) =
            crate::map::zone_manager::ZONE_MANAGER.get_zone(pet.player.map_id, pet.player.zone_id)
        {
            let handle = PlayerHandle::new(pet.player.id, true, tx, pet.player.public_state.clone());
            crate::player::player_manager::PLAYER_MANAGER.add(pet.player.id, handle.clone());
            let _ = zone.add_player(handle).await;
            let _ = zone.load_me_to_another(pet.player.id).await;
        }

        let actor = PetActor::new(pet, rx);
        tokio::spawn(actor.run());

        Ok(handle)
    }
}
