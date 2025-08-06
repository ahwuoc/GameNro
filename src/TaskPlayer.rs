use crate::{SideTaskTemplate::SideTaskTemplate, TaskMain::TaskMain};

#[derive(Debug, Clone)]
pub struct TaskPlayer {
    pub side_task: SideTaskTemplate,
    pub task_main: TaskMain,
}

impl TaskPlayer {
    pub fn new() -> Self {
        TaskPlayer {
            side_task: SideTaskTemplate::new(),
            task_main: TaskMain::new(),
        }
    }
}
