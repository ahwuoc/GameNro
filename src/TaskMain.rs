#[derive(Debug, Clone)]
pub struct TaskMain {
    pub id: i8,
    pub index: i8,
}

impl TaskMain {
    pub fn new() -> Self {
        TaskMain { id: -1, index: -1 }
    }
}
