use crate::constant::menu_enum::MenuId;
use crate::npc::handlers::{NpcContext, NpcHandler};
use crate::player::player_actor::{MagicTreeMsg, PlayerMessage};

pub struct DauThanHandler;

#[async_trait::async_trait]
impl NpcHandler for DauThanHandler {
    async fn open_menu(&self, ctx: &NpcContext<'_>) -> anyhow::Result<()> {
        ctx.send_player_message(PlayerMessage::MagicTree(MagicTreeMsg::OpenOrLoad(1)));
        Ok(())
    }

    async fn handle_menu(
        &self,
        ctx: &NpcContext<'_>,
        menu_id: MenuId,
        select: i8,
    ) -> anyhow::Result<()> {
        tracing::info!(
            "DauThanHandler::handle_menu: menu_id={:?}, select={}",
            menu_id,
            select
        );
        match menu_id {
            MenuId::MagicTreeNonUpgradeLeftPea => match select {
                0 => ctx.send_player_message(PlayerMessage::MagicTree(MagicTreeMsg::Harvest)),
                1 => {
                    if let Some(player) = ctx.get_snapshot().await {
                        if player.magic_tree.level == 10 {
                            ctx.send_player_message(PlayerMessage::MagicTree(
                                MagicTreeMsg::FastRespawn,
                            ));
                        } else {
                            let npc_say = format!(
                                "Bạn có muốn nâng cấp {} với giá {} vàng không?",
                                format!("Đậu thần cấp {}", player.magic_tree.level),
                                player
                                    .magic_tree
                                    .get_text_menu_upgrade()
                                    .split('\n')
                                    .last()
                                    .unwrap_or("")
                            );
                            ctx.create_menu(
                                &npc_say,
                                vec!["Đồng ý", "Từ chối"],
                                MenuId::MagicTreeConfirmUpgrade,
                            )?;
                        }
                    }
                }
                2 => ctx.send_player_message(PlayerMessage::MagicTree(MagicTreeMsg::FastRespawn)),
                _ => {}
            },
            MenuId::MagicTreeNonUpgradeFullPea => match select {
                0 => ctx.send_player_message(PlayerMessage::MagicTree(MagicTreeMsg::Harvest)),
                1 => {
                    if let Some(player) = ctx.get_snapshot().await {
                        let npc_say = format!(
                            "Bạn có muốn nâng cấp {} với giá {} vàng không?",
                            format!("Đậu thần cấp {}", player.magic_tree.level),
                            player
                                .magic_tree
                                .get_text_menu_upgrade()
                                .split('\n')
                                .last()
                                .unwrap_or("")
                        );
                        ctx.create_menu(
                            &npc_say,
                            vec!["Đồng ý", "Từ chối"],
                            MenuId::MagicTreeConfirmUpgrade,
                        )?;
                    }
                }
                _ => {}
            },
            MenuId::MagicTreeConfirmUpgrade => {
                if select == 0 {
                    ctx.send_player_message(PlayerMessage::MagicTree(MagicTreeMsg::Upgrade));
                }
            }
            MenuId::MagicTreeUpgrade => match select {
                0 => ctx.send_player_message(PlayerMessage::MagicTree(MagicTreeMsg::FastUpgrade)),
                1 => {
                    if let Some(player) = ctx.get_snapshot().await {
                        let idx = (player.magic_tree.level - 1) as usize;
                        let gold = crate::player::magic_tree::PEA_UPGRADE[idx][3];
                        let unit = if player.magic_tree.level <= 3 {
                            " k"
                        } else {
                            " Tr"
                        };
                        let npc_say = format!(
                            "Bạn có muốn hủy nâng cấp và nhận lại {} {} vàng không?",
                            gold / 2,
                            unit
                        );
                        ctx.create_menu(
                            &npc_say,
                            vec!["Đồng ý", "Từ chối"],
                            MenuId::MagicTreeConfirmUnupgrade,
                        )?;
                    }
                }
                _ => {}
            },
            MenuId::MagicTreeConfirmUnupgrade => {
                if select == 0 {
                    ctx.send_player_message(PlayerMessage::MagicTree(MagicTreeMsg::Unupgrade));
                }
            }
            _ => {}
        }
        Ok(())
    }
}
