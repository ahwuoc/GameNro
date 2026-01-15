use std::any::Any;

use crate::data::data_game;
use crate::item::{item_template_manager, option_template_manager};
use crate::network::message::Message;
use crate::network::session::AsyncSession;
use crate::services::GodGK;
use sea_orm::EntityTrait;
pub struct ItemData;

impl ItemData {
    pub async fn update_item(session: &mut AsyncSession) -> anyhow::Result<()> {
        Self::update_item_option_template(session).await?;
        Self::update_item_arr_head_2_f(session).await?;
        Self::update_item_template(session, 750).await?;
        Self::update_item_template_range(
            session,
            750,
            item_template_manager::get_all().len() as i16,
        )
        .await?;
        Ok(())
    }
    async fn update_item_option_template(session: &mut AsyncSession) -> anyhow::Result<()> {
        let mut msg = Message::new(-28);
        msg.write_byte(8)?;
        msg.write_byte(data_game::DataGame::VS_ITEM)?;
        msg.write_byte(0)?;
        msg.write_byte(option_template_manager::get_all().len() as i8)?;
        for opt in option_template_manager::get_all().iter() {
            println!("send client option => {} {}", opt.id, opt.name);
            msg.write_utf(&opt.name)?;
            msg.write_byte(0)?;
        }
        session.send_message(&msg).await?;
        Ok(())
    }

    async fn update_item_arr_head_2_f(session: &mut AsyncSession) -> anyhow::Result<()> {
        let mut msg = Message::new(-28);
        msg.write_byte(8)?;
        msg.write_byte(data_game::DataGame::VS_ITEM)?;
        msg.write_byte(50 as i8)?;

        let god_gk = GodGK::get_instance();
        let db = {
            let god_gk_guard = god_gk.lock().unwrap();
            god_gk_guard.db.clone()
        };

        let mut arrays: Vec<Vec<i16>> = Vec::new();

        if let Some(db) = db {
            if let Ok(arrs) = crate::entities::array_head_2_frames::Entity::find()
                .all(&db)
                .await
            {
                for a in arrs {
                    let parsed: Vec<i16> =
                        if let Ok(json_val) = serde_json::from_str::<serde_json::Value>(&a.data) {
                            if let Some(arr) = json_val.as_array() {
                                arr.iter()
                                    .filter_map(|v| v.as_i64().map(|x| x as i16))
                                    .collect()
                            } else {
                                Vec::new()
                            }
                        } else {
                            a.data
                                .split([',', ' '])
                                .filter_map(|s| s.parse::<i16>().ok())
                                .collect()
                        };
                    arrays.push(parsed);
                }
            }
        }

        msg.write_short(arrays.len() as i16)?;
        for arr in &arrays {
            msg.write_byte((arr.len().min(255)) as i8)?;
            for val in arr.iter().take(255) {
                msg.write_short(*val)?;
            }
        }
        session.send_message(&msg).await?;
        Ok(())
    }

    async fn update_item_template(session: &mut AsyncSession, count: i16) -> anyhow::Result<()> {
        let mut msg = Message::new(-28);
        msg.write_byte(8)?; // sub-command
        msg.write_byte(data_game::DataGame::VS_ITEM)?; // vsItem version
        msg.write_byte(1)?; // reload itemtemplate
        msg.write_short(count)?;
        for id in 0..count {
            if let Some(item_template) = item_template_manager::get(id) {
                msg.write_byte(item_template.r#type as i8)?;
                msg.write_byte(item_template.gender as i8)?;
                msg.write_utf(&item_template.name)?;
                msg.write_utf(&item_template.description)?;
                msg.write_byte(0)?;
                msg.write_int(item_template.power_require)?;
                msg.write_short(item_template.icon_id as i16)?;
                msg.write_short(item_template.part as i16)?;
                msg.write_boolean(item_template.is_up_to_up != 0)?;
            }
        }
        session.send_message(&msg).await?;
        Ok(())
    }

    async fn update_item_template_range(
        session: &mut AsyncSession,
        start: i16,
        end: i16,
    ) -> anyhow::Result<()> {
        let mut msg = Message::new(-28);
        msg.write_byte(8)?; // sub-command
        msg.write_byte(data_game::DataGame::VS_ITEM)?; // vsItem version
        msg.write_byte(2)?; // add itemtemplate
        msg.write_short(start)?;
        msg.write_short(end)?;

        for id in start..end {
            if let Some(item) = item_template_manager::get(id) {
                msg.write_byte(item.r#type as i8)?;
                msg.write_byte(item.gender as i8)?;
                msg.write_utf(&item.name)?;
                msg.write_utf(&item.description)?;
                msg.write_byte(0)?;
                msg.write_int(item.power_require as i32)?;
                msg.write_short(item.icon_id as i16)?;
                msg.write_short(item.part as i16)?;
                msg.write_boolean(item.is_up_to_up != 0)?;
            }
        }
        session.send_message(&msg).await?;
        Ok(())
    }
}
