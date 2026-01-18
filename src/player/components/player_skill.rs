use crate::models::skill_model::Skill;

#[derive(Debug, Clone)]
pub struct PlayerSkill {
    pub skills: Vec<Skill>,
    pub skill_select: Option<Skill>,
    pub skill_shortcut: Vec<u8>,
}

impl PlayerSkill {
    pub fn new() -> Self {
        PlayerSkill {
            skills: Vec::new(),
            skill_select: None,
            skill_shortcut: vec![0; 10],
        }
    }
}
