# 🐉 GameNro - Rust Game Server (Actor-Based Architecture) 🚧 WIP

GameNro là một dự án **game server mạng hiệu năng cao**, được viết hoàn toàn bằng **Rust**. Dự án tập trung vào tính **an toàn bộ nhớ**, **khả năng mở rộng** và kiến trúc **Actor Model** hiện đại.

![Rust](https://img.shields.io/badge/rust-%23E32F26.svg?style=for-the-badge&logo=rust&logoColor=white)
![Tokio](https://img.shields.io/badge/tokio-%23000000.svg?style=for-the-badge&logo=tokio&logoColor=white)
![MySQL](https://img.shields.io/badge/mysql-%234479A1.svg?style=for-the-badge&logo=mysql&logoColor=white)

> [!WARNING]
> **Trạng thái**: Dự án đang trong quá trình phát triển (Work In Progress). API và kiến trúc có thể thay đổi bất kỳ lúc nào. Sử dụng cho mục đích học tập và nghiên cứu.

---

## ✨ Điểm Nổi Bật (Highlights)

- ⚡ **Siêu Tốc Độ**: Tận dụng tối đa sức mạnh của Rust và Tokio Async Runtime.
- 🏗️ **Actor Model**: Quản lý trạng thái thông qua Message Passing, loại bỏ Race Condition.
- 🛡️ **An Toàn**: Ngăn chặn Deadlock và lỗi bộ nhớ ngay từ khâu thiết kế.
- 🎮 **Tính Năng Đa Dạng**: Hỗ trợ PvP, Clan, Phó Bản, Nâng Cấp Trang Bị, Giao Thương...

---

## 🏗️ Kiến Trúc Hệ Thống (Architecture)

Server được xây dựng trên mô hình **Actor Model** giúp tối ưu hóa việc quản lý trạng thái phân tán:

- **Actor Layer**: `PlayerActor` và `ZoneActor` hoạt động độc lập, xử lý logic riêng biệt thông qua các kênh channel (`tokio::mpsc`).
- **Deadlock Prevention**: Áp dụng cơ chế **Asynchronous Info Exchange (Push Model)**. Các Actor không gọi trực tiếp `.await` chéo nhau mà chủ động gửi thông tin khi có yêu cầu.
- **Stateless Services**: Tách biệt logic nghiệp vụ khỏi trạng thái, giúp mã nguồn sạch (Clean Code) và dễ dàng bảo trì.

---

## 📁 Cấu Trúc Thư Mục `src/` (Updated)

| Thư mục | Mô tả |
| :--- | :--- |
| `account/` | Quản lý tài khoản, đăng nhập và bảo mật. |
| `player/` | **Core Player Actor**: Xử lý logic và trạng thái nhân vật. |
| `map/` | **ZoneActor**: Quản lý thế giới, Quái vật (Mobs) và Vật phẩm trên đất. |
| `boss/` | Hệ thống Boss thế giới, trí tuệ nhân tạo (AI) và kịch bản xuất hiện. |
| `clan/` | Hệ thống Bang hội, trò chuyện và trạng thái chia sẻ. |
| `matches/` | Hệ thống PvP, Đại Hội Võ Thuật và các trận đấu đối kháng. |
| `dungoen/` | Quản lý phó bản, instance và các khu vực đặc biệt. |
| `combine/` | Logic nâng cấp, ép trang bị và các loại pha lê. |
| `shop/` | Hệ thống NPC cửa hàng và menu tương tác. |
| `entities/` | Các mô hình dữ liệu (SeaORM) ánh xạ trực tiếp từ Cơ sở dữ liệu. |
| `templates/` | Tầng Cache dữ liệu tĩnh (Vật phẩm, Kỹ năng, Quái vật...). |
| `network/` | Lớp mạng (Network Layer), xử lý Packet và Session TCP Async. |
| `services/` | Các dịch vụ nghiệp vụ thuần túy (Stateless logic). |

---

## 🚀 Cách Chạy (Development)

1. **Chuẩn bị**: Cài đặt Rust, MySQL và import `database/nro.sql`.
2. **Cấu hình**: Chỉnh sửa file `config_arc.toml`.
3. **Chạy Server**:
   ```bash
   # Chế độ Phát triển (Có log info)
   RUST_LOG=info cargo run

   # Chế độ Production (Release)
   cargo build --release
   ./target/release/arc_nro
   ```

---

## 🛠️ Công Cụ Phát Triển

```bash
# Kiểm tra lỗi biên dịch
cargo check

# Định dạng mã nguồn chuẩn Rust
cargo fmt

# Tạo và xem tài liệu kỹ thuật
cargo doc --open
```

---
[Hướng Dẫn Sử Dụng Chi Tiết (HDSD)](docs/HDSD.md)


