#![allow(dead_code)]
use crate::models::{Intrinsic, IntrinsicPlayer};
use crate::network::message::Message;
use crate::network::session::{AsyncSession, SessionArc};
use crate::npc::npc_service;
use crate::player::Player as RtPlayer;
use crate::templates::intrinsic_template_manager;
use anyhow::Result;
use rand::Rng;

pub struct IntrinsicService;

impl IntrinsicService {
    const COST_OPEN: [i32; 8] = [10, 20, 40, 80, 160, 320, 640, 1280];

    pub fn get_intrinsics(player_gender: i8) -> Vec<Intrinsic> {
        intrinsic_template_manager::get_all()
            .into_iter()
            .filter(|t| t.gender == player_gender)
            .map(|t| Intrinsic::from_entity(&t))
            .collect()
    }

    pub fn get_intrinsic_by_id(id: i8) -> Option<Intrinsic> {
        intrinsic_template_manager::get(id).map(|t| Intrinsic::from_entity(&t))
    }

    pub async fn send_info_intrinsic(session: &SessionArc, player: &RtPlayer) -> Result<()> {
        let player_instrict = &player.intrinsic;
        let mut msg = Message::new(112);
        msg.write_byte(0);
        msg.write_short(player_instrict.intrinsic.icon);
        msg.write_utf(&player_instrict.intrinsic.get_name());
        session.transmit(msg);
        Ok(())
    }

    pub async fn show_all_intrinsic(session: &SessionArc, player_gender: i8) -> Result<()> {
        let list_intrinsic = Self::get_intrinsics(player_gender);
        let mut msg = Message::new(112);

        msg.write_byte(1);
        msg.write_byte(1);
        msg.write_utf("Nội tại");
        msg.write_byte((list_intrinsic.len() - 1) as i8);
        for intrinsic in list_intrinsic.iter().skip(1) {
            msg.write_short(intrinsic.icon);
            msg.write_utf(&intrinsic.get_description());
        }
        session.transmit(msg);
        Ok(())
    }

    pub async fn show_menu(session: &SessionArc) -> anyhow::Result<()> {
        use crate::constant::menu_enum::MenuId;
        use crate::npc::npc_service;

        npc_service::npc_service::create_menu(
            session,
            crate::constant::const_npc::CON_MEO,
            "Nội tại là một kỹ năng bị động hỗ trợ đặc biệt\nBạn có muốn mở hoặc thay đổi nội tại không?",
            vec!["Xem\ntất cả\nNội Tại", "Mở\nNội Tại", "Mở VIP", "Từ chối"],
            MenuId::Intrinsic,
        )
        .await?;
        Ok(())
    }

    pub async fn show_confirm_open(session: &SessionArc, count_open: i8) -> anyhow::Result<()> {
        use crate::constant::menu_enum::MenuId;

        let index = if count_open as usize >= Self::COST_OPEN.len() {
            Self::COST_OPEN.len() - 1
        } else {
            count_open as usize
        };
        let cost = Self::COST_OPEN[index];

        npc_service::npc_service::create_menu(
            session,
            crate::constant::const_npc::CON_MEO,
            &format!("Bạn muốn đổi Nội Tại khác\nvới giá là {} Tr vàng ?", cost),
            vec!["Mở\nNội Tại", "Từ chối"],
            MenuId::ConfirmOpenIntrinsic,
        )
        .await?;
        Ok(())
    }

    pub async fn show_confirm_open_vip(session: &SessionArc) -> anyhow::Result<()> {
        use crate::constant::menu_enum::MenuId;
        npc_service::npc_service::create_menu(
            session,
            crate::constant::const_npc::CON_MEO as i16,
            "Bạn có muốn mở Nội Tại\nvới giá là 100 ngọc và\ntái lập giá vàng quay lại ban đầu không?",
            vec!["Mở\nNội VIP", "Từ chối"],
            MenuId::ConfirmOpenIntrinsicVip,
        )
        .await?;
        Ok(())
    }

    fn change_intrinsic(player_intrinsic: &mut IntrinsicPlayer, player_gender: i8) {
        let list_intrinsic = Self::get_intrinsics(player_gender);
        if list_intrinsic.len() > 1 {
            let mut rng = rand::rng();
            let random_index = rng.random_range(1..list_intrinsic.len());
            let selected_intrinsic = &list_intrinsic[random_index];

            player_intrinsic.intrinsic = Intrinsic::from_intrinsic(selected_intrinsic);

            player_intrinsic.intrinsic.param1 =
                rng.random_range(selected_intrinsic.param_from_1..=selected_intrinsic.param_to_1);
            player_intrinsic.intrinsic.param2 =
                rng.random_range(selected_intrinsic.param_from_2..=selected_intrinsic.param_to_2);
        }
    }

    pub fn open(
        player_intrinsic: &mut IntrinsicPlayer,
        player_gender: i8,
        player_power: i64,
        player_gold: i64,
    ) -> Result<String, String> {
        if player_power < 10_000_000_000 {
            return Err("Yêu cầu sức mạnh tối thiểu 10 tỷ".to_string());
        }

        let index = if player_intrinsic.count_open as usize >= Self::COST_OPEN.len() {
            Self::COST_OPEN.len() - 1
        } else {
            player_intrinsic.count_open as usize
        };

        let gold_require = Self::COST_OPEN[index] as i64 * 1_000_000;

        if player_gold >= gold_require {
            Self::change_intrinsic(player_intrinsic, player_gender);
            player_intrinsic.count_open += 1;

            let intrinsic_name = player_intrinsic.intrinsic.get_name();
            let name_part = if let Some(bracket_pos) = intrinsic_name.find(" [") {
                &intrinsic_name[..bracket_pos]
            } else {
                &intrinsic_name
            };

            Ok(format!("Bạn nhận được Nội tại:\n{}", name_part))
        } else {
            let missing = gold_require - player_gold;
            Err(format!("Bạn không đủ vàng, còn thiếu {} vàng nữa", missing))
        }
    }

    pub fn open_vip(
        player_intrinsic: &mut IntrinsicPlayer,
        player_gender: i8,
        player_power: i64,
        player_gem: i32,
    ) -> Result<String, String> {
        if player_power < 10_000_000_000 {
            return Err("Yêu cầu sức mạnh tối thiểu 10 tỷ".to_string());
        }
        let gem_require = 100;
        if player_gem >= gem_require {
            Self::change_intrinsic(player_intrinsic, player_gender);
            player_intrinsic.count_open = 0;

            let intrinsic_name = player_intrinsic.intrinsic.get_name();
            let name_part = if let Some(bracket_pos) = intrinsic_name.find(" [") {
                &intrinsic_name[..bracket_pos]
            } else {
                &intrinsic_name
            };
            Ok(format!("Bạn nhận được Nội tại:\n{}", name_part))
        } else {
            let missing = gem_require - player_gem;
            Err(format!(
                "Bạn không có đủ ngọc, còn thiếu {} ngọc nữa",
                missing
            ))
        }
    }
}
