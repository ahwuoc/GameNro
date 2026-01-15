#![allow(unused)]
mod account;
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
mod utils;
use anyhow::Result;
use config::Config;
use database::DbManager;

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    let config = Config::load()?;
    let db_manager = DbManager::new(&config.database).await?;

    {
        let manager = services::Manager::get_instance();
        let mut manager_guard = manager.lock().unwrap();
        if let Err(e) = manager_guard.init().await {
            return Err(anyhow::anyhow!("Manager initialization failed: {:?}", e));
        }
        if let Err(e) = manager_guard.init_maps_world().await {
            return Err(anyhow::anyhow!("Map world init failed: {:?}", e));
        }
        manager_guard.start_map_update_task();
    }

    {
        let god_gk = services::GodGK::get_instance();
        let mut god_gk_guard = god_gk.lock().unwrap();
        let pool = DbManager::get_pool();
        god_gk_guard.set_database(pool.clone());
    }

    if let Err(e) = network::start_server(&config.server).await {
        return Err(anyhow::anyhow!("Server failed: {:?}", e));
    }

    Ok(())
}
