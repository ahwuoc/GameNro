use crate::combine::handlers::saophale::SaoPhaLe;
use crate::combine::CombineHandler;
use enum_dispatch::enum_dispatch;

#[derive(Debug, Clone, Copy)]
#[enum_dispatch(CombineHandler)]

pub enum CombineType {
    PhaLeHoaTrangBi(SaoPhaLe),
}

impl Default for CombineType {
    fn default() -> Self {
        Self::PhaLeHoaTrangBi(SaoPhaLe)
    }
}
