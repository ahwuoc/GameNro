pub mod cmd {
    pub const LOGIN: i8 = 0;
    pub const KEY: i8 = -27;
    pub const NOT_LOGIN: i8 = -29;
    pub const GET_IMAGES_SOURCE: i8 = -74;
    pub const SEND_ALTER_MESSAGE: i8 = -26;
    pub const CHAT: i8 = 44;

    // Combat CMD constants
    pub const ATTACK_MOB: i8 = 54;
    pub const PLAYER_ATTACK_PLAYER: i8 = -60;
    pub const USE_SKILL: i8 = -45;
    pub const SELECT_SKILL: i8 = 34;
    pub const GET_EFFECT_TEMPLATE: i8 = -66;
    pub const CHANGE_TYPE_PK: i8 = -30;

    // Item CMD constants
    pub const PICK_ITEM: i8 = -20;

    // Change Map CMD constants
    pub const OPEN_ZONE_UI: i8 = 29;
    pub const CHANGE_ZONE: i8 = 21;
    pub const CHANGE_MAP_WAYPOINT: i8 = -33;
    pub const CHANGE_MAP_WAYPOINT_ALT: i8 = -23;
    pub const FINISH_LOAD_MAP: i8 = -39;
    pub const GO_HOME: i8 = -15;
    pub const EFFECT_CHANGE_MAP: i8 = -105;
    pub const CAPSULE_MENU: i8 = -91;
    pub const MAP_INFO: i8 = -24;
    pub const PLAYER_LEAVE: i8 = -6;
    pub const SPACESHIP_ARRIVE: i8 = -65;
}
