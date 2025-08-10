use std::io;
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::net::{tcp::OwnedReadHalf, tcp::OwnedWriteHalf, TcpStream};
use crate::network::async_net::message::Message;
use crate::player::Player as RtPlayer;

pub struct AsyncSession {
    pub keys: Vec<u8>,
    pub sent_key: bool,
    pub zoom_level: u8,
    read_half: OwnedReadHalf,
    write_half: OwnedWriteHalf,
    cur_r: usize,
    cur_w: usize,
    pub player: Option<RtPlayer>,
    pub user_id: Option<i32>,
    pub username: Option<String>,
    pub password: Option<String>,
    pub is_admin: bool,
    pub version: i32,
    pub vnd: i32,
}

impl AsyncSession {
    pub fn new(stream: TcpStream) -> Self {
        let (read_half, write_half) = stream.into_split();
        Self {
            keys: b"AHWUOCDZ".to_vec(),
            sent_key: false,
            zoom_level: 1,
            read_half,
            write_half,
            cur_r: 0,
            cur_w: 0,
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
        self.reset_key_position();
    }

    pub fn get_key(&self) -> &[u8] {
        &self.keys
    }

    fn read_key(&mut self, b: u8) -> u8 {
        let key_byte = self.keys[self.cur_r % self.keys.len()];
        self.cur_r = (self.cur_r + 1) % self.keys.len();
        b ^ key_byte
    }

    fn write_key(&mut self, b: u8) -> u8 {
        let key_byte = self.keys[self.cur_w % self.keys.len()];
        self.cur_w = (self.cur_w + 1) % self.keys.len();
        b ^ key_byte
    }

    pub fn reset_key_position(&mut self) {
        self.cur_r = 0;
        self.cur_w = 0;
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

        // Payload
        let mut data = vec![0u8; size];
        if size > 0 {
            self.read_half.read_exact(&mut data).await?;
        }
        if self.sent_key {
            for byte in &mut data {
                *byte = self.read_key(*byte);
            }
        }

        Ok(Message::new(cmd, data))
    }

    pub async fn send_message(&mut self, msg: &Message) -> io::Result<()> {
        // Command
        if self.sent_key {
            let enc = self.write_key(msg.command as u8);
            self.write_half.write_all(&[enc]).await?;
        } else {
            self.write_half.write_all(&[msg.command as u8]).await?;
        }

        let data = msg.get_data();
        let size = data.len();

        const SPECIAL_CMDS: [i8; 7] = [-32, -66, -74, 11, -67, -87, 66];
        if self.sent_key && SPECIAL_CMDS.contains(&msg.command) {
            // 3-byte size: write (xor_byte) - 128 for each
            let s = size as u32;
            let b0 = (s & 0xFF) as u8;
            let b1 = ((s >> 8) & 0xFF) as u8;
            let b2 = ((s >> 16) & 0xFF) as u8;
            let mut out = [b0, b1, b2];
            for x in &mut out {
                let enc = self.write_key(*x);
                *x = enc.wrapping_sub(128);
            }
            self.write_half.write_all(&out).await?;
        } else if self.sent_key {
            let hi = ((size >> 8) & 0xFF) as u8;
            let lo = (size & 0xFF) as u8;
            let out = [self.write_key(hi), self.write_key(lo)];
            self.write_half.write_all(&out).await?;
        } else {
            let len_be = (size as u16).to_be_bytes();
            self.write_half.write_all(&len_be).await?;
        }

        if self.sent_key {
            let mut encrypted = Vec::with_capacity(data.len());
            for &b in &data {
                encrypted.push(self.write_key(b));
            }
            self.write_half.write_all(&encrypted).await?;
        } else {
            self.write_half.write_all(&data).await?;
        }

        self.write_half.flush().await
    }

    

    pub async fn send_key_async(&mut self) -> io::Result<()> {
        let n = self.keys.len();
        let mut payload = Vec::with_capacity(1 + n);
        payload.push(n as u8);
        if n > 0 {
            payload.push(self.keys[0]);
        }
        for i in 1..n {
            payload.push(self.keys[i] ^ self.keys[i - 1]);
        }

        let msg = Message::new(-27, payload);
        self.send_message(&msg).await
    }

    pub async fn send_message_old(&mut self, command: i8, data: Vec<u8>) -> io::Result<()> {
        let msg = Message::new(command, data);
        self.send_message(&msg).await
    }

    pub fn set_player(&mut self, player: RtPlayer) {
        self.player = Some(player);
    }

    pub fn get_player(&self) -> Option<&RtPlayer> {
        self.player.as_ref()
    }

    pub fn get_player_mut(&mut self) -> Option<&mut RtPlayer> {
        self.player.as_mut()
    }

    pub fn set_user_id(&mut self, user_id: i32) {
        self.user_id = Some(user_id);
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

    pub fn set_admin(&mut self, is_admin: bool) {
        self.is_admin = is_admin;
    }

    pub fn is_admin(&self) -> bool {
        self.is_admin
    }

    pub fn set_version(&mut self, version: i32) {
        self.version = version;
    }

    pub fn get_version(&self) -> i32 {
        self.version
    }

    pub fn set_vnd(&mut self, vnd: i32) {
        self.vnd = vnd;
    }

    pub fn get_vnd(&self) -> i32 {
        self.vnd
    }
}


