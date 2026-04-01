//! Clan creation logic
use crate::clan::clan_manager::CLAN_MANAGER;
use crate::database::DbManager;
use crate::models::clan::{Clan, ClanMember};
use crate::player::Player;
use crate::services::player_info_service;
use crate::services::services::ServiceHandles;
use sea_orm::{ActiveModelTrait, Set};

const RED: i8 = 1;

pub struct CreationService;

impl CreationService {
    pub async fn get_clan(player: &Player, mut msg: crate::network::message::Message) -> anyhow::Result<()> {
        let action = msg.read_byte()?;
        match action {
            1 => {
                let mut msg_res = crate::network::message::Message::new(-46);
                msg_res.write_byte(1)?;
                msg_res.write_byte(5)?;
                for i in 1..=5i8 {
                    msg_res.write_byte(i)?;
                    msg_res.write_utf(&format!("Cờ bang {}", i))?;
                    msg_res.write_int(2_000_000)?;
                    msg_res.write_int(0)?;
                }
                player.send_to_client(msg_res);
            }
            2 => {
                let img_id = msg.read_byte()?;
                let name = msg.read_utf()?;
                Self::create_clan(player, img_id, &name).await?;
            }
            _ => tracing::warn!("Unknown clan action: {}", action),
        }
        Ok(())
    }

    pub async fn create_clan(player: &Player, img_id: i8, name: &str) -> anyhow::Result<()> {
        if player.clan_id != -1 {
            ServiceHandles::send_message_alert(player, "Bạn đang ở trong bang hội")?;
            return Ok(());
        }
        if name.len() < 5 || name.len() > 30 {
            ServiceHandles::send_message_alert(player, "Tên bang hội từ 5 đến 30 ký tự")?;
            return Ok(());
        }
        let fee: i64 = 2_000_000;
        if player.inventory.gold < fee {
            ServiceHandles::send_message_alert(
                player,
                &format!("Bạn không đủ vàng, còn thiếu {} vàng",
                    crate::utils::number_util::number_to_money(fee - player.inventory.gold))
            )?;
            return Ok(());
        }

        // Deduct via actor message
        if let Some(h) = session_to_player_handle(player).await {
            h.send_forget(crate::player::player_actor::message::PlayerMessage::Modify(
                Box::new(move |p| { p.inventory.sub_gold(fee); }),
            ));
            player_info_service::send_info_hp_mp_money(player)?;
        }

        // Build clan model
        let mut clan = Clan::new();
        clan.name = name.to_string();
        clan.img_id = img_id as i32;
        clan.create_time = (crate::utils::time::current_time_millis() / 1000) as i32;
        clan.add_member(ClanMember {
            id: player.id as i32,
            name: player.name.clone(),
            head: player.get_head(),
            body: player.get_body(),
            leg: player.get_leg(),
            role: Clan::LEADER,
            power_point: player.n_point.power,
            donate: 0, receive_donate: 0, member_point: 0, clan_point: 0,
            join_time: clan.create_time,
            time_ask_pea: 0,
        });

        // Persist to DB
        use crate::entities::clan as clan_entity;
        let db = DbManager::get_pool();
        let members_json = serde_json::to_string(&clan.members).unwrap_or_default();
        let active_model = clan_entity::ActiveModel {
            name:       Set(clan.name.clone()),
            slogan:     Set(clan.slogan.clone()),
            img_id:     Set(clan.img_id),
            power_point: Set(clan.power_point),
            max_member: Set(clan.max_member as i16),
            level:      Set(clan.level),
            members:    Set(members_json),
            name_2:     Set(clan.name_2.clone()),
            clan_point: Set(clan.capsule_clan),
            create_time: Set(chrono::Local::now()),
            tops:       Set("[]".to_string()),
            ..Default::default()
        };
        let result = active_model.insert(db).await?;
        clan.id = result.id;
        CLAN_MANAGER.add_clan(clan.clone());
        if let Some(h) = session_to_player_handle(player).await {
            let clan_id = clan.id;
            h.send_forget(crate::player::player_actor::message::PlayerMessage::Modify(
                Box::new(move |p| { p.clan_id = clan_id; }),
            ));
            if let Some(clan_handle) = CLAN_MANAGER.get_clan(clan_id) {
                clan_handle.add_member_online(h);
            }
        }

        ServiceHandles::send_message_alert(player, "Chúc mừng bạn đã tạo bang thành công!")?;
        super::info::InfoService::send_my_clan(player).await?;
        Ok(())
    }
}

async fn session_to_player_handle(player: &Player) -> Option<crate::player::player_actor::PlayerHandle> {
    if let Some(ref session) = player.session {
        session.get_player_handle().await
    } else {
        if let Some(session) = crate::network::SESSION_MANAGER.get_session(player.id as i64) {
            return session.get_player_handle().await;
        }
        None
    }
}
