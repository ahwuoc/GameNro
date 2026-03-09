use super::actor::ClanActor;
use super::handle::ClanHandle;
use crate::database::DbManager;
use crate::entities::clan::Entity as ClanEntity;
use crate::models::clan::Clan;
use dashmap::DashMap;
use sea_orm::EntityTrait;
use std::sync::LazyLock;
use tokio::sync::mpsc;
use tracing::info;

pub static CLAN_MANAGER: LazyLock<ClanManager> = LazyLock::new(|| ClanManager::new());

pub struct ClanManager {
    clans: DashMap<i32, ClanHandle>,
}

impl ClanManager {
    pub fn new() -> Self {
        Self {
            clans: DashMap::new(),
        }
    }

    pub async fn load_all(&self) -> anyhow::Result<()> {
        let db = DbManager::get_pool().clone();
        let clan_entities = ClanEntity::find().all(&db).await?;

        info!("Loading {} clans from database", clan_entities.len());

        for entity in clan_entities {
            let clan = Clan::from_entity(entity);
            self.add_clan(clan);
        }

        Ok(())
    }

    pub fn add_clan(&self, clan: Clan) {
        let clan_id = clan.id;
        let (tx, rx) = mpsc::channel(100);
        let handle = ClanHandle::new(clan_id, tx);

        let mut actor = ClanActor::new(clan, rx);
        tokio::spawn(async move {
            actor.run().await;
        });

        self.clans.insert(clan_id, handle);
    }

    pub fn get_clan(&self, id: i32) -> Option<ClanHandle> {
        self.clans.get(&id).map(|c| c.value().clone())
    }

    pub fn remove_clan(&self, id: i32) {
        self.clans.remove(&id);
    }

    pub fn get_all_clans(&self) -> Vec<ClanHandle> {
        self.clans.iter().map(|c| c.value().clone()).collect()
    }

    pub async fn search_clans(&self, name: &str) -> Vec<ClanHandle> {
        let mut list = Vec::new();
        for entry in self.clans.iter() {
            let handle = entry.value();
            if let Some(clan) = handle.get_snapshot().await {
                if clan.name.contains(name) {
                    list.push(handle.clone());
                }
            }
            if list.len() >= 20 {
                break;
            }
        }
        list
    }

    pub async fn save_all(&self) -> anyhow::Result<()> {
        use crate::entities::clan;
        use sea_orm::Set;

        let db = DbManager::get_pool();
        info!("Saving {} clans to database", self.clans.len());

        for entry in self.clans.iter() {
            let handle = entry.value();
            let Some(clan) = handle.get_snapshot().await else {
                continue;
            };

            // Serialize members
            let members_json = serde_json::to_string(&clan.members).unwrap_or_default();

            let model = clan::ActiveModel {
                id: Set(clan.id),
                slogan: Set(clan.slogan.clone()),
                img_id: Set(clan.img_id),
                power_point: Set(clan.power_point),
                max_member: Set(clan.max_member as i16),
                level: Set(clan.level),
                members: Set(members_json),
                name_2: Set(clan.name_2.clone()),
                clan_point: Set(clan.capsule_clan),
                ..Default::default()
            };

            if let Err(e) = clan::Entity::update(model).exec(db).await {
                tracing::error!("Failed to save clan {}: {:?}", clan.id, e);
            }
        }

        info!("All clans saved successfully");
        Ok(())
    }
}
