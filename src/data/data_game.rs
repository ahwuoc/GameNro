#![allow(dead_code)]
use crate::network::message::Message;
use crate::network::session::{AsyncSession, SessionArc};
use crate::templates::mob_template_manager;
use crate::templates::npc_template_manager;
use crate::templates::{head_avatar_manager, skill_template_manager};
use crate::templates::{image_by_name_template, map_template_manager};
use dashmap::DashMap;
use dotenv::dotenv;
use fast_image_resize as fir;
use image::ImageFormat;
use std::sync::LazyLock;
use std::env;
use std::fs;

pub use crate::templates::skill_template_manager::{NClass, Skill, SkillTemplate};

pub static CACHE_ICON: LazyLock<DashMap<String, Vec<u8>>> = LazyLock::new(|| DashMap::new());
pub static CACHE_IMG_BY_NAME: LazyLock<DashMap<String, Vec<u8>>> = LazyLock::new(|| DashMap::new());
pub static CACHE_EFFECT: LazyLock<DashMap<String, (Vec<u8>, Vec<u8>)>> = LazyLock::new(|| DashMap::new());

pub struct DataGame;

impl DataGame {
    pub const VS_RES: i32 = 1;
    pub const VS_DATA: i8 = 9;
    pub const VS_MAP: i8 = 2;
    pub const VS_ITEM: i8 = 9;
    pub const VS_SKILL: i8 = 1;
    pub const MAX_SMALL_VS: i16 = 32767;
    pub const STANDARD_LEVELS: [i64; 20] = [
        1000i64,
        3000,
        15000,
        40000,
        90000,
        170000,
        340000,
        700000,
        1500000,
        15000000,
        150000000,
        1500000000,
        5000000000,
        10000000000,
        40000000000,
        50010000000,
        60010000000,
        70010000000,
        80010000000,
        100010000000,
    ];

    pub async fn send_head_to_client(msg: &mut Message) -> anyhow::Result<()> {
        let head_avatars = head_avatar_manager::get_all();
        msg.write_short(head_avatars.len() as i16)?;
        for head in head_avatars.iter() {
            msg.write_short(head.head_id as i16)?;
            msg.write_short(head.avatar_id as i16)?;
        }
        Ok(())
    }
    pub async fn send_size_res(session: &SessionArc) -> anyhow::Result<()> {
        let zoom_level = session.get_zoom_level().await;
        let res_path = format!("data/arc/res/x{}", zoom_level);

        let mut file_count: i32 = 0;
        if let Ok(entries) = std::fs::read_dir(&res_path) {
            for entry in entries {
                if let Ok(entry) = entry {
                    if entry.file_type().map(|ft| ft.is_file()).unwrap_or(false) {
                        file_count += 1;
                    }
                }
            }
        }
        let mut msg = Message::new(-74);
        msg.write_byte(1)?;
        msg.write_short(file_count as i16)?;
        session.transmit(msg);
        Ok(())
    }

    pub async fn send_res(session: &SessionArc) -> anyhow::Result<()> {
        let zoom_level = session.get_zoom_level().await;
        let res_path = format!("data/arc/res/x{}", zoom_level);

        if let Ok(entries) = std::fs::read_dir(&res_path) {
            for entry in entries {
                if let Ok(entry) = entry {
                    if entry.file_type().map(|ft| ft.is_file()).unwrap_or(false) {
                        let file_path = entry.path();
                        if let Some(file_name) = file_path.file_name() {
                            if let Some(name_str) = file_name.to_str() {
                                if let Ok(content) = std::fs::read(&file_path) {
                                    let mut msg = Message::new(-74);
                                    msg.write_byte(2)?;
                                    msg.write_utf(name_str)?;
                                    msg.write_int(content.len() as i32)?;
                                    msg.write(&content)?;
                                    session.transmit(msg);
                                }
                            }
                        }
                    }
                }
            }
        }
        let mut msg = Message::new(-74);
        msg.write_byte(3)?;
        msg.write_int(Self::VS_RES)?;
        session.transmit(msg);

        Ok(())
    }

    pub async fn send_version_res(session: &SessionArc) -> anyhow::Result<()> {
        let mut msg = Message::new(-74);
        msg.write_byte(0)?;
        msg.write_int(Self::VS_RES)?;
        session.transmit(msg);

        Ok(())
    }

    pub async fn send_small_version(session: &SessionArc) -> anyhow::Result<()> {
        let mut msg = Message::new(-77);
        let zoom_level = session.get_zoom_level().await;
        let file_path = format!("data/arc/data_img_version/x{}/img_version", zoom_level);

        match std::fs::read(&file_path) {
            Ok(data) => {
                msg.write(&data)?;
            }
            Err(_) => {
                tracing::warn!("Small version file not found: {}", file_path);
            }
        }

        session.transmit(msg);
        Ok(())
    }

    pub async fn send_version_game(session: &SessionArc) -> anyhow::Result<()> {
        let mut msg = Message::new(-28);
        msg.write_byte(4);
        msg.write_byte(Self::VS_DATA)?;
        msg.write_byte(Self::VS_MAP)?;
        msg.write_byte(Self::VS_SKILL)?;
        msg.write_byte(Self::VS_ITEM)?;
        msg.write_byte(0)?;

        msg.write_byte(Self::STANDARD_LEVELS.len() as i8)?;

        for level in Self::STANDARD_LEVELS {
            msg.write_long(level)?;
        }

        session.transmit(msg);
        Ok(())
    }

    pub async fn send_data_item_bg(session: &SessionArc) -> anyhow::Result<()> {
        let mut msg = Message::new(-31);

        match std::fs::read("data/arc/item_bg_temp/item_bg_data") {
            Ok(data) => {
                msg.write(&data)?;
            }
            Err(_) => {
                tracing::warn!("Item background data file not found");
            }
        }

        session.transmit(msg);
        Ok(())
    }

    pub async fn update_data(session: &SessionArc) -> anyhow::Result<()> {
        tracing::info!("Updating data for client");

        let dart_data = match std::fs::read("data/arc/update_data/dart") {
            Ok(data) => data,
            Err(_) => vec![],
        };

        let arrow_data = match std::fs::read("data/arc/update_data/arrow") {
            Ok(data) => data,
            Err(_) => vec![],
        };

        let effect_data = match std::fs::read("data/arc/update_data/effect") {
            Ok(data) => data,
            Err(_) => vec![],
        };

        let image_data = match std::fs::read("data/arc/update_data/image") {
            Ok(data) => data,
            Err(_) => vec![],
        };

        let part_data = match std::fs::read("data/arc/update_data/part") {
            Ok(data) => data,
            Err(_) => vec![],
        };

        let skill_data = match std::fs::read("data/arc/update_data/skill") {
            Ok(data) => data,
            Err(_) => vec![],
        };

        let mut msg = Message::new(-87);
        msg.write_byte(DataGame::VS_DATA)?;

        msg.write_int(dart_data.len() as i32)?;
        msg.write(&dart_data)?;

        msg.write_int(arrow_data.len() as i32)?;
        msg.write(&arrow_data)?;

        msg.write_int(effect_data.len() as i32)?;
        msg.write(&effect_data)?;

        msg.write_int(image_data.len() as i32)?;
        msg.write(&image_data)?;
        msg.write_int(part_data.len() as i32)?;
        msg.write(&part_data)?;

        msg.write_int(skill_data.len() as i32)?;
        msg.write(&skill_data)?;

        session.transmit(msg);

        tracing::info!("Update data sent successfully");
        Ok(())
    }

    pub async fn update_map(session: &SessionArc) -> anyhow::Result<()> {
        let npc_templates = npc_template_manager::get_all();
        let map_templates = crate::templates::map_template_manager::get_all();
        let mut msg = Message::new(-28);
        msg.write_byte(6)?;
        msg.write_byte(Self::VS_MAP)?;
        msg.write_byte(map_templates.len() as i8)?;

        for template in &map_templates {
            msg.write_utf(&template.name)?;
        }

        msg.write_byte(npc_templates.len() as i8)?;
        for template in &npc_templates {
            msg.write_utf(&template.name)?;
            msg.write_short(template.head)?;
            msg.write_short(template.body)?;
            msg.write_short(template.leg)?;
            msg.write_byte(0)?;
        }
        let mob_templates = mob_template_manager::get_all();
        let mob_count = mob_templates.len() as i8;
        msg.write_byte(mob_count)?;
        for mob_template in mob_templates {
            msg.write_byte(mob_template.r#type as i8)?;
            msg.write_utf(&mob_template.name)?;
            msg.write_int(mob_template.hp)?;
            msg.write_byte(mob_template.range_move as i8)?;
            msg.write_byte(mob_template.speed as i8)?;
            msg.write_byte(mob_template.dart_type as i8)?;
        }
        session.transmit(msg);
        println!(
            "Map data updated successfully with {} maps, {} NPCs, {} mobs",
            map_templates.len(),
            npc_templates.len(),
            mob_count
        );
        Ok(())
    }

    pub async fn update_skill(session: &SessionArc) -> anyhow::Result<()> {
        let mut msg = Message::new(-28);
        msg.write_byte(7)?;
        msg.write_byte(Self::VS_SKILL)?;
        msg.write_byte(0)?;

        let nclasses = skill_template_manager::get_all_nclasses();
        println!(
            "[UPDATE_SKILL] Sending {} nclasses to client",
            nclasses.len()
        );
        msg.write_byte(nclasses.len() as i8)?;

        for nclass in &nclasses {
            msg.write_utf(&nclass.name)?;
            msg.write_byte(nclass.skill_templates.len() as i8)?;
            println!(
                "[UPDATE_SKILL] NClass: {} with {} skill templates",
                nclass.name,
                nclass.skill_templates.len()
            );

            for skill_temp in &nclass.skill_templates {
                msg.write_byte(skill_temp.id)?;
                msg.write_utf(&skill_temp.name)?;
                msg.write_byte(skill_temp.max_point)?;
                msg.write_byte(skill_temp.mana_use_type)?;
                msg.write_byte(skill_temp.r#type)?;
                msg.write_short(skill_temp.icon_id)?;
                msg.write_utf(&skill_temp.dam_info)?;
                msg.write_utf("ahwuocdz")?;
                if skill_temp.id != 0 {
                    msg.write_byte(skill_temp.skills.len() as i8)?;
                    for skill in &skill_temp.skills {
                        msg.write_short(skill.skill_id)?;
                        msg.write_byte(skill.point)?;
                        msg.write_long(skill.pow_require)?;
                        msg.write_short(skill.mana_use)?;
                        msg.write_int(skill.cool_down)?;
                        msg.write_short(skill.dx)?;
                        msg.write_short(skill.dy)?;
                        msg.write_byte(skill.max_fight)?;
                        msg.write_short(skill.damage)?;
                        msg.write_short(skill.price)?;
                        msg.write_utf(&skill.more_info)?;
                    }
                } else {
                    msg.write_byte((skill_temp.skills.len() + 2) as i8)?;
                    for skill in &skill_temp.skills {
                        msg.write_short(skill.skill_id)?;
                        msg.write_byte(skill.point)?;
                        msg.write_long(skill.pow_require)?;
                        msg.write_short(skill.mana_use)?;
                        msg.write_int(skill.cool_down)?;
                        msg.write_short(skill.dx)?;
                        msg.write_short(skill.dy)?;
                        msg.write_byte(skill.max_fight)?;
                        msg.write_short(skill.damage)?;
                        msg.write_short(skill.price)?;
                        msg.write_utf(&skill.more_info)?;
                    }
                    for i in 105..=106 {
                        msg.write_short(i)?;
                        msg.write_byte(0)?;
                        msg.write_long(0)?;
                        msg.write_short(0)?;
                        msg.write_int(0)?;
                        msg.write_short(0)?;
                        msg.write_short(0)?;
                        msg.write_byte(0)?;
                        msg.write_short(0)?;
                        msg.write_short(0)?;
                        msg.write_utf("")?;
                    }
                }
            }
        }

        println!("[UPDATE_SKILL] Finished sending skill data");
        session.transmit(msg);
        Ok(())
    }

    pub async fn send_map_temp(session: &SessionArc, map_id: u8) -> anyhow::Result<()> {
        let file_path = format!("data/arc/map/tile_map_data/{}", map_id);

        match fs::read(&file_path) {
            Ok(data) => {
                if data.len() < 2 {
                    let mut msg = Message::new(-28);
                    msg.write_byte(0)?;
                    msg.write_byte(0)?;
                    session.transmit(msg);
                    return Ok(());
                }

                let tmw = data[0] as usize;
                let tmh = data[1] as usize;
                let expected = 2 + tmw * tmh;

                let to_send: &[u8] = if data.len() >= expected {
                    &data[..expected]
                } else {
                    &data[..]
                };

                let mut msg = Message::new(-28);
                msg.write_byte(10)?;
                msg.write(to_send)?;
                session.transmit(msg);
            }
            Err(_) => {}
        }

        Ok(())
    }

    pub async fn send_skill_data(session: &SessionArc) -> anyhow::Result<()> {
        let mut response = Message::new(-72);
        response.write_byte(0)?;
        session.transmit(response);
        Ok(())
    }

    pub async fn send_item_data(session: &SessionArc) -> anyhow::Result<()> {
        let mut response = Message::new(-73);
        response.write_byte(0)?;
        session.transmit(response);

        Ok(())
    }

    pub async fn send_effect_template(
        session: &SessionArc,
        id: i16,
        id_t: Option<i16>,
    ) -> anyhow::Result<()> {
        let id_t = id_t.filter(|&v| v != 0).unwrap_or(id);

        let zoom_level = session.get_zoom_level().await;
        let version = session.get_version().await;
        let base_zoom = 4;
        let cache_key = format!("{}_{}", zoom_level, id_t);

        if let Some(cached) = CACHE_EFFECT.get(&cache_key) {
            let (eff_data, eff_img) = cached.value().clone();
            let mut msg = Message::new(-66);
            msg.write_short(id)?;
            msg.write_int(eff_data.len() as i32)?;
            msg.write(&eff_data)?;

            if version > 216 {
                msg.write_byte(if id_t == 60 { 2 } else { 1 })?;
            }

            msg.write_int(eff_img.len() as i32)?;
            msg.write(&eff_img)?;
            session.transmit(msg);
            return Ok(());
        }

        let eff_data_path = format!("data/arc/effdata/DataEffect_{}", id_t);
        let eff_img_path = format!("data/arc/effect/x{}/ImgEffect_{}.png", base_zoom, id_t);

        let eff_data = match tokio::fs::read(&eff_data_path).await {
            Ok(v) => v,
            Err(e) => {
                println!("[EFFECT] File not found: {} - {}", eff_data_path, e);
                return Ok(());
            }
        };

        let eff_img_bytes = match tokio::fs::read(&eff_img_path).await {
            Ok(v) => v,
            Err(e) => {
                println!("[EFFECT] Image not found: {} - {}", eff_img_path, e);
                return Ok(());
            }
        };

        let eff_img = if zoom_level as i32 != base_zoom {
            let scale = zoom_level as f32 / base_zoom as f32;
            match Self::scale_png_async(eff_img_bytes, scale).await {
                Ok(v) => v,
                Err(e) => {
                    println!("Error scaling image effect {}: {}", id_t, e);
                    return Ok(());
                }
            }
        } else {
            eff_img_bytes
        };

        CACHE_EFFECT.insert(cache_key, (eff_data.clone(), eff_img.clone()));

        let mut msg = Message::new(-66);
        msg.write_short(id)?;
        msg.write_int(eff_data.len() as i32)?;
        msg.write(&eff_data)?;

        if version > 216 {
            msg.write_byte(if id_t == 60 { 2 } else { 0 })?;
        }

        msg.write_int(eff_img.len() as i32)?;
        msg.write(&eff_img)?;

        session.transmit(msg);
        Ok(())
    }

    pub async fn send_mob_temp(session: &SessionArc, mob_id: i8) -> anyhow::Result<()> {
        let zoom = session.get_zoom_level().await;
        let file_path = format!("data/arc/mob/x{zoom}/{mob_id}");
        let mut msg = Message::new(11);
        match std::fs::read(&file_path) {
            Ok(mob) => {
                msg.write_byte(mob_id)?;
                msg.write(&mob)?;
                session.transmit(msg);
            }
            Err(_) => {
                println!("Warning: Mob temp file not found")
            }
        }
        Ok(())
    }
    pub async fn scale_png_async(bytes: Vec<u8>, scale: f32) -> anyhow::Result<Vec<u8>> {
        tokio::task::spawn_blocking(move || Self::scale_png(&bytes, scale)).await?
    }

    fn scale_png(bytes: &[u8], scale: f32) -> anyhow::Result<Vec<u8>> {
        let img = image::load_from_memory(bytes)?.to_rgba8();
        let (w, h) = img.dimensions();

        let new_w = (w as f32 * scale).round() as u32;
        let new_h = (h as f32 * scale).round() as u32;

        let src = fir::images::Image::from_vec_u8(w, h, img.into_raw(), fir::PixelType::U8x4)?;

        let mut dst = fir::images::Image::new(new_w, new_h, fir::PixelType::U8x4);

        let mut resizer = fir::Resizer::new();
        let options = fir::ResizeOptions::new().resize_alg(fir::ResizeAlg::Nearest);
        resizer.resize(&src, &mut dst, &options)?;

        let out_img = image::RgbaImage::from_raw(new_w, new_h, dst.buffer().to_vec())
            .ok_or_else(|| anyhow::anyhow!("invalid image"))?;

        let mut out = Vec::new();
        out_img.write_to(&mut std::io::Cursor::new(&mut out), ImageFormat::Png)?;

        Ok(out)
    }
    pub async fn send_icon(session: &SessionArc, id: i32) -> anyhow::Result<()> {
        let zoom = session.get_zoom_level().await;
        let base_zoom = 4;
        let key = format!("{}_{}", zoom, id);

        if let Some(icon) = CACHE_ICON.get(&key) {
            let mut msg = Message::new(-67);
            msg.write_int(id)?;
            msg.write_int(icon.len() as i32)?;
            msg.write(&*icon)?;
            session.transmit(msg);
            return Ok(());
        }

        let file_path = format!("data/arc/icon/x{}/{}.png", base_zoom, id);
        let icon_bytes = tokio::fs::read(&file_path).await?;

        let icon = if zoom == base_zoom {
            icon_bytes
        } else {
            let scale = zoom as f32 / base_zoom as f32;
            Self::scale_png_async(icon_bytes, scale).await?
        };

        CACHE_ICON.insert(key, icon.clone());

        let mut msg = Message::new(-67);
        msg.write_int(id)?;
        msg.write_int(icon.len() as i32)?;
        msg.write(&icon)?;
        session.transmit(msg);

        Ok(())
    }

    pub async fn update_item(session: &SessionArc) -> anyhow::Result<()> {
        crate::data::ItemData::update_item(session).await
    }

    pub async fn send_tile_set_info(session: &SessionArc) -> anyhow::Result<()> {
        if let Ok(data) = std::fs::read("data/arc/map/tile_set_info") {
            let mut msg = Message::new(-82);
            msg.write(&data)?;
            session.transmit(msg);
        } else {
            println!("Warning: Tile set info file not found");
        }

        Ok(())
    }

    pub async fn send_client_ok(session: &SessionArc) -> anyhow::Result<()> {
        let mut response = Message::new(-75);
        response.write_byte(0)?;
        session.transmit(response);
        Ok(())
    }

    pub async fn send_link_ip(session: &SessionArc) -> anyhow::Result<()> {
        dotenv().ok();
        let link_data =
            env::var("GAME_LINK").unwrap_or_else(|_| "ArcNro:127.0.0.1:14445:0,0,0".to_string());

        let mut msg = Message::new(-29);
        msg.write_byte(2)?;
        msg.write_utf(&link_data)?;
        msg.write_byte(1)?;
        session.transmit(msg);

        Ok(())
    }

    pub async fn send_image_by_name(session: &SessionArc, img_name: &str) -> anyhow::Result<()> {
        let zoom = session.get_zoom_level().await;
        let base_zoom = 4;
        let n_frame = image_by_name_template::get_n_frame(img_name);
        let file_path = format!("data/arc/img_by_name/x{}/{}.png", base_zoom, img_name);
        let img_bytes = match tokio::fs::read(&file_path).await {
            Ok(v) => v,
            Err(e) => {
                return Ok(());
            }
        };

        let img_data = if zoom as i32 != base_zoom {
            let scale = zoom as f32 / base_zoom as f32;
            match Self::scale_png_async(img_bytes, scale).await {
                Ok(v) => v,
                Err(e) => {
                    return Ok(());
                }
            }
        } else {
            img_bytes
        };

        let mut msg = Message::new(66);
        msg.write_utf(img_name)?;
        msg.write_byte(n_frame)?;
        msg.write_int(img_data.len() as i32)?;
        msg.write(&img_data)?;
        session.transmit(msg);

        Ok(())
    }

    pub async fn send_item_bg_template(session: &SessionArc, id: i16) -> anyhow::Result<()> {
        let zoom = session.get_zoom_level().await;
        let file_path = format!("data/arc/item_bg_temp/x{}/{}.png", zoom, id);

        match tokio::fs::read(&file_path).await {
            Ok(data) => {
                let mut msg = Message::new(-32);
                msg.write_short(id)?;
                msg.write_int(data.len() as i32)?;
                msg.write(&data)?;
                session.transmit(msg);
            }
            Err(_) => {
                // Ignore if not found to avoid spamming
            }
        }
        Ok(())
    }
}
