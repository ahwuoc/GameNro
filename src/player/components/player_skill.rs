use crate::models::skill_model::Skill;

#[derive(Debug, Clone)]
pub struct PlayerSkill {
    pub skills: Vec<Skill>,
    pub skill_select: Option<Skill>,
    pub skill_shortcut: Vec<i8>,
    pub prepare_qckk: bool,
    pub last_time_prepare_qckk: u64,
    pub prepare_laze: bool,
    pub last_time_prepare_laze: u64,
    pub prepare_tu_sat: bool,
    pub last_time_prepare_tu_sat: u64,
}

impl PlayerSkill {
    pub fn new() -> Self {
        PlayerSkill {
            skills: Vec::new(),
            skill_select: None,
            skill_shortcut: vec![-1; 10],
            prepare_qckk: false,
            last_time_prepare_qckk: 0,
            prepare_laze: false,
            last_time_prepare_laze: 0,
            prepare_tu_sat: false,
            last_time_prepare_tu_sat: 0,
        }
    }
}
