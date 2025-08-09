use sea_orm::*;
use crate::entities::{account, player};

#[derive(Clone)]
pub struct Database {
    pub connection: DatabaseConnection,
}

impl Database {
    pub async fn new() -> Result<Self, DbErr> {
        let database_url = "mysql://root@localhost:3306/gamenro";
        let pool = sea_orm::Database::connect(database_url).await?;
        Ok(Database { connection: pool })
    }

    pub fn new_placeholder() -> Self {
        panic!("new_placeholder() should not be called - use async initialization instead");
    }

    pub fn new_sync() -> Self {
        panic!("new_sync() should not be called - use async initialization instead");
    }

    pub async fn test_connection(&self) -> Result<(), DbErr> {
        self.connection.ping().await
    }

    pub async fn init_database(&self) -> Result<(), DbErr> {
        self.test_connection().await
    }

    pub async fn get_account(&self, username: &str) -> Result<Option<account::Model>, DbErr> {
        account::Entity::find()
            .filter(account::Column::Username.eq(username))
            .one(&self.connection)
            .await
    }

    pub async fn get_account_by_id(&self, id: i32) -> Result<Option<account::Model>, DbErr> {
        account::Entity::find_by_id(id)
            .one(&self.connection)
            .await
    }

    pub async fn get_player_by_account_id(&self, account_id: i32) -> Result<Option<player::Model>, DbErr> {
        player::Entity::find()
            .filter(player::Column::AccountId.eq(account_id))
            .one(&self.connection)
            .await
    }

    pub async fn create_player(&self, player_data: player::ActiveModel) -> Result<player::Model, DbErr> {
        player_data.insert(&self.connection).await
    }

    pub async fn update_account(&self, account_data: account::ActiveModel) -> Result<account::Model, DbErr> {
        account_data.update(&self.connection).await
    }
}

