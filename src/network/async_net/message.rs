use std::io::{Cursor, Error, ErrorKind, Read, Write};

#[derive(Debug)]
pub struct Message {
    pub command: i8,
    pub data: Vec<u8>,
    reader: Option<Cursor<Vec<u8>>>,
    writer: Option<Vec<u8>>,
}

impl Message {
    /// Constructor for writing messages (like Java Message(int command))
    pub fn new_for_writing(command: i8) -> Self {
        Message {
            command,
            data: Vec::new(),
            reader: None,
            writer: Some(Vec::new()),
        }
    }

    /// Constructor for reading messages (like Java Message(byte command, byte[] data))
    pub fn new(command: i8, data: Vec<u8>) -> Self {
        let reader = Cursor::new(data.clone());
        Message {
            command,
            data,
            reader: Some(reader),
            writer: None,
        }
    }

    /// Get writer data (like Java getData())
    pub fn get_data(&self) -> Vec<u8> {
        if let Some(ref writer_data) = self.writer {
            writer_data.clone()
        } else {
            self.data.clone()
        }
    }

    // Reading methods (match Java DataInputStream methods)
    pub fn read(&mut self) -> Result<u8, Error> {
        if let Some(ref mut reader) = self.reader {
            let mut buf = [0u8; 1];
            reader.read_exact(&mut buf)?;
            Ok(buf[0])
        } else {
            Err(Error::new(ErrorKind::InvalidInput, "No reader available"))
        }
    }

    pub fn read_bytes(&mut self, buf: &mut [u8]) -> Result<usize, Error> {
        if let Some(ref mut reader) = self.reader {
            reader.read(buf)
        } else {
            Err(Error::new(ErrorKind::InvalidInput, "No reader available"))
        }
    }

    pub fn read_boolean(&mut self) -> Result<bool, Error> {
        let byte = self.read_byte()?;
        Ok(byte != 0)
    }

    pub fn read_byte(&mut self) -> Result<i8, Error> {
        let byte = self.read()?;
        Ok(byte as i8)
    }

    pub fn read_unsigned_byte(&mut self) -> Result<u8, Error> {
        self.read()
    }

    pub fn read_short(&mut self) -> Result<i16, Error> {
        if let Some(ref mut reader) = self.reader {
            let mut buf = [0u8; 2];
            reader.read_exact(&mut buf)?;
            Ok(i16::from_be_bytes(buf))
        } else {
            Err(Error::new(ErrorKind::InvalidInput, "No reader available"))
        }
    }

    pub fn read_unsigned_short(&mut self) -> Result<u16, Error> {
        if let Some(ref mut reader) = self.reader {
            let mut buf = [0u8; 2];
            reader.read_exact(&mut buf)?;
            Ok(u16::from_be_bytes(buf))
        } else {
            Err(Error::new(ErrorKind::InvalidInput, "No reader available"))
        }
    }

    pub fn read_int(&mut self) -> Result<i32, Error> {
        if let Some(ref mut reader) = self.reader {
            let mut buf = [0u8; 4];
            reader.read_exact(&mut buf)?;
            Ok(i32::from_be_bytes(buf))
        } else {
            Err(Error::new(ErrorKind::InvalidInput, "No reader available"))
        }
    }

    pub fn read_long(&mut self) -> Result<i64, Error> {
        if let Some(ref mut reader) = self.reader {
            let mut buf = [0u8; 8];
            reader.read_exact(&mut buf)?;
            Ok(i64::from_be_bytes(buf))
        } else {
            Err(Error::new(ErrorKind::InvalidInput, "No reader available"))
        }
    }

    pub fn read_float(&mut self) -> Result<f32, Error> {
        let int_val = self.read_int()?;
        Ok(f32::from_bits(int_val as u32))
    }

    pub fn read_double(&mut self) -> Result<f64, Error> {
        let long_val = self.read_long()?;
        Ok(f64::from_bits(long_val as u64))
    }

    pub fn read_char(&mut self) -> Result<char, Error> {
        let short_val = self.read_unsigned_short()?;
        char::from_u32(short_val as u32)
            .ok_or_else(|| Error::new(ErrorKind::InvalidData, "Invalid char"))
    }

    pub fn read_utf(&mut self) -> Result<String, Error> {
        let length = self.read_unsigned_short()? as usize;
        let mut buf = vec![0u8; length];
        if let Some(ref mut reader) = self.reader {
            reader.read_exact(&mut buf)?;
        } else {
            return Err(Error::new(ErrorKind::InvalidInput, "No reader available"));
        }
        String::from_utf8(buf).map_err(|e| Error::new(ErrorKind::InvalidData, e))
    }

    pub fn read_fully(&mut self, buf: &mut [u8]) -> Result<(), Error> {
        if let Some(ref mut reader) = self.reader {
            reader.read_exact(buf)
        } else {
            Err(Error::new(ErrorKind::InvalidInput, "No reader available"))
        }
    }

    // Writing methods (match Java DataOutputStream methods)
    pub fn write(&mut self, bytes: &[u8]) -> Result<(), Error> {
        if let Some(ref mut writer_data) = self.writer {
            writer_data.extend_from_slice(bytes);
            Ok(())
        } else {
            Err(Error::new(ErrorKind::InvalidInput, "No writer available"))
        }
    }

    pub fn write_byte(&mut self, value: i8) -> Result<(), Error> {
        self.write(&[value as u8])
    }

    pub fn write_boolean(&mut self, value: bool) -> Result<(), Error> {
        self.write_byte(if value { 1 } else { 0 })
    }

    pub fn write_bytes(&mut self, s: &str) -> Result<(), Error> {
        self.write(s.as_bytes())
    }

    pub fn write_char(&mut self, value: char) -> Result<(), Error> {
        let char_val = value as u16;
        self.write(&char_val.to_be_bytes())
    }

    pub fn write_chars(&mut self, s: &str) -> Result<(), Error> {
        for c in s.chars() {
            self.write_char(c)?;
        }
        Ok(())
    }

    pub fn write_double(&mut self, value: f64) -> Result<(), Error> {
        let bits = value.to_bits();
        self.write(&bits.to_be_bytes())
    }

    pub fn write_float(&mut self, value: f32) -> Result<(), Error> {
        let bits = value.to_bits();
        self.write(&bits.to_be_bytes())
    }

    pub fn write_int(&mut self, value: i32) -> Result<(), Error> {
        self.write(&value.to_be_bytes())
    }

    pub fn write_long(&mut self, value: i64) -> Result<(), Error> {
        self.write(&value.to_be_bytes())
    }

    pub fn write_short(&mut self, value: i16) -> Result<(), Error> {
        self.write(&value.to_be_bytes())
    }

    pub fn write_utf(&mut self, value: &str) -> Result<(), Error> {
        let bytes = value.as_bytes();
        let len = bytes.len() as u16;
        self.write(&len.to_be_bytes())?;
        self.write(bytes)
    }

    pub fn finalize_write(&mut self) {
        if let Some(ref writer_data) = self.writer {
            self.data = writer_data.clone();
        }
    }

    pub fn cleanup(&mut self) {
        if let Some(ref mut writer_data) = self.writer {
            writer_data.clear();
        }
        self.data.clear();
        self.reader = None;
        self.writer = None;
    }

    pub fn dispose(&mut self) {
        self.cleanup();
    }
}

impl Clone for Message {
    fn clone(&self) -> Self {
        Message::new(self.command, self.data.clone())
    }
}