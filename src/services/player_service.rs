use crate::database::DbManager;
use crate::player::player::Player;
use crate::player::player_mapper;
use anyhow::Result;
use sea_orm::ActiveModelTrait;

pub async fn save_player(player: &Player) -> Result<()> {
    let db = DbManager::get_pool();
    let active_model = player_mapper::to_active_model(player);
    active_model.update(db).await?;
    Ok(())
}
