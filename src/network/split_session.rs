#![allow(dead_code)]
use super::message::Message;
use crate::network::session::SessionArc;
use crate::player::Player as RtPlayer;
use std::io;
use std::sync::Arc;
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::net::{tcp::OwnedReadHalf, tcp::OwnedWriteHalf, TcpStream};
use tokio::sync::{mpsc, Mutex, RwLock};

pub struct SessionReader {
    read_half: OwnedReadHalf,
    keys: Vec<u8>,
    cur_r: usize,
    sent_key: bool,
}

impl SessionReader {
    fn read_key(&mut self, b: u8) -> u8 {
        let key_byte = self.keys[self.cur_r % self.keys.len()];
        self.cur_r = (self.cur_r + 1) % self.keys.len();
        b ^ key_byte
    }

    pub fn reset_key_position(&mut self) {
        self.cur_r = 0;
    }

    pub fn set_sent_key(&mut self, sent: bool) {
        self.sent_key = sent;
        self.reset_key_position();
    }

    pub fn update_keys(&mut self, keys: Vec<u8>) {
        self.keys = keys;
    }

    pub async fn read_message(&mut self) -> io::Result<Message> {
        let mut cmd_buf = [0u8; 1];
        self.read_half.read_exact(&mut cmd_buf).await?;
        let mut cmd_u8 = cmd_buf[0];
        if self.sent_key {
            cmd_u8 = self.read_key(cmd_u8);
        }
        let cmd = cmd_u8 as i8;

        let size: usize;
        if self.sent_key {
            let mut b = [0u8; 2];
            self.read_half.read_exact(&mut b).await?;
            let hi = self.read_key(b[0]) as u16;
            let lo = self.read_key(b[1]) as u16;
            size = ((hi << 8) | lo) as usize;
        } else {
            let mut size_buf = [0u8; 2];
            self.read_half.read_exact(&mut size_buf).await?;
            size = u16::from_be_bytes(size_buf) as usize;
        }

        let mut data = vec![0u8; size];
        if size > 0 {
            self.read_half.read_exact(&mut data).await?;
        }
        if self.sent_key {
            for byte in &mut data {
                *byte = self.read_key(*byte);
            }
        }

        Ok(Message::with_data(cmd, data))
    }
}

pub struct SessionWriter {
    write_half: OwnedWriteHalf,
    keys: Vec<u8>,
    cur_w: usize,
    sent_key: bool,
}

impl SessionWriter {
    pub const SEPICAL_CMDS: [i8; 7] = [-32, -66, -74, 11, -67, -87, 66];

    fn write_key(&mut self, b: u8) -> u8 {
        let key_byte = self.keys[self.cur_w % self.keys.len()];
        self.cur_w = (self.cur_w + 1) % self.keys.len();
        b ^ key_byte
    }

    pub fn reset_key_position(&mut self) {
        self.cur_w = 0;
    }

    pub fn set_sent_key(&mut self, sent: bool) {
        self.sent_key = sent;
        self.reset_key_position();
    }

    pub fn update_keys(&mut self, keys: Vec<u8>) {
        self.keys = keys;
    }

    fn check_special_cmd(&self, cmd: i8) -> bool {
        Self::SEPICAL_CMDS.contains(&cmd)
    }

    pub async fn send_message(&mut self, msg: &Message) -> io::Result<()> {
        if self.sent_key {
            let enc = self.write_key(msg.command as u8);
            self.write_half.write_all(&[enc]).await?;
        } else {
            self.write_half.write_all(&[msg.command as u8]).await?;
        }

        let data = msg.get_data();
        let size = data.len();

        if self.sent_key && self.check_special_cmd(msg.command) {
            let s = size as u32;
            let b0 = (s & 255) as u8;
            let b1 = ((s >> 8) & 255) as u8;
            let b2 = ((s >> 16) & 255) as u8;
            let mut out = [b0, b1, b2];
            for x in &mut out {
                let enc = self.write_key(*x);
                *x = enc.wrapping_sub(128);
            }
            self.write_half.write_all(&out).await?;
        } else if self.sent_key {
            let hi = ((size >> 8) & 255) as u8;
            let lo = (size & 255) as u8;
            let out = [self.write_key(hi), self.write_key(lo)];
            self.write_half.write_all(&out).await?;
        } else {
            let len_be = (size as u16).to_be_bytes();
            self.write_half.write_all(&len_be).await?;
        }

        if self.sent_key {
            let mut encrypted = Vec::with_capacity(data.len());
            for &b in data {
                encrypted.push(self.write_key(b));
            }
            self.write_half.write_all(&encrypted).await?;
        } else {
            self.write_half.write_all(data).await?;
        }

        self.write_half.flush().await
    }

    pub async fn send_key(&mut self, keys: &[u8]) -> io::Result<()> {
        let n = keys.len();
        let mut payload = Vec::with_capacity(1 + n);
        payload.push(n as u8);
        if n > 0 {
            payload.push(keys[0]);
        }
        for i in 1..n {
            payload.push(keys[i] ^ keys[i - 1]);
        }

        let msg = Message::with_data(-27, payload);
        self.send_message(&msg).await
    }

    pub async fn shutdown(&mut self) -> io::Result<()> {
        self.write_half.shutdown().await?;
        Ok(())
    }
}

/// Session state (player data, user info, etc.) - separate from I/O
pub struct SessionState {
    pub keys: Vec<u8>,
    pub sent_key: bool,
    pub zoom_level: u8,
    pub player: Option<RtPlayer>,
    pub user_id: Option<i32>,
    pub username: Option<String>,
    pub password: Option<String>,
    pub is_admin: bool,
    pub version: i32,
    pub vnd: i32,
}

impl SessionState {
    pub fn new() -> Self {
        Self {
            keys: b"AHWUOCDZ".to_vec(),
            sent_key: false,
            zoom_level: 1,
            player: None,
            user_id: None,
            username: None,
            password: None,
            is_admin: false,
            version: 0,
            vnd: 0,
        }
    }

    pub fn set_sent_key(&mut self, sent: bool) {
        self.sent_key = sent;
    }

    pub fn get_key(&self) -> &[u8] {
        &self.keys
    }

    pub fn set_player(&mut self, player: RtPlayer) {
        self.player = Some(player);
    }

    pub fn get_player(&self) -> Option<&RtPlayer> {
        self.player.as_ref()
    }

    pub fn take_player(&mut self) -> Option<RtPlayer> {
        self.player.take()
    }

    pub fn set_user_id(&mut self, user_id: i32) {
        self.user_id = Some(user_id);
    }

    pub fn set_is_admin(&mut self, is_admin: bool) {
        self.is_admin = is_admin;
    }

    pub fn get_user_id(&self) -> Option<i32> {
        self.user_id
    }

    pub fn set_credentials(&mut self, username: String, password: String) {
        self.username = Some(username);
        self.password = Some(password);
    }

    pub fn get_username(&self) -> Option<&String> {
        self.username.as_ref()
    }

    pub fn get_password(&self) -> Option<&String> {
        self.password.as_ref()
    }

    pub fn set_version(&mut self, version: i32) {
        self.version = version;
    }

    pub fn get_version(&self) -> i32 {
        self.version
    }
}

pub struct SplitSession {
    pub reader: Arc<Mutex<SessionReader>>,
    pub writer: Arc<Mutex<SessionWriter>>,
    pub state: Arc<RwLock<SessionState>>,
    message_tx: Option<mpsc::Sender<Message>>,
}

impl SplitSession {
    pub fn new(stream: TcpStream) -> Self {
        let (read_half, write_half) = stream.into_split();
        let keys = b"AHWUOCDZ".to_vec();

        Self {
            reader: Arc::new(Mutex::new(SessionReader {
                read_half,
                keys: keys.clone(),
                cur_r: 0,
                sent_key: false,
            })),
            writer: Arc::new(Mutex::new(SessionWriter {
                write_half,
                keys,
                cur_w: 0,
                sent_key: false,
            })),
            state: Arc::new(RwLock::new(SessionState::new())),
            message_tx: None,
        }
    }

    pub fn set_message_channel(&mut self, tx: mpsc::Sender<Message>) {
        self.message_tx = Some(tx);
    }

    pub fn queue_message(&self, msg: Message) -> bool {
        if let Some(tx) = &self.message_tx {
            match tx.try_send(msg) {
                Ok(_) => true,
                Err(mpsc::error::TrySendError::Full(_)) => {
                    eprintln!("[QUEUE] Message queue full, dropping message");
                    false
                }
                Err(mpsc::error::TrySendError::Closed(_)) => {
                    eprintln!("[QUEUE] Message channel closed");
                    false
                }
            }
        } else {
            false
        }
    }

    pub async fn send_sync(&self, msg: &Message) -> io::Result<()> {
        let mut writer = self.writer.lock().await;
        writer.send_message(msg).await
    }
    pub async fn set_sent_key(&self, sent: bool) {
        {
            let mut reader = self.reader.lock().await;
            reader.set_sent_key(sent);
        }
        {
            let mut writer = self.writer.lock().await;
            writer.set_sent_key(sent);
        }
        {
            let mut state = self.state.write().await;
            state.set_sent_key(sent);
        }
    }

    /// Send encryption key to client
    pub async fn send_key_async(&self) -> io::Result<()> {
        let keys = {
            let state = self.state.read().await;
            state.keys.clone()
        };
        let mut writer = self.writer.lock().await;
        writer.send_key(&keys).await
    }

    /// Get cloned references for spawning tasks
    pub fn get_writer(&self) -> Arc<Mutex<SessionWriter>> {
        self.writer.clone()
    }

    pub fn get_reader(&self) -> Arc<Mutex<SessionReader>> {
        self.reader.clone()
    }

    pub fn get_state(&self) -> Arc<RwLock<SessionState>> {
        self.state.clone()
    }
}

/// Type alias for Arc-wrapped SplitSession
pub type SplitSessionArc = Arc<SplitSession>;
