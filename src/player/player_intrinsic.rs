use crate::models::IntrinsicPlayer;
use crate::models::Intrinsic;

#[derive(Debug, Clone)]
pub struct PlayerIntrinsic {
    pub intrinsic: IntrinsicPlayer,
}

impl PlayerIntrinsic {
    pub fn new() -> Self {
        Self {
            intrinsic: IntrinsicPlayer::new(),
        }
    }

    pub fn from_intrinsic(intrinsic: Intrinsic) -> Self {
        let mut intrinsic_player = IntrinsicPlayer::new();
        intrinsic_player.intrinsic = intrinsic;
        Self {
            intrinsic: intrinsic_player,
        }
    }

    pub fn get_intrinsic(&self) -> &IntrinsicPlayer {
        &self.intrinsic
    }

    pub fn get_intrinsic_mut(&mut self) -> &mut IntrinsicPlayer {
        &mut self.intrinsic
    }

    pub fn has_intrinsic(&self) -> bool {
        self.intrinsic.intrinsic.id > 0
    }

    pub fn get_intrinsic_name(&self) -> String {
        self.intrinsic.intrinsic.get_name()
    }

    pub fn get_intrinsic_icon(&self) -> i16 {
        self.intrinsic.intrinsic.icon
    }

    pub fn get_intrinsic_level(&self) -> u8 {
        self.intrinsic.count_open
    }

    pub fn set_intrinsic_level(&mut self, level: u8) {
        self.intrinsic.count_open = level;
    }

    pub fn upgrade_intrinsic(&mut self) -> bool {
        if self.intrinsic.count_open < 8 {
            self.intrinsic.count_open += 1;
            true
        } else {
            false
        }
    }

    pub fn get_upgrade_cost(&self) -> i32 {
        if self.intrinsic.count_open < 8 {
            match self.intrinsic.count_open {
                0 => 10,
                1 => 20,
                2 => 40,
                3 => 80,
                4 => 160,
                5 => 320,
                6 => 640,
                7 => 1280,
                _ => 0,
            }
        } else {
            0
        }
    }

    pub fn can_upgrade(&self, player_gold: i64) -> bool {
        self.intrinsic.count_open < 8 && player_gold >= self.get_upgrade_cost() as i64
    }
}
