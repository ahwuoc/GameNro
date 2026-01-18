use crate::entities::skill_template;
use crate::utils::skill_util;
use dashmap::DashMap;
use once_cell::sync::Lazy;
use sea_orm::{DatabaseConnection, EntityTrait};
use std::collections::HashMap;

#[derive(Debug, Clone)]
pub struct Skill {
    pub skill_id: i16,
    pub point: i8,
    pub pow_require: i64,
    pub mana_use: i16,
    pub cool_down: i32,
    pub dx: i16,
    pub dy: i16,
    pub max_fight: i8,
    pub damage: i16,
    pub price: i16,
    pub more_info: String,
}

#[derive(Debug, Clone)]
pub struct SkillTemplate {
    pub id: i8,
    pub class_id: i32,
    pub name: String,
    pub max_point: i8,
    pub mana_use_type: i8,
    pub r#type: i8,
    pub icon_id: i16,
    pub dam_info: String,
    pub skills: Vec<Skill>,
}

#[derive(Debug, Clone)]
pub struct NClass {
    pub class_id: i32,
    pub name: String,
    pub skill_templates: Vec<SkillTemplate>,
}

static SKILL_TEMPLATES: Lazy<DashMap<i32, SkillTemplate>> = Lazy::new(|| DashMap::new());
static NCLASSES: Lazy<DashMap<i32, NClass>> = Lazy::new(|| DashMap::new());

pub async fn load(db: &DatabaseConnection) -> anyhow::Result<()> {
    let templates = skill_template::Entity::find().all(db).await?;

    let mut nclass_map: HashMap<i32, Vec<SkillTemplate>> = HashMap::new();

    for template in templates {
        // Parse skills JSON once here
        let skills = parse_skills(&template.skills);

        let skill_template = SkillTemplate {
            id: template.id as i8,
            class_id: template.nclass_id,
            name: template.name.clone(),
            max_point: template.max_point as i8,
            mana_use_type: template.mana_use_type as i8,
            r#type: template.r#type as i8,
            icon_id: template.icon_id as i16,
            dam_info: template.dam_info.clone(),
            skills,
        };

        // Store in template cache
        SKILL_TEMPLATES.insert(template.id, skill_template.clone());

        // Group by nclass
        nclass_map
            .entry(template.nclass_id)
            .or_default()
            .push(skill_template);
    }

    // Build NClass cache
    for (nclass_id, mut templates) in nclass_map {
        // Sort by id for consistent ordering
        templates.sort_by_key(|t| t.id);

        let nclass = NClass {
            class_id: nclass_id,
            name: get_nclass_name(nclass_id),
            skill_templates: templates,
        };
        NCLASSES.insert(nclass_id, nclass);
    }

    println!(
        "Loaded {} skill templates, {} nclasses into cache",
        SKILL_TEMPLATES.len(),
        NCLASSES.len()
    );
    Ok(())
}

fn parse_skills(skills_json: &str) -> Vec<Skill> {
    skill_util::parse_skills_json(skills_json)
        .into_iter()
        .map(|data| Skill {
            skill_id: data.id,
            point: data.point,
            pow_require: data.pow_require,
            mana_use: data.mana_use,
            cool_down: data.cool_down,
            dx: data.dx,
            dy: data.dy,
            max_fight: data.max_fight,
            damage: data.damage,
            price: data.price,
            more_info: data.more_info,
        })
        .collect()
}

fn get_nclass_name(nclass_id: i32) -> String {
    match nclass_id {
        0 => "Trái Đất".to_string(),
        1 => "Namếc".to_string(),
        2 => "Xayda".to_string(),
        _ => format!("NClass {}", nclass_id),
    }
}

/// Get a parsed skill template by its id
pub fn get(id: i32) -> Option<SkillTemplate> {
    SKILL_TEMPLATES.get(&id).map(|v| v.clone())
}

/// Get all parsed skill templates
pub fn get_all() -> Vec<SkillTemplate> {
    SKILL_TEMPLATES
        .iter()
        .map(|kv| kv.value().clone())
        .collect()
}

/// Get all skill templates grouped by nclass_id
pub fn get_by_nclass(nclass_id: i32) -> Vec<SkillTemplate> {
    SKILL_TEMPLATES
        .iter()
        .filter(|kv| kv.value().class_id == nclass_id)
        .map(|kv| kv.value().clone())
        .collect()
}

/// Get all NClasses with their skill templates
pub fn get_all_nclasses() -> Vec<NClass> {
    let mut nclasses: Vec<NClass> = NCLASSES.iter().map(|kv| kv.value().clone()).collect();
    nclasses.sort_by_key(|n| n.class_id);
    nclasses
}

/// Get raw skills JSON for a template (for skill_util::create_skill)
pub fn get_raw_skills(id: i32) -> Option<String> {
    // Not needed anymore since we store parsed skills
    None
}
