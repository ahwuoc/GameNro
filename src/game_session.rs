use crate::Player::Player;
use crate::message::Message;
use std::io::{Error, Write};
use std::net::TcpStream;
use std::sync::{Arc, Mutex};

#[derive(Debug)]
pub struct GameSession {
    pub id: String,
    pub user_id: Option<u32>,
    pub player: Option<Player>,
    pub stream: Arc<Mutex<TcpStream>>,
    pub is_authenticated: bool,
    pub version: u16,
    pub ip_address: String,
    pub sent_key: bool,
}

impl GameSession {
    pub fn new(id: String, stream: TcpStream, ip_address: String) -> Self {
        GameSession {
            id,
            user_id: None,
            player: None,
            stream: Arc::new(Mutex::new(stream)),
            is_authenticated: false,
            version: 0,
            ip_address,
            sent_key: false,
        }
    }

    pub fn send_message(&self, command: i8, data: &[u8]) -> Result<(), Error> {
        let mut stream = self.stream.lock().unwrap();
        stream.write_all(&[command as u8])?;
        let length = data.len() as u16;
        stream.write_all(&length.to_be_bytes())?;
        stream.write_all(data)?;
        stream.flush()
    }

    pub fn send_notification(&self, message: &str) -> Result<(), Error> {
        let data = format!("{}\0", message).into_bytes();
        self.send_message(-71, &data)
    }

    fn generate_key() -> [u8; 8] {
        "PHUCNEAE".as_bytes().try_into().unwrap()
    }

    pub fn send_key(&mut self) -> Result<(), Error> {
        if self.sent_key {
            return Ok(());
        }

        let keys = GameSession::generate_key();
        let mut msg = Message::new(-27, vec![]);
        msg.write_byte(keys.len() as i8)?;
        msg.write_byte(keys[0] as i8)?;
        for i in 1..keys.len() {
            msg.write_byte((keys[i] ^ keys[i - 1]) as i8)?;
        }
        let ip2 = "127.0.0.1";
        let port2 = 14445;
        let is_connect2 = true;

        msg.write_utf(ip2)?;
        msg.write_int(port2)?;
        msg.write_byte(is_connect2 as i8)?;

        self.send_message(msg.command, &msg.data)?;
        self.sent_key = true;

        Ok(())
    }

    pub fn login(&mut self, username: &str, password: &str) -> Result<(), Error> {
        if self.is_authenticated {
            return Ok(());
        }
        println!("Login successful for user: {}", username);
        self.is_authenticated = true;
        self.send_key()?;

        Ok(())
    }
}
