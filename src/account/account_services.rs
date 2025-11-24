use crate::{
    account::account_dao::AccountDao, database::DbManager, entities::account,
    services::ServiceHandles,
};
use anyhow::Result;
use sea_orm::DatabaseConnection;
#[derive(Debug)]

pub struct AccountServices;

impl AccountServices {
    pub async fn login(
        pool: &DatabaseConnection,
        username: &str,
        password: &str,
    ) -> Result<account::Model> {
        let account = AccountDao::get_account(pool, username)
            .await?
            .ok_or_else(|| anyhow::anyhow!("Account not found"))?;
        if account.ban == true {
            return Err(anyhow::anyhow!(
                "Tài khoản của bạn đã bị khóa do vi phạm quy định của game."
            ));
        }
        if account.password != password {
            return Err(anyhow::anyhow!("Tài khoản hoặc mật khẩu không đúng"));
        }
        Ok(account)
    }
}
