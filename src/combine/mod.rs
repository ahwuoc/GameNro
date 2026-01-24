pub mod combine_constants;
pub mod combine_service;
pub mod combine_type;
pub mod handlers;
pub mod model;
use crate::combine::handlers::saophale::SaoPhaLe;
use enum_dispatch::enum_dispatch;

use crate::combine::combine_type::CombineType;
use crate::network::session::{self, AsyncSession, SessionArc};

#[async_trait::async_trait]
#[enum_dispatch]
pub trait CombineHandler {
    async fn show_info_combine(&self, session: &SessionArc) -> anyhow::Result<()>;
    async fn confirm_combine(&self, session: &AsyncSession) -> anyhow::Result<()>;
    fn get_text_info_tab_combine(&self) -> String;
    fn get_text_top_tab_combine(&self) -> String;
}
