use std::time::Duration;

use sea_orm::{ConnectOptions, Database, DatabaseConnection};

use crate::config::DatabaseConfig;
use anyhow::Result;

pub struct DbManager {
    pool: DatabaseConnection,
}

impl DbManager {
    pub async fn new(config: &DatabaseConfig) -> Result<Self> {
        let database_url = format!(
            "mysql://{}:{}@{}:{}/{}",
            config.username, config.password, config.host, config.port, config.db_name
        );

        let mut opt = ConnectOptions::new(database_url);
        opt.max_connections(config.max_connections)
            .min_connections(config.min_connections)
            .connect_timeout(Duration::from_secs(5))
            .sqlx_logging(true);
        let pool = Database::connect(opt).await?;
        Ok(Self { pool })
    }
    pub async fn get_pool(&self) -> Result<DatabaseConnection> {
        Ok(self.pool.clone())
    }
}
