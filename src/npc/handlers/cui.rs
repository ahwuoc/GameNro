use crate::constant::const_map::{
    MAP_SIEU_THI, MAP_TRAM_TAU_VU_TRU_NAMEC, MAP_TRAM_TAU_VU_TRU_TRAI_DAT,
};
use crate::constant::menu_enum::MenuId;
use crate::entities::player;
use crate::npc::handlers::{NpcContext, NpcHandler};

pub struct CuiHandler;

#[async_trait::async_trait]
impl NpcHandler for CuiHandler {
    async fn open_menu(&self, ctx: &NpcContext<'_>) -> anyhow::Result<()> {
        ctx.create_menu(
            "Tàu vũ trụ Xayda sử dụng công nghệ mới nhất, có thể đưa ngươi đi bất kỳ đâu, chỉ cần trả tiền là được.",
            vec!["Đến Trái Đất", "Đến Namếc", "Siêu thị"],
            MenuId::BaseMenu,
        ).await?;
        Ok(())
    }

    async fn handle_menu(
        &self,
        ctx: &NpcContext<'_>,
        menu_id: MenuId,
        select: i8,
    ) -> anyhow::Result<()> {
        match menu_id {
            MenuId::BaseMenu => match select {
                0 => {
                    ctx.change_map_by_spaceship(MAP_TRAM_TAU_VU_TRU_TRAI_DAT, -1, 5)
                        .await?;
                }
                1 => {
                    ctx.change_map_by_spaceship(MAP_TRAM_TAU_VU_TRU_NAMEC, -1, 5)
                        .await?;
                }
                2 => {
                    ctx.change_map_by_spaceship(MAP_SIEU_THI, -1, 5).await?;
                }
                _ => {}
            },
            _ => {}
        }
        Ok(())
    }
}
