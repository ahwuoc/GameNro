use serde::{Deserialize, Serialize};

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct BossComponent {
    pub group_id: Option<u64>,
    pub group_index: i32,
    pub sequence: Vec<String>,
}

impl BossComponent {
    pub fn new() -> Self {
        Self {
            group_id: None,
            group_index: -1,
            sequence: Vec::new(),
        }
    }
}
