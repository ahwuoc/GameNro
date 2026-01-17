use sea_orm::{ConnectOptions, Database, DatabaseConnection};

use crate::config::DatabaseConfig;
use anyhow::Result;
use std::{sync::OnceLock, time::Duration};

static DB_POOL: OnceLock<DatabaseConnection> = OnceLock::new();
pub struct DbManager;

impl DbManager {
    pub async fn init(config: &DatabaseConfig) -> Result<()> {
        let database_url = if config.type_database == "sqlite" {
            format!("sqlite://{}?mode=rwc", config.db_name)
        } else {
            format!(
                "{}://{}:{}@{}:{}/{}",
                config.type_database,
                config.username,
                config.password,
                config.host,
                config.port,
                config.db_name
            )
        };

        let mut opt = ConnectOptions::new(database_url);
        opt.max_connections(config.max_connections)
            .min_connections(config.min_connections)
            .connect_timeout(Duration::from_secs(5))
            .sqlx_logging(true);
        let pool = Database::connect(opt).await?;
        let _ = DB_POOL.set(pool);
        Ok(())
    }
    pub fn get_pool() -> &'static DatabaseConnection {
        DB_POOL.get().expect("Database not initialized")
    }
}
