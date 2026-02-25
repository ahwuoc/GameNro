pub mod boss;
pub mod charms;
pub mod fusion;
pub mod interaction_state;
pub mod n_point;
pub mod player_friend;
pub mod player_intrinsic;
pub mod player_item_time;
pub mod player_skill;

#[derive(Debug, Clone, Copy)]
enum PointType {
    Hp,
    Mp,
    Dame,
    Def,
    Crit,
}
impl TryFrom<u8> for PointType {
    type Error = &'static str;
    fn try_from(value: u8) -> Result<Self, Self::Error> {
        match value {
            0 => Ok(PointType::Hp),
            1 => Ok(PointType::Mp),
            2 => Ok(PointType::Dame),
            3 => Ok(PointType::Def),
            4 => Ok(PointType::Crit),
            _ => Err("Invalid point type"),
        }
    }
}
