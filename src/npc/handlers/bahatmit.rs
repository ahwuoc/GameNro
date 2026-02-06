use crate::combine::combine_service;
use crate::combine::combine_type::CombineType;
use crate::constant::menu_enum::MenuId;
use crate::npc::handlers::{NpcContext, NpcHandler};
use async_trait::async_trait;

pub struct BahatmitHandler;

#[async_trait]
impl NpcHandler for BahatmitHandler {
    async fn open_menu(&self, ctx: &NpcContext<'_>) -> anyhow::Result<()> {
        let Some(snapshot) = ctx.get_player_snapshot().await else {
            return Ok(());
        };
        let map_id = snapshot.map_id;

        match map_id {
            5 => {
                ctx.create_menu(
                    "Ngươi tìm ta có việc gì?",
                    vec![
                        "Chức năng\npha lê",
                        "Chức năng\nđệ tử",
                        "Chức năng\nSét Kích Hoạt",
                        "Chức năng\nItem cấp 2",
                        "Võ đài sinh tử",
                    ],
                    MenuId::BaseMenu,
                )
                .await?;
            }
            _ => {}
        }
        Ok(())
    }

    async fn handle_menu(
        &self,
        ctx: &NpcContext<'_>,
        menu_id: MenuId,
        select: i8,
    ) -> anyhow::Result<()> {
        let map_id = ctx
            .get_player_snapshot()
            .await
            .map(|p| p.map_id)
            .ok_or(anyhow::anyhow!("Player not found"))?;

        match menu_id {
            MenuId::BaseMenu => match map_id {
                5 => match select {
                    0 => {
                        combine_service::open_tab_combine(
                            ctx.session,
                            CombineType::PhaLeHoaTrangBi(
                                crate::combine::handlers::saophale::SaoPhaLe,
                            ),
                            ctx.npc_id,
                        )
                        .await?;
                    }
                    1 => println!("Xử lý Chức năng đệ tử"),
                    _ => {}
                },
                112 => match select {
                    0 => println!("Xem Top 100"),
                    1 => println!("Xử lý Đồng ý"),
                    2 => println!("Xử lý Từ chối"),
                    3 => println!("Về đảo rùa"),
                    _ => {}
                },
                _ => match select {
                    0 => {
                        ctx.create_menu(
                            "Xin chào, ta có một số vật phẩm đặc biệt cậu có muốn xem không?",
                            vec!["Sub Menu"],
                            MenuId::SubMenuSanta,
                        )
                        .await?;
                    }
                    _ => {}
                },
            },
            MenuId::SubMenuSanta => match select {
                0 => println!("sub menu"),
                _ => {}
            },
            MenuId::MenuCombine => match select {
                0 => {
                    combine_service::confirm_combine(ctx.session).await?;
                }
                _ => {}
            },
            _ => {}
        }
        Ok(())
    }
}
