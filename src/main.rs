#![allow(unused)]
mod account;
pub mod clan;
pub mod combine;
mod config;
mod constant;
mod data;
mod database;
mod dungoen;
mod entities;
mod item;
mod map;
mod matches;
mod mob;
mod models;
mod network;
mod npc;
mod player;
mod services;
mod shop;
mod startup;
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
    let config = Config::load()?;
    DbManager::init(&config.database).await?;

    startup::init_config().await?;
    startup::init_maps_world().await?;
    boss::manager::BossManager::init_boss().await;
    dungoen::doanh_trai::manager::global_init();
    matches::pvp_manager::init_pvp();
    matches::dhvt::manager::init_dhvt();
    tracing::info!(
        "Server started successfully on {}:{}",
        config.server.listen_host,
        config.server.listen_port
    );

    network::start_server(&config.server).await?;

    Ok(())
}
