use crate::combine::combine_type::CombineType;
use crate::combine::handlers::saophale::SaoPhaLe;
use crate::combine::CombineHandler;
use crate::combine::{combine_constants::*, combine_type};
use crate::entities::player;
use crate::network::message::Message;
use crate::network::session::{self, AsyncSession, SessionArc};
use crate::player::player::Player;
pub async fn open_tab_combine(
    session: &SessionArc,
    type_combine: CombineType,
    npc_id: i16,
) -> anyhow::Result<()> {
    let _ = session
        .modify_player(|player| {
            player.combine_new.set_type_combine(type_combine);
            Ok(())
        })
        .await;
    let text_info = get_text_info_tab_combine(type_combine);
    let text_top = get_text_top_tab_combine(type_combine);
    let mut msg = Message::new(-81);
    msg.write_byte(OPEN_TAB_COMBINE as i8)?;
    msg.write_utf(&text_info)?;
    msg.write_utf(&text_top)?;
    msg.write_short(npc_id)?;
    let _ = session.transmit(msg);
    Ok(())
}
pub async fn show_info_combine(session: &SessionArc, index: Vec<i16>) -> anyhow::Result<()> {
    let mut type_c = CombineType::default();
    let _ = session
        .modify_player(|player| {
            player.combine_new.clear_param_combine();
            if !index.is_empty() {
                for &i in index.iter() {
                    if let Some(it) = player.inventory.items_bag.get(i as usize) {
                        player.combine_new.items_combine.push(it.clone());
                    }
                }
            }
            type_c = player.combine_new.type_combine;
            Ok(())
        })
        .await;

    let type_combine = type_c;

    type_combine.show_info_combine(session).await
}

pub async fn confirm_combine(session: &SessionArc) -> anyhow::Result<()> {
    let type_combine = session
        .get_player_ref(|player| {
            player
                .map(|p| p.combine_new.type_combine)
                .unwrap_or_default()
        })
        .await;
    type_combine.confirm_combine(session).await
}

pub fn get_text_info_tab_combine(type_combine: CombineType) -> String {
    type_combine.get_text_info_tab_combine()
}

pub fn get_text_top_tab_combine(type_combine: CombineType) -> String {
    type_combine.get_text_top_tab_combine()
}
