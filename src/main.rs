mod account;
mod config;
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
#[tokio::main]
async fn main() -> Result<()> {
    println!("Starting GameNro Rust Server...");

    let config = Config::load()?;
    println!("Load config success");
    {
        let manager = services::Manager::get_instance();
        let mut manager_guard = manager.lock().unwrap();
        if let Err(e) = manager_guard.init(&config.database).await {
            return Err(anyhow::anyhow!("Manager initialization failed: {:?}", e));
        }
        if let Err(e) = manager_guard.init_maps_world().await {
            return Err(anyhow::anyhow!("Map world init failed: {:?}", e));
        }
        manager_guard.start_map_update_task();
    }

    let god_gk = services::GodGK::get_instance();
    {
        let mut god_gk_guard = god_gk.lock().unwrap();
        if let Err(e) = god_gk_guard.init_database(&config.database).await {
            return Err(anyhow::anyhow!("Database initialization failed: {:?}", e));
        }
    }
    println!("Database initialized successfully!");
    if let Err(e) = network::start_server().await {
        return Err(anyhow::anyhow!("Server failed: {:?}", e));
    }
    Ok(())
}
