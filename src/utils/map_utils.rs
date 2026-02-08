#![allow(dead_code)]

use crate::map::Map;
use crate::utils::{self, Location};

pub struct MapUtils;

impl MapUtils {
    pub fn calculate_distance(source: &Location, target: &Location) -> f64 {
        let dx = (target.x - source.x) as f64;
        let dy = (target.y - source.y) as f64;
        (dx * dx + dy * dy).sqrt()
    }

    pub fn is_position_in_range(source: &Location, target: &Location, range: i16) -> bool {
        Self::calculate_distance(source, target) <= range as f64
    }
}
