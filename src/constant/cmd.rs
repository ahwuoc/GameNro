pub mod cmd {
    // ===== Auth & Login =====
    pub const LOGIN: i8 = 0;
    pub const KEY: i8 = -27;
    pub const NOT_LOGIN: i8 = -29;
    pub const NOT_LOGIN_ALT: i8 = -93;
    pub const NOT_MAP: i8 = -28;
    pub const FINISH_UPDATE: i8 = -38;

    // ===== Data Loading =====
    pub const GET_IMAGES_SOURCE: i8 = -74;
    pub const UPDATE_DATA: i8 = -87;
    pub const GET_MOB_TEMPLATE: i8 = 11;
    pub const GET_ITEM_BG_TEMPLATE: i8 = -32;
    pub const GET_ICON: i8 = -67;
    pub const GET_IMAGE_BY_NAME: i8 = 66;
    pub const GET_CAPTIONS: i8 = -41;

    // ===== Chat =====
    pub const CHAT: i8 = 44;
    pub const SEND_ALTER_MESSAGE: i8 = -26;
    pub const THONG_BAO: i8 = -25;

    // ===== Combat =====
    pub const ATTACK_MOB: i8 = 54;
    pub const PLAYER_ATTACK_PLAYER: i8 = -60;
    pub const USE_SKILL: i8 = -45;
    pub const SELECT_SKILL: i8 = 34;
    pub const GET_EFFECT_TEMPLATE: i8 = -66;
    pub const HOI_SINH: i8 = -16;

    // ===== Player Movement & Map =====
    pub const PLAYER_MOVE: i8 = -7;
    pub const OPEN_ZONE_UI: i8 = 29;
    pub const CHANGE_ZONE: i8 = 21;
    pub const CHANGE_MAP_WAYPOINT: i8 = -33;
    pub const CHANGE_MAP_WAYPOINT_ALT: i8 = -23;
    pub const FINISH_LOAD_MAP: i8 = -39;
    pub const GO_HOME: i8 = -15;
    pub const EFFECT_CHANGE_MAP: i8 = -105;
    pub const CAPSULE_MENU: i8 = -91;
    pub const MAP_INFO: i8 = -24;
    pub const MAP_CLEAR: i8 = -22;
    pub const PLAYER_LEAVE: i8 = -6;
    pub const SPACESHIP_ARRIVE: i8 = -65;

    // ===== Item =====
    pub const PICK_ITEM: i8 = -20;
    pub const GET_ITEM: i8 = -40;
    pub const DO_ITEM: i8 = -43;
    pub const BUY_ITEM: i8 = 6;
    pub const SELL_ITEM: i8 = 7;
    pub const COMBINE_INFO: i8 = -81;

    // ===== NPC =====
    pub const NPC_MENU: i8 = 33;
    pub const NPC_SELECT: i8 = 32;
    pub const DAU_THAN_CONFIRM: i8 = 22;

    // ===== Skill =====
    pub const SKILL_SHORTCUT_UPDATE: i8 = -113;
    pub const CHANGE_TYPE_PK: i8 = -30;

    // ===== Pet =====
    pub const SHOW_INFO_PET: i8 = -107;
    pub const PET_CHANGE_STATUS: i8 = -108;

    // ===== Intrinsic =====
    pub const INTRINSIC_MENU: i8 = 112;

    // ===== Magic Tree =====
    pub const MAGIC_TREE: i8 = -34;

    // ===== Player Info =====
    pub const GET_PLAYER_MENU: i8 = -79;
    pub const CHECK_MOVE: i8 = -78;
    pub const FLAG_BAG_ICON: i8 = -63;

    // ===== Clan =====
    pub const CLAN_MESSAGE: i8 = -51;
    pub const GET_MY_CLAN: i8 = -53;
    pub const GET_CLAN_LIST: i8 = -47;
    pub const GET_MEMBER_LIST: i8 = -50;
    pub const CLAN_INFO: i8 = -46;
    pub const CLAN_MEMBER_INFO: i8 = -49;
    pub const CLAN_DONATE: i8 = -54;
    pub const CLAN_REMOTE: i8 = -55;
    pub const CLAN_INVITE: i8 = -57;
    pub const CLAN_JOIN: i8 = -48;
    pub const RADAR: i8 = 127;

    // ===== PVP =====
    pub const PVP_CMD: i8 = -59;
}
