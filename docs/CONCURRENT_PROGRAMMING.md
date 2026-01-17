# Concurrent Programming trong Rust Game Server

## Mục lục
1. [Giới thiệu](#giới-thiệu)
2. [Các kiểu dữ liệu đồng thời](#các-kiểu-dữ-liệu-đồng-thời)
3. [Deadlock là gì?](#deadlock-là-gì)
4. [Pattern tránh Deadlock](#pattern-tránh-deadlock)
5. [Best Practices](#best-practices)

---

## Giới thiệu

Trong game server, nhiều player kết nối đồng thời, mỗi player chạy trên một task async riêng. Điều này đòi hỏi việc chia sẻ dữ liệu an toàn giữa các task.

### Vấn đề cốt lõi
```
Task A đọc dữ liệu
                    -> Xung đột nếu cùng lúc
Task B ghi dữ liệu
```

---

## Các kiểu dữ liệu đồng thời

### 1. `Arc<T>` - Atomic Reference Counting

**Mục đích:** Chia sẻ ownership của dữ liệu giữa nhiều task.

```rust
use std::sync::Arc;

let data = Arc::new(vec![1, 2, 3]);
let data_clone = Arc::clone(&data);  // Tăng reference count, KHÔNG clone data thực

// Cả 2 biến đều trỏ đến cùng một Vec
```

**Lưu ý:** `Arc` chỉ cho phép **đọc**. Muốn ghi cần kết hợp với Lock.

---

### 2. `RwLock<T>` - Read-Write Lock

**Mục đích:** Cho phép nhiều reader HOẶC một writer tại một thời điểm.

#### a) `std::sync::RwLock` - Blocking (Đồng bộ)
```rust
use std::sync::RwLock;

let lock = RwLock::new(0);

// Đọc - nhiều thread có thể đọc cùng lúc
let read_guard = lock.read().unwrap();

// Ghi - chỉ 1 thread được ghi, block tất cả reader/writer khác
let write_guard = lock.write().unwrap();
```

#### b) `tokio::sync::RwLock` - Async (Bất đồng bộ)
```rust
use tokio::sync::RwLock;

let lock = RwLock::new(0);

// Trong async context
let read_guard = lock.read().await;   // Không block thread, yield cho runtime
let write_guard = lock.write().await;
```

**Khi nào dùng cái nào?**
| Trường hợp | Chọn |
|------------|------|
| Trong async function, hold lock qua `.await` | `tokio::sync::RwLock` |
| Lock thời gian ngắn, không có `.await` bên trong | `std::sync::RwLock` |

---

### 3. `DashMap<K, V>` - Concurrent HashMap

**Mục đích:** HashMap thread-safe với hiệu năng cao, không cần bọc trong `Arc<RwLock<...>>`.

```rust
use dashmap::DashMap;

let map: DashMap<u64, String> = DashMap::new();

// Ghi - tự động lock nội bộ
map.insert(1, "hello".to_string());

// Đọc
if let Some(value) = map.get(&1) {
    println!("{}", *value);
}

// Xóa
map.remove(&1);

// Iterate
for entry in map.iter() {
    println!("{}: {}", entry.key(), entry.value());
}
```

**Tại sao dùng DashMap thay vì `Arc<RwLock<HashMap>>`?**

| Tiêu chí | `Arc<RwLock<HashMap>>` | `DashMap` |
|----------|------------------------|-----------|
| Locking | Lock toàn bộ map | Sharded locking (chỉ lock phần nhỏ) |
| Đọc/Ghi đồng thời | ❌ Writer block tất cả | ✅ Có thể trên các key khác nhau |
| Deadlock risk | Cao nếu không cẩn thận | Thấp hơn nhiều |
| Code | Cần `.read().await`, `.write().await` | Gọi trực tiếp `.get()`, `.insert()` |

---

## Deadlock là gì?

Deadlock xảy ra khi 2+ task chờ đợi lẫn nhau vô hạn.

### Ví dụ Deadlock kinh điển

```rust
// Task A
let guard_a = lock_a.write().await;  // Giữ lock_a
let guard_b = lock_b.write().await;  // Chờ lock_b ← BLOCK!

// Task B (chạy song song)
let guard_b = lock_b.write().await;  // Giữ lock_b
let guard_a = lock_a.write().await;  // Chờ lock_a ← BLOCK!

// Kết quả: Cả 2 đều chờ mãi mãi
```

### Deadlock trong Game Server (Ví dụ thực tế)

**Code cũ có vấn đề:**
```rust
pub async fn send_message_to_all_players(&self, msg: Message) {
    let players = self.players.read().await;  // Lock players
    for player in players.values() {
        // Mỗi player.send_message() cần lock session
        // Nếu session đang bị lock ở nơi khác → DEADLOCK
        player.send_message(msg.clone()).await;  
    }
}
```

**Vấn đề:**
1. Giữ lock `players` trong suốt vòng lặp
2. Mỗi lần gọi `send_message` lại cần acquire lock khác (`session`)
3. Nếu session đó đang xử lý incoming message (cũng cần lock) → Deadlock

---

## Pattern tránh Deadlock

### Pattern 1: Clone và Drop Lock Sớm

```rust
pub async fn load_me_to_another(&self, player_id: u64) {
    // 1. Acquire lock
    let players_guard = self.players.read().await;
    
    // 2. Clone dữ liệu cần thiết
    let receivers: Vec<u64> = players_guard.keys().cloned().collect();
    let target = players_guard.get(&player_id).cloned();
    
    // 3. DROP LOCK NGAY LẬP TỨC
    drop(players_guard);
    
    // 4. Làm việc với dữ liệu đã clone (không cần lock nữa)
    if let Some(player) = target {
        for receiver_id in receivers {
            // Gọi async function an toàn vì không còn giữ lock
            self.send_to_player(receiver_id, &player).await;
        }
    }
}
```

### Pattern 2: Dùng DashMap (Không cần lock thủ công)

```rust
// Trước (Arc<RwLock<HashMap>>)
let players = self.players.read().await;
for (id, player) in players.iter() { ... }
drop(players);

// Sau (DashMap)
for entry in self.players.iter() {
    let id = entry.key();
    let player = entry.value();
    // DashMap tự quản lý lock cho từng entry
}
```

### Pattern 3: Fire-and-Forget với `tokio::spawn`

```rust
pub async fn send_message(&self, msg: Message) {
    if let Some(session) = &self.session {
        let session_clone = session.clone();
        
        // Spawn task riêng, return ngay lập tức
        tokio::spawn(async move {
            let mut guard = session_clone.write().await;
            let _ = guard.send_message(&msg).await;
        });
    }
    Ok(())
}
```

**Ưu điểm:**
- Caller không bị block
- Tránh deadlock vì lock được acquire trong context tách biệt

**Nhược điểm:**
- Không đảm bảo thứ tự message
- Lỗi bị ignore (cần logging nếu quan trọng)

### Pattern 4: Lock Ordering

Nếu bắt buộc phải giữ nhiều lock, **LUÔN acquire theo thứ tự cố định**:

```rust
// Quy ước: Luôn lock A trước B
async fn process() {
    let _guard_a = lock_a.write().await;  // Luôn lock A trước
    let _guard_b = lock_b.write().await;  // Rồi mới lock B
    
    // Xử lý...
}
```

---

## Best Practices

### ✅ Nên làm

1. **Giữ lock thời gian ngắn nhất có thể**
   ```rust
   let data = {
       let guard = lock.read().await;
       guard.clone()  // Clone và drop lock ngay
   };
   do_expensive_operation(data).await;
   ```

2. **Dùng DashMap cho HashMap cần concurrent access**

3. **Clone Arc thay vì reference khi spawn task**
   ```rust
   let arc_clone = Arc::clone(&my_arc);
   tokio::spawn(async move {
       // Dùng arc_clone bên trong
   });
   ```

4. **Dùng `try_write()` / `try_read()` nếu có thể bỏ qua**
   ```rust
   if let Ok(guard) = lock.try_write() {
       // Làm việc
   } else {
       // Bỏ qua hoặc retry sau
   }
   ```

### ❌ Không nên làm

1. **Giữ lock qua `.await`** (trừ khi dùng `tokio::sync`)
   ```rust
   // ❌ NGUY HIỂM với std::sync::RwLock
   let guard = std_lock.write().unwrap();
   some_async_function().await;  // Guard vẫn tồn tại!
   ```

2. **Nested locks theo thứ tự không nhất quán**

3. **Gọi unknown async function trong khi giữ lock**

---

## Tổng kết

| Vấn đề | Giải pháp |
|--------|-----------|
| Chia sẻ dữ liệu giữa tasks | `Arc<T>` |
| Đọc/Ghi dữ liệu chia sẻ (async) | `tokio::sync::RwLock` |
| HashMap concurrent | `DashMap` |
| Tránh deadlock | Clone sớm, drop lock sớm, `tokio::spawn` |
| Gửi message không block | Fire-and-forget pattern |

---

## Tài liệu tham khảo

- [Tokio Sync Primitives](https://docs.rs/tokio/latest/tokio/sync/index.html)
- [DashMap Documentation](https://docs.rs/dashmap/latest/dashmap/)
- [Rust Atomics and Locks (Book)](https://marabos.nl/atomics/)
