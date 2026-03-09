use crate::dungoen::doanh_trai::handle::DoanhTraiHandle;
use crate::models::clan::{Clan, ClanMember, ClanMessage as ClanMsg};
use crate::player::player_actor::PlayerHandle;
use tokio::sync::oneshot;

#[derive(Debug)]
pub enum ClanMessage {
    GetSnapshot(oneshot::Sender<Clan>),
    AddMemberOnline(PlayerHandle),
    RemoveMemberOnline(u64),
    AddMessage(ClanMsg),
    JoinDungeon(DoanhTraiHandle),
    UpdateMemberPower(i32, i64),
    KickMember(i32),
    PromoteMember(i32, i8),
    LeaveClan(u64),
    AddMember(ClanMember),
    UpdateDonate(i32, i32, i32),
    UpdateAskPeaTime(i32, i64),
    ClearDungeon,
    SetGoneDungeon(bool),
    AddInvite(i32), // target_player_id
    RemoveInvite(i32),
    UpdateMessage(ClanMsg),
    // Add more as needed during refactoring
}
