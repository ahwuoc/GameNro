use crate::combine::combine_service;
use crate::combine::combine_type::CombineType;
use crate::constant::menu_enum::MenuId;
use crate::npc::handlers::{NpcContext, NpcHandler};
use crate::shop::shop_dao;
use crate::shop::shop_services::shop_service;
use async_trait::async_trait;

pub struct BahatmitHandler;

#[async_trait]
impl NpcHandler for BahatmitHandler {
    async fn open_menu(&self, ctx: &NpcContext<'_>) -> anyhow::Result<()> {
        let Some(snapshot) = ctx.get_snapshot().await else {
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
                )?;
            }
            _ => ctx.create_menu("", vec!["Cửa hàng\nBùa"], MenuId::BaseMenu)?,
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
            .get_snapshot()
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
                    1 => tracing::info!("Xử lý Chức năng đệ tử"),
                    _ => {}
                },
                112 => match select {
                    0 => tracing::info!("Xem Top 100"),
                    1 => tracing::info!("Xử lý Đồng ý"),
                    2 => tracing::info!("Xử lý Từ chối"),
                    3 => tracing::info!("Về đảo rùa"),
                    _ => {}
                },
                _ => match select {
                    0 => {
                        ctx.create_menu(
                            "Bùa của ta rất lợi hại, nhìn ngươi yếu đuối thế này, chắc muốn mua bùa để mạnh mẽ à, mua không ta bán cho, xài rồi lại thích cho mà xem.",
                            vec!["Bùa\n1 giờ", "Bùa\n8 giờ", "Bùa\n1 tháng", "Đóng"],
                            MenuId::MenuShopBua,
                        )?;
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
            MenuId::MenuShopBua => match select {
                0 => {
                    shop_service::open_shop("BUA_1H", ctx.session).await?;
                }
                1 => {
                    shop_service::open_shop("BUA_8H", ctx.session).await?;
                }
                2 => {
                    shop_service::open_shop("BUA_1M", ctx.session).await?;
                }
                3 => println!("Đóng"),
                _ => {}
            },
            _ => {}
        }
        Ok(())
    }
}
