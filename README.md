# GameNro - Rust Game Server

GameNro là một game server được viết bằng Rust, sử dụng SeaORM để tương tác với cơ sở dữ liệu MySQL và Tokio cho xử lý bất đồng bộ.

## Cấu trúc thư mục `src/`

### 📁 `data/` - Quản lý dữ liệu và cấu hình
- **`config_manager.rs`** - Quản lý cấu hình hệ thống
- **`data_game.rs`** - Dữ liệu game chính
- **`game_session.rs`** - Quản lý phiên game
- **`waypoint.rs`** - Định nghĩa các điểm đường đi
- **`mod.rs`** - Module exports

### 📁 `entities/` - Các entity của cơ sở dữ liệu
Được tạo tự động bởi SeaORM codegen, chứa tất cả các model tương ứng với bảng trong database:

- **`account.rs`** - Tài khoản người dùng
- **`player.rs`** - Thông tin người chơi
- **`item_template.rs`** - Template cho các item
- **`mob_template.rs`** - Template cho quái vật
- **`map_template.rs`** - Template cho bản đồ
- **`npc_template.rs`** - Template cho NPC
- **`skill_template.rs`** - Template cho kỹ năng
- **`shop.rs`** - Hệ thống shop
- **`clan_sv1.rs`, `clan_sv2.rs`** - Hệ thống clan
- **`giftcode.rs`** - Hệ thống gift code
- **`news.rs`** - Hệ thống tin tức
- **`task_main_template.rs`** - Template nhiệm vụ chính
- **`side_task_template.rs`** - Template nhiệm vụ phụ
- **`achievement_template.rs`** - Template thành tích
- **`bank_accounts.rs`** - Tài khoản ngân hàng
- **`orders.rs`** - Đơn hàng
- **`withdrawals.rs`** - Rút tiền
- **`history_*.rs`** - Các bảng lịch sử
- **`mod.rs`** - Module exports và prelude

### 📁 `features/` - Các tính năng đặc biệt
- **`option_card.rs`** - Hệ thống thẻ tùy chọn
- **`side_task_template.rs`** - Quản lý nhiệm vụ phụ
- **`task_player.rs`** - Nhiệm vụ của người chơi
- **`mod.rs`** - Module exports

### 📁 `item/` - Hệ thống item và inventory
- **`item.rs`** - Định nghĩa item cơ bản
- **`item_model.rs`** - Model cho item
- **`item_service.rs`** - Service xử lý item
- **`item_dao.rs`** - Data Access Object cho item
- **`item_manager.rs`** - Quản lý item
- **`item_validator.rs`** - Validation cho item
- **`item_utils.rs`** - Tiện ích cho item
- **`item_option.rs`** - Tùy chọn item
- **`item_time.rs`** - Item có thời hạn
- **`item_time_service.rs`** - Service cho item thời hạn
- **`inventory.rs`** - Hệ thống inventory
- **`inventory_model.rs`** - Model inventory
- **`inventory_service.rs`** - Service inventory
- **`mod.rs`** - Module exports

### 📁 `map/` - Hệ thống bản đồ và zone
- **`map.rs`** - Định nghĩa bản đồ
- **`map_service.rs`** - Service xử lý bản đồ
- **`map_dao.rs`** - Data Access Object cho bản đồ
- **`map_manager.rs`** - Quản lý bản đồ
- **`map_utils.rs`** - Tiện ích cho bản đồ
- **`zone.rs`** - Định nghĩa zone
- **`zone_manager.rs`** - Quản lý zone
- **`waypoint.rs`** - Điểm đường đi
- **`tile_loader.rs`** - Loader cho tile
- **`change_map_service.rs`** - Service chuyển bản đồ
- **`item_map.rs`** - Item trên bản đồ
- **`item_map_service.rs`** - Service cho item trên bản đồ
- **`mod.rs`** - Module exports

### 📁 `mob/` - Hệ thống quái vật
- **`mob.rs`** - Định nghĩa quái vật
- **`mob_service.rs`** - Service xử lý quái vật
- **`mob_dao.rs`** - Data Access Object cho quái vật
- **`mod.rs`** - Module exports

### 📁 `models/` - Các model chung
- **`npc.rs`** - Model NPC
- **`npc_factory.rs`** - Factory tạo NPC
- **`skill_model.rs`** - Model kỹ năng
- **`intrinsic.rs`** - Thuộc tính nội tại
- **`mod.rs`** - Module exports

### 📁 `network/` - Hệ thống mạng
- **`async_net/`** - Network bất đồng bộ
  - **`session.rs`** - Quản lý session
  - **`controller.rs`** - Controller xử lý message
  - **`message.rs`** - Định nghĩa message
  - **`mod.rs`** - Module exports
- **`mod.rs`** - Module exports và server startup

### 📁 `player/` - Hệ thống người chơi
- **`player.rs`** - Định nghĩa người chơi
- **`player_service.rs`** - Service xử lý người chơi
- **`player_dao.rs`** - Data Access Object cho người chơi
- **`player_friend.rs`** - Hệ thống bạn bè
- **`player_intrinsic.rs`** - Thuộc tính nội tại của người chơi
- **`player_item_time.rs`** - Item thời hạn của người chơi
- **`player_skill.rs`** - Kỹ năng của người chơi
- **`n_point.rs`** - Điểm N
- **`mod.rs`** - Module exports

### 📁 `services/` - Các service chính
- **`manager.rs`** - Manager chính của hệ thống
- **`god_gk.rs`** - Service quản lý database
- **`player_info_service.rs`** - Service thông tin người chơi
- **`npc_service.rs`** - Service NPC
- **`intrinsic_service.rs`** - Service thuộc tính nội tại
- **`services.rs`** - Service handles
- **`mod.rs`** - Module exports

### 📁 `utils/` - Tiện ích
- **`database.rs`** - Tiện ích database
- **`location.rs`** - Tiện ích vị trí
- **`mod.rs`** - Module exports

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
