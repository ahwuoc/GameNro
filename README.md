# GameNro - Rust Game Server (Actor-Based Architecture)

GameNro là một dự án game server mạng được xây dựng trên ngôn ngữ **Rust**, tập trung vào hiệu năng cao, an toàn bộ nhớ và khả năng mở rộng. Dự án áp dụng mô hình **Actor Model** để quản lý trạng thái người chơi và thế giới game một cách hiệu quả nhất.

> **Lưu ý**: Đây là dự án học tập nhằm mục đích rèn luyện và thể hiện kỹ năng lập trình Rust nâng cao.

## 🏗️ Kiến Trúc Hệ Thống (Architecture)

Dự án đã chuyển đổi từ kiến trúc Mutex-heavy truyền thống sang mô hình **Actor Model** sử dụng `tokio::mpsc`:

- **Actor Layer (State Management)**: Mỗi Người chơi (`PlayerActor`) và mỗi Khu vực (`ZoneActor`) là một actor riêng biệt, chạy độc lập và giao tiếp qua tin nhắn. Điều này loại bỏ hoàn toàn tình trạng Deadlock và Race Condition.
- **Dispatcher Pattern**: Các Actor đóng vai trò là bộ điều hướng, nhận tin nhắn và ủy quyền xử lý cho các logic cụ thể (Services).
- **Dirty Flag Pattern**: Hệ thống tối ưu hóa việc tính toán chỉ số (`stats_need_update`), chỉ cập nhật lại khi thực sự có thay đổi (thay đồ, buff...), giúp giảm tải CPU.
- **Asynchronous Services**: Các dịch vụ logic hoàn toàn bất đồng bộ, không gây nghẽn luồng xử lý chính.

## Cấu trúc thư mục `src/`

### 📁 `player/` - Trái tim của hệ thống Actor
- **`player.rs`**: Khai báo cấu trúc dữ liệu người chơi và các logic cơ bản.
- **`player_actor/`**: Triển khai Actor Model cho người chơi (Handle, Actor, Message, Logic xử lý riêng).

### 📁 `map/` - Quản lý Thế giới
- **`models/zone.rs`**: Triển khai **ZoneActor**, quản lý quái vật, vật phẩm và người chơi trong một khu vực.
- **`managers/`**: Quản lý đăng ký Map và điều phối các Zone.
- **`services/`**: Chứa logic về di chuyển, quái vật (`mob_service`) và thay đổi bản đồ.

### 📁 `network/` - Tầng Giao Tiếp
- **`controller.rs`**: Được refactor thành một Dispatcher mỏng, nhận gói tin từ Client và chuyển hóa thành các Message gửi tới Actor tương ứng.
- **`session.rs`**: Quản lý kết nối TCP bất đồng bộ.
- **`session_manager.rs`**: Theo dõi trạng thái Online/Offline của tài khoản.

### 📁 `templates/` - Dữ liệu tĩnh (Cache)
- **`*_template_manager.rs`**: Tải và quản lý cache cho dữ liệu từ Database (Item, Map, Mob, Skill, NPC...).

### 📁 `services/` - Logic Nghiệp Vụ (Business Logic)
- **`skill_service.rs`**, **`item_service.rs`**, **`mob_service.rs`**: Chứa các logic tính toán thuần túy, nhận input từ Actor và trả về kết quả.

## 📋 Công Nghệ Sử Dụng

- **Rust**: Ngôn ngữ lập trình hệ thống an toàn và hiệu năng cao.
- **Tokio**: Async Runtime mạnh mẽ nhất cho Rust.
- **DashMap**: Concurrent Hashmap cho các bảng tra cứu nhanh.
- **SeaORM & SQLx**: Giao tiếp với MySQL một cách an toàn và mạnh mẽ.
- **Tracing**: Hệ thống logging chuyên nghiệp theo cấp độ (Info, Warn, Debug).

## 🚀 Cách Chạy Dự Án

1. **Chuẩn bị môi trường**:
   - Cài đặt Rust (phiên bản mới nhất).
   - Setup cơ sở dữ liệu MySQL và import dữ liệu mẫu.
   - Cấu hình file `.env` (DATABASE_URL, SERVER_ADDR...).

2. **Khởi động server**:
```bash
# Debug mode với log chi tiết
RUST_LOG=info cargo run

# Production mode
cargo build --release
./target/release/game_server
```

Server sẽ khởi động, load toàn bộ Template từ DB và lắng nghe tại cổng đã cấu hình (mặc định `:14445`).

## 🎯 Mục Tiêu Dự Án

- **Mastering Rust Actors**: Thực hành thiết kế hệ thống phân tán và concurrency cao.
- **Zero-Lock Strategy**: Giảm thiểu việc khóa dữ liệu (Mutex/RwLock) để tận dụng tối đa đa nhân CPU.
- **Clean Architecture**: Tổ chức mã nguồn theo hướng mô-đun hóa, dễ dàng mở rộng và bảo trì.
- **Performance Optimization**: Áp dụng các kỹ thuật như Dirty Flag, Binary Search, và Lazy Loading để tối ưu tốc độ xử lý.

---
*Dự án phát triển bởi AHWUOCDZ - 2026*
