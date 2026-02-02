use crate::network::session::SessionArc;
use crate::{
    combine::{combine_type::CombineType, CombineHandler},
    constant::{const_npc, menu_enum::MenuId},
    network::session::AsyncSession,
    npc::{handlers::bahatmit, npc_service},
};

#[derive(Debug, Clone, Copy, Default)]
pub struct SaoPhaLe;
#[async_trait::async_trait]
impl CombineHandler for SaoPhaLe {
    async fn show_info_combine(&self, session: &SessionArc) -> anyhow::Result<()> {
        let is_empty = if let Some(snapshot) = session.get_player_snapshot().await {
            snapshot.combine_new.items_combine.is_empty()
        } else {
            true
        };

        if is_empty {
            return Ok(());
        }
        let menu_items = vec!["Ep", "Tu Chon"];
        npc_service::npc_service::create_menu(
            session,
            const_npc::BA_HAT_MIT,
            "Ngươi muon gi ?",
            menu_items,
            MenuId::MenuCombine,
        )
        .await?;

        Ok(())
    }
    async fn confirm_combine(&self, session: &AsyncSession) -> anyhow::Result<()> {
        Ok(())
    }
    fn get_text_info_tab_combine(&self) -> String {
        "Chọn trang bị\n(Áo, quần, găng, giày hoặc rađa)\nSau đó chọn 'Nâng cấp'".to_string()
    }
    fn get_text_top_tab_combine(&self) -> String {
        "Ta sẽ phù phép\ncho trang bị của ngươi\ntrở lên mạnh mẽ".to_string()
    }
}
