use crate::constant::menu_enum::MenuId;
use crate::matches::dhvt::constants::*;
use crate::matches::dhvt::manager::get_dhvt_handle;
use crate::npc::handlers::{NpcContext, NpcHandler};
use crate::player::player_actor::PlayerMessage;
use crate::player::Player;
use crate::services::ServiceHandles;
use async_trait::async_trait;
use chrono::{Local, Timelike};

pub struct GhiDanhHandler;

// ─── Map 52: DHVT (World Martial Arts Tournament) ───

#[async_trait]
impl NpcHandler for GhiDanhHandler {
    async fn open_menu(&self, ctx: &NpcContext<'_>) -> anyhow::Result<()> {
        let snapshot = match ctx.get_player_snapshot().await {
            Some(p) => p,
            None => return Ok(()),
        };

        match snapshot.map_id {
            MAP_PHONG_CHO => self.open_menu_map52(ctx, &snapshot).await,
            MAP_DHVT_23 => self.open_menu_map129(ctx, &snapshot).await,
            _ => {
                ctx.hide_wait_dialog()?;
                Ok(())
            }
        }
    }

    async fn handle_menu(
        &self,
        ctx: &NpcContext<'_>,
        menu_id: MenuId,
        select: i8,
    ) -> anyhow::Result<()> {
        let snapshot = match ctx.get_player_snapshot().await {
            Some(p) => p,
            None => return Ok(()),
        };

        match snapshot.map_id {
            MAP_PHONG_CHO => self.confirm_map52(ctx, &snapshot, menu_id, select).await,
            MAP_DHVT_23 => self.confirm_map129(ctx, &snapshot, menu_id, select).await,
            _ => Ok(()),
        }
    }
}

impl GhiDanhHandler {
    // ═══════════════════════════════════════════════
    // Map 52: Đại Hội Võ Thuật (DHVT PvP)
    // ═══════════════════════════════════════════════

    async fn open_menu_map52(
        &self,
        ctx: &NpcContext<'_>,
        snapshot: &crate::player::Player,
    ) -> anyhow::Result<()> {
        let dhvt = get_dhvt_handle();
        let info = dhvt.get_info(snapshot.id as i64).await;

        // Player đang chờ vòng sau
        if info.round != 0 && info.is_in_wait_list {
            ctx.npc_chat(&format!(
                "Bạn được vào vòng {}\nTrận tiếp theo sắp diễn ra, hãy đợi tại đây",
                info.round + 1
            ))?;
            ctx.hide_wait_dialog()?;
            return Ok(());
        }

        let say = say_text(info.can_reg, info.tournament, info.reg_count, info.hour);
        let mut menu_options: Vec<&str> = vec!["Thông tin\nChi tiết"];
        if info.can_reg {
            if !info.is_registered {
                menu_options.push("Dang ky");
            } else {
                menu_options.push("Huy\nDang ky");
            }
        } else {
            menu_options.push("Giai\nSieu Hang");
            menu_options.push("Dai Hoi\nVo Thuat\nLan thu\n23");
            menu_options.push("Dong");
        }

        ctx.create_menu(&say, menu_options, MenuId::BaseMenu)
            .await?;
        Ok(())
    }

    async fn confirm_map52(
        &self,
        ctx: &NpcContext<'_>,
        snapshot: &crate::player::Player,
        menu_id: MenuId,
        select: i8,
    ) -> anyhow::Result<()> {
        let dhvt = get_dhvt_handle();
        let info = dhvt.get_info(snapshot.id as i64).await;

        match menu_id {
            MenuId::DhvtConfirm => {
                // Xác nhận đăng ký
                if select == 0 && info.can_reg {
                    self.do_register(ctx, snapshot).await?;
                }
            }
            MenuId::BaseMenu => {
                match select {
                    0 => {
                        // Thông tin chi tiết
                        ctx.npc_chat(
                            "Đại hội võ thuật diễn ra hàng giờ từ 8h đến 23h\n\
                             Có 5 hạng: Nhi đồng, Siêu cấp 1-3, Ngoại hạng\n\
                             Đăng ký trước phút 25, thi đấu từ phút 30\n\
                             Thắng vô địch sẽ nhận phần thưởng giá trị",
                        )?;
                    }
                    1 => {
                        if info.can_reg {
                            if !info.is_registered {
                                let cost = info.tournament.register_cost();
                                let fee_text = match cost {
                                    CostType::Gem(_) | CostType::Gold(_) => format!(
                                        "Giải\n{}\n({})",
                                        info.tournament.get_name(),
                                        cost.get_text()
                                    ),
                                };
                                ctx.create_menu(
                                    &format!(
                                        "Hiện đang có giải đấu {} bạn có muốn đăng ký không?",
                                        info.tournament.get_name()
                                    ),
                                    vec![&fee_text, "Từ chối"],
                                    MenuId::DhvtConfirm,
                                )
                                .await?;
                            } else {
                                // Hủy đăng ký
                                self.do_unregister(ctx, snapshot).await?;
                            }
                        } else {
                            // Chuyển map Siêu Hạng
                            ctx.change_map_by_spaceship(MAP_SIEU_HANG, snapshot.location.x, 360)
                                .await?;
                        }
                    }
                    2 => {
                        if info.can_reg {
                            // Chuyển map Siêu Hạng
                            ctx.change_map_by_spaceship(MAP_SIEU_HANG, snapshot.location.x, 360)
                                .await?;
                        } else {
                            // Chuyển map ĐHVT 23
                            ctx.change_map_by_spaceship(MAP_DHVT_23, snapshot.location.x, 360)
                                .await?;
                        }
                    }
                    3 => {
                        if info.can_reg {
                            // Chuyển map ĐHVT 23
                            ctx.change_map_by_spaceship(MAP_DHVT_23, snapshot.location.x, 360)
                                .await?;
                        }
                    }
                    _ => {}
                }
            }
            _ => {}
        }

        Ok(())
    }

    /// Đăng ký DHVT (map 52)
    async fn do_register(
        &self,
        ctx: &NpcContext<'_>,
        snapshot: &crate::player::Player,
    ) -> anyhow::Result<()> {
        let dhvt = get_dhvt_handle();
        let player_id = snapshot.id as i64;
        let player_name = snapshot.name.clone();

        // Check đã vô địch hôm nay
        if dhvt.is_champion(&player_name).await {
            ctx.npc_chat(TEXT_DA_VO_DICH)?;
            return Ok(());
        }

        let info = dhvt.get_info(player_id).await;
        let cost = info.tournament.register_cost();
        match cost {
            CostType::Gem(amt) => {
                if snapshot.inventory.get_gem() < amt {
                    ctx.npc_chat(&format!(
                        "Bạn không đủ ngọc, còn thiếu {} ngọc nữa",
                        amt - snapshot.inventory.get_gem()
                    ))?;
                    return Ok(());
                }
                let dhvt_clone = dhvt.clone();
                ctx.send_player_message(PlayerMessage::Modify(Box::new(move |player| {
                    if player.inventory.sub_gem(amt) {
                        let _ = ServiceHandles::send_gold_gem_ruby_to_client(player);
                        dhvt_clone.register(player.id as i64);
                    }
                })));
            }
            CostType::Gold(amt) => {
                if snapshot.inventory.get_gold() < amt as i64 {
                    ctx.npc_chat(&format!(
                        "Bạn không đủ thỏi vàng, còn thiếu {} thỏi vàng nữa",
                        amt - snapshot.inventory.get_gold() as i32
                    ))?;
                    return Ok(());
                }
                let dhvt_clone = dhvt.clone();
                ctx.send_player_message(PlayerMessage::Modify(Box::new(move |player| {
                    if player.inventory.sub_gold(amt as i64) {
                        let _ = ServiceHandles::send_gold_gem_ruby_to_client(player);
                        dhvt_clone.register(player.id as i64);
                    }
                })));
            }
        }

        let now = Local::now();
        let text = TEXT_DANG_KY_THANH_CONG
            .replace("%1", &now.hour().to_string())
            .replace("%2", &format!("{}h{}", now.hour(), now.minute()));
        ctx.npc_chat(&text)?;

        Ok(())
    }

    /// Hủy đăng ký DHVT (map 52)
    async fn do_unregister(
        &self,
        ctx: &NpcContext<'_>,
        snapshot: &crate::player::Player,
    ) -> anyhow::Result<()> {
        let dhvt = get_dhvt_handle();
        dhvt.unregister(snapshot.id as i64);
        ctx.npc_chat(TEXT_HUY_DANG_KY)?;
        Ok(())
    }

    // ═══════════════════════════════════════════════
    // Map 129: Đại Hội Võ Thuật Lần Thứ 23 (DHVT23)
    // ═══════════════════════════════════════════════

    async fn open_menu_map129(
        &self,
        ctx: &NpcContext<'_>,
        snapshot: &crate::player::Player,
    ) -> anyhow::Result<()> {
        // TODO: Khi implement DHVT23 boss system
        // - Reset daily: goldChallenge=50000, rubyChallenge=2, levelWoodChest=0
        // - Menu phụ thuộc vào levelWoodChest (0 vs >0)
        // Tạm thời hiện menu cơ bản
        ctx.create_menu(
            "Đại hội võ thuật lần thứ 23\n\
             Diễn ra bất kể ngày đêm, ngày nghỉ, ngày lễ\n\
             Phần thưởng vô cùng quý giá\n\
             Nhanh chóng tham gia nào",
            vec![
                "Hướng\ndẫn\nthêm",
                "Thi đấu\n2 ngọc",
                "Thi đấu\n50,000 vàng",
                "Về\nĐại Hội\nVõ Thuật",
            ],
            MenuId::DhvtMenu129,
        )
        .await?;
        Ok(())
    }

    async fn confirm_map129(
        &self,
        ctx: &NpcContext<'_>,
        snapshot: &crate::player::Player,
        menu_id: MenuId,
        select: i8,
    ) -> anyhow::Result<()> {
        match menu_id {
            MenuId::DhvtMenu129 => {
                match select {
                    0 => {
                        // Hướng dẫn thêm
                        ctx.npc_chat(
                            "Đại hội quy tụ nhiều cao thủ như là Jacky Chun, Thiên Xin Hăng, Tàu Bảy Bảy...\n\
                             Phần thưởng là 1 rương gỗ chứa nhiều vật phẩm giá trị.\n\
                             Khi hạ được 1 đối thủ, phần thưởng sẽ nâng lên 1 cấp.\n\
                             Rương càng cao cấp, vật phẩm trong đó càng giá trị hơn.",
                        )?;
                    }
                    1 => {
                        // Thi đấu bằng ngọc
                        // TODO: Implement The23rdMartialArtCongress boss-rush system
                        ctx.npc_chat("Chức năng Đại Hội Võ Thuật lần 23 đang được phát triển")?;
                    }
                    2 => {
                        // Thi đấu bằng vàng
                        ctx.npc_chat("Chức năng Đại Hội Võ Thuật lần 23 đang được phát triển")?;
                    }
                    3 => {
                        // Về DHVT (map 52)
                        ctx.change_map_by_spaceship(MAP_PHONG_CHO, snapshot.location.x, 336)
                            .await?;
                    }
                    _ => {}
                }
            }
            _ => {}
        }

        Ok(())
    }
}
