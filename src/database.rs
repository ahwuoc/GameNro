use sea_orm::{ConnectOptions, Database, DatabaseConnection};

use crate::config::DatabaseConfig;
use anyhow::Result;
use std::{sync::OnceLock, time::Duration};

static DB_POOL: OnceLock<DatabaseConnection> = OnceLock::new();
pub struct DbManager;

impl DbManager {
    pub async fn init(config: &DatabaseConfig) -> Result<()> {
        let database_url = if config.type_database == "sqlite" {
            println!("Connecting to SQLite database: {}", config.db_name);
            format!("sqlite://{}?mode=rwc", config.db_name)
        } else {
            println!(
                "Connecting to {} database at {}:{} (DB: {})",
                config.type_database, config.host, config.port, config.db_name
            );
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
            .sqlx_logging(false);
        let pool = Database::connect(opt).await?;
        let _ = DB_POOL.set(pool);
        println!("Database connected successfully");
        Ok(())
    }
    pub fn get_pool() -> &'static DatabaseConnection {
        DB_POOL.get().expect("Database not initialized")
    }
}
