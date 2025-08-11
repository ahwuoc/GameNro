mod entities;
mod network;
mod player;
mod mob;
mod map;
mod item;
mod services;
mod data;
mod models;
mod utils;
mod features;
mod npc;
mod macros;

use std::io;

#[tokio::main]
async fn main() -> io::Result<()> {
    println!("Starting GameNro Rust Server...");
    
    {
        let manager = services::Manager::get_instance();
        let mut manager_guard = manager.lock().unwrap();
        if let Err(e) = manager_guard.init().await {
            eprintln!("Failed to initialize Manager: {:?}", e);
            return Err(io::Error::new(io::ErrorKind::Other, "Manager initialization failed"));
        }
        if let Err(e) = manager_guard.init_maps_world().await {
            eprintln!("Failed to init maps world: {:?}", e);
            return Err(io::Error::new(io::ErrorKind::Other, "Map world init failed"));
        }
        manager_guard.start_map_update_task();
    }
    
    let god_gk = services::GodGK::get_instance();
    {
        let mut god_gk_guard = god_gk.lock().unwrap();
        if let Err(e) = god_gk_guard.init_database().await {
            eprintln!("Failed to initialize database: {:?}", e);
            return Err(io::Error::new(io::ErrorKind::Other, "Database initialization failed"));
        }
    }
    println!("Database initialized successfully!");
    if let Err(e) = network::start_server().await {
        eprintln!("Server error: {:?}", e);
        return Err(io::Error::new(io::ErrorKind::Other, "Server failed"));
    }
    
    Ok(())
}