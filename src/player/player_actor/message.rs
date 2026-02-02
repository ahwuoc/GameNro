use crate::network::message::Message;
use crate::player::player::Player;

pub enum PlayerMessage {
    NetworkMessage(Message),
    Chat {
        text: String,
    },

    SendPacket(Message),

    Injured {
        damage: u64,
        piercing: bool,
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
        type_combine: crate::combine::combine_type::CombineType,
        npc_id: i16,
    },
    CombineShowInfo {
        index: Vec<i16>,
    },
    CombineConfirm,
    ItemAction {
        type_action: crate::item::type_item_inventory::TypeItemAction,
        where_item: i8,
        index: i8,
    },
    GetItem {
        type_item_inventory: crate::item::type_item_inventory::TypeItemInventory,
        index: i8,
    },
    HoiSinh,
    UpdateTick,
    GetSnapshot(tokio::sync::oneshot::Sender<Player>),
    UpdateSkillShortcuts {
        shortcuts: Vec<u8>,
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
        space_type: crate::map::services::change_map_models::SpaceShipType,
    },
    CreateMenu {
        npc_id: i16,
        npc_say: String,
        menu_options: Vec<String>,
        state: crate::constant::menu_enum::MenuId,
    },
    FinishLoadMap,
    Modify(Box<dyn FnOnce(&mut Player) + Send>),

    /// Logout and save data
    Logout,
}
