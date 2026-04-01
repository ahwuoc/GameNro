use crate::player::player::Player;

pub struct SkillHandler;

impl SkillHandler {
    pub fn handle_select_skill(player: &mut Player, skill_template_id: i32) {
        let _ = crate::services::skill_service::select_skill(player, skill_template_id);
    }

    pub fn handle_update_skill_shortcuts(player: &mut Player, shortcuts: Vec<i8>) {
        player.player_skill.skill_shortcut = shortcuts;
        let _ = crate::services::skill_service::send_skill_shortcut(player);
    }
}
