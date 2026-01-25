# GameNro - Rust Game Server

GameNro là một dự án game server mạng được viết bằng Rust với mục đích học tập và trau dồi kỹ năng lập trình Rust. Dự án sử dụng SeaORM để tương tác với cơ sở dữ liệu MySQL và Tokio cho xử lý bất đồng bộ.

> **Lưu ý**: Đây là dự án học tập nhằm mục đích rèn luyện và thể hiện kỹ năng lập trình Rust.

## Cấu trúc thư mục `src/`

### 📁 `account/` - Quản lý tài khoản
- **`account_dao.rs`** - Data Access Object cho tài khoản
- **`account_services.rs`** - Dịch vụ xử lý tài khoản

### 📁 `combine/` - Hệ thống pha chế
- **`combine_service.rs`** - Dịch vụ pha chế
- **`combine_type.rs`** - Định nghĩa các loại kết hợp
- **`combine_constants.rs`** - Hằng số
- **`handlers/`** - Xử lý logic từng loại
- **`model.rs`** - Model dữ liệu

### 📁 `config.rs` - Cấu hình hệ thống

### 📁 `constant/` - Định nghĩa hằng số
- **`cmd.rs`** - Mã lệnh xử lý
- **`const_menu.rs`, `const_npc.rs`** - Hằng số menu, NPC
- **`menu_enum.rs`** - Enum menu

### 📁 `data/` - Quản lý dữ liệu game
- **`data_game.rs`** - Dữ liệu game chính
- **`game_session.rs`** - Quản lý phiên
- **`item_data.rs`** - Dữ liệu item
- **`waypoint.rs`** - Định nghĩa điểm đường đi

### 📁 `entities/` - SeaORM Entities
Được tạo tự động bởi SeaORM codegen, chứa tất cả các model tương ứng với bảng trong database.

### 📁 `features/` - Tính năng đặc biệt
- **`option_card.rs`** - Thẻ tùy chọn
- **`task_player.rs`** - Nhiệm vụ người chơi

### 📁 `item/` - Hệ thống vật phẩm
- **`item.rs`** - Struct Item cơ bản
- **`item_service.rs`, `item_dao.rs`, `item_manager.rs`** - Quản lý item
- **`inventory.rs`, `inventory_service.rs`** - Quản lý hành trang
- **`use_item.rs`, `item_time.rs`, `item_option.rs`** - Logic sử dụng item

### 📁 `map/` - Hệ thống bản đồ
- **`dao/`** - Truy cập dữ liệu bản đồ
- **`managers/`** - Quản lý map, zone
- **`models/`** - Model map, zone, waypoint
- **`services/`** - Dịch vụ map, mob, change map
- **`utils/`** - Tiện ích bản đồ

### 📁 `mob/` - Quái vật
- **`mob.rs`** - Model quái vật

### 📁 `models/` - Model chung
- **`intrinsic.rs`** - Nội tại
- **`skill_model.rs`** - Kỹ năng

### 📁 `network/` - Mạng
- **`controller.rs`** - Xử lý message từ client
- **`message.rs`** - Protocol message
- **`session.rs`** - Phiên kết nối
- **`session_manager.rs`** - Quản lý session online

### 📁 `npc/` - Hệ thống NPC
- **`npc_manager.rs`, `npc_service.rs`** - Quản lý và xử lý NPC
- **`npc_struct.rs`** - Model NPC
- **`handlers/`** - Logic từng NPC

### 📁 `player/` - Người chơi
- **`player.rs`** - Struct Player chính
- **`player_mapper.rs`** - Chuyển đổi dữ liệu player
- **`components/`** - Các thành phần con (skill, item time, v.v.)

### 📁 `services/` - Dịch vụ Business Logic
- **`manager.rs`** - Server Manager
- **`services.rs`** - Service Wrapper
- **`auth_service.rs`** - Xác thực
- **`command.rs`** - Xử lý lệnh chat
- **`mob_service.rs`, `skill_service.rs`** - Xử lý mob, skill
- **`player_info_service.rs`**, **`intrinsic_service.rs`**

### 📁 `shop/` - Cửa hàng
- **`shop_services.rs`** - Dịch vụ shop

### 📁 `templates/` - Quản lý Template dữ liệu
- **`*_template_manager.rs`** - Cache dữ liệu tĩnh (Item, Map, Mob, NPC, Skill...)

### 📁 `utils/` - Tiện ích
- **`database.rs`** - Tiện ích database
- **`location.rs`** - Tiện ích vị trí
- **`skill_util.rs`** - Tiện ích kỹ năng

### 📄 `main.rs` - Entry point
File chính khởi động server, khởi tạo các service và bắt đầu lắng nghe kết nối.

## Công nghệ sử dụng

- **Rust** - Ngôn ngữ lập trình chính
- **Tokio** - Runtime bất đồng bộ
- **SeaORM** - ORM cho database
- **MySQL** - Cơ sở dữ liệu
- **Serde** - Serialization/Deserialization
- **Chrono** - Xử lý thời gian
- **Rand** - Tạo số ngẫu nhiên

## Cách chạy

1. Cài đặt dependencies:
```bash
cargo build
```

2. Cấu hình database trong file `.env`

3. Chạy server:
```bash
cargo run
```

Server sẽ lắng nghe trên địa chỉ mặc định `127.0.0.1:14445`

## Cấu trúc kiến trúc

Dự án được thiết kế theo mô hình layered architecture:
- **Entities Layer**: Định nghĩa cấu trúc dữ liệu
- **DAO Layer**: Truy cập dữ liệu
- **Service Layer**: Logic nghiệp vụ
- **Network Layer**: Xử lý giao tiếp mạng
- **Utils Layer**: Tiện ích hỗ trợ

Mỗi module được tổ chức theo nguyên tắc separation of concerns, giúp code dễ bảo trì và mở rộng.

## Mục đích dự án

Dự án này được phát triển với các mục tiêu học tập sau:

- **Học Rust**: Nắm vững các khái niệm cốt lõi của Rust như ownership, borrowing, lifetimes
- **Async Programming**: Thực hành lập trình bất đồng bộ với Tokio
- **Database ORM**: Sử dụng SeaORM để làm việc với database
- **Network Programming**: Xây dựng TCP server và xử lý protocol
- **Architecture Design**: Thiết kế kiến trúc phần mềm theo mô hình layered
- **Error Handling**: Xử lý lỗi hiệu quả với Result và anyhow
- **Code Organization**: Tổ chức code theo module và crate structure

Dự án này phục vụ mục đích học tập và thể hiện năng lực lập trình Rust trong việc xây dựng một hệ thống phức tạp.
