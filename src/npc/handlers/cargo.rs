use crate::constant::const_map::{
    MAP_SIEU_THI, MAP_TRAM_TAU_VU_TRU_NAMEC, MAP_TRAM_TAU_VU_TRU_TRAI_DAT,
    MAP_TRAM_TAU_VU_TRU_XAYDA,
};
use crate::constant::menu_enum::MenuId;
use crate::npc::handlers::{NpcContext, NpcHandler};

pub struct CargoHandler;

#[async_trait::async_trait]
impl NpcHandler for CargoHandler {
    async fn open_menu(&self, ctx: &NpcContext<'_>) -> anyhow::Result<()> {
        ctx.create_menu(
            "Tàu vũ trụ Xayda sử dụng công nghệ mới nhất, có thể đưa ngươi đi bất kỳ đâu, chỉ cần trả tiền là được.",
            vec!["Den Trai Dat", "Den Xayda", "Siêu thị"],
            MenuId::BaseMenu,
        )?;
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
                    ctx.change_map_by_spaceship(MAP_TRAM_TAU_VU_TRU_TRAI_DAT, -1, 5)?;
                }
                1 => {
                    ctx.change_map_by_spaceship(MAP_TRAM_TAU_VU_TRU_XAYDA, -1, 5)?;
                }
                2 => {
                    ctx.change_map_by_spaceship(MAP_SIEU_THI, -1, 5)?;
                }
                _ => {}
            },
            _ => {}
        }
        Ok(())
    }
}
