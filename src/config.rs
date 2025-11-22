use std::fs;

use anyhow::Result;
use serde::Deserialize;
use serde_json::from_str;

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
    pub username: String,
    pub password: String,
    pub db_name: String,
    pub pool_size: u32,
    pub max_connections: u32,
    pub min_connections: u32,
}

impl Config {
    const PATH: &str = "./config.toml";
    pub fn load() -> Result<Self> {
        let content = fs::read_to_string(Self::PATH)?;
        let config = from_str(&content)?;
        Ok(config)
    }
}
