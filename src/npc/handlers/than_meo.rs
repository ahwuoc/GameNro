use crate::boss::boss_id::{BOSS_THAN_MEO_KARIN, BOSS_YAJIRO};
use crate::constant::menu_enum::MenuId;
use crate::map::services::training_services;
use crate::npc::handlers::{NpcContext, NpcHandler};
use crate::player::player_actor::PlayerMessage;

pub struct ThanMeoKarinHandler;

const TAP_TU_DONG_TUTORIAL: &str =
    "Đăng ký để mỗi khi Offline quá 30 phút, con sẽ được tự động luyện tập với tốc độ 1280 sức mạnh mỗi phút";

#[async_trait::async_trait]
impl NpcHandler for ThanMeoKarinHandler {
    async fn open_menu(&self, ctx: &NpcContext<'_>) -> anyhow::Result<()> {
        let player = ctx.get_snapshot().await;
        let Some(player) = player else {
            return Ok(());
        };

        let dang_ky_label = if player.dang_ky_tap_tu_dong {
            "Hủy đăng\nký tập\ntự động"
        } else {
            "Đăng ký\ntập\ntự động"
        };

        match player.level_luyentap {
            0 => {
                ctx.create_menu(
                    "Muốn chiến thắng Tàu Pảy Pảy phải đánh bại được ta đã",
                    vec![
                        dang_ky_label,
                        "Nhiệm vụ",
                        "Tập luyện\nvới\nThần Mèo",
                        "Thách đấu\nThần Mèo",
                    ],
                    MenuId::BaseMenu,
                )?;
            }
            1 => {
                ctx.create_menu(
                    "Từ giờ Yajirô sẽ luyện tập cùng ngươi. Yajirô đã từng lên đây tập luyện và bây giờ hắn mạnh hơn ta đấy",
                    vec![
                        dang_ky_label,
                        "Tập luyện\nvới\nYajirô",
                        "Thách đấu\nYajirô",
                    ],
                    MenuId::BaseMenu,
                )?;
            }
            _ => {
                ctx.create_menu(
                    "Con hãy bay theo cây Gậy Như Ý trên đỉnh tháp để đến Thần Điện gặp Thượng đế\nCon rất xứng đáng để làm đệ tử ông ấy.",
                    vec![
                        dang_ky_label,
                        "Tập luyện\nvới\nThần Mèo",
                        "Tập luyện\nvới\nYajirô",
                    ],
                    MenuId::BaseMenu,
                )?;

            }
        }
        Ok(())
    }

    async fn handle_menu(
        &self,
        ctx: &NpcContext<'_>,
        menu_id: MenuId,
        select: i8,
    ) -> anyhow::Result<()> {
        let player = ctx.get_snapshot().await;
        let Some(player) = player else {
            return Ok(());
        };

        match menu_id {
            MenuId::BaseMenu => {
                match player.level_luyentap {
                    // ======================== LEVEL 0 ========================
                    0 => match select {
                        // Đăng ký / Hủy tập tự động
                        0 => {
                            if player.dang_ky_tap_tu_dong {
                                // Hủy đăng ký
                                ctx.send_player_message(PlayerMessage::Modify(Box::new(|p| {
                                    p.dang_ky_tap_tu_dong = false;
                                })));
                                ctx.npc_chat("Con đã hủy thành công đăng ký tập tự động\ntừ giờ con muốn tập Offline hãy tự đến đây trước")?;
                            } else {
                                // Mở menu đăng ký
                                ctx.create_menu(
                                    TAP_TU_DONG_TUTORIAL,
                                    vec![
                                        "Hướng\ndẫn\nthêm",
                                        "Đồng ý\n1 ngọc\nmỗi lần",
                                        "Không\nđồng ý",
                                    ],
                                    MenuId::TapTuDong,
                                )?;
                            }
                        }
                        // Nhiệm vụ
                        1 => {
                            ctx.npc_chat("...")?;
                        }
                        // Tập luyện với Thần Mèo
                        2 => {
                            ctx.create_menu(
                                "Con có chắc muốn tập luyện ?\nTập luyện với ta sẽ tăng 20 sức mỗi phút",
                                vec!["Đồng ý\nluyện tập", "Không\nđồng ý"],
                                MenuId::TapLuyen,
                            )?;
                        }
                        // Thách đấu Thần Mèo
                        3 => {
                            ctx.create_menu(
                                "Con có chắc muốn thách đấu ?\nNếu thắng ta sẽ được tập luyện với Yajirô, tăng 40 sức mạnh mỗi phút",
                                vec!["Đồng ý\ngiao đấu", "Không\nđồng ý"],
                                MenuId::ThachDau,
                            )?;
                        }
                        _ => {}
                    },
                    // ======================== LEVEL 1 ========================
                    1 => match select {
                        0 => {
                            if player.dang_ky_tap_tu_dong {
                                ctx.send_player_message(PlayerMessage::Modify(Box::new(|p| {
                                    p.dang_ky_tap_tu_dong = false;
                                })));
                                ctx.npc_chat("Con đã hủy thành công đăng ký tập tự động\ntừ giờ con muốn tập Offline hãy tự đến đây trước")?;
                            } else {
                                ctx.create_menu(
                                    TAP_TU_DONG_TUTORIAL,
                                    vec![
                                        "Hướng\ndẫn\nthêm",
                                        "Đồng ý\n1 ngọc\nmỗi lần",
                                        "Không\nđồng ý",
                                    ],
                                    MenuId::TapTuDong,
                                )?;
                            }
                        }
                        // Tập luyện với Yajirô
                        1 => {
                            ctx.create_menu(
                                "Con có chắc muốn tập luyện ?\nTập luyện với Yajirô sẽ tăng 40 sức mỗi phút",
                                vec!["Đồng ý\nluyện tập", "Không\nđồng ý"],
                                MenuId::TapLuyen,
                            )?;
                        }
                        // Thách đấu Yajirô
                        2 => {
                            ctx.create_menu(
                                "Con có chắc muốn thách đấu ?\nNếu thắng được Yajirô, con sẽ được học võ với người mạnh hơn để tăng đến 80 sức mạnh mỗi phút",
                                vec!["Đồng ý\ngiao đấu", "Không\nđồng ý"],
                                MenuId::ThachDau,
                            )?;
                        }
                        _ => {}
                    },
                    // ======================== LEVEL >= 2 ========================
                    _ => match select {
                        0 => {
                            if player.dang_ky_tap_tu_dong {
                                ctx.send_player_message(PlayerMessage::Modify(Box::new(|p| {
                                    p.dang_ky_tap_tu_dong = false;
                                })));
                                ctx.npc_chat("Con đã hủy thành công đăng ký tập tự động\ntừ giờ con muốn tập Offline hãy tự đến đây trước")?;
                            } else {
                                ctx.create_menu(
                                    TAP_TU_DONG_TUTORIAL,
                                    vec![
                                        "Hướng\ndẫn\nthêm",
                                        "Đồng ý\n1 ngọc\nmỗi lần",
                                        "Không\nđồng ý",
                                    ],
                                    MenuId::TapTuDong,
                                )?;
                            }
                        }
                        // Tập luyện với Thần Mèo
                        1 => {
                            ctx.create_menu(
                                "Con có chắc muốn tập luyện ?\nTập luyện với ta sẽ tăng 20 sức mỗi phút",
                                vec!["Đồng ý\nluyện tập", "Không\nđồng ý"],
                                MenuId::TapLuyen,
                            )?;
                        }
                        // Tập luyện với Yajirô
                        2 => {
                            ctx.create_menu(
                                "Con có chắc muốn tập luyện ?\nTập luyện với Yajirô sẽ tăng 40 sức mỗi phút",
                                vec!["Đồng ý\nluyện tập", "Không\nđồng ý"],
                                MenuId::ThachDau, // reuse ThachDau for 2nd training option
                            )?;
                        }
                        _ => {}
                    },
                }
            }

            // ======================== MENU TẬP TỰ ĐỘNG (2001) ========================
            MenuId::TapTuDong => match select {
                // Hướng dẫn thêm
                0 => {
                    ctx.npc_chat("Khi Offline quá 30 phút, con sẽ được tự động luyện tập.\nCần 1 ngọc mỗi lần đăng ký.")?;
                }
                // Đồng ý
                1 => {
                    let map_id = player.map_id;
                    ctx.send_player_message(PlayerMessage::Modify(Box::new(move |p| {
                        p.dang_ky_tap_tu_dong = true;
                        p.map_id_dang_tap_tu_dong = map_id;
                    })));
                    ctx.npc_chat("Từ giờ, quá 30 phút Offline con sẽ được tự động luyện tập")?;
                }
                _ => {}
            },

            // ======================== MENU TẬP LUYỆN (2002) ========================
            MenuId::TapLuyen => {
                if select == 0 {
                    // Chọn boss theo level
                    let boss_id = match player.level_luyentap {
                        1 => BOSS_YAJIRO,
                        _ => BOSS_THAN_MEO_KARIN, // level 0, >= 2 → Karin
                    };
                    ctx.send_player_message(PlayerMessage::CallTrainingBoss {
                        boss_id: boss_id.to_string(),
                        is_thachdau: false,
                    });
                }
            }

            // ======================== MENU THÁCH ĐẤU (2003) ========================
            MenuId::ThachDau => {
                if select == 0 {
                    let boss_id = match player.level_luyentap {
                        0 => BOSS_THAN_MEO_KARIN,
                        1 => BOSS_YAJIRO,
                        _ => BOSS_YAJIRO, // level >= 2: Tập luyện với Yajirô (từ BaseMenu select 2)
                    };
                    let is_thachdau = player.level_luyentap < 2; // level >= 2 là tập luyện, không phải thách đấu
                    ctx.send_player_message(PlayerMessage::CallTrainingBoss {
                        boss_id: boss_id.to_string(),
                        is_thachdau,
                    });
                }
            }
            _ => {}
        }
        Ok(())
    }
}
