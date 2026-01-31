use crate::models::skill_model::Skill;
use crate::templates::skill_template_manager;
use serde::Deserialize;

#[derive(Debug, Clone, Deserialize)]
pub struct SkillData {
    #[serde(default)]
    pub id: i16,
    #[serde(default)]
    pub point: i8,
    #[serde(alias = "powerRequire", alias = "power_require", default)]
    pub pow_require: i64,
    #[serde(alias = "manaUse", alias = "mana_use", default)]
    pub mana_use: i16,
    #[serde(alias = "coolDown", alias = "cool_down", default)]
    pub cool_down: i32,
    #[serde(default)]
    pub dx: i16,
    #[serde(default)]
    pub dy: i16,
    #[serde(alias = "maxFight", alias = "max_fight", default)]
    pub max_fight: i8,
    #[serde(default)]
    pub damage: i16,
    #[serde(default)]
    pub price: i16,
    #[serde(alias = "info", default)]
    pub more_info: String,
}

impl SkillData {
    pub fn to_player_skill(&self, template_id: i32) -> Skill {
        let mut skill = Skill::new(template_id);
        skill.point = self.point;
        skill.power_require = self.pow_require;
        skill.mana_use = self.mana_use;
        skill.cool_down = self.cool_down;
        skill.dx = self.dx;
        skill.dy = self.dy;
        skill.max_fight = self.max_fight as i16;
        skill.damage = self.damage;
        skill.price = self.price;
        skill.more_info = self.more_info.clone();
        skill
    }
}

pub fn parse_skills_json(skills_json: &str) -> Vec<SkillData> {
    if skills_json.is_empty() {
        return Vec::new();
    }

    if let Ok(skill_strings) = serde_json::from_str::<Vec<String>>(skills_json) {
        let skills: Vec<SkillData> = skill_strings
            .iter()
            .filter_map(|s| serde_json::from_str::<SkillData>(s).ok())
            .collect();

        if !skills.is_empty() {
            return skills;
        }
    }

    if let Ok(skills) = serde_json::from_str::<Vec<SkillData>>(skills_json) {
        return skills;
    }

    let cleaned = skills_json
        .replace("[\"", "[")
        .replace("\"[", "[")
        .replace("\"]", "]")
        .replace("]\"", "]")
        .replace("}\",\"{", "},{")
        .replace("\\\"", "\"");

    if let Ok(skills) = serde_json::from_str::<Vec<SkillData>>(&cleaned) {
        return skills;
    }

    println!(
        "[SKILL_UTIL] Failed to parse skills JSON: {:.100}...",
        skills_json
    );
    Vec::new()
}

pub async fn create_skill(temp_id: i32, level: i32) -> Option<Skill> {
    if let Some(template) = skill_template_manager::get(temp_id) {
        if level >= 1 && (level as usize) <= template.skills.len() {
            let skill_data = &template.skills[(level - 1) as usize];
            let mut player_skill = Skill::new(temp_id);
            player_skill.skill_id = skill_data.skill_id; // Copy skill_id from template
            player_skill.point = skill_data.point;
            player_skill.power_require = skill_data.pow_require;
            player_skill.mana_use = skill_data.mana_use;
            player_skill.cool_down = skill_data.cool_down;
            player_skill.dx = skill_data.dx;
            player_skill.dy = skill_data.dy;
            player_skill.max_fight = skill_data.max_fight as i16;
            player_skill.damage = skill_data.damage;
            player_skill.price = skill_data.price;
            player_skill.more_info = skill_data.more_info.clone();
            return Some(player_skill);
        }

        if !template.skills.is_empty() {
            println!(
                "[SKILL_UTIL] Level {} out of range for template id={}, max={}",
                level,
                temp_id,
                template.skills.len()
            );
        }
    } else {
        println!("[SKILL_UTIL] Template not found for id={}", temp_id);
    }
    None
}

pub async fn create_skill_level0(temp_id: i32) -> Option<Skill> {
    let mut skill = Skill::new(temp_id);
    skill.point = 0;
    Some(skill)
}

pub fn get_time_stun(skill_point: i8) -> u64 {
    (skill_point as u64 + 2) * 1000
}

pub fn get_range_stun(skill_point: i8) -> i16 {
    120 + (skill_point as i16 * 30)
}

pub fn get_time_shield(skill_point: i8) -> u64 {
    (skill_point as u64 + 2) * 5000
}

pub fn get_range_qckk(skill_point: i8) -> i16 {
    300 + (skill_point as i16 * 100)
}

pub fn get_time_dctt(skill_point: i8) -> u64 {
    (skill_point as u64 + 2) * 500
}

pub fn get_time_thoi_mien(skill_point: i8) -> u64 {
    (skill_point as u64 + 4) * 1000
}

pub fn get_time_monkey(skill_point: i8) -> u64 {
    (skill_point as u64 + 5) * 10000
}

pub fn get_percent_charge(skill_point: i8) -> i32 {
    skill_point as i32 + 3
}

pub fn get_percent_hp_huyt_sao(skill_point: i8) -> i32 {
    (skill_point as i32 + 3) * 10
}

pub fn get_range_bom(skill_point: i8) -> i16 {
    400 + (skill_point as i16 * 30)
}
