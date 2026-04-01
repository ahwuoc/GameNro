use crate::constant::task_type::TaskType;
use crate::map::zone_manager::ZONE_MANAGER;
use crate::player::player::Player;
use crate::services::task_service::TaskService;
use crate::services::task_utils::TaskUtils;

pub struct TaskHandler;

impl TaskHandler {
    pub async fn handle_task_action(player: &mut Player, task_type: TaskType, target_id: String) {
        let old_task = (
            TaskUtils::get_id_task(player),
            TaskUtils::get_task_index(player),
        );
        
        TaskService::check_done_task(player, task_type, &target_id);
        Self::handle_task_advance(player, old_task).await;
    }

    pub async fn handle_task_advance(player: &Player, old_task: (i32, i32)) {
        let new_task = (
            TaskUtils::get_id_task(player),
            TaskUtils::get_task_index(player),
        );
        
        if old_task != new_task {
            if let Some(zone) = ZONE_MANAGER.get_zone(player.map_id, player.zone_id) {
                let _ = zone.check_spawn_task_item(player.id, new_task).await;
            }
        }
    }
}
