use crate::entities::clan::Model as ClanEntity;
use crate::player::player_actor::PlayerHandle;
use serde::{Deserialize, Serialize};
use serde_json;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ClanMember {
    pub id: i32,
    pub name: String,
    pub head: i16,
    pub body: i16,
    pub leg: i16,
    pub role: i8,
    #[serde(rename = "power")]
    pub power_point: i64,
    pub donate: i32,
    pub receive_donate: i32,
    pub member_point: i32,
    pub clan_point: i32,
    pub join_time: i32,
    #[serde(rename = "ask_pea_time")]
    pub time_ask_pea: i64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ClanMessage {
    pub id: i32,
    pub message_type: i8,
    pub player_id: i32,
    pub player_name: String,
    pub player_power: i64,
    pub role: i8,
    pub time: i32,
    pub text: String,
    pub receive_donate: i8,
    pub max_donate: i8,
    pub is_new_message: i8,
    pub color: i8,
}

#[derive(Debug, Clone)]
pub struct Clan {
    pub id: i32,
    pub name: String,
    pub name_2: String,
    pub slogan: String,
    pub img_id: i32,
    pub power_point: i64,
    pub max_member: i8,
    pub level: i32,
    pub capsule_clan: i32,
    pub create_time: i32,
    pub members: Vec<ClanMember>,
    pub clan_messages: Vec<ClanMessage>,
    pub clan_message_id: i32,

    pub have_gone_doanh_trai: bool,
    pub doanh_trai_id: Option<i32>,

    // In-memory online members
    pub members_online: Vec<PlayerHandle>,

    // Dungeon related flags
    pub last_time_open_doanh_trai: i64,
    pub last_time_open_ban_do_kho_bau: i64,
    pub last_time_open_con_duong_ran_doc: i64,
    pub last_time_open_khi_gas_huy_diet: i64,

    pub doanh_trai_handle: Option<crate::dungoen::doanh_trai::handle::DoanhTraiHandle>,
    pub last_time_save: i64,
    pub invites: Vec<i32>,
}

impl Clan {
    pub const LEADER: i8 = 0;
    pub const DEPUTY: i8 = 1;
    pub const MEMBER: i8 = 2;

    pub fn new() -> Self {
        Self {
            id: 0,
            name: String::new(),
            name_2: String::new(),
            slogan: String::new(),
            img_id: 0,
            power_point: 0,
            max_member: 10,
            level: 1,
            capsule_clan: 0,
            create_time: 0,
            doanh_trai_id: None,
            have_gone_doanh_trai: false,
            members: Vec::new(),
            clan_messages: Vec::new(),
            clan_message_id: 0,
            members_online: Vec::new(),
            last_time_open_doanh_trai: 0,
            last_time_open_ban_do_kho_bau: 0,
            last_time_open_con_duong_ran_doc: 0,
            last_time_open_khi_gas_huy_diet: 0,
            doanh_trai_handle: None,
            last_time_save: 0,
            invites: Vec::new(),
        }
    }

    pub fn from_entity(entity: ClanEntity) -> Self {
        let members: Vec<ClanMember> = serde_json::from_str(&entity.members).unwrap_or_default();
        Self {
            id: entity.id,
            name: entity.name,
            name_2: entity.name_2,
            slogan: entity.slogan,
            img_id: entity.img_id,
            power_point: entity.power_point,
            max_member: entity.max_member as i8,
            level: entity.level,
            capsule_clan: entity.clan_point,
            create_time: entity.create_time.timestamp() as i32,
            members,
            doanh_trai_id: None,
            have_gone_doanh_trai: false,
            clan_messages: Vec::new(),
            clan_message_id: 0,
            members_online: Vec::new(),
            last_time_open_doanh_trai: 0,
            last_time_open_ban_do_kho_bau: 0,
            last_time_open_con_duong_ran_doc: 0,
            last_time_open_khi_gas_huy_diet: 0,
            doanh_trai_handle: None,
            last_time_save: 0,
            invites: Vec::new(),
        }
    }

    pub fn get_role(&self, player_id: i32) -> i8 {
        for m in &self.members {
            if m.id == player_id {
                return m.role;
            }
        }
        -1
    }

    pub fn is_leader(&self, player_id: i32) -> bool {
        self.get_role(player_id) == Self::LEADER
    }

    pub fn is_deputy(&self, player_id: i32) -> bool {
        self.get_role(player_id) == Self::DEPUTY
    }

    pub fn add_member(&mut self, member: ClanMember) {
        if !self.members.iter().any(|m| m.id == member.id) {
            self.members.push(member);
        }
    }

    pub fn remove_member(&mut self, player_id: i32) {
        self.members.retain(|m| m.id != player_id);
    }

    pub fn get_curr_members(&self) -> i8 {
        self.members.len() as i8
    }

    pub fn add_member_online(&mut self, handle: PlayerHandle) {
        if !self.members_online.iter().any(|h| h.id == handle.id) {
            self.members_online.push(handle);
        }
    }

    pub fn remove_member_online(&mut self, player_id: u64) {
        self.members_online.retain(|h| h.id != player_id);
    }

    pub fn get_leader_name(&self) -> String {
        for m in &self.members {
            if m.role == Self::LEADER {
                return m.name.clone();
            }
        }
        "Không có".to_string()
    }
}
