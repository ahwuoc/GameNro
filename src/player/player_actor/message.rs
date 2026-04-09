// @workflow: /add_packet - Defines internal actor messages for new features
use sea_orm::sea_query::extension::postgres::Type;

use crate::combine::combine_type::CombineType;
use crate::constant::task_type::TaskType;
use crate::item::type_item_inventory::{TypeItemAction, TypeItemInventory};
use crate::map::SpaceShipType;
use crate::network::message::Message;
use crate::player::player::Player;
use crate::player::player_actor::handle::PlayerHandle;
use crate::player::player_actor::pet::message::PetMessage;
use crate::player::player_actor::pet::PetHandle;
use crate::services::player_tnsm_services::TypeTNSM;

#[derive(Debug)]
pub enum MagicTreeMsg {
    OpenOrLoad(u8), // action 1 = open menu, 2 = load
    Harvest,
    FastRespawn,
    Upgrade,
    FastUpgrade,
    Unupgrade,
}

pub enum PlayerMessage {
    NetworkMessage(Message),
    TaskAction(TaskType, String),
    Chat {
        text: String,
    },
    AddTNSM {
        type_tnsm: TypeTNSM,
        param: i64,
        is_ori: bool,
    },

    SendPacket(Message),

    Injured {
        damage: u64,
        piercing: bool,
        from_mob: bool,
        attacker_id: Option<u64>,
    },
    AttackMob {
        mob_id: i32,
    },
    SelectSkill {
        skill_template_id: i32,
    },
    UseSkill {
        msg: Message,
    },
    PickItem {
        item_map_id: i32,
    },
    ApplyHuytSaoBuff {
        percent_hp: i32,
    },
    CombineOpenTab {
        type_combine: CombineType,
        npc_id: i16,
    },
    CombineShowInfo {
        index: Vec<i16>,
    },
    CombineConfirm,
    ItemAction {
        type_action: TypeItemAction,
        where_item: i8,
        index: i8,
    },
    GetItem {
        type_item_inventory: TypeItemInventory,
        index: i8,
    },
    HoiSinh,
    UpdateTick,
    GetSnapshot(tokio::sync::oneshot::Sender<Player>),
    UpdateSkillShortcuts {
        shortcuts: Vec<i8>,
    },
    IncreasePoint {
        type_increment: u8,
        point: i16,
    },
    Move {
        x: i16,
        y: i16,
    },
    ChangeMap {
        map_id: i32,
        zone_id: i32,
        x: i16,
        y: i16,
        space_type: SpaceShipType,
    },
    CreateMenu {
        npc_id: i16,
        npc_say: String,
        menu_options: Vec<String>,
        state: crate::constant::menu_enum::MenuId,
    },
    FinishLoadMap,
    Modify(Box<dyn FnOnce(&mut Player) + Send>),

    Logout,
    HandleAnTroi(bool, u64, Option<u64>),
    SetPetHandle(PetHandle),
    Fusion {
        type_fusion: i8,
        template_id: i32,
    },
    Unfusion,
    Pet(PetMessage),
    UpdatePetUI(Box<crate::player::player_actor::pet::Pet>, Option<String>),

    ShowInfoPet,
    AttackPlayer {
        player_id: i32,
    },
    PetAskPea {
        pet_id: u64,
    },
    ClearPetHandle,

    MagicTree(MagicTreeMsg),

    RadarAction(i8, Message),
    ChangeMapCapsule(i32),
    ChangeMapBlackBall(i8),
    SendInfoTo(PlayerHandle),
    SendInfoToAll(Vec<PlayerHandle>),
    CallTrainingBoss {
        boss_id: String,
        is_thachdau: bool,
    },
}
