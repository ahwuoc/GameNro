//! ClanService – thin facade that delegates to the sub-modules in `services/`.
//!
//! Keeping this struct here preserves all existing call sites throughout the
//! codebase (nothing outside this module needs to change).

use crate::clan::clan_manager::CLAN_MANAGER;
use crate::network::message::Message;
use crate::player::player_actor::PlayerHandle;
use crate::player::Player;

use super::services::*;

pub struct ClanService;

impl ClanService {
    // ── Online tracking ───────────────────────────────────────────────────────
    pub async fn add_player_to_clan_online(player: &Player, handle: PlayerHandle) {
        if player.clan_id != -1 {
            if let Some(clan_handle) = CLAN_MANAGER.get_clan(player.clan_id) {
                clan_handle.add_member_online(handle);
                clan_handle.update_power(player.id as i32, player.n_point.power);
            }
        }
    }

    pub async fn remove_player_from_clan_online(player_id: u64, clan_id: i32) {
        if clan_id != -1 {
            if let Some(clan_handle) = CLAN_MANAGER.get_clan(clan_id) {
                clan_handle.remove_member_online(player_id);
            }
        }
    }

    pub fn get_clan_by_id(clan_id: i32) -> Option<super::handle::ClanHandle> {
        CLAN_MANAGER.get_clan(clan_id)
    }

    // ── Info ──────────────────────────────────────────────────────────────────

    pub async fn send_my_clan(player: &Player) -> anyhow::Result<()> {
        InfoService::send_my_clan(player).await
    }

    pub async fn send_clan_list(player: &Player, name: &str) -> anyhow::Result<()> {
        InfoService::send_clan_list(player, name).await
    }

    pub async fn send_member_list(player: &Player, clan_id: i32) -> anyhow::Result<()> {
        InfoService::send_member_list(player, clan_id).await
    }

    pub async fn send_my_clan_for_all_members(clan_id: i32) -> anyhow::Result<()> {
        send_my_clan_for_all_members(clan_id).await
    }

    pub async fn send_message_clan(
        clan_id: i32,
        cmg: crate::models::clan::ClanMessage,
    ) -> anyhow::Result<()> {
        send_message_clan(clan_id, cmg).await
    }

    // ── Chat & messages (-51) ─────────────────────────────────────────────────

    pub async fn clan_message(player: &Player, msg: Message) -> anyhow::Result<()> {
        ChatService::clan_message(player, msg).await
    }

    pub async fn chat(player: &Player, text: &str) -> anyhow::Result<()> {
        ChatService::chat(player, text).await
    }

    pub async fn ask_for_pea(player: &Player) -> anyhow::Result<()> {
        ChatService::ask_for_pea(player).await
    }

    pub async fn ask_for_join_clan(player: &Player, clan_id: i32) -> anyhow::Result<()> {
        ChatService::ask_for_join_clan(player, clan_id).await
    }

    // ── Membership management (-55 / -49) ─────────────────────────────────────

    pub async fn clan_remote(player: &Player, msg: Message) -> anyhow::Result<()> {
        MembershipService::clan_remote(player, msg).await
    }

    pub async fn kick_out(player: &Player, member_id: i32) -> anyhow::Result<()> {
        MembershipService::kick_out(player, member_id).await
    }

    pub async fn promote_deputy(player: &Player, member_id: i32) -> anyhow::Result<()> {
        MembershipService::promote_deputy(player, member_id).await
    }

    pub async fn demote_member(player: &Player, member_id: i32) -> anyhow::Result<()> {
        MembershipService::demote_member(player, member_id).await
    }

    pub async fn transfer_leader(player: &Player, member_id: i32) -> anyhow::Result<()> {
        MembershipService::transfer_leader(player, member_id).await
    }

    pub async fn leave_clan(player: &Player) -> anyhow::Result<()> {
        MembershipService::leave_clan(player).await
    }

    pub async fn clan_invite(player: &Player, msg: Message) -> anyhow::Result<()> {
        MembershipService::clan_invite(player, msg).await
    }

    pub async fn join_clan(
        player_handle: PlayerHandle,
        clan_id: i32,
        role: i8,
    ) -> anyhow::Result<()> {
        MembershipService::join_clan(player_handle, clan_id, role).await
    }

    pub async fn join_clan_controller(
        player_handle: PlayerHandle,
        msg: Message,
    ) -> anyhow::Result<()> {
        MembershipService::join_clan_controller(player_handle, msg).await
    }

    pub async fn update_player_clan_id(player_id: i32, clan_id: i32) -> anyhow::Result<()> {
        MembershipService::update_player_clan_id(player_id, clan_id).await
    }

    // ── Creation (-46) ────────────────────────────────────────────────────────

    pub async fn get_clan(player: &Player, msg: Message) -> anyhow::Result<()> {
        CreationService::get_clan(player, msg).await
    }

    pub async fn create_clan(player: &Player, img_id: i8, name: &str) -> anyhow::Result<()> {
        CreationService::create_clan(player, img_id, name).await
    }

    // ── Donations (-50) ───────────────────────────────────────────────────────

    pub async fn clan_donate(player: &Player, msg: Message) -> anyhow::Result<()> {
        DonationService::clan_donate(player, msg).await
    }
}
