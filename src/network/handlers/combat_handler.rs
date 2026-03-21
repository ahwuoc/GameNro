use crate::network::message::Message;
use crate::network::session::SessionArc;
use crate::player::player_actor::{
    pet::{message::PetMessage, PetStatus},
    PlayerMessage,
};
use crate::player::Fusion;
use anyhow::Result;
use tracing::{debug, warn};

pub struct CombatHandler;

impl CombatHandler {
    pub async fn attack_mob(session: &SessionArc, mut msg: Message) -> Result<()> {
        let mob_id = msg.read_byte()? as i32;
        let _is_mob_me = mob_id == -1;
        let _master_id = if _is_mob_me { msg.read_int()? } else { -1 };

        if let Some(handle) = session.get_player_handle().await {
            handle.send_forget(PlayerMessage::AttackMob { mob_id });
        }
        Ok(())
    }

    pub async fn attack_player(session: &SessionArc, mut msg: Message) -> Result<()> {
        let player_id = msg.read_int()?;
        if let Some(handle) = session.get_player_handle().await {
            handle.send_forget(PlayerMessage::AttackPlayer { player_id });
        }
        Ok(())
    }

    pub async fn select_skill(session: &SessionArc, mut msg: Message) -> Result<()> {
        if let Some(handle) = session.get_player_handle().await {
            let skill_template_id = msg.read_short().unwrap_or(0);
            handle.send_forget(PlayerMessage::SelectSkill {
                skill_template_id: skill_template_id as i32,
            });
        }
        Ok(())
    }

    pub async fn use_skill(session: &SessionArc, msg: Message) -> Result<()> {
        if let Some(handle) = session.get_player_handle().await {
            handle.send_forget(PlayerMessage::UseSkill { msg });
        }
        Ok(())
    }

    pub async fn hoi_sinh(session: &SessionArc) -> Result<()> {
        if let Some(handle) = session.get_player_handle().await {
            handle.send_forget(PlayerMessage::HoiSinh);
        }
        Ok(())
    }

    pub async fn pet_change_status(session: &SessionArc, mut msg: Message) -> Result<()> {
        let status_byte = msg.read_byte()?;
        if let Ok(status) = PetStatus::try_from(status_byte) {
            if let Some(handle) = session.get_player_handle().await {
                if status == PetStatus::Fusion {
                    handle.send_forget(PlayerMessage::Fusion {
                        type_fusion: Fusion::LUONG_LONG_NHAT_THE,
                        template_id: 1,
                    });
                } else if status == PetStatus::HTVV {
                    handle.send_forget(PlayerMessage::Fusion {
                        type_fusion: Fusion::HOP_THE_VINH_VIEN,
                        template_id: 1,
                    });
                } else {
                    handle.send_forget(PlayerMessage::Pet(PetMessage::ChangeStatus(status)));
                }
            }
        }
        Ok(())
    }

    pub async fn pvp_command(session: &SessionArc, msg: Message) -> Result<()> {
        crate::matches::pvp_service::controller_thach_dau(session, msg).await?;
        Ok(())
    }
}
