use sea_orm::FromJsonQueryResult;
use serde::{Deserialize, Serialize};

#[derive(Debug, Serialize, Deserialize, Clone, FromJsonQueryResult, PartialEq, Eq, Default)]
pub struct BossChat {
    #[serde(default)]
    pub s: Vec<String>,
    #[serde(default)]
    pub m: Vec<String>,
    #[serde(default)]
    pub e: Vec<String>,
}

#[derive(Debug, Serialize, Deserialize, Clone, FromJsonQueryResult, PartialEq, Eq, Default)]
pub struct BossStage {
    #[serde(default)]
    pub name: Option<String>,
    #[serde(default)]
    pub hp: i32,
    #[serde(default)]
    pub mp: i32,
    #[serde(default)]
    pub dame: i32,
    #[serde(default)]
    pub def: i32,
    #[serde(default)]
    pub outfit: Vec<i16>,
    #[serde(alias = "skill", default)]
    pub skills: Vec<Vec<i32>>,
    #[serde(default)]
    pub chat: BossChat,
    #[serde(default)]
    pub together: Vec<String>,
}

#[derive(Debug, Serialize, Deserialize, Clone, FromJsonQueryResult, PartialEq, Eq, Default)]
pub struct MapJoin(pub Vec<i32>);

#[derive(Debug, Serialize, Deserialize, Clone, FromJsonQueryResult, PartialEq, Eq, Default)]
pub struct BossStages(pub Vec<BossStage>);

#[derive(Debug, Serialize, Deserialize, Clone, FromJsonQueryResult, PartialEq, Eq, Default)]
pub struct BossOutfit(pub Vec<i16>);

#[derive(Debug, Serialize, Deserialize, Clone, FromJsonQueryResult, PartialEq, Eq, Default)]
pub struct BossSkills(pub Vec<Vec<i32>>);

#[derive(Debug, Serialize, Deserialize, Clone, FromJsonQueryResult, PartialEq, Eq, Default)]
pub struct BossTogether(pub Vec<String>);
