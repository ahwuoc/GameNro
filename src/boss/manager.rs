use crate::entities::player;
use crate::map::map_manager::MAP_MANAGER;
use crate::map::map_service;
use crate::map::zone_manager::ZONE_MANAGER;
use crate::network::message::Message;
use crate::player::components::boss::BossComponent;
use crate::player::player::Player;
use crate::player::player_actor::handle::PlayerHandle;
use crate::player::player_actor::PlayerMessage;
use crate::player::player_manager::PLAYER_MANAGER;
use crate::templates::boss_template_manager;
use crate::{boss::boss_actor::BossActor, player::player_actor::TypePk};
use tokio::sync::mpsc;
use tracing::{error, info};

pub struct BossManager;

impl BossManager {
    pub async fn show_list_boss(player_id: u64, version: i32) -> anyhow::Result<()> {
        let Some(player_handle) = PLAYER_MANAGER.get(player_id) else {
            return Err(anyhow::anyhow!("Player {} not found", player_id));
        };
        let boss_handle: Vec<PlayerHandle> = PLAYER_MANAGER
            .iter()
            .filter(|b| b.value().boss_info.is_some())
            .map(|e| e.value().clone())
            .collect();
        let mut active_boss = Vec::new();
        for handle in boss_handle {
            if let Some(boss) = handle.get_snapshot().await {
                if !map_service::is_map_black_ball_war(boss.map_id)
                    && !map_service::is_map_boss_final(boss.map_id)
                    && !map_service::is_map_huy_diet(boss.map_id)
                    && !map_service::is_map_yardart(boss.map_id)
                    && !map_service::is_map_ma_bu(boss.map_id)
                {
                    active_boss.push(boss);
                }
            }
        }
        let mut msg = Message::new(-96);
        msg.write_byte(0)?;
        msg.write_utf("Danh Sách Boss")?;
        msg.write_byte(active_boss.len() as i8)?;
        for (i, b) in active_boss.iter().enumerate() {
            msg.write_int(i as i32)?;
            msg.write_int(i as i32)?;
            msg.write_short(b.get_head())?;
            if version >= 214 {
                msg.write_short(-1)?;
            }
            msg.write_short(b.get_body())?;
            msg.write_short(b.get_leg())?;
            msg.write_utf(&b.name)?;
            msg.write_utf("Đang sống")?;

            let map_name = MAP_MANAGER
                .find_by_id(b.map_id)
                .map(|m| m.info.name.clone())
                .unwrap_or_else(|| "Unknown".to_string());
            msg.write_utf(&format!("{}({}) khu {}", map_name, b.map_id, b.zone_id))?;
        }
        player_handle.send(PlayerMessage::SendPacket(msg)).await?;
        Ok(())
    }
    pub async fn init_boss() {
        use rand::seq::IndexedRandom;
        use std::collections::HashSet;

        let templates = boss_template_manager::get_all();
        info!("Initializing {} boss templates...", templates.len());

        let child_boss_ids: HashSet<String> = templates
            .iter()
            .flat_map(|t| t.stages.0.iter())
            .flat_map(|s| s.together.iter())
            .cloned()
            .collect();

        if !child_boss_ids.is_empty() {
            info!(
                "Found {} child boss IDs to skip: {:?}",
                child_boss_ids.len(),
                child_boss_ids
            );
        }

        for template in templates {
            if child_boss_ids.contains(&template.id) {
                tracing::debug!("Boss {} is a child boss, skipping init", template.id);
                continue;
            }

            let map_join = &template.map_join.0;
            if map_join.is_empty() {
                tracing::debug!("Boss {} has no map_join, skipping", template.id);
                continue;
            }

            let Some(&map_id) = map_join.choose(&mut rand::rng()) else {
                continue;
            };

            let zones = ZONE_MANAGER.get_zones_for_map(map_id);
            if zones.is_empty() {
                tracing::warn!(
                    "Boss {} - No zone found for map {}, skipping",
                    template.id,
                    map_id
                );
                continue;
            }
            let Some(zone) = zones.choose(&mut rand::rng()) else {
                tracing::warn!(
                    "Boss {} - No valid zone selected for map {}, skipping",
                    template.id,
                    map_id
                );
                continue;
            };

            let x = rand::random_range(100..400);
            let y = rand::random_range(300..400);

            info!(
                "Spawning boss {} on map {} zone {} at ({}, {})",
                template.id, map_id, zone.zone_id, x, y
            );

            Self::spawn_boss_async(
                template.id.clone(),
                map_id,
                zone.zone_id,
                x,
                y,
                None,
                -1,
                Vec::new(),
                None,
            );
        }
    }
    pub async fn spawn_boss(
        template_id: &str,
        map_id: i32,
        zone_id: i32,
        x: i16,
        y: i16,
        group_id: Option<u64>,
        group_index: i32,
        sequence: Vec<String>,
        attacker_player_id: Option<u64>,
    ) -> anyhow::Result<()> {
        let Some(template) = boss_template_manager::get(template_id) else {
            return Err(anyhow::anyhow!("Boss template {} not found", template_id));
        };

        let Some(zone) = ZONE_MANAGER.get_zone(map_id, zone_id) else {
            return Err(anyhow::anyhow!("Zone {}-{} not found", map_id, zone_id));
        };
        static BOSS_ID_COUNTER: std::sync::atomic::AtomicI64 =
            std::sync::atomic::AtomicI64::new(-1000000);
        let boss_id = BOSS_ID_COUNTER.fetch_sub(1, std::sync::atomic::Ordering::Relaxed) as u64;

        let mut player = Player::new(boss_id, template.name.clone(), 0);
        player.is_boss = true;
        player.map_id = map_id;
        player.zone_id = zone_id;
        player.location.x = x;
        player.location.y = y;
        player.type_pk = TypePk::PkNon;

        let mut boss_component = BossComponent::new();
        boss_component.group_id = group_id;
        boss_component.group_index = group_index;
        boss_component.sequence = sequence;
        player.boss_component = Some(boss_component);

        tracing::debug!("Boss stages count: {}", template.stages.0.len());
        if let Some(stage) = template.stages.0.get(0) {
            if let Some(stage_name) = &stage.name {
                player.name = stage_name.clone();
            }
            tracing::debug!(
                "Initializing Boss from Stage 0: hp={}, mp={}, dame={}, skills={:?}",
                stage.hp,
                stage.mp,
                stage.dame,
                stage.skills
            );
            player.n_point.hp_max = stage.hp;
            player.n_point.hp_current = stage.hp;
            player.n_point.mp_max = stage.mp;
            player.n_point.mp_current = stage.mp;
            player.n_point.dame = stage.dame;
            if stage.outfit.len() >= 3 {
                player.head = stage.outfit[0];
                player.body = stage.outfit[1];
                player.leg = stage.outfit[2];
            }

            // Thêm skill ban đầu
            for skill_info in &stage.skills {
                if skill_info.is_empty() {
                    continue;
                }
                let skill_template_id = skill_info[0];
                let level = skill_info.get(1).cloned().unwrap_or(1);

                let sk =
                    match crate::utils::skill_util::create_skill(skill_template_id, level).await {
                        Some(s) => Some(s),
                        None => {
                            tracing::warn!(
                                "Skill {} level {} not found, trying level 1",
                                skill_template_id,
                                level
                            );
                            crate::utils::skill_util::create_skill(skill_template_id, 1).await
                        }
                    };

                if let Some(sk) = sk {
                    tracing::info!(
                        "Added skill {} (level {}) to boss {}",
                        sk.template_id,
                        sk.point,
                        boss_id
                    );
                    player.player_skill.skills.push(sk);
                } else {
                    tracing::error!(
                        "FAILED to create skill {} for boss {}",
                        skill_template_id,
                        boss_id
                    );
                }
            }
        } else {
            tracing::error!("Boss template {} HAS NO STAGES!", template_id);
        }

        tracing::info!(
            "Boss {} initialized with {} skills",
            boss_id,
            player.player_skill.skills.len()
        );

        if let Some(stage) = template.stages.0.get(0) {
            if template.r#type == "group" && !stage.together.is_empty() {
                let my_group_id = group_id.unwrap_or(boss_id);
                if let Some(boss_comp) = &mut player.boss_component {
                    boss_comp.group_id = Some(my_group_id);
                    if group_index == -1 {
                        boss_comp.group_index = 99;
                    }
                }

                for (idx, sub_boss_id) in stage.together.iter().enumerate() {
                    let sub_x = x + rand::random_range(-50..50);
                    Self::spawn_boss_async(
                        sub_boss_id.clone(),
                        map_id,
                        zone_id,
                        sub_x,
                        y,
                        Some(my_group_id),
                        idx as i32,
                        Vec::new(),
                        None,
                    );
                }
            } else if template.r#type == "sequence" {
                if let Some(boss_comp) = &mut player.boss_component {
                    if boss_comp.sequence.is_empty() {
                        boss_comp.sequence = stage.together.clone();
                    }
                }
            }
        }

        let (tx, rx) = mpsc::channel(100);
        let mut handle = PlayerHandle::new(boss_id, false, tx);

        if let Some(comp) = &player.boss_component {
            handle.boss_info = Some(crate::player::player_actor::handle::BossInfo {
                group_id: comp.group_id,
                group_index: comp.group_index,
            });
        }

        let mut boss_actor = BossActor::new(
            player,
            template_id.to_string(),
            zone.clone(),
            rx,
            attacker_player_id,
        );

        PLAYER_MANAGER.add(boss_id, handle.clone());

        zone.add_player(handle).await?;
        zone.load_me_to_another(boss_id).await?;

        let boss_name = template.name.clone();
        let template_id_cloned = template_id.to_string();
        tokio::spawn(async move {
            info!(
                "Boss spawned: {} at map {}, zone {}",
                boss_name, map_id, zone_id
            );
            boss_actor.run().await;
            info!("Boss actor terminated for: {}", boss_id);
            PLAYER_MANAGER.remove(boss_id);
        });

        Ok(())
    }

    pub fn spawn_boss_async(
        template_id: String,
        map_id: i32,
        zone_id: i32,
        x: i16,
        y: i16,
        group_id: Option<u64>,
        group_index: i32,
        sequence: Vec<String>,
        attacker_player_id: Option<u64>,
    ) {
        tokio::spawn(async move {
            if let Err(e) = Self::spawn_boss(
                &template_id,
                map_id,
                zone_id,
                x,
                y,
                group_id,
                group_index,
                sequence,
                attacker_player_id,
            )
            .await
            {
                error!("Error spawning boss {}: {}", template_id, e);
            }
        });
    }
}
