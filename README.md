# GameNro - Rust Game Server (Actor-Based Architecture) 🚧 WIP

GameNro là một **game server mạng viết bằng Rust**, tập trung vào **hiệu năng cao**, **an toàn bộ nhớ** và **khả năng mở rộng**.  
Dự án hiện **đang trong quá trình phát triển (Work In Progress)** và được dùng chủ yếu cho mục đích **học tập, nghiên cứu kiến trúc và tối ưu hệ thống**.

> ⚠️ **Trạng thái**: Đang phát triển, API & kiến trúc có thể thay đổi bất kỳ lúc nào.  
> 🎯 **Mục đích**: Thực hành Rust nâng cao, Actor Model, async/concurrency.

---

## 🏗️ Kiến Trúc Hệ Thống (Architecture)

Dự án được **migrate từ server Java multi-thread cũ** sang mô hình **Actor Model** sử dụng `tokio::mpsc` để cải thiện khả năng mở rộng và độ an toàn:

- **Actor Layer (State Management)**  
  Mỗi `PlayerActor` và `ZoneActor` là một actor độc lập, xử lý state thông qua message → giảm ~90% nguy cơ **Race Condition** & **Deadlock** so với shared-state truyền thống.

- **Dispatcher Pattern**  
  Actor đóng vai trò điều phối, nhận message và chuyển xử lý sang các service chuyên biệt → code clean hơn (~70% logic tách rời state).

- **Dirty Flag Pattern**  
  Các chỉ số nhân vật (`stats_need_update`) chỉ được recalculated khi cần (thay đồ, buff, debuff…) → giảm tải CPU đáng kể khi đông người chơi.

- **Asynchronous Services**  
  Toàn bộ logic nghiệp vụ chạy async, tránh block main loop, phù hợp với server realtime.

---

## 📁 Cấu Trúc Thư Mục `src/`

### `player/` – Core Player Actor
- `player.rs`: Định nghĩa dữ liệu người chơi.
- `player_actor/`: Actor, Handle, Message và logic xử lý riêng cho Player.

### `map/` – Quản Lý Thế Giới
- `models/zone.rs`: **ZoneActor** – quản lý mob, item, player trong map.
- `managers/`: Điều phối map/zone.
- `services/`: Logic di chuyển, mob, change map…

### `network/` – Network Layer
- `controller.rs`: Dispatcher mỏng (thin controller), chỉ nhận packet và forward message.
- `session.rs`: TCP async session.
- `session_manager.rs`: Quản lý trạng thái online/offline.

### `templates/` – Static Data Cache
- `*_template_manager.rs`: Load & cache dữ liệu từ DB (Item, Mob, Skill, NPC…).

### `services/` – Business Logic
- `skill_service.rs`, `item_service.rs`, `mob_service.rs`: Logic thuần (stateless), dễ test & reuse.

---

## 🧰 Công Nghệ Sử Dụng

- **Rust** – Memory-safe & high performance.
- **Tokio** – Async runtime.
- **DashMap** – Concurrent hashmap.
- **SeaORM / SQLx** – Database layer (MySQL).
- **Tracing** – Structured logging & debug.

---

## 🚀 Cách Chạy (Development)

> ⚠️ Hiện tại **chỉ khuyến nghị chạy cho dev/testing**, chưa sẵn sàng production.

```bash
# Dev mode
RUST_LOG=info cargo run

# Build release (test only)
cargo build --release
./target/release/game_server

