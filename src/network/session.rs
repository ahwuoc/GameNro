use super::message::Message;
use crate::player::player_manager::PLAYER_MANAGER;
use crate::player::Player as RtPlayer;
use leaky_bucket::RateLimiter;
use std::io;
use std::sync::Arc;
use std::time::Duration;
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::net::{tcp::OwnedReadHalf, tcp::OwnedWriteHalf, TcpStream};
use tokio::sync::{mpsc, Mutex, RwLock};

#[derive(Debug)]
pub struct SessionReader {
    read_half: OwnedReadHalf,
    keys: Arc<Vec<u8>>,
    cur_r: usize,
    sent_key: bool,
    limiter: RateLimiter,
}

impl SessionReader {
    pub fn new(read_half: OwnedReadHalf, keys: Arc<Vec<u8>>) -> Self {
        Self {
            read_half,
            keys,
            cur_r: 0,
            sent_key: false,
            limiter: RateLimiter::builder()
                .max(50)
                .initial(50)
                .refill(50)
                .interval(Duration::from_secs(1))
                .build(),
        }
    }
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

    pub async fn read_message(&mut self) -> io::Result<Message> {
        self.limiter.acquire(1).await;
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

#[derive(Debug)]
pub struct SessionWriter {
    write_half: OwnedWriteHalf,
    keys: Arc<Vec<u8>>,
    cur_w: usize,
    sent_key: bool,
}

impl SessionWriter {
    pub const SPECIAL_CMDS: [i8; 7] = [-32, -66, -74, 11, -67, -87, 66];

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

    fn check_special_cmd(&self, cmd: i8) -> bool {
        Self::SPECIAL_CMDS.contains(&cmd)
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

use crate::player::player_actor::{PlayerActor, PlayerHandle, PlayerMessage};

#[derive(Debug)]
pub struct SessionState {
    pub keys: Arc<Vec<u8>>,
    pub sent_key: bool,
    pub zoom_level: u8,
    pub player_handle: Option<PlayerHandle>,
    pub user_id: Option<i32>,
    pub version: i32,
}

impl SessionState {
    pub fn new(keys: Arc<Vec<u8>>) -> Self {
        Self {
            keys,
            sent_key: false,
            zoom_level: 1,
            player_handle: None,
            user_id: None,
            version: 0,
        }
    }
}
#[derive(Debug)]
pub struct AsyncSession {
    pub reader: Arc<Mutex<SessionReader>>,
    pub writer: Arc<Mutex<SessionWriter>>,
    pub state: Arc<RwLock<SessionState>>,
    pub message_tx: Arc<std::sync::RwLock<Option<mpsc::Sender<Message>>>>,
}

impl AsyncSession {
    pub fn new(stream: TcpStream) -> Self {
        let (read_half, write_half) = stream.into_split();
        let keys = Arc::new(b"AHWUOCDZ".to_vec());

        Self {
            reader: Arc::new(Mutex::new(SessionReader::new(read_half, keys.clone()))),
            writer: Arc::new(Mutex::new(SessionWriter {
                write_half,
                keys: keys.clone(),
                cur_w: 0,
                sent_key: false,
            })),
            state: Arc::new(RwLock::new(SessionState::new(keys))),
            message_tx: Arc::new(std::sync::RwLock::new(None)),
        }
    }

    pub fn get_reader(&self) -> Arc<Mutex<SessionReader>> {
        self.reader.clone()
    }

    pub fn get_writer(&self) -> Arc<Mutex<SessionWriter>> {
        self.writer.clone()
    }

    pub async fn set_message_channel(&self, tx: mpsc::Sender<Message>) {
        if let Ok(mut guard) = self.message_tx.write() {
            *guard = Some(tx);
        }
    }

    pub fn transmit(&self, msg: Message) -> bool {
        let guard = match self.message_tx.read() {
            Ok(g) => g,
            Err(_) => return false,
        };

        let tx = match guard.as_ref() {
            Some(t) => t,
            None => return false,
        };

        match tx.try_send(msg) {
            Ok(_) => true,
            Err(mpsc::error::TrySendError::Full(_)) => {
                eprintln!("[WARN] Message queue full");
                false
            }
            Err(mpsc::error::TrySendError::Closed(_)) => false,
        }
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
            state.sent_key = sent;
        }
    }

    pub async fn send_key_async(&self) -> io::Result<()> {
        let keys = {
            let state = self.state.read().await;
            state.keys.as_ref().clone()
        };
        let mut writer = self.writer.lock().await;
        writer.send_key(&keys).await
    }

    pub async fn set_player(&self, mut player: RtPlayer, session_arc: SessionArc) {
        let (tx, rx) = tokio::sync::mpsc::channel(100);
        let player_id = player.id;
        let handle = PlayerHandle::new(player_id, false, tx.clone());
        player.session = Some(session_arc.clone());

        let pet_handle = if let Some(pet_data) = player.pet_data.take() {
            crate::player::player_actor::pet::PetService::load_pet(&mut player, pet_data)
                .await
                .ok()
        } else {
            None
        };

        let mut actor = PlayerActor::new(player, session_arc.clone(), rx);
        actor.pet_handle = pet_handle;

        tokio::spawn(actor.run());
        PLAYER_MANAGER.add(player_id, handle.clone());
        let mut state = self.state.write().await;
        state.player_handle = Some(handle);
    }

    pub async fn get_player_handle(&self) -> Option<PlayerHandle> {
        let state = self.state.read().await;
        state.player_handle.clone()
    }

    pub async fn get_player_snapshot(&self) -> Option<RtPlayer> {
        let handle = self.get_player_handle().await?;
        let (tx, rx) = tokio::sync::oneshot::channel();
        if let Ok(_) = handle.send(PlayerMessage::GetSnapshot(tx)).await {
            rx.await.ok()
        } else {
            None
        }
    }

    pub async fn set_user_id(&self, user_id: i32) {
        let mut state = self.state.write().await;
        state.user_id = Some(user_id);
    }

    pub async fn get_user_id(&self) -> Option<i32> {
        let state = self.state.read().await;
        state.user_id
    }

    pub async fn set_version(&self, version: i32) {
        let mut state = self.state.write().await;
        state.version = version;
    }

    pub async fn get_version(&self) -> i32 {
        let state = self.state.read().await;
        state.version
    }

    pub async fn get_zoom_level(&self) -> u8 {
        let state = self.state.read().await;
        state.zoom_level
    }

    pub async fn set_zoom_level(&self, level: u8) {
        let mut state = self.state.write().await;
        state.zoom_level = level;
    }

    pub async fn shutdown(&self) -> io::Result<()> {
        let mut writer = self.writer.lock().await;
        writer.shutdown().await
    }
}

pub type SessionArc = Arc<AsyncSession>;
