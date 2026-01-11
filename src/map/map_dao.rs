use crate::entities::map_template;
use crate::map::map::{MobSpawn, NpcSpawn};
use crate::map::WayPoint;
use sea_orm::*;

pub struct MapDao;

impl MapDao {
    pub async fn load_map_waypoints(
        database: &DatabaseConnection,
        map_id: i32,
    ) -> anyhow::Result<Vec<WayPoint>> {
        let template = map_template::Entity::find_by_id(map_id)
            .one(database)
            .await?;

        if let Some(template) = template {
            return Ok(WayPoint::parse(&template.waypoints));
        }

        Ok(Vec::new())
    }

    pub async fn load_map_mobs(
        db: &DatabaseConnection,
        map_id: i32,
    ) -> anyhow::Result<Vec<MobSpawn>> {
        let Some(template) = map_template::Entity::find_by_id(map_id).one(db).await? else {
            return Ok(Vec::new());
        };
        Ok(MobSpawn::parse(&template.mobs))
    }
    pub async fn load_map_npcs(
        database: &DatabaseConnection,
        map_id: i32,
    ) -> anyhow::Result<Vec<NpcSpawn>> {
        let template = map_template::Entity::find_by_id(map_id)
            .one(database)
            .await?;

        if let Some(template) = template {
            return Ok(NpcSpawn::parse(&template.npcs));
        }

        Ok(Vec::new())
    }
}
