# Tài liệu: Xử lý lỗi Panic trong Network Message

## 📋 Mục lục
1. [Lý do gây ra lỗi Panic](#1-lý-do-gây-ra-lỗi-panic)
2. [Cách giải quyết](#2-cách-giải-quyết)
3. [Kiến thức cần nắm](#3-kiến-thức-cần-nắm)
4. [Code mẫu hoàn chỉnh](#4-code-mẫu-hoàn-chỉnh)
5. [Test cases](#5-test-cases)

---

## 1. Lý do gây ra lỗi Panic

### 1.1. Cơ chế hoạt động của `bytes::Buf`

Trait `Buf` từ crate `bytes` cung cấp các phương thức để đọc dữ liệu từ buffer:

```rust
use bytes::Buf;

// Các hàm này KHÔNG kiểm tra độ dài buffer
let value = buffer.get_i8();   // Đọc 1 byte
let value = buffer.get_i16();  // Đọc 2 bytes
let value = buffer.get_i32();  // Đọc 4 bytes
let value = buffer.get_i64();  // Đọc 8 bytes
```

**⚠️ Vấn đề:** Nếu buffer không đủ bytes, các hàm này sẽ **PANIC** (crash chương trình) thay vì trả về lỗi.

### 1.2. Kịch bản thực tế

```rust
// Client gửi gói tin bị lỗi hoặc bị cắt giữa chừng
let malformed_data = vec![0xAA, 0xBB]; // Chỉ 2 bytes

let mut msg = Message::with_data(CMD_LOGIN, malformed_data);

// Server cố đọc 4 bytes
let user_id = msg.read_int()?; // ❌ PANIC! Thread crashed!
```

**Hậu quả:**
- Server game bị crash
- Tất cả người chơi mất kết nối
- Hacker có thể khai thác để tấn công DoS

### 1.3. Tại sao lại nguy hiểm?

Trong lập trình mạng:
- **Dữ liệu từ client KHÔNG BAO GIỜ được tin tưởng**
- Gói tin có thể bị:
  - Cắt xén (truncated)
  - Sửa đổi bởi hacker
  - Lỗi trong quá trình truyền tải
  - Gửi cố ý để tấn công

---

## 2. Cách giải quyết

### 2.1. Nguyên tắc vàng

**"Luôn kiểm tra trước khi đọc"**

```rust
// ✅ ĐÚNG
if buffer.remaining() >= 4 {
    let value = buffer.get_i32();
} else {
    return Err("Not enough data");
}

// ❌ SAI
let value = buffer.get_i32(); // Có thể panic
```

### 2.2. Sử dụng `remaining()` method

`BytesMut` implements trait `Buf`, cung cấp method `remaining()`:

```rust
pub fn remaining(&self) -> usize;
// Trả về số bytes còn lại trong buffer
```

**Ví dụ:**
```rust
let mut buffer = BytesMut::from(&[1, 2, 3, 4][..]);
println!("{}", buffer.remaining()); // Output: 4

buffer.get_i16(); // Đọc 2 bytes
println!("{}", buffer.remaining()); // Output: 2
```

### 2.3. Template chuẩn cho hàm read

```rust
pub fn read_TYPE(&mut self) -> Result<TYPE> {
    // Bước 1: Kiểm tra độ dài
    if self.payload.remaining() < SIZE_IN_BYTES {
        bail!("Not enough bytes: need {}, have {}", 
              SIZE_IN_BYTES, 
              self.payload.remaining());
    }
    
    // Bước 2: Đọc an toàn
    Ok(self.payload.get_TYPE())
}
```

---

## 3. Kiến thức cần nắm

### 3.1. Kích thước các kiểu dữ liệu trong Rust

| Kiểu dữ liệu | Kích thước | Phương thức Buf |
|--------------|------------|-----------------|
| `i8`, `u8`   | 1 byte     | `get_i8()`, `get_u8()` |
| `i16`, `u16` | 2 bytes    | `get_i16()`, `get_u16()` |
| `i32`, `u32` | 4 bytes    | `get_i32()`, `get_u32()` |
| `i64`, `u64` | 8 bytes    | `get_i64()`, `get_u64()` |
| `bool`       | 1 byte     | `get_u8()` |

### 3.2. Crate `anyhow` cho error handling

```rust
use anyhow::{bail, Result};

// Result<T> là alias cho Result<T, anyhow::Error>
pub fn my_function() -> Result<i32> {
    // Trả về lỗi với message
    if condition_failed {
        bail!("Error message with {}", variable);
    }
    
    // Hoặc dùng ? để propagate error
    let value = some_operation()?;
    
    Ok(value)
}
```

### 3.3. Trait `Buf` từ crate `bytes`

**Các method quan trọng:**

```rust
use bytes::Buf;

// Kiểm tra số bytes còn lại
fn remaining(&self) -> usize;

// Đọc và di chuyển con trỏ
fn get_i8(&mut self) -> i8;
fn get_u8(&mut self) -> u8;
fn get_i16(&mut self) -> i16;
fn get_i32(&mut self) -> i32;
fn get_i64(&mut self) -> i64;

// Lấy slice bytes và di chuyển con trỏ
fn copy_to_slice(&mut self, dst: &mut [u8]);
```

### 3.4. UTF-8 String encoding

Cấu trúc gói tin UTF-8 trong protocol:
```
[2 bytes length][N bytes UTF-8 data]
```

Ví dụ:
```
String "Hello" -> [0x00, 0x05, 'H', 'e', 'l', 'l', 'o']
                   ^^^^^^^^   ^^^^^^^^^^^^^^^^^^^^^^^^^^^
                   Length=5   UTF-8 bytes
```

**Đọc UTF-8 cần kiểm tra:**
1. Có đủ 2 bytes để đọc length?
2. Có đủ N bytes để đọc string data?
3. Dữ liệu có phải UTF-8 hợp lệ?

---

## 4. Code mẫu hoàn chỉnh

### 4.1. Fixed Message struct

```rust
use anyhow::{bail, Result};
use bytes::{Buf, BufMut, BytesMut};

#[derive(Debug)]
pub struct Message {
    pub command: i8,
    pub payload: BytesMut,
}

impl Message {
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

    // === WRITE METHODS (không cần fix) ===
    
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

    // === READ METHODS (ĐÃ FIX) ===

    pub fn read_byte(&mut self) -> Result<i8> {
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
        // Bước 1: Đọc length (2 bytes)
        if self.payload.remaining() < 2 {
            bail!(
                "Cannot read UTF-8 length: need 2 bytes, but only {} bytes remaining",
                self.payload.remaining()
            );
        }
        let len = self.payload.get_u16() as usize;

        // Bước 2: Đọc string data (len bytes)
        if self.payload.remaining() < len {
            bail!(
                "Cannot read UTF-8 string: need {} bytes, but only {} bytes remaining",
                len,
                self.payload.remaining()
            );
        }
        let bytes = self.payload.split_to(len);

        // Bước 3: Validate UTF-8
        String::from_utf8(bytes.to_vec())
            .map_err(|e| anyhow::anyhow!("Invalid UTF-8 string: {}", e))
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
```

### 4.2. Giải thích chi tiết các thay đổi

#### `read_byte()`
```rust
pub fn read_byte(&mut self) -> Result<i8> {
    // Kiểm tra: cần 1 byte
    if self.payload.remaining() < 1 {
        bail!("Cannot read i8: need 1 byte, but only {} bytes remaining",
              self.payload.remaining());
    }
    // Đọc an toàn
    Ok(self.payload.get_i8())
}
```

#### `read_short()`
```rust
pub fn read_short(&mut self) -> Result<i16> {
    // Kiểm tra: cần 2 bytes
    if self.payload.remaining() < 2 {
        bail!("Cannot read i16: need 2 bytes, but only {} bytes remaining",
              self.payload.remaining());
    }
    Ok(self.payload.get_i16())
}
```

#### `read_utf()` - Phức tạp nhất
```rust
pub fn read_utf(&mut self) -> Result<String> {
    // Kiểm tra 1: Đủ 2 bytes để đọc length?
    if self.payload.remaining() < 2 {
        bail!("Cannot read UTF-8 length: need 2 bytes, but only {} bytes remaining",
              self.payload.remaining());
    }
    let len = self.payload.get_u16() as usize;

    // Kiểm tra 2: Đủ len bytes để đọc string?
    if self.payload.remaining() < len {
        bail!("Cannot read UTF-8 string: need {} bytes, but only {} bytes remaining",
              len, self.payload.remaining());
    }
    let bytes = self.payload.split_to(len);

    // Kiểm tra 3: Dữ liệu có phải UTF-8 hợp lệ?
    String::from_utf8(bytes.to_vec())
        .map_err(|e| anyhow::anyhow!("Invalid UTF-8 string: {}", e))
}
```

---

## 5. Test cases

### 5.1. Test normal cases

```rust
#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_read_write_int() {
        let mut msg = Message::new(1);
        msg.write_int(12345).unwrap();
        
        let value = msg.read_int().unwrap();
        assert_eq!(value, 12345);
    }

    #[test]
    fn test_read_write_string() {
        let mut msg = Message::new(1);
        msg.write_utf("Hello Rust").unwrap();
        
        let value = msg.read_utf().unwrap();
        assert_eq!(value, "Hello Rust");
    }
}
```

### 5.2. Test panic prevention (QUAN TRỌNG)

```rust
#[cfg(test)]
mod panic_tests {
    use super::*;

    #[test]
    fn test_read_int_insufficient_data() {
        // Chỉ 2 bytes, nhưng cần 4 bytes
        let mut msg = Message::with_data(1, vec![0xAA, 0xBB]);
        
        let result = msg.read_int();
        assert!(result.is_err()); // ✅ Trả về Err thay vì panic
        assert!(result.unwrap_err().to_string().contains("need 4 bytes"));
    }

    #[test]
    fn test_read_short_empty_buffer() {
        let mut msg = Message::new(1);
        
        let result = msg.read_short();
        assert!(result.is_err());
    }

    #[test]
    fn test_read_utf_truncated_length() {
        // Chỉ 1 byte, không đủ đọc length (2 bytes)
        let mut msg = Message::with_data(1, vec![0xFF]);
        
        let result = msg.read_utf();
        assert!(result.is_err());
        assert!(result.unwrap_err().to_string().contains("length"));
    }

    #[test]
    fn test_read_utf_truncated_data() {
        // Length = 10, nhưng chỉ có 5 bytes data
        let data = vec![
            0x00, 0x0A, // length = 10
            0x41, 0x42, 0x43, 0x44, 0x45, // Chỉ 5 bytes
        ];
        let mut msg = Message::with_data(1, data);
        
        let result = msg.read_utf();
        assert!(result.is_err());
        assert!(result.unwrap_err().to_string().contains("need 10 bytes"));
    }

    #[test]
    fn test_read_utf_invalid_utf8() {
        // Length = 3, data không phải UTF-8 hợp lệ
        let data = vec![
            0x00, 0x03, // length = 3
            0xFF, 0xFE, 0xFD, // Invalid UTF-8
        ];
        let mut msg = Message::with_data(1, data);
        
        let result = msg.read_utf();
        assert!(result.is_err());
        assert!(result.unwrap_err().to_string().contains("Invalid UTF-8"));
    }
}
```

### 5.3. Chạy tests

```bash
# Chạy tất cả tests
cargo test

# Chạy chỉ panic tests
cargo test panic_tests

# Chạy với output chi tiết
cargo test -- --nocapture
```

---

## 6. Best Practices

### 6.1. Defensive Programming

```rust
// ❌ BAD: Tin tưởng dữ liệu từ client
pub fn handle_login(msg: &mut Message) -> Result<()> {
    let username = msg.read_utf()?; // Có thể panic nếu không fix
    let password = msg.read_utf()?;
    // ...
}

// ✅ GOOD: Validate mọi thứ
pub fn handle_login(msg: &mut Message) -> Result<()> {
    let username = msg.read_utf()?; // Trả về Err nếu invalid
    let password = msg.read_utf()?;
    
    // Validate thêm
    if username.len() > 50 {
        bail!("Username too long");
    }
    
    // ...
}
```

### 6.2. Error Logging

```rust
use log::{error, warn};

pub fn process_message(msg: &mut Message) -> Result<()> {
    match msg.read_int() {
        Ok(value) => {
            // Process value
        }
        Err(e) => {
            error!("Failed to read int from message: {}", e);
            warn!("Malformed packet from client, dropping connection");
            return Err(e);
        }
    }
    Ok(())
}
```

### 6.3. Protocol Documentation

Luôn document protocol format:

```rust
/// Login Request Message
/// 
/// Format:
/// - Command: -1 (LOGIN)
/// - Payload:
///   - username: String (UTF-8, max 50 chars)
///   - password: String (UTF-8, max 50 chars)
///   - version: i16
/// 
/// Total size: ~110 bytes
pub const CMD_LOGIN: i8 = -1;
```

---

## 7. Tổng kết

### Checklist trước khi deploy

- [ ] Đã fix tất cả các hàm `read_*` với kiểm tra `remaining()`
- [ ] Đã viết test cases cho trường hợp thiếu dữ liệu
- [ ] Đã test với dữ liệu rác (fuzzing)
- [ ] Đã add error logging
- [ ] Đã document protocol format

### Kiến thức đã học

1. ✅ Cơ chế panic trong Rust
2. ✅ Trait `Buf` và `BufMut` từ crate `bytes`
3. ✅ Error handling với `anyhow`
4. ✅ Defensive programming trong network protocol
5. ✅ UTF-8 encoding/decoding
6. ✅ Unit testing cho edge cases

### Next Steps

1. Apply fix vào file `message.rs`
2. Chạy tests để verify
3. Review toàn bộ code xử lý network để tìm lỗi tương tự
4. Implement rate limiting để chống DoS
5. Add monitoring/alerting cho malformed packets

---

**🎯 Mục tiêu:** Zero panic trong production!
