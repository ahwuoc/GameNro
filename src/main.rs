#![allow(unused)]
mod account;
mod combine;
mod config;
mod constant;
mod data;
mod database;
mod entities;
mod features;
mod item;
mod map;
mod mob;
mod models;
mod network;
mod npc;
mod player;
mod services;
mod shop;
mod templates;
mod utils;
use anyhow::Result;
use config::Config;
mod boss;
use database::DbManager;
#[allow(dead_code)]
#[tokio::main]
async fn main() -> anyhow::Result<()> {
    tracing_subscriber::fmt()
        .with_env_filter(tracing_subscriber::EnvFilter::from_default_env())
        .init();

    tracing::info!("Starting game server...");
    let config = Config::load()?;
    DbManager::init(&config.database).await?;

    services::manager::init().await?;
    services::manager::init_maps_world().await?;
    boss::manager::BossManager::init_boss().await;
    network::start_server(&config.server).await?;

    Ok(())
}
