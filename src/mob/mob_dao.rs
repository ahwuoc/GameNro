use sea_orm::*;
use crate::entities::mob_template;
use crate::mob::RtMob;

pub struct MobDao;

impl MobDao {
    pub async fn load_mob_template(
        database: &DatabaseConnection,
        template_id: i32,
    ) -> Result<Option<mob_template::Model>, DbErr> {
        let template = mob_template::Entity::find_by_id(template_id)
            .one(database)
            .await?;

        Ok(template)
    }

    pub async fn load_all_mob_templates(
        database: &DatabaseConnection,
    ) -> Result<Vec<mob_template::Model>, DbErr> {
        let templates = mob_template::Entity::find().all(database).await?;
        Ok(templates)
    }

    pub async fn create_mob_from_template(
        database: &DatabaseConnection,
        template_id: i32,
        mob_id: u64,
    ) -> Result<Option<RtMob>, Box<dyn std::error::Error>> {
        if let Some(template) = Self::load_mob_template(database, template_id).await? {
            let mob = RtMob::from_template(template, mob_id);
            Ok(Some(mob))
        } else {
            Ok(None)
        }
    }

    pub async fn save_mob_state(
        database: &DatabaseConnection,
        mob: &RtMob,
    ) -> Result<(), Box<dyn std::error::Error>> {
        // TODO: Implement saving mob state to database if needed
        // This could be used for persistent mobs or mobs with state
        Ok(())
    }

    pub async fn load_mob_state(
        database: &DatabaseConnection,
        mob_id: u64,
    ) -> Result<Option<RtMob>, Box<dyn std::error::Error>> {
        // TODO: Implement loading mob state from database if needed
        Ok(None)
    }
}
