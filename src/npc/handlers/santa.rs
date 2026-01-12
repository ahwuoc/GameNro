use super::NpcHandler;
use crate::constant::const_npc;
use crate::constant::menu_enum::MenuId;
use crate::entities::player;
use crate::network::session::{self, AsyncSession};
use crate::npc::NpcService;
use async_trait::async_trait;

pub struct SantaHandler;

#[async_trait]
impl NpcHandler for SantaHandler {
    async fn open_menu(&self, session: &mut AsyncSession) -> anyhow::Result<()> {
        if let Some(player) = session.get_player_mut() {
            player.id_mark.set_index_menu(MenuId::BaseMenu);
        } else {
            return Ok(());
        }

        let menu_items = vec![
            "Cửa hàng",
            "Mở rộng\nHành trang\nRương đồ",
            "Nhập mã\nquà tặng",
            "Cửa hàng\nHạn sử dụng",
            "Tiệm\nHớt tóc",
            "Danh\nhiệu",
            "Shop Vip",
        ];
        let npc_say = "Xin chào, ta có một số vật phẩm đặc biệt cậu có muốn xem không?";
        NpcService::create_menu(session, const_npc::SANTA, npc_say, menu_items).await?;
        Ok(())
    }
    async fn handle_menu(
        &self,
        session: &mut AsyncSession,
        menu_id: MenuId,
        select: i8,
    ) -> anyhow::Result<()> {
        let _player = match session.get_player_mut() {
            Some(p) => p,
            None => return Ok(()),
        };

        match menu_id {
            MenuId::BaseMenu => match select {
                0 => {
                    println!("chaoem");
                }
                _ => {}
            },
            _ => {
                println!("Unhandled menu state: {:?}", menu_id);
            }
        }
        Ok(())
    }
}
