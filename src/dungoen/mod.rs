use async_trait::async_trait;

use crate::{
    clan::clan_manager::{ClanManager, CLAN_MANAGER},
    constant::menu_enum::MenuId,
    map::{zone_manager::ZONE_MANAGER, ZoneManager},
    npc::handlers::{NpcContext, NpcHandler},
    utils::time::{current_time_millis, get_time_left, is_after_midnight},
};

const DAYS_JOIN_PT: i32 = 0;
const N_PLAYER_MAP: i32 = 0;
const N_PLAYER_CLAN: usize = 1;
const TIME_DOANH_TRAI_SEC: i32 = 1800;
const HUONG_DAN_DOANH_TRAI: &str =
    "1) Trại độc nhãn là nơi các ngươi không nên vào vì những tướng tá rất mạnh. Hahaha";

pub mod doanh_trai;
pub mod redribbon;
pub struct NpcLinhCanhHandler;
#[async_trait]
impl NpcHandler for NpcLinhCanhHandler {
    async fn open_menu(&self, ctx: &NpcContext<'_>) -> anyhow::Result<()> {
        let Some(snap_shot) = ctx.get_player_snapshot().await else {
            return Ok(());
        };
        if snap_shot.clan_id == -1 {
            ctx.npc_chat("Chỉ tiếp các bang hội, miễn tiếp khách vãng lai")?;
            return Ok(());
        }

        let Some(clan_handle) = CLAN_MANAGER.get_clan(snap_shot.clan_id) else {
            ctx.npc_chat("Bang hội không tồn tại")?;
            return Ok(());
        };

        let Some(clan) = clan_handle.get_snapshot().await else {
            ctx.npc_chat("Bang hội không tồn tại")?;
            return Ok(());
        };

        // 2. Check >= 5 members
        if clan.members.len() < N_PLAYER_CLAN {
            ctx.npc_chat("Bang hội phải có ít nhất 5 thành viên mới có thể tham gia")?;
            return Ok(());
        }

        // 3. Check join time >= 1 ngày
        let now_seconds = (current_time_millis() / 1000) as i32;
        if let Some(m) = clan.members.iter().find(|m| m.id == snap_shot.id as i32) {
            let days_since_join = (now_seconds - m.join_time) / 86400;
            if days_since_join < DAYS_JOIN_PT {
                let text = format!(
                    "Gia nhập bang hội trên {} ngày mới được tham gia",
                    DAYS_JOIN_PT
                );
                ctx.npc_chat(&text)?;
                ctx.hide_wait_dialog()?;
                return Ok(());
            }
        }
        if clan.doanh_trai_id.is_some() {
            let time_left = get_time_left(clan.last_time_open_doanh_trai, TIME_DOANH_TRAI_SEC);
            let text = format!(
                "Bang hội của ngươi đang đánh trại độc nhãn\nThời gian còn lại là {}. Ngươi có muốn tham gia không?",
                time_left
            );
            ctx.create_menu(
                &text,
                vec!["Tham gia", "Không", "Hướng\ndẫn\nthêm"],
                MenuId::MenuJoinDoanhTrai,
            )
            .await?;
            return Ok(());
        }

        let Some(zone) = ZONE_MANAGER.get_zone(snap_shot.map_id, snap_shot.zone_id) else {
            return Ok(());
        };
        let players = zone.get_all_players().await?;
        let mut teammate_count = 0;
        for ph in players.iter() {
            if ph.id == snap_shot.id {
                continue;
            }
            if let Some(other) = ph.get_snapshot().await {
                if other.clan_id == snap_shot.clan_id
                    && other.location.x >= 1285
                    && other.location.x <= 1645
                {
                    teammate_count += 1;
                }
            }
        }

        if teammate_count < N_PLAYER_MAP {
            let text = format!(
                "Ngươi phải có ít nhất {} đồng đội cùng bang đứng gần mới có thể vào\ntuy nhiên ta khuyên ngươi nên đi cùng với 3-4 người để khỏi chết. Hahaha.",
                N_PLAYER_MAP
            );
            ctx.create_menu(&text, vec!["OK", "Hướng\ndẫn\nthêm"], MenuId::IgnoreMenu)
                .await?;
            return Ok(());
        }
        if let Some(clan_handle2) = CLAN_MANAGER.get_clan(snap_shot.clan_id) {
            if let Some(clan2) = clan_handle2.get_snapshot().await {
                if clan2.have_gone_doanh_trai && !is_after_midnight(clan2.last_time_open_doanh_trai)
                {
                    ctx.npc_chat(
                        "Bang hội của ngươi ngày hôm nay đã vào 1 lần rồi.\nHãy chờ đến ngày mai để có thể vào miễn phí",
                    )?;
                    return Ok(());
                }
            }
        }

        // 7. Menu chính — mở mới
        ctx.create_menu(
            "Hôm nay bang hội của ngươi chưa vào trại lần nào. Ngươi có muốn vào\nkhông?\nĐể vào, ta khuyên ngươi nên có 3-4 người cùng bang đi cùng.",
            vec!["Vào\n(miễn phí)", "Không", "Hướng\ndẫn\nthêm"],
            MenuId::MenuJoinDoanhTrai,
        )
        .await?;

        Ok(())
    }

    async fn handle_menu(
        &self,
        ctx: &NpcContext<'_>,
        menu_id: MenuId,
        select: i8,
    ) -> anyhow::Result<()> {
        match menu_id {
            MenuId::MenuJoinDoanhTrai => {
                if select == 0 {
                    let Some(snap) = ctx.get_player_snapshot().await else {
                        return Ok(());
                    };
                    let Some(ref player_handle) = ctx.player_handle else {
                        return Ok(());
                    };

                    // Gather teammate handles cùng bang đứng gần NPC
                    let mut teammates = Vec::new();
                    if let Some(zone) = ZONE_MANAGER.get_zone(snap.map_id, snap.zone_id) {
                        if let Ok(players) = zone.get_all_players().await {
                            for ph in players.iter() {
                                if ph.id == snap.id {
                                    continue;
                                }
                                if let Some(other) = ph.get_snapshot().await {
                                    if other.clan_id == snap.clan_id
                                        && other.location.x >= 1285
                                        && other.location.x <= 1645
                                    {
                                        teammates.push(ph.clone());
                                    }
                                }
                            }
                        }
                    }

                    let manager = doanh_trai::manager::get();
                    match manager
                        .join_doanh_trai(snap.clan_id, player_handle.clone(), teammates)
                        .await
                    {
                        Ok(()) => {}
                        Err(msg) => {
                            ctx.npc_chat(&msg)?;
                        }
                    }
                } else if select == 2 {
                    ctx.npc_chat(HUONG_DAN_DOANH_TRAI)?;
                }
            }
            MenuId::IgnoreMenu => {
                if select == 1 {
                    ctx.npc_chat(HUONG_DAN_DOANH_TRAI)?;
                }
            }
            _ => {}
        }
        Ok(())
    }
}
