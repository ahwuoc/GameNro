use std::io::{Cursor, Error, ErrorKind, Read};

#[derive(Debug)]
pub struct Message {
    pub command: i8,
    pub data: Vec<u8>,
    reader: Cursor<Vec<u8>>,
}

impl Message {
    pub fn new(command: i8, data: Vec<u8>) -> Self {
        let reader = Cursor::new(data.clone());
        Message {
            command,
            data,
            reader,
        }
    }

    pub fn read_byte(&mut self) -> Result<i8, Error> {
        let mut buf = [0u8; 1];
        self.reader.read_exact(&mut buf)?;
        Ok(buf[0] as i8)
    }

    pub fn read_unsigned_byte(&mut self) -> Result<u8, Error> {
        let mut buf = [0u8; 1];
        self.reader.read_exact(&mut buf)?;
        Ok(buf[0])
    }

    pub fn read_short(&mut self) -> Result<i16, Error> {
        let mut buf = [0u8; 2];
        self.reader.read_exact(&mut buf)?;
        Ok(i16::from_be_bytes(buf))
    }

    pub fn read_unsigned_short(&mut self) -> Result<u16, Error> {
        let mut buf = [0u8; 2];
        self.reader.read_exact(&mut buf)?;
        Ok(u16::from_be_bytes(buf))
    }

    pub fn read_int(&mut self) -> Result<i32, Error> {
        let mut buf = [0u8; 4];
        self.reader.read_exact(&mut buf)?;
        Ok(i32::from_be_bytes(buf))
    }

    pub fn read_utf(&mut self) -> Result<String, Error> {
        let length = self.read_short()? as usize;
        let mut buf = vec![0u8; length];
        self.reader.read_exact(&mut buf)?;
        String::from_utf8(buf).map_err(|e| Error::new(ErrorKind::InvalidData, e))
    }
    pub fn write(&mut self, bytes: &[u8]) -> Result<(), Error> {
        self.data.extend_from_slice(bytes);
        self.reader = Cursor::new(self.data.clone());
        Ok(())
    }
    pub fn write_byte(&mut self, value: i8) -> Result<(), Error> {
        self.write(&[value as u8])
    }

    pub fn write_int(&mut self, value: i32) -> Result<(), Error> {
        self.write(&value.to_be_bytes())
    }
    pub fn write_utf(&mut self, value: &str) -> Result<(), Error> {
        let bytes = value.as_bytes();
        let len = bytes.len() as u16;
        self.data.extend_from_slice(&len.to_be_bytes());
        self.data.extend_from_slice(bytes);
        Ok(())
    }

    pub fn cleanup(&mut self) {
        self.data.clear();
    }
}
