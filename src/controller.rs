use crate::game_session::GameSession;

use crate::{DataGame::DataGame, Player::Player, game_session, message::Message};
use std::collections::HashMap;
use std::io::{Cursor, Error, ErrorKind, Read, Write};
use std::net::TcpStream;

use std::sync::{Arc, Mutex};
use std::time::Instant;
macro_rules! define_handlers {
    ($($name:ident),*) => {
        $(
            fn $name(&self, _session: &GameSession, _message: &mut Message) -> Result<(), Error> {
                println!("Handling {} for session: {}", stringify!($name), _session.id);
                Ok(())
            }
        )*
    };
}
pub struct MessageController {
    sessions: Arc<Mutex<HashMap<String, Arc<Mutex<GameSession>>>>>,
    players: Arc<Mutex<HashMap<u32, Player>>>,
    maintenance_running: bool,
}

impl MessageController {
    pub fn new() -> Self {
        MessageController {
            sessions: Arc::new(Mutex::new(HashMap::new())),
            players: Arc::new(Mutex::new(HashMap::new())),
            maintenance_running: false,
        }
    }

    pub fn handle_message(&self, session_id: &str, mut message: Message) -> Result<(), Error> {
        println!("Client connect {} {:?}", session_id, message);
        let start_time = Instant::now();
        let sessions = self.sessions.lock().unwrap();
        let session_arc = sessions
            .get(session_id)
            .ok_or(Error::new(ErrorKind::NotFound, "Session not found"))?
            .clone();

        let mut session = session_arc.lock().unwrap();
        match message.command {
            -100 => {
                if let Ok(action) = message.read_byte() {
                    match action {
                        0 => self.handle_shop_deposit(&session, &mut message)?,
                        1 | 2 => self.handle_shop_claim(&session, &mut message)?,
                        3 => self.handle_shop_buy(&session, &mut message)?,
                        4 => self.handle_shop_next_page(&session, &mut message)?,
                        5 => self.handle_shop_up_top(&session, &mut message)?,
                        _ => println!("Unknown shop action: {}", action),
                    }
                }
            }
            -118 | -120 => self.handle_boss_teleport(&session, &mut message)?,
            127 => {
                if let Ok(action) = message.read_byte() {
                    match action {
                        1 => self.handle_radar_use(&session, &mut message)?,
                        _ => println!("Unknown radar action: {}", action),
                    }
                }
            }
            42 => self.handle_register_account(&session, &mut message)?,
            -127 => self.handle_lucky_round(&session, &mut message)?,
            -125 => self.handle_input(&session, &mut message)?,
            -99 => self.handle_enemy_controller(&session, &mut message)?,
            18 => self.handle_yardrat_teleport(&session, &mut message)?,
            -72 => self.handle_private_chat(&session, &mut message)?,
            -80 => self.handle_friend_controller(&session, &mut message)?,
            -59 => self.handle_pvp_challenge(&session, &mut message)?,
            -86 => self.handle_transaction(&session, &mut message)?,
            -108 => self.handle_pet_status(&session, &mut message)?,
            6 => {
                if !self.maintenance_running {
                    self.handle_buy_item(&session, &mut message)?;
                }
            }
            7 => {
                if !self.maintenance_running {
                    self.handle_sell_item(&session, &mut message)?;
                }
            }
            21 => self.handle_change_zone(&session, &mut message)?,
            -71 => self.handle_global_chat(&session, &mut message)?,
            -79 => self.handle_player_menu(&session, &mut message)?,
            -113 => self.handle_skill_shortcut(&session, &mut message)?,
            -101 => self.handle_login2(&session, &mut message)?,
            -103 => self.handle_flag_system(&session, &mut message)?,
            -7 => self.handle_player_move(&session, &mut message)?,
            -74 => self.handle_data_request(&session, &mut message)?,
            -81 => self.handle_combine_service(&session, &mut message)?,
            -67 => self.handle_icon_request(&session, &mut message)?,
            66 => self.handle_image_by_name(&session, &mut message)?,
            -66 => self.handle_effect_data(&session, &mut message)?,
            -62 => self.handle_flag_bag(&session, &mut message)?,
            -63 => self.handle_flag_effect(&session, &mut message)?,
            -76 => self.handle_achievement(&session, &mut message)?,
            -32 => self.handle_bg_template(&session, &mut message)?,
            22 => self.handle_npc_confirmation(&session, &mut message)?,
            -45 => self.handle_use_skill(&session, &mut message)?,
            -46 => self.handle_clan_get(&session, &mut message)?,
            -51 => self.handle_clan_message(&session, &mut message)?,
            -54 => self.handle_clan_donate(&session, &mut message)?,
            -49 => self.handle_clan_join(&session, &mut message)?,
            -50 => self.handle_clan_list_members(&session, &mut message)?,
            -56 => self.handle_clan_remote(&session, &mut message)?,
            -47 => self.handle_clan_list(&session, &mut message)?,
            -57 => self.handle_clan_invite(&session, &mut message)?,
            -40 => self.handle_get_item(&session, &mut message)?,
            -41 => self.handle_caption(&session, &mut message)?,
            -43 => self.handle_do_item(&session, &mut message)?,
            -91 => self.handle_map_change(&session, &mut message)?,
            32 => self.handle_npc_select(&session, &mut message)?,
            33 => self.handle_npc_open(&session, &mut message)?,
            34 => self.handle_select_skill(&session, &mut message)?,
            54 => self.handle_attack_mob(&session, &mut message)?,
            -60 => self.handle_attack_player(&session, &mut message)?,
            -27 => self.handle_version_key(&mut *session)?,
            -20 => self.handle_pick_item(&session, &mut message)?,
            -28 => self.handle_message_not_map(&session, &mut message)?,
            -29 => self.handle_message_not_login(&session, &mut message)?,
            -30 => self.handle_message_sub_command(&session, &mut message)?,
            -121 => self.handle_summon_boss(&session, &mut message)?,
            -122 => self.handle_manage_player(&session, &mut message)?,
            _ => {
                println!("Unhandled command: {}", message.command);
            }
        }

        let elapsed = start_time.elapsed();
        println!("Message processing time: {:?}", elapsed);

        Ok(())
    }

    define_handlers! {
        handle_shop_deposit, handle_shop_claim, handle_shop_buy, handle_shop_next_page,
        handle_shop_up_top, handle_boss_teleport, handle_radar_send, handle_radar_use,
        handle_special_transport, handle_register_account, handle_lucky_round, handle_input,
        handle_intrinsic_menu, handle_magic_tree_open, handle_magic_tree_load, handle_enemy_controller,
        handle_yardrat_teleport, handle_private_chat, handle_friend_controller, handle_pvp_challenge,
        handle_transaction, handle_pet_info, handle_pet_status, handle_buy_item, handle_sell_item,
        handle_open_zone_ui, handle_change_zone, handle_global_chat, handle_player_menu,
        handle_skill_shortcut, handle_login2, handle_flag_system, handle_data_request,
        handle_combine_service, handle_update_data, handle_icon_request, handle_image_by_name,
        handle_effect_data, handle_flag_bag, handle_flag_effect, handle_achievement,
        handle_bg_template, handle_npc_confirmation, handle_waypoint, handle_use_skill,
        handle_clan_get, handle_clan_message, handle_clan_donate, handle_clan_join,
        handle_clan_list_members, handle_clan_remote, handle_clan_list, handle_clan_leave_menu,
        handle_clan_invite, handle_get_item, handle_caption, handle_do_item, handle_map_change,
        handle_finish_load_map, handle_clear_client, handle_mob_template, handle_npc_select,
        handle_npc_open, handle_select_skill, handle_attack_mob, handle_attack_player,
         handle_data_image_version, handle_pick_item, handle_message_not_map,
        handle_message_not_login, handle_message_sub_command, handle_summon_boss, handle_manage_player,
        handle_go_home, handle_revive
    }
    fn handle_version_key(&self, session: &mut GameSession) -> Result<(), Error> {
        session.send_key();
        DataGame::send_version_res(session).unwrap();
        Ok(())
    }

    fn handle_player_move(
        &self,
        session: &GameSession,
        message: &mut Message,
    ) -> Result<(), Error> {
        if let (Ok(_action), Ok(x), Ok(y)) = (
            message.read_byte(),
            message.read_short(),
            message.read_short(),
        ) {
            println!("Player {} moving to ({}, {})", session.id, x, y);
        }
        Ok(())
    }

    pub fn add_session(&self, session: Arc<Mutex<GameSession>>) {
        let session_id = {
            let s = session.lock().unwrap();
            s.id.clone()
        };

        let mut sessions = self.sessions.lock().unwrap();
        sessions.insert(session_id.clone(), session);
        println!("Session {} added to controller", session_id);
    }

    pub fn remove_session(&self, session_id: &str) {
        let mut sessions = self.sessions.lock().unwrap();
        sessions.remove(session_id);
        println!("Session {} removed from controller", session_id);
    }

    pub fn set_maintenance(&mut self, running: bool) {
        self.maintenance_running = running;
    }
}

impl Default for MessageController {
    fn default() -> Self {
        Self::new()
    }
}
