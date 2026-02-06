use crate::constant::menu_enum::MenuId;
use crate::npc::handlers::{NpcContext, NpcHandler};
use crate::shop::shop_menu_manager::ShopMenuManager;
use crate::shop::shop_services::shop_service;

pub struct DynamicShopHandler {
    pub menu_id: MenuId,
    pub npc_greeting: String,
}

impl DynamicShopHandler {
    pub fn new(menu_id: MenuId, greeting: &str) -> Self {
        Self {
            menu_id,
            npc_greeting: greeting.to_string(),
        }
    }
}

#[async_trait::async_trait]
impl NpcHandler for DynamicShopHandler {
    async fn open_menu(&self, ctx: &NpcContext<'_>) -> anyhow::Result<()> {
        let player_gender = ctx.get_player_snapshot().await.map(|p| p.gender as i32);

        let options = ShopMenuManager::get_menu_options(ctx.npc_id as i32, player_gender).await?;

        if options.is_empty() {
            ctx.npc_chat("Xin lỗi, tôi không có gì để bán cho bạn.")?;
            ctx.hide_wait_dialog()?;
            return Ok(());
        }

        let option_ref: Vec<&str> = options.iter().map(|s| s.as_str()).collect();

        ctx.create_menu(&self.npc_greeting, option_ref, self.menu_id)
            .await?;

        Ok(())
    }

    async fn handle_menu(
        &self,
        ctx: &NpcContext<'_>,
        menu_id: MenuId,
        select: i8,
    ) -> anyhow::Result<()> {
        if menu_id != self.menu_id {
            return Ok(());
        }

        let player_gender = ctx.get_player_snapshot().await.map(|p| p.gender as i32);

        if let Some(shop_item) =
            ShopMenuManager::get_shop_by_index(ctx.npc_id as i32, select as usize, player_gender)
                .await?
        {
            if let Some(gender) = player_gender {
                if !shop_item.is_available_for_gender(gender) {
                    ctx.npc_chat(shop_item.get_gender_reject_message())?;
                    return Ok(());
                }
            }

            shop_service::open_shop(&shop_item.tag_name, ctx.session).await?;
        } else {
            ctx.npc_chat("Shop không tồn tại!")?;
        }

        Ok(())
    }
}
