use sea_orm::entity::prelude::*;

/// Definies the type of task actions for the data-driven task system.
#[derive(
    Debug,
    PartialEq,
    Eq,
    Clone,
    Copy,
    serde::Serialize,
    serde::Deserialize,
    EnumIter,
    DeriveActiveEnum,
)]
#[sea_orm(rs_type = "String", db_type = "String(Some(32))")]
pub enum TaskType {
    /// Speak with an NPC
    #[sea_orm(string_value = "TALK_NPC")]
    TalkNpc,
    /// Kill a specific Mob template
    #[sea_orm(string_value = "KILL_MOB")]
    KillMob,
    /// Kill a specific Boss template
    #[sea_orm(string_value = "KILL_BOSS")]
    KillBoss,
    /// Pick up a specific ItemMap
    #[sea_orm(string_value = "PICK_ITEM")]
    PickItem,
    /// Use an item from inventory
    #[sea_orm(string_value = "USE_ITEM")]
    UseItem,
    /// Enter a specific Map
    #[sea_orm(string_value = "GO_TO_MAP")]
    GoToMap,
    /// Interact with an NPC Menu option
    #[sea_orm(string_value = "CONFIRM_MENU")]
    ConfirmMenu,
    /// Specialized logic handled by custom code/scripts (Power, TiemNang, Clan, etc.)
    #[sea_orm(string_value = "TASK_SCRIPTS")]
    TaskScripts,
    /// Reward reached a certain power level
    #[sea_orm(string_value = "TASK_POWER")]
    TaskPower,
}
