#![allow(dead_code)]
use anyhow::{bail, Result};
use bytes::{Buf, BufMut, BytesMut};

#[derive(Debug)]
pub struct Message {
    pub command: i8,
    pub payload: BytesMut,
}

impl Message {
    const BYTES: i8 = 1;
    pub fn new(command: i8) -> Self {
        Self {
            command,
            payload: BytesMut::new(),
        }
    }
    pub fn with_data(command: i8, data: Vec<u8>) -> Self {
        Self {
            command,
            payload: BytesMut::from(&data[..]),
        }
    }

    pub fn write_byte(&mut self, value: i8) -> Result<()> {
        self.payload.put_i8(value);
        Ok(())
    }
    pub fn write_short(&mut self, value: i16) -> Result<()> {
        self.payload.put_i16(value);
        Ok(())
    }

    pub fn write_int(&mut self, value: i32) -> Result<()> {
        self.payload.put_i32(value);
        Ok(())
    }

    pub fn write_long(&mut self, value: i64) -> Result<()> {
        self.payload.put_i64(value);
        Ok(())
    }

    pub fn write_bool(&mut self, value: bool) -> Result<()> {
        self.payload.put_u8(if value { 1 } else { 0 });
        Ok(())
    }

    pub fn write_boolean(&mut self, value: bool) -> Result<()> {
        self.write_bool(value)
    }

    pub fn write_utf(&mut self, value: &str) -> Result<()> {
        let bytes = value.as_bytes();
        self.payload.put_u16(bytes.len() as u16);
        self.payload.put_slice(bytes);
        Ok(())
    }

    pub fn write(&mut self, bytes: &[u8]) -> Result<()> {
        self.payload.put_slice(bytes);
        Ok(())
    }

    pub fn read_byte(&mut self) -> Result<i8> {
        // Ok(self.payload.get_i8())
        if self.payload.remaining() < 1 {
            bail!(
                "Cannot read i8: need 1 byte, but only {} bytes remaining",
                self.payload.remaining()
            );
        }
        Ok(self.payload.get_i8())
    }

    pub fn read_short(&mut self) -> Result<i16> {
        if self.payload.remaining() < 2 {
            bail!(
                "Cannot read i16: need 2 bytes, but only {} bytes remaining",
                self.payload.remaining()
            );
        }
        Ok(self.payload.get_i16())
    }

    pub fn read_int(&mut self) -> Result<i32> {
        if self.payload.remaining() < 4 {
            bail!(
                "Cannot read i32: need 4 bytes, but only {} bytes remaining",
                self.payload.remaining()
            );
        }
        Ok(self.payload.get_i32())
    }

    pub fn read_long(&mut self) -> Result<i64> {
        if self.payload.remaining() < 8 {
            bail!(
                "Cannot read i64: need 8 bytes, but only {} bytes remaining",
                self.payload.remaining()
            );
        }
        Ok(self.payload.get_i64())
    }

    pub fn read_bool(&mut self) -> Result<bool> {
        if self.payload.remaining() < 1 {
            bail!(
                "Cannot read bool: need 1 byte, but only {} bytes remaining",
                self.payload.remaining()
            );
        }
        Ok(self.payload.get_u8() != 0)
    }

    pub fn read_utf(&mut self) -> Result<String> {
        if self.payload.remaining() < 2 {
            bail!(
                "Cannot read UTF string: need 2 bytes for length, but only {} bytes remaining",
                self.payload.remaining()
            );
        }
        let len = self.payload.get_u16() as usize;
        if self.payload.remaining() < len {
            bail!(
                "Cannot read UTF string: need {} bytes, but only {} bytes remaining",
                len,
                self.payload.remaining()
            );
        }
        let bytes = self.payload.split_to(len);
        Ok(String::from_utf8(bytes.to_vec())?)
    }

    pub fn get_data(&self) -> &[u8] {
        &self.payload
    }
}

impl Clone for Message {
    fn clone(&self) -> Self {
        Message {
            command: self.command,
            payload: self.payload.clone(),
        }
    }
}
