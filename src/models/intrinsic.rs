use crate::entities::intrinsic::Model as IntrinsicEntity;

#[derive(Clone, Debug)]
pub struct Intrinsic {
    pub id: i32,
    pub name: String,
    pub param_from_1: i16,
    pub param_to_1: i16,
    pub param_from_2: i16,
    pub param_to_2: i16,
    pub icon: i16,
    pub gender: i8,
    pub param1: i16,
    pub param2: i16,
}

impl Intrinsic {
    pub fn new() -> Self {
        Self {
            id: 0,
            name: String::new(),
            param_from_1: 0,
            param_to_1: 0,
            param_from_2: 0,
            param_to_2: 0,
            icon: 0,
            gender: 0,
            param1: 0,
            param2: 0,
        }
    }

    pub fn from_entity(entity: &IntrinsicEntity) -> Self {
        Self {
            id: entity.id,
            name: entity.name.clone(),
            param_from_1: entity.param_from_1 as i16,
            param_to_1: entity.param_to_1 as i16,
            param_from_2: entity.param_from_2 as i16,
            param_to_2: entity.param_to_2 as i16,
            icon: entity.icon as i16,
            gender: entity.gender as i8,
            param1: 0, // Will be set randomly
            param2: 0,  // Will be set randomly
        }
    }

    pub fn from_intrinsic(other: &Intrinsic) -> Self {
        Self {
            id: other.id,
            name: other.name.clone(),
            param_from_1: other.param_from_1,
            param_to_1: other.param_to_1,
            param_from_2: other.param_from_2,
            param_to_2: other.param_to_2,
            icon: other.icon,
            gender: other.gender,
            param1: other.param1,
            param2: other.param2,
        }
    }

    pub fn get_description(&self) -> String {
        self.name
            .replace("p0", &self.param_from_1.to_string())
            .replace("p1", &self.param_to_1.to_string())
            .replace("p2", &self.param_from_2.to_string())
            .replace("p3", &self.param_to_2.to_string())
    }

    pub fn get_name(&self) -> String {
        let mut name = self.name
            .replace("p0% đến p1", "p0")
            .replace("p2% đến p3", "p1")
            .replace("p0", &self.param1.to_string())
            .replace("p1", &self.param2.to_string());
        
        if self.id != 0 {
            name.push_str(&format!(" [{} đến {}]", self.param_from_1, self.param_to_1));
        }
        
        name
    }
}

#[derive(Clone, Debug)]
pub struct IntrinsicPlayer {
    pub count_open: u8,
    pub intrinsic: Intrinsic,
}

impl IntrinsicPlayer {
    pub fn new() -> Self {
        Self {
            count_open: 0,
            intrinsic: Intrinsic::new(),
        }
    }

    pub fn dispose(&mut self) {
        self.intrinsic = Intrinsic::new();
    }
}
