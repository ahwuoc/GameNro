use crate::entities::account;
use crate::entities::player;
use anyhow::Result;
use sea_orm::ActiveModelTrait;
use sea_orm::{ColumnTrait, DatabaseConnection, DbErr, EntityTrait, QueryFilter};
pub struct AccountDao;

impl AccountDao {
    pub async fn get_account(
        pool: &DatabaseConnection,
        username: &str,
    ) -> Result<Option<account::Model>, DbErr> {
        let account = account::Entity::find()
            .filter(account::Column::Username.eq(username))
            .one(pool)
            .await?;

        Ok(account)
    }
    pub async fn get_account_by_id(
        pool: &DatabaseConnection,
        id: i32,
    ) -> Result<Option<account::Model>, DbErr> {
        account::Entity::find_by_id(id).one(pool).await
    }
    pub async fn get_player_by_account_id(
        pool: &DatabaseConnection,
        account_id: i32,
    ) -> Result<Option<player::Model>, DbErr> {
        let player = player::Entity::find()
            .filter(player::Column::AccountId.eq(account_id))
            .one(pool)
            .await?;
        Ok(player)
    }

    pub async fn create_player(
        pool: &DatabaseConnection,
        player_data: player::ActiveModel,
    ) -> Result<player::Model, DbErr> {
        let player = player_data.insert(pool).await?;
        Ok(player)
    }

    pub async fn update_account(
        pool: &DatabaseConnection,
        account_data: account::ActiveModel,
    ) -> Result<account::Model, DbErr> {
        let account = account_data.update(pool).await?;
        Ok(account)
    }
}
