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
use database::DbManager;

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    let config = Config::load()?;
    DbManager::init(&config.database).await?;

    services::manager::init().await?;

    services::manager::init_maps_world().await?;

    services::manager::start_map_update_task();

    network::start_server(&config.server).await?;

    Ok(())
}
