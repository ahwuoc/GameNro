/// Enum defining the types of spaceships available in the game.
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

/// Represents the outcome of an attempt to change map via a waypoint.
#[derive(Debug, Clone, PartialEq)]
pub enum WaypointChangeResult {
    /// Map change successful.
    Success {
        destination_map_id: i32,
        destination_zone_id: i32,
        x: i16,
        y: i16,
    },
    /// No waypoint exists at the player's current position.
    NoWaypointFound,
    /// Player does not meet the task requirements to enter the map.
    TaskRequirementNotMet { required_task_id: i32 },
    /// Player is currently in an invalid zone/state.
    InvalidPlayerZone,
    /// The destination map/zone is unavailable (e.g., full or offline).
    DestinationUnavailable,
}

/// Represents the outcome of the "Go Home" operation.
#[derive(Debug, Clone, PartialEq)]
pub enum GoHomeResult {
    /// Successfully determined home location.
    Success {
        home_map_id: i32,
        zone_id: i32,
        x: i16,
        y: i16,
        space_type: SpaceShipType,
    },
    /// No valid zone found for the home map.
    NoAvailableZone,
    /// Bosses are not allowed to use the Go Home feature.
    PlayerIsBoss,
}

/// Represents the outcome of traveling by spaceship.
#[derive(Debug, Clone, PartialEq)]
pub enum SpaceshipTravelResult {
    /// Travel successful.
    Success {
        map_id: i32,
        zone_id: i32,
        x: i16,
        y: i16,
        space_type: SpaceShipType,
        healing_result: SpaceshipHealingResult,
    },
    /// Destination zone is not available.
    NoAvailableZone,
    /// Invalid map or zone ID provided.
    InvalidDestination,
}

/// Result of healing effects applied during spaceship travel.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SpaceshipHealingResult {
    /// No healing applied.
    NoHealing,
    /// Player fully healed (e.g., using Tennis spaceship).
    HealedToFull,
    /// Player revived and fully healed.
    RevivedFullHp,
    /// Player revived with minimal HP (standard spaceship).
    RevivedMinimalHp,
}

/// Defines who receives the spaceship arrival notification.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SpaceshipSendType {
    /// Broadcast to all players in the map (including self).
    AllPlayersInMap,
    /// Send only to the player traveling.
    SelfOnly,
    /// Broadcast to other players in the map interactions.
    OthersInMap,
}

/// General result for map change operations.
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
    /// Destination zone is full.
    ZoneFull,
    /// Task requirements not met.
    TaskRequirementNotMet { required_task_id: i32 },
    /// Invalid zone specified.
    InvalidZone,
}

/// Effect applied when entering/leaving Cold Planet maps.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ColdPlanetEffect {
    /// Stats reduced upon entering.
    Entering,
    /// Stats restored upon leaving.
    Leaving,
}

/// Outcome of using a capsule to change maps.
#[derive(Debug, Clone, PartialEq)]
pub enum CapsuleChangeResult {
    /// Travel successful.
    Success { map_id: i32, zone_id: i32 },
    /// Invalid selection index.
    InvalidDestination,
    /// Destination cannot be reached.
    DestinationUnavailable,
}

/// Information about a destination in the capsule menu.
#[derive(Debug, Clone)]
pub struct CapsuleDestination {
    pub map_id: i32,
    pub map_name: String,
    pub planet_name: String,
}

/// Validation result for map access permission.
#[derive(Debug, Clone, PartialEq)]
pub enum MapAccessResult {
    /// Access allowed.
    Allowed,
    /// Blocked by task progress.
    TaskRequirementNotMet { required_task_id: i32 },
    /// Blocked by gender restriction (e.g., home maps).
    GenderRestricted {
        player_gender: i8,
        allowed_gender: i8,
    },
    /// Map/Zone is invalid.
    InvalidZone,
}
