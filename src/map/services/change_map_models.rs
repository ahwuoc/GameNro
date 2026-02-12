#[derive(Debug, Clone, Copy, PartialEq, Eq)]
#[repr(i8)]
pub enum SpaceShipType {
    Auto = -1,
    None = 0,
    Default = 1,
    TeleportYardrat = 2,
    Tennis = 3,
}

impl SpaceShipType {
    pub fn from_i8(value: i8) -> Option<Self> {
        match value {
            -1 => Some(SpaceShipType::Auto),
            0 => Some(SpaceShipType::None),
            1 => Some(SpaceShipType::Default),
            2 => Some(SpaceShipType::TeleportYardrat),
            3 => Some(SpaceShipType::Tennis),
            _ => None,
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
#[repr(i8)]
pub enum ChangeMapType {
    Capsule = 0,
    BlackBall = 1,
    MaBu = 2,
}

impl ChangeMapType {
    pub fn from_i8(value: i8) -> Option<Self> {
        match value {
            0 => Some(ChangeMapType::Capsule),
            1 => Some(ChangeMapType::BlackBall),
            2 => Some(ChangeMapType::MaBu),
            _ => None,
        }
    }
}

#[derive(Debug, Clone, PartialEq)]
pub enum WaypointChangeResult {
    /// Map change successful.
    Success {
        destination_map_id: i32,
        destination_zone_id: i32,
        x: i16,
        y: i16,
    },
    NoWaypointFound,
    TaskRequirementNotMet {
        required_task_id: i32,
    },
    InvalidPlayerZone,
    DestinationUnavailable,
}

#[derive(Debug, Clone, PartialEq)]
pub enum GoHomeResult {
    Success {
        home_map_id: i32,
        zone_id: i32,
        x: i16,
        y: i16,
        space_type: SpaceShipType,
    },
    NoAvailableZone,
    PlayerIsBoss,
}

#[derive(Debug, Clone, PartialEq)]
pub enum SpaceshipTravelResult {
    Success {
        map_id: i32,
        zone_id: i32,
        x: i16,
        y: i16,
        space_type: SpaceShipType,
        healing_result: SpaceshipHealingResult,
    },
    NoAvailableZone,
    InvalidDestination,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SpaceshipHealingResult {
    NoHealing,
    HealedToFull,
    RevivedFullHp,
    RevivedMinimalHp,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SpaceshipSendType {
    AllPlayersInMap,
    SelfOnly,
    OthersInMap,
}

#[derive(Debug, Clone, PartialEq)]
pub enum ChangeMapResult {
    /// Change map successful.
    Success {
        map_id: i32,
        zone_id: i32,
        x: i16,
        y: i16,
        cold_planet_effect: Option<ColdPlanetEffect>,
    },
    ZoneFull,
    TaskRequirementNotMet {
        required_task_id: i32,
    },
    InvalidZone,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ColdPlanetEffect {
    Entering,
    Leaving,
}

#[derive(Debug, Clone, PartialEq)]
pub enum CapsuleChangeResult {
    Success { map_id: i32, zone_id: i32 },
    InvalidDestination,
    DestinationUnavailable,
}

#[derive(Debug, Clone)]
pub struct CapsuleDestination {
    pub map_id: i32,
    pub map_name: String,
    pub planet_name: String,
}

#[derive(Debug, Clone, PartialEq)]
pub enum MapAccessResult {
    Allowed,
    TaskRequirementNotMet {
        required_task_id: i32,
    },
    GenderRestricted {
        player_gender: i8,
        allowed_gender: i8,
    },
    InvalidZone,
}
