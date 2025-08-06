use std::io::Error;

use crate::{game_session::GameSession, message::Message};

pub struct DataGame;
pub const VS_RES: u32 = 752012;

impl DataGame {
    pub fn send_version_res(session: &mut GameSession) -> Result<(), Error> {
        let mut msg = Message::new(-74, vec![]);
        msg.write_byte(0);
        msg.write_int(!VS_RES as i32)?;
        session.send_message(msg.command, &msg.data)?;
        msg.cleanup();
        Ok(())
    }
}
