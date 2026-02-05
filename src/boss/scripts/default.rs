use async_trait::async_trait;

use crate::boss::scripts::traits::BossScript;

pub struct DefaultScript;

#[async_trait]
impl BossScript for DefaultScript {
    fn name(&self) -> &'static str {
        "default"
    }
}
