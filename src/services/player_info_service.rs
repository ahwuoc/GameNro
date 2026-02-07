use sqlx::any;

use crate::data::DataGame;
use crate::entities::prelude::ItemShop;
use crate::item::Item;
use crate::map::zone_manager::ZONE_MANAGER;
use crate::map::Zone;
use crate::network::message::Message;
use crate::network::session::{AsyncSession, SessionArc};
use crate::player::player_actor::pet::Pet;
use crate::player::player_actor::PlayerActor;
use crate::player::Player as RtPlayer;
use crate::services::task_service::TaskService;
use crate::services::{skill_service, IntrinsicService, ServiceHandles};
use crate::templates::task_template_manager::TASK_TEMPLATE_MANAGER;
use std::fs::OpenOptions;
use std::io::Write;

struct SubTaskInfo {
    name: String,
    npc_id: i8,
    map_id: i16,
    notify: String,
    count: i16,
    max_count: i16,
}

pub async fn send_point_info(player: &RtPlayer) -> anyhow::Result<()> {
    send_point_info_sync(player)
}
pub fn send_message_info_hpmp(player: &RtPlayer) -> anyhow::Result<()> {
    send_hp(player)?;
    send_mp(player)?;
    Ok(())
}
pub fn send_hp(player: &RtPlayer) -> anyhow::Result<()> {
    let mut msg = ServiceHandles::sub_command_30(5)?;
    msg.write_int(player.n_point.hp_current)?;
    player.send_to_client(msg)?;
    Ok(())
}
pub fn send_mp(player: &RtPlayer) -> anyhow::Result<()> {
    let mut msg = ServiceHandles::sub_command_30(6)?;
    msg.write_int(player.n_point.mp_current)?;
    player.send_to_client(msg)?;
    Ok(())
}

pub fn send_point_info_sync(player: &RtPlayer) -> anyhow::Result<()> {
    let mut msg = Message::new(-42);
    msg.write_int(player.n_point.hp_base)?;
    msg.write_int(player.n_point.mp_base)?;
    msg.write_int(player.n_point.dame_base)?;
    msg.write_int(player.n_point.hp_max)?;
    msg.write_int(player.n_point.mp_max)?;
    msg.write_int(player.n_point.hp_current)?;
    msg.write_int(player.n_point.mp_current)?;
    msg.write_byte(player.n_point.speed)?;
    msg.write_byte(20)?;
    msg.write_byte(20)?;
    msg.write_byte(1)?;
    msg.write_int(player.n_point.dame)?;
    msg.write_int(player.n_point.def)?;
    msg.write_byte(player.n_point.crit)?;
    msg.write_long(player.n_point.tiem_nang)?;
    msg.write_short(100)?;
    msg.write_short(player.n_point.def_base as i16)?;
    msg.write_byte(player.n_point.crit_base)?;

    player.send_to_client(msg)?;
    Ok(())
}

pub async fn send_task_info(player: &RtPlayer) -> anyhow::Result<()> {
    TaskService::send_task_main(player)
}
pub fn clear_map(player: &RtPlayer) -> anyhow::Result<()> {
    let msg = Message::new(-22);
    player.send_to_client(msg)?;
    Ok(())
}
pub fn send_max_stamina(player: &RtPlayer) -> anyhow::Result<()> {
    let mut msg = Message::new(-69);
    msg.write_short(player.n_point.max_stamina as i16)?;
    player.send_to_client(msg)?;
    Ok(())
}

pub fn send_current_stamina(player: &RtPlayer) -> anyhow::Result<()> {
    let mut msg = Message::new(-68);
    msg.write_short(player.n_point.stamina as i16)?; // current stamina

    player.send_to_client(msg)?;
    Ok(())
}

pub fn send_pet_info(player: &RtPlayer) -> anyhow::Result<()> {
    println!("Sending pet info");
    let mut msg = Message::new(-107);
    msg.write_byte(player.is_pet as i8)?;
    player.send_to_client(msg)?;
    Ok(())
}

pub fn send_top_rank_info(player: &RtPlayer) -> anyhow::Result<()> {
    println!("Sending top rank info");

    let mut msg = Message::new(-119);
    msg.write_utf("1630679754740_-119_r")?;

    player.send_to_client(msg)?;
    Ok(())
}
pub fn send_notification_tab(player: &RtPlayer) -> anyhow::Result<()> {
    let mut msg = Message::new(-50);
    msg.write_byte(0)?; // notification count

    player.send_to_client(msg)?;
    Ok(())
}

pub fn send_time_skill(player: &RtPlayer) -> anyhow::Result<()> {
    println!("Sending time skill info");
    let mut msg = sub_command_i30(62)?;
    player.send_to_client(msg)?;
    Ok(())
}

pub fn sub_command_i30(sub_command: i8) -> anyhow::Result<Message> {
    let mut msg = Message::new(-30);
    msg.write_byte(sub_command)?;
    Ok(msg)
}

pub fn write_inventory(
    msg: &mut Message,
    items: &Vec<Item>,
    default_opt: i8,
) -> anyhow::Result<()> {
    let len_items = items.len().min(255);
    msg.write_byte(len_items as i8)?;
    for item in items.iter().take(len_items) {
        if item.is_null_item() {
            msg.write_short(-1)?;
        } else {
            msg.write_short(item.get_template_id().unwrap_or(-1))?;
            msg.write_int(item.quantity)?;
            msg.write_utf(&item.get_info())?;
            msg.write_utf(&item.get_content())?;
            if item.item_options.is_empty() {
                msg.write_byte(1)?;
                msg.write_byte(default_opt)?;
                msg.write_short(1)?;
            } else {
                let opts_len = item.item_options.len() as i8;
                msg.write_byte(opts_len)?;
                for opt in item.item_options.iter() {
                    msg.write_byte(opt.get_option_id())?;
                    msg.write_short(opt.get_param())?;
                }
            }
        }
    }
    Ok(())
}

pub async fn send_player_blob_internal(player: &RtPlayer) -> anyhow::Result<()> {
    let mut msg = sub_command_i30(0)?;
    msg.write_int(player.id as i32)?; // charID
    msg.write_byte(0)?; // ctaskId
    msg.write_byte(player.gender)?; // cgender
    msg.write_short(player.head)?; // head
    msg.write_utf(&player.name)?; // cName
    msg.write_byte(0)?; // cPk

    msg.write_byte(player.type_pk as i8)?; // cTypePk

    msg.write_long(player.n_point.power)?; // cPower
    msg.write_short(0)?; // eff5BuffHp
    msg.write_short(0)?; // eff5BuffMp

    msg.write_byte(player.gender)?;

    let valid_skill: Vec<_> = player
        .player_skill
        .skills
        .iter()
        .filter(|sk| sk.template_id != -1)
        .collect();
    msg.write_byte(valid_skill.len() as i8);
    for skill in valid_skill {
        msg.write_short(skill.skill_id)?;
    }

    msg.write_long(player.inventory.get_gold())?; // xu
    msg.write_int(player.inventory.get_ruby())?; // luongKhoa
    msg.write_int(player.inventory.get_gem())?; // luong

    write_inventory(&mut msg, &player.inventory.items_body, 73)?;

    write_inventory(&mut msg, &player.inventory.items_bag, 73)?;

    write_inventory(&mut msg, &player.inventory.items_box, 73)?;
    DataGame::send_head_to_client(&mut msg).await?;
    msg.write_short(player.get_head())?; // num17 - number of head/avatar pairs
    msg.write_short(514)?; // charId[gender][0]
    msg.write_short(515)?; // charId[gender][1]
    msg.write_short(537)?; // charId[gender][2]
    msg.write_byte(0)?; // isNhapThe (0 = false, 1 = true)
    msg.write_int(333)?; // deltaTime (server time)
    msg.write_byte(if player.is_new_member { 1 } else { 0 })?; // isNewMember
    msg.write_short(0)?; // idAuraEff
    msg.write_byte(0)?; // idEff_Set_Item
    msg.write_short(0)?; // idHat

    player.send_to_client(msg)?;
    Ok(())
}

pub async fn clear_vtsk(_session: &SessionArc) -> anyhow::Result<()> {
    Ok(())
}

pub async fn send_all_player_info(session: &SessionArc) -> anyhow::Result<()> {
    println!("Sending all player info");

    let player = session
        .get_player_snapshot()
        .await
        .ok_or_else(|| anyhow::anyhow!("Player not set"))?;
    DataGame::send_data_item_bg(session).await?;

    // -82 tile set
    DataGame::send_tile_set_info(session).await?;

    // -112 intrinsic
    IntrinsicService::send_info_intrinsic(&player).await?;

    // -42 my point
    send_point_info(&player).await?;

    // 40 task
    send_task_info(&player).await?;

    // -22 reset all
    clear_map(&player)?;

    // -30 sub 0 player blob
    send_player_blob_internal(&player).await?;

    // -53 my clan
    crate::clan::clan_service::ClanService::send_my_clan(&player).await?;

    // -69 max stamina
    send_max_stamina(&player)?;

    // -68 cur stamina
    send_current_stamina(&player)?;

    // -107 have pet
    send_pet_info(&player)?;

    // -119 top rank
    send_top_rank_info(&player)?;

    // -50 thông tin bảng thông báo
    send_notification_tab(&player)?;

    ServiceHandles::send_cai_trang(&player);

    send_time_skill(&player)?;

    skill_service::send_skill_shortcut(&player)?;

    // clear vt sk
    clear_vtsk(session).await?;

    println!("All player info sent successfully");
    Ok(())
}

pub fn send_info_hp_mp_money(player: &RtPlayer) -> anyhow::Result<()> {
    send_hp(player)?;
    send_mp(player)?;
    Ok(())
}

pub fn send_info_pet(master: &RtPlayer, pet: &Pet) -> anyhow::Result<()> {
    let mut msg = Message::new(-107);
    msg.write_byte(2)?;
    msg.write_short(get_pet_avatar(pet))?;

    write_inventory(&mut msg, &pet.player.inventory.items_body, 73)?;

    // 3. Chỉ số chi tiết
    msg.write_int(pet.player.n_point.hp_current)?;
    msg.write_int(pet.player.n_point.hp_max)?;
    msg.write_int(pet.player.n_point.mp_current)?;
    msg.write_int(pet.player.n_point.mp_max)?;
    msg.write_int(pet.player.n_point.dame)?;
    msg.write_utf(&pet.player.name)?;
    msg.write_utf("Đệ tử")?; // Có thể cải tiến lấy cấp bậc sau
    msg.write_long(pet.player.n_point.power)?;
    msg.write_long(pet.player.n_point.tiem_nang)?;
    msg.write_byte(pet.status as i8)?;
    msg.write_short(pet.player.n_point.stamina)?;
    msg.write_short(pet.player.n_point.max_stamina)?;
    msg.write_byte(pet.player.n_point.crit)?;
    msg.write_short(pet.player.n_point.def as i16)?;

    let skills = &pet.player.player_skill.skills;
    msg.write_byte(4)?;
    for i in 0..4 {
        if let Some(skill) = skills.get(i) {
            if skill.template_id != -1 {
                msg.write_short(skill.template_id as i16)?;
            } else {
                write_skill_hint(&mut msg, i)?;
            }
        } else {
            write_skill_hint(&mut msg, i)?;
        }
    }
    master.send_to_client(msg)?;

    Ok(())
}

fn write_skill_hint(msg: &mut Message, index: usize) -> anyhow::Result<()> {
    msg.write_short(-1)?;
    let hint = match index {
        1 => "Cần đạt sức mạnh 150tr để mở",
        2 => "Cần đạt sức mạnh 1tỷ5 để mở",
        3 => "Cần đạt sức mạnh 20tỷ để mở",
        _ => "Cần đạt sức mạnh 60tỷ để mở",
    };
    msg.write_utf(hint)?;
    Ok(())
}

fn get_pet_avatar(pet: &Pet) -> i16 {
    match pet.type_pet {
        1 => 297,
        2 => 508,
        3 => 1627,
        4 => 1624,
        _ => match pet.player.gender {
            0 => 304,
            1 => 305,
            _ => 303,
        },
    }
}
