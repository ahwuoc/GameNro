use crate::entities::prelude::*;
use crate::entities::{task_main_template, task_sub_template};
use once_cell::sync::Lazy;
use sea_orm::*;
use std::collections::HashMap;
use std::sync::RwLock;

pub struct TaskTemplateManager {
    main_tasks: RwLock<HashMap<i32, task_main_template::Model>>,
    sub_tasks: RwLock<HashMap<i32, Vec<task_sub_template::Model>>>,
}

pub static TASK_TEMPLATE_MANAGER: Lazy<TaskTemplateManager> =
    Lazy::new(|| TaskTemplateManager::new());

impl TaskTemplateManager {
    fn new() -> Self {
        Self {
            main_tasks: RwLock::new(HashMap::new()),
            sub_tasks: RwLock::new(HashMap::new()),
        }
    }

    pub async fn init(&self, db: &DatabaseConnection) -> anyhow::Result<()> {
        let task_mains = TaskMainTemplate::find().all(db).await?;
        let mut main_map = HashMap::new();
        for task in task_mains {
            main_map.insert(task.id, task);
        }

        let task_subs = TaskSubTemplate::find().all(db).await?;
        let mut sub_map: HashMap<i32, Vec<task_sub_template::Model>> = HashMap::new();
        for sub in task_subs {
            sub_map
                .entry(sub.task_main_id)
                .or_insert_with(Vec::new)
                .push(sub);
        }

        {
            let mut main_lock = self.main_tasks.write().unwrap();
            let main_count = main_map.len();
            *main_lock = main_map;

            let mut sub_lock = self.sub_tasks.write().unwrap();
            let sub_count = sub_map.values().map(|v| v.len()).sum::<usize>();
            *sub_lock = sub_map;

            tracing::info!(
                "Loaded {} main tasks and {} sub tasks",
                main_count,
                sub_count
            );
        }

        Ok(())
    }

    pub fn get_main_task(&self, id: i32) -> Option<task_main_template::Model> {
        self.main_tasks.read().unwrap().get(&id).cloned()
    }

    pub fn get_sub_tasks(&self, main_id: i32) -> Vec<task_sub_template::Model> {
        self.sub_tasks
            .read()
            .unwrap()
            .get(&main_id)
            .cloned()
            .unwrap_or_default()
    }
}
