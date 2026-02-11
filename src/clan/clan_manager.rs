use crate::database::DbManager;
use crate::entities::clan::Entity as ClanEntity;
use crate::models::clan::Clan;
use dashmap::DashMap;
use std::sync::LazyLock;
use sea_orm::EntityTrait;
use std::sync::Arc;
use tokio::sync::RwLock;
use tracing::info;

pub static CLAN_MANAGER: LazyLock<ClanManager> = LazyLock::new(|| ClanManager::new());

pub struct ClanManager {
    clans: DashMap<i32, Arc<RwLock<Clan>>>,
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
            self.clans.insert(clan.id, Arc::new(RwLock::new(clan)));
        }

        Ok(())
    }

    pub fn add_clan(&self, clan: Clan) {
        self.clans.insert(clan.id, Arc::new(RwLock::new(clan)));
    }

    pub fn get_clan(&self, id: i32) -> Option<Arc<RwLock<Clan>>> {
        self.clans.get(&id).map(|c| Arc::clone(c.value()))
    }

    pub fn remove_clan(&self, id: i32) {
        self.clans.remove(&id);
    }

    pub fn get_all_clans(&self) -> Vec<Arc<RwLock<Clan>>> {
        self.clans.iter().map(|c| Arc::clone(c.value())).collect()
    }

    pub async fn search_clans(&self, name: &str) -> Vec<Arc<RwLock<Clan>>> {
        let mut list = Vec::new();
        for entry in self.clans.iter() {
            let clan = entry.value().read().await;
            if clan.name.contains(name) {
                list.push(Arc::clone(entry.value()));
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
            let clan = entry.value().read().await;

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
