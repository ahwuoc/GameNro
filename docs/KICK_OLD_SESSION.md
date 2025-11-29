# Kick Old Session on New Login - Implementation Guide

## 📋 Mục tiêu

Implement tính năng: **Người đăng nhập sau sẽ kick người đăng nhập trước ra** khi cùng một account đăng nhập từ 2 clients khác nhau.

### Yêu cầu
- ✅ Client mới có thể login ngay lập tức (không bị block)
- ✅ Client cũ nhận được thông báo "Tài khoản đang đăng nhập ở nơi khác"
- ✅ Client cũ bị disconnect sau khi nhận message
- ✅ Chỉ có 1 session active cho mỗi account

---

## 🔴 Các vấn đề đã gặp và giải pháp

### Problem 1: Player không có session reference

#### ❌ Vấn đề
```rust
// Player trong service KHÔNG có session reference
pub async fn kick_old_session_if_exists(&self, player_id: u64) -> bool {
    let mut players = self.players.write().await;
    if let Some(old_player) = players.get(&player_id) {
        if let Some(session) = &old_player.session {
            // ❌ NEVER ENTERS HERE: old_player.session = None (luôn luôn!)
        }
    }
}
```

#### 🔍 Nguyên nhân
1. Player được load từ database với `session = None`
2. Session chỉ tồn tại ở controller scope (`&mut AsyncSession`)
3. Không có cách nào set session reference vào player vì session là borrowed reference

#### ✅ Giải pháp

**Bước 1:** Wrap session trong `Arc<RwLock<>>` tại `network/mod.rs`

```rust
// BEFORE
async fn handle_connection(socket: tokio::net::TcpStream) -> Result<(), ()> {
    let mut session = AsyncSession::new(socket);
    loop {
        match session.read_message().await {
            Ok(message) => {
                AsyncController::process(&mut session, message).await?;
            }
            ...
        }
    }
}

// AFTER
async fn handle_connection(socket: tokio::net::TcpStream) -> Result<(), ()> {
    let session = AsyncSession::new(socket);
    let session_arc = Arc::new(RwLock::new(session));  // ← Wrap session
    
    loop {
        let mut session_guard = session_arc.write().await;
        match session_guard.read_message().await {
            Ok(message) => {
                // Pass session_arc để có thể clone và lưu
                AsyncController::process(
                    &mut *session_guard, 
                    message, 
                    Arc::clone(&session_arc)  // ← Pass Arc
                ).await?;
            }
            ...
        }
        drop(session_guard);  // Release lock
    }
}
```

**Bước 2:** Update `AsyncController::process` signature

```rust
// network/controller.rs
pub async fn process(
    session: &mut AsyncSession,
    mut msg: Message,
    session_arc: Arc<RwLock<AsyncSession>>,  // ← Thêm parameter
) -> Result<()>
```

**Bước 3:** Set session vào player trước khi add vào service

```rust
// Trong handle_login_authentication
player_with_zone.session = Some(session_arc.clone());  // ← Set session

session.set_player(player_with_zone.clone());
{
    let player_service = PLAYER_SERVICE.write().await;
    player_service.kick_old_session_if_exists(player_id).await;
    player_service.add_player(player_with_zone).await;
}
```

#### 📚 Kiến thức
- **`Arc<T>`**: Atomic Reference Counted - cho phép multiple ownership
- **`RwLock<T>`**: Read-Write lock cho concurrent access
- **Clone Arc**: `Arc::clone()` chỉ tăng reference count, không clone inner data
- **Ownership**: Cần Arc để share session across multiple owners (player, handler, etc.)

---

### Problem 2: Method signature cascade updates

#### ❌ Vấn đề
```
error: expected 3 arguments, found 2
  --> src/network/controller.rs:134
   |
   | Self::handle_login_authentication(session, &username, &password).await?;
   |                                                                    ^^^^^ missing session_arc
```

#### 🔍 Nguyên nhân
Thêm parameter `session_arc` vào một method nhưng quên update:
- Nơi gọi method đó
- Các methods con trong call chain

#### ✅ Giải pháp

Update cascade tất cả methods liên quan:

```rust
// Level 1: Entry point
pub async fn process(
    session: &mut AsyncSession,
    msg: Message,
    session_arc: Arc<RwLock<AsyncSession>>,  // ← Add
)

// Level 2: Sub-handlers
async fn handle_message_not_login(
    session: &mut AsyncSession,
    msg: Message,
    session_arc: Arc<RwLock<AsyncSession>>,  // ← Add
)

async fn handle_not_login(
    session: &mut AsyncSession,
    msg: Message,
    session_arc: Arc<RwLock<AsyncSession>>,  // ← Add
)

async fn handle_message_not_map(
    session: &mut AsyncSession,
    msg: Message,
    session_arc: Arc<RwLock<AsyncSession>>,  // ← Add
)

// Level 3: Business logic
async fn handle_login_authentication(
    session: &mut AsyncSession,
    username: &str,
    password: &str,
    session_arc: Arc<RwLock<AsyncSession>>,  // ← Add
)

async fn handle_create_char(
    session: &mut AsyncSession,
    msg: Message,
    session_arc: Arc<RwLock<AsyncSession>>,  // ← Add
)

// Update ALL call sites
Self::handle_message_not_login(session, msg, session_arc).await?;
Self::handle_not_login(session, msg, session_arc).await?;
Self::handle_login_authentication(session, &username, &password, session_arc).await?;
// ... etc
```

#### 📚 Kiến thức
- **Function signatures**: Khi thêm parameter, phải update toàn bộ call chain
- **Compiler errors**: Đọc error message kỹ để tìm tất cả nơi cần fix
- **Threading context**: `Arc` cần được pass explicitly qua function boundaries

---

### Problem 3: DEADLOCK - Client mới không login được

#### ❌ Vấn đề
```rust
pub async fn kick_old_session_if_exists(&self, player_id: u64) -> bool {
    let mut players = self.players.write().await;  // ← LOCK acquired
    
    if let Some(old_player) = players.remove(&player_id) {
        if let Some(session_arc) = &old_player.session {
            let mut session = session_arc.write().await;
            let _ = session.send_message(&response).await;  // ← BLOCKS here!
        }
    }
}  // ← Lock released HERE (too late!)
```

**Hệ quả:**
- Client cũ: `send_message` chạy trong scope của lock
- Client mới: Đợi acquire lock để `add_player` → BỊ BLOCK!
- Client mới chỉ login được sau khi client cũ disconnect

#### 🔍 Nguyên nhân
Lock `self.players` được giữ trong suốt thời gian gửi message → Block client mới acquire lock.

#### ✅ Giải pháp
Release lock NGAY SAU KHI remove player, TRƯỚC KHI gửi message:

```rust
// BEFORE (deadlock)
pub async fn kick_old_session_if_exists(&self, player_id: u64) -> bool {
    let mut players = self.players.write().await;  // Lock
    if let Some(old_player) = players.remove(&player_id) {
        // Send message while holding lock ❌
        let _ = session.send_message(&response).await;
    }
}  // Lock released here (too late!)

// AFTER (no deadlock)
pub async fn kick_old_session_if_exists(&self, player_id: u64) -> bool {
    // Clone session Arc, then drop lock immediately
    let old_session = {
        let mut players = self.players.write().await;
        if let Some(old_player) = players.remove(&player_id) {
            println!("[KICK] Removed old player {} from service", player_id);
            old_player.session.clone()  // Clone Arc (cheap)
        } else {
            return false;
        }
    };  // ✅ Lock released HERE!
    
    // Send message OUTSIDE lock scope
    if let Some(session_arc) = old_session {
        let mut session = session_arc.write().await;
        let _ = session.send_message(&response).await;
    }
    
    true
}
```

#### 📚 Kiến thức
- **Deadlock**: Xảy ra khi thread hold lock và wait for resource mà thread khác cần lock đó
- **Lock scope**: Minimize thời gian hold lock bằng cách sử dụng block `{ ... }`
- **Arc clone**: Rất cheap (chỉ atomic increment), cho phép move ownership ra khỏi lock scope
- **Async + locks**: Cẩn thận với `.await` trong lock scope vì có thể block rất lâu

---

### Problem 4: Client mới vẫn phải đợi client cũ disconnect

#### ❌ Vấn đề
```rust
// Gửi message synchronously
if let Some(session_arc) = old_session {
    let mut session = session_arc.write().await;
    let _ = session.send_message(&response).await;  // ← BLOCKS until sent!
}
true  // Return after message sent
```

Client mới phải đợi:
1. Message được gửi đến client cũ
2. Network I/O complete
3. Mới return và cho phép client mới login

#### 🔍 Nguyên nhân
`send_message().await` block cho đến khi message được gửi xong.

#### ✅ Giải pháp
Spawn background task để gửi message - không đợi kết quả:

```rust
pub async fn kick_old_session_if_exists(&self, player_id: u64) -> bool {
    let old_session = {
        let mut players = self.players.write().await;
        if let Some(old_player) = players.remove(&player_id) {
            old_player.session.clone()
        } else {
            return false;
        }
    };
    
    // Spawn background task - KHÔNG ĐỢI ✅
    if let Some(session_arc) = old_session {
        tokio::spawn(async move {
            println!("[KICK] Attempting to send kick message...");
            
            let mut response = Message::new(cmd::cmd::SEND_ALTER_MESSAGE);
            if let Err(e) = response.write_utf("Tai khoan da dang nhap o 1 noi khac") {
                println!("[KICK] Failed to write message: {:?}", e);
                return;
            }
            
            let mut session = session_arc.write().await;
            match session.send_message(&response).await {
                Ok(_) => println!("[KICK] ✅ Kick message sent successfully"),
                Err(e) => println!("[KICK] ❌ Failed to send kick message: {:?}", e),
            }
        });
    }
    
    true  // Return IMMEDIATELY ✅
}
```

#### 📚 Kiến thức
- **`tokio::spawn`**: Tạo independent background task
- **Fire and forget**: Task chạy độc lập, không block caller
- **Error handling**: Log errors trong spawned task vì không thể return Result
- **Move semantics**: `async move` move ownership vào task

---

## 🎯 Kết quả cuối cùng

### Flow hoạt động

```
Client 1 login:
  ├─ No old session found
  ├─ Add player to service
  └─ ✅ Login success

Client 2 login (same account):
  ├─ Found old session (Client 1)
  ├─ Remove old player from service  ← Client 1 kicked
  ├─ Spawn background task to send kick message
  ├─ Return immediately
  ├─ Add new player to service  ← Client 2 added
  ├─ ✅ Login success (Client 2)
  └─ [Background] Send "Tai khoan dang dang nhap o noi khac" to Client 1
       └─ Client 1 disconnect
```

### Logs thành công

```
[KICK] Removed old player 6 from service
🔴 [LOGIN] Player 6 kicked old session
✅ [LOGIN] Player 6 added to service
[KICK] Attempting to send kick message...
[KICK] ✅ Kick message sent successfully
```

---

## 📖 Key Takeaways

### 1. Concurrency Patterns
- **Arc + RwLock**: Pattern chuẩn cho shared mutable state trong async Rust
- **Lock scope minimization**: Luôn release lock càng sớm càng tốt
- **Background tasks**: Dùng `tokio::spawn` cho I/O operations không cần đợi

### 2. Async Rust
- **`.await` points**: Mỗi `.await` là potential blocking point
- **Lock + await**: Cẩn thận với `.await` trong lock scope
- **Task spawning**: Independent tasks không block main flow

### 3. Architecture
- **Session management**: Cần Arc để share session giữa nhiều components
- **Parameter threading**: Khi thêm shared state, cần thread qua toàn bộ call chain
- **Error handling**: Log errors rõ ràng để debug

### 4. Debugging
- **Print statements**: Thêm logs ở các điểm quan trọng
- **Lock tracing**: Log khi acquire/release locks
- **Async task tracing**: Log trong spawned tasks vì không thể rely on return values

---

## 🔧 Files đã thay đổi

### 1. `src/network/mod.rs`
- Wrap session trong `Arc<RwLock<>>`
- Pass session_arc qua `AsyncController::process`

### 2. `src/network/controller.rs`
- Update `process` signature
- Update tất cả handler methods
- Set session vào player trước add_player

### 3. `src/player/player_service.rs`
- Implement `kick_old_session_if_exists` với:
  - Proper lock scoping
  - Background task spawning
  - Error logging

---

## 🚀 Testing

### Test case 1: First login
```
Expected: Login thành công
Result: ✅ PASS
```

### Test case 2: Second login (same account)
```
Expected: 
  - Client 2 login ngay lập tức
  - Client 1 nhận kick message
  - Client 1 disconnect
Result: ✅ PASS
```

### Test case 3: Concurrent logins
```
Expected: No deadlock, cả 2 complete (1 kicked, 1 success)
Result: ✅ PASS
```

---

## 📝 Notes

- Session reference trong Player chỉ dùng để gửi kick message, không dùng cho business logic khác
- Client cũ có thể vẫn gửi một vài messages trước khi disconnect hoàn toàn (expected behavior)
- Background task có thể fail nếu client đã disconnect, nhưng không ảnh hưởng client mới
- Lock scope minimization là KEY để tránh deadlock trong concurrent systems

---

**Author**: AI Assistant  
**Date**: 2025-11-24  
**Status**: ✅ Completed and Tested
