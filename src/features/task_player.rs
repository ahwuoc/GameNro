use crate::features::side_task_template::SideTaskTemplate;

#[derive(Debug, Clone)]
pub struct TaskMain {
    pub id: i32,
    pub name: String,
}

#[derive(Debug, Clone)]
pub struct TaskPlayer {
    pub side_task: SideTaskTemplate,
    pub task_main: TaskMain,
}

impl TaskPlayer {
    pub fn new() -> Self {
        TaskPlayer {
            side_task: SideTaskTemplate::new(0, String::new()),
            task_main: TaskMain { id: -1, name: String::new() },
        }
    }
}
