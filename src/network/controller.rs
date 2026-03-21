// @workflow: /add_packet - See .agent/workflows/add_packet.md for adding new features
use crate::constant::cmd::cmd;
use crate::network::handlers::{
    auth_handler::AuthHandler,
    clan_handler::ClanHandler,
    combat_handler::CombatHandler,
    data_handler::DataHandler,
    map_handler::MapHandler,
    player_handler::PlayerHandler,
};
use anyhow::Result;
use tracing::{instrument, warn};

use super::message::Message;
use super::session::SessionArc;

pub struct AsyncController;

impl AsyncController {
    #[instrument(skip(session, msg), fields(command = msg.command, sub_cmd))]
    pub async fn process(session: SessionArc, msg: Message) -> Result<()> {
        match msg.command {
            // ── Key & Version ──────────────────────────────────
            cmd::KEY => DataHandler::send_key(&session).await,

            // ── Authentication ─────────────────────────────────
            cmd::NOT_LOGIN => AuthHandler::handle_not_login(&session, msg).await,
            cmd::NOT_LOGIN_ALT => AuthHandler::handle_not_login_alt(&session, msg).await,

            // ── Data & Resources ───────────────────────────────
            cmd::GET_IMAGES_SOURCE => DataHandler::get_image_source(&session, msg).await,
            cmd::GET_EFFECT_TEMPLATE => DataHandler::get_effect_template(&session, msg).await,
            cmd::GET_MOB_TEMPLATE => DataHandler::get_mob_template(&session, msg).await,
            cmd::GET_ITEM_BG_TEMPLATE => DataHandler::get_item_bg_template(&session, msg).await,
            cmd::GET_IMAGE_BY_NAME => DataHandler::get_image_by_name(&session, msg).await,
            cmd::GET_ICON => DataHandler::get_icon(&session, msg).await,
            cmd::GET_CAPTIONS => DataHandler::get_captions(&session, msg),
            cmd::UPDATE_DATA => DataHandler::update_data(&session).await,
            cmd::NOT_MAP => DataHandler::handle_not_map(&session, msg).await,

            // ── Combat ────────────────────────────────────────
            cmd::ATTACK_MOB => CombatHandler::attack_mob(&session, msg).await,
            cmd::PLAYER_ATTACK_PLAYER => CombatHandler::attack_player(&session, msg).await,
            cmd::SELECT_SKILL => CombatHandler::select_skill(&session, msg).await,
            cmd::USE_SKILL => CombatHandler::use_skill(&session, msg).await,
            cmd::HOI_SINH => CombatHandler::hoi_sinh(&session).await,
            cmd::PET_CHANGE_STATUS => CombatHandler::pet_change_status(&session, msg).await,
            cmd::PVP_CMD => CombatHandler::pvp_command(&session, msg).await,

            // ── Map & Movement ─────────────────────────────────
            cmd::OPEN_ZONE_UI => MapHandler::open_zone_ui(&session).await,
            cmd::CHANGE_ZONE => MapHandler::change_zone(&session, msg).await,
            cmd::CHANGE_MAP_WAYPOINT | cmd::CHANGE_MAP_WAYPOINT_ALT => {
                MapHandler::change_map_waypoint(&session, msg).await
            }
            cmd::GO_HOME => MapHandler::go_home(&session, msg).await,
            cmd::FINISH_LOAD_MAP => MapHandler::finish_load_map(&session).await,
            cmd::CAPSULE_MENU => MapHandler::capsule_menu(&session, msg).await,
            cmd::PLAYER_MOVE => MapHandler::player_move(&session, msg).await,

            // ── Player Actions ─────────────────────────────────
            cmd::GET_ITEM => PlayerHandler::get_item(&session, msg).await,
            cmd::DO_ITEM => PlayerHandler::do_item(&session, msg).await,
            cmd::BUY_ITEM => PlayerHandler::buy_item(&session, msg).await,
            cmd::NPC_SELECT => PlayerHandler::npc_select(&session, msg).await,
            cmd::NPC_MENU => PlayerHandler::npc_menu(&session, msg).await,
            cmd::DAU_THAN_CONFIRM => PlayerHandler::dau_than_confirm(&session, msg).await,
            cmd::CHAT => PlayerHandler::chat(&session, msg).await,
            cmd::GET_PLAYER_MENU => PlayerHandler::get_player_menu(&session, msg).await,
            cmd::SHOW_INFO_PET => PlayerHandler::show_info_pet(&session).await,
            cmd::CHANGE_TYPE_PK => PlayerHandler::change_type_pk(&session, msg).await,
            cmd::MAGIC_TREE => PlayerHandler::magic_tree(&session, msg).await,
            cmd::RADAR => PlayerHandler::radar(&session, msg).await,
            cmd::PICK_ITEM => PlayerHandler::pick_item(&session, msg).await,
            cmd::COMBINE_INFO => PlayerHandler::combine_info(&session, msg).await,
            cmd::SKILL_SHORTCUT_UPDATE => PlayerHandler::skill_shortcut_update(&session, msg).await,
            cmd::INTRINSIC_MENU => PlayerHandler::intrinsic_menu(&session).await,

            // ── Clan ──────────────────────────────────────────
            cmd::GET_MY_CLAN => ClanHandler::get_my_clan(&session).await,
            cmd::CLAN_MESSAGE => ClanHandler::clan_message(&session, msg).await,
            cmd::GET_CLAN_LIST => ClanHandler::get_clan_list(&session, msg).await,
            cmd::GET_MEMBER_LIST => ClanHandler::get_member_list(&session, msg).await,
            cmd::CLAN_REMOTE => ClanHandler::clan_remote(&session, msg).await,
            cmd::LEAVE_CLAN => ClanHandler::leave_clan(&session).await,
            cmd::CLAN_INVITE => ClanHandler::clan_invite(&session, msg).await,
            cmd::CLAN_JOIN => ClanHandler::clan_join(&session, msg).await,
            cmd::CLAN_INFO => ClanHandler::clan_info(&session, msg).await,
            cmd::CLAN_DONATE => ClanHandler::clan_donate(&session, msg).await,

            // ── No-op / ignored ───────────────────────────────
            cmd::FINISH_UPDATE | cmd::FLAG_BAG_ICON | cmd::CHECK_MOVE => Ok(()),

            // ── Unknown ───────────────────────────────────────
            _ => {
                warn!("Unknown command: {}", msg.command);
                Ok(())
            }
        }
    }
}
