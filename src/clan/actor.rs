use super::message::ClanMessage;
use crate::models::clan::Clan;
use tokio::sync::mpsc;
use tracing::{error, info};

pub struct ClanActor {
    pub clan: Clan,
    pub rx: mpsc::Receiver<ClanMessage>,
}

impl ClanActor {
    pub fn new(clan: Clan, rx: mpsc::Receiver<ClanMessage>) -> Self {
        Self { clan, rx }
    }

    pub async fn run(&mut self) {
        info!("Clan actor {} started", self.clan.id);
        while let Some(msg) = self.rx.recv().await {
            match msg {
                ClanMessage::GetSnapshot(tx) => {
                    let _ = tx.send(self.clan.clone());
                }
                ClanMessage::AddMemberOnline(handle) => {
                    self.clan.add_member_online(handle);
                }
                ClanMessage::RemoveMemberOnline(player_id) => {
                    self.clan.remove_member_online(player_id);
                }
                ClanMessage::AddMessage(cmg) => {
                    self.clan.clan_messages.push(cmg);
                    if self.clan.clan_messages.len() > 20 {
                        self.clan.clan_messages.remove(0);
                    }
                }
                ClanMessage::JoinDungeon(handle) => {
                    self.clan.doanh_trai_handle = Some(handle.clone());
                    self.clan.doanh_trai_id = Some(handle.id);
                }
                ClanMessage::UpdateMemberPower(player_id, power) => {
                    if let Some(member) = self.clan.members.iter_mut().find(|m| m.id == player_id) {
                        member.power_point = power;
                    }
                }
                ClanMessage::KickMember(player_id) => {
                    self.clan.remove_member(player_id);
                    self.clan.remove_member_online(player_id as u64);
                }
                ClanMessage::PromoteMember(player_id, role) => {
                    if let Some(member) = self.clan.members.iter_mut().find(|m| m.id == player_id) {
                        member.role = role;
                    }
                }
                ClanMessage::LeaveClan(player_id) => {
                    self.clan.remove_member(player_id as i32);
                    self.clan.remove_member_online(player_id);
                }
                ClanMessage::AddMember(member) => {
                    self.clan.add_member(member);
                }
                ClanMessage::UpdateDonate(player_id, donate, clan_point) => {
                    if let Some(member) = self.clan.members.iter_mut().find(|m| m.id == player_id) {
                        member.donate += donate;
                        member.clan_point += clan_point;
                        self.clan.capsule_clan += clan_point;
                    }
                }
                ClanMessage::UpdateAskPeaTime(player_id, time) => {
                    if let Some(member) = self.clan.members.iter_mut().find(|m| m.id == player_id) {
                        member.time_ask_pea = time;
                    }
                }
                ClanMessage::ClearDungeon => {
                    self.clan.doanh_trai_handle = None;
                    self.clan.doanh_trai_id = None;
                }
                ClanMessage::SetGoneDungeon(gone) => {
                    self.clan.have_gone_doanh_trai = gone;
                }
                ClanMessage::AddInvite(id) => {
                    self.clan.invites.push(id);
                }
                ClanMessage::RemoveInvite(id) => {
                    self.clan.invites.retain(|&i| i != id);
                }
                ClanMessage::UpdateMessage(cmg) => {
                    if let Some(msg) = self.clan.clan_messages.iter_mut().find(|m| m.id == cmg.id) {
                        *msg = cmg;
                    }
                }
            }
            let now = crate::utils::time::current_time_millis() as i64;
            if now - self.clan.last_time_save > 300_000 {
                self.clan.last_time_save = now;
            }
        }
        info!("Clan actor {} terminated", self.clan.id);
    }
}
