use super::NpcHandler;
use crate::combine::combine_type::CombineType;
use crate::combine::{combine_constants, combine_service, CombineHandler};
use crate::constant::const_npc;
use crate::constant::menu_enum::MenuId;
use crate::entities::player;
use crate::network::session::{self, AsyncSession, SessionArc};
use crate::npc::npc_service;
use async_trait::async_trait;

pub struct BahatmitHandler;

#[async_trait]
impl NpcHandler for BahatmitHandler {
    async fn open_menu(&self, session: &SessionArc, npc_id: i16) -> anyhow::Result<()> {
        let Some(map_id) = session.get_player().await.map(|p| p.map_id) else {
            return Ok(());
        };

        match map_id {
            5 => {
                let menu_items = vec![
                    "Chức năng\npha lê",
                    "Chức năng\nđệ tử",
                    "Chức năng\nSét Kích Hoạt",
                    "Chức năng\nItem cấp 2",
                    "võ dài sinh tử",
                ];
                let npc_say = "Ngươi tìm ta có việc gì?";
                npc_service::npc_service::create_menu(
                    session,
                    npc_id,
                    npc_say,
                    menu_items,
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
        session: &SessionArc,
        npc_id: i16,
        menu_id: MenuId,
        select: i8,
    ) -> anyhow::Result<()> {
        let map_id = session
            .get_player()
            .await
            .map(|p| p.map_id)
            .ok_or(anyhow::anyhow!("Player not found"))?;

        match menu_id {
            MenuId::BaseMenu => match map_id {
                5 => match select {
                    0 => {
                        combine_service::open_tab_combine(
                            session,
                            CombineType::PhaLeHoaTrangBi(
                                crate::combine::handlers::saophale::SaoPhaLe,
                            ),
                            npc_id,
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
                        let menu_items = vec!["Sub Menu"];
                        let npc_say =
                            "Xin chào, ta có một số vật phẩm đặc biệt cậu có muốn xem không?";
                        npc_service::npc_service::create_menu(
                            session,
                            npc_id,
                            npc_say,
                            menu_items,
                            MenuId::SubMenuSanta,
                        )
                        .await?;
                    }
                    _ => {}
                },
            },
            MenuId::SubMenuSanta => match select {
                0 => {
                    println!("sub menu")
                }
                _ => {}
            },
            MenuId::MenuCombine => match select {
                0 => {
                    combine_service::confirm_combine(session).await?;
                }
                _ => {}
            },
            _ => {}
        }
        Ok(())
    }
}
