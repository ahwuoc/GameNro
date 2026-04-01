use std::fs;

use anyhow::Result;
use serde::Deserialize;

#[derive(Debug, Deserialize, Clone)]
pub struct Config {
    pub server: ServerConfig,
    pub database: DatabaseConfig,
}
#[derive(Debug, Deserialize, Clone)]
pub struct ServerConfig {
    pub listen_port: u16,
    pub listen_host: String,
}
#[derive(Debug, Deserialize, Clone)]
pub struct DatabaseConfig {
    pub host: String,
    pub port: u16,
    pub type_database: String,
    #[serde(alias = "user")]
    pub username: String,
    pub password: String,
    pub db_name: String,
    pub pool_size: u32,
    pub max_connections: u32,
    pub min_connections: u32,
}

impl Config {
    const PATH: &str = "./config_arc.toml";

    pub fn load() -> Result<Self> {
        let content = fs::read_to_string(Self::PATH)?;
        let mut config: Config = toml::from_str(&content)?;
        if let Ok(host) = std::env::var("DATABASE_HOST") {
            config.database.host = host;
        }
        if let Ok(user) = std::env::var("DATABASE_USER") {
            config.database.username = user;
        }
        if let Ok(pass) = std::env::var("DATABASE_PASSWORD") {
            config.database.password = pass;
        }
        if let Ok(db_name) = std::env::var("DATABASE_NAME") {
            config.database.db_name = db_name;
        }
        if let Ok(port_str) = std::env::var("LISTEN_PORT") {
            if let Ok(port) = port_str.parse::<u16>() {
                config.server.listen_port = port;
            }
        }
        if let Ok(host) = std::env::var("LISTEN_HOST") {
            config.server.listen_host = host;
        }

        Ok(config)
    }
}
