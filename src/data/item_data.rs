use crate::network::message::Message;
use crate::network::session::AsyncSession;
use crate::services::GodGK;
use sea_orm::EntityTrait;

pub struct ItemData;

impl ItemData {
    pub async fn update_item(session: &mut AsyncSession) -> anyhow::Result<()> {
        println!("ItemData::update_item called");
        Self::update_item_option_template(session).await?;
        Self::update_item_arr_head_2_f(session).await?;
        Self::update_item_template(session, 750).await?;
        Self::update_item_template_range(session, 750, 751).await?;
        Ok(())
    }

    async fn update_item_option_template(
        session: &mut AsyncSession,
    ) -> anyhow::Result<()> {
        println!("Updating item option templates");
        let mut msg = Message::new(-28);
        msg.write_byte(8)?; // sub-command
        msg.write_byte(1)?; // vsItem version
        msg.write_byte(0)?; // update option

        let manager = crate::services::manager::Manager::get_instance();
        let options = {
            let manager_guard = manager.lock().unwrap();
            manager_guard.item_option_templates.clone()
        };

        let options_count = (options.len().min(255)) as u8;
        msg.write_byte(options_count as i8)?;

        for opt in options.iter().take(options_count as usize) {
            msg.write_utf(&opt.name)?;
            msg.write_byte(0)?; // Assuming type is 0 for now
        }

        session.send_message(&msg).await?;
        println!("Sent {} item option templates from cache", options_count);
        Ok(())
    }

    async fn update_item_arr_head_2_f(
        session: &mut AsyncSession,
    ) -> anyhow::Result<()> {
        println!("Updating item array head 2 frames");
        let mut msg = Message::new(-28);
        msg.write_byte(8)?; // sub-command
        msg.write_byte(1)?; // vsItem version
        msg.write_byte(100)?; // update ArrHead2F

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
                    // Try parse JSON array first, else comma-separated
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
        println!("Sent {} array head 2 frames", arrays.len());
        Ok(())
    }

    async fn update_item_template(
        session: &mut AsyncSession,
        count: i16,
    ) -> anyhow::Result<()> {
        println!("Updating item templates (count: {})", count);
        let mut msg = Message::new(-28);
        msg.write_byte(8)?; // sub-command
        msg.write_byte(1)?; // vsItem version
        msg.write_byte(1)?; // reload itemtemplate

        let item_manager = crate::item::item_manager::ITEM_MANAGER.read().await;
        let items = item_manager.get_all_templates().await;
        let count = (items.len().min(count as usize)) as i16;

        msg.write_short(count)?;

        for it in items.iter().take(count as usize) {
            msg.write_byte((it.r#type as u8) as i8)?;
            msg.write_byte((it.gender as u8) as i8)?;
            msg.write_utf(&it.name)?;
            msg.write_utf(&it.description)?;
            msg.write_byte(0)?;
            msg.write_int(it.power_require as i32)?;
            msg.write_short(it.icon_id as i16)?;
            msg.write_short(it.part as i16)?;
            msg.write_boolean(it.is_up_to_up != 0)?;
        }

        session.send_message(&msg).await?;
        println!("Sent {} item templates from cache", count);
        Ok(())
    }

    async fn update_item_template_range(
        session: &mut AsyncSession,
        start: i16,
        end: i16,
    ) -> anyhow::Result<()> {
        println!("Updating item templates range ({} to {})", start, end);
        let mut msg = Message::new(-28);
        msg.write_byte(8)?; // sub-command
        msg.write_byte(1)?; // vsItem version
        msg.write_byte(2)?; // add itemtemplate
        msg.write_short(start)?;
        msg.write_short(end)?;

        let manager = crate::services::manager::Manager::get_instance();
        let items_by_id = {
            let manager_guard = manager.lock().unwrap();
            manager_guard.item_templates_by_id.clone()
        };

        let mut items_sent = 0;
        for id in start..end {
            if let Some(item) = items_by_id.get(&(id as i32)) {
                msg.write_byte((item.r#type as u8) as i8)?;
                msg.write_byte((item.gender as u8) as i8)?;
                msg.write_utf(&item.name)?;
                msg.write_utf(&item.description)?;
                msg.write_byte(0)?;
                msg.write_int(item.power_require as i32)?;
                msg.write_short(item.icon_id as i16)?;
                msg.write_short(item.part as i16)?;
                msg.write_boolean(item.is_up_to_up != 0)?;
                items_sent += 1;
            }
        }

        session.send_message(&msg).await?;
        println!(
            "Sent {} additional item templates from cache",
            items_sent
        );
        Ok(())
    }
}
