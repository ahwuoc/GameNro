# Tài Liệu Giải Thích: Hệ Thống Map, Zone và Mob

Tài liệu này giải thích kiến trúc hệ thống bản đồ (Map), khu vực (Zone) và quái vật (Mob) trong server GameNro, cũng như các thay đổi refactoring gần đây.

## 1. Tổng Quan Kiến Trúc
Hệ thống hoạt động dựa trên sự phối hợp giữa hai thành phần quản lý chính:

- **MapManager**: Quản lý danh sách các bản đồ (`Map`). Mỗi bản đồ đại diện cho một ID bản đồ (ví dụ: Map 21 - Tương lai) và chứa thông tin tĩnh (MapInfo) như kích thước, địa hình, waypoint, và danh sách spawn mob/npc.
- **ZoneManager**: Quản lý tập trung tất cả các khu vực (`Zone`) đang hoạt động trong game (Singleton). Đây là nơi chứa trạng thái thực (runtime state) của game.
- **Zone**: Một instance cụ thể của bản đồ (ví dụ: Map 21, Khu 0; Map 21, Khu 1). Zone chứa danh sách người chơi, mob đang sống/chết, vật phẩm rơi.

## 2. Quy Trình Khởi Tạo Server (Initialization Flow)
Quá trình khởi tạo đóng vai trò quyết định để Mob hiển thị đúng.

### Bước 1: Tạo Map (`MapManager::create_map`)
- Server đọc dữ liệu template (`MapTemplate`) từ cơ sở dữ liệu.
- Server tạo đối tượng `Map`.

### Bước 2: Khởi tạo Zone (`Map::init_zones`)
- `Map` yêu cầu `ZoneManager` tạo các `Zone` tương ứng (số lượng zone tùy thuộc vào configure của map).
- `ZoneManager` tạo Zone và lưu vào bộ nhớ chung (`Arc<RwLock<HashMap<...>>>`).
- **Điểm quan trọng**: `Map` sẽ lấy tham chiếu (reference) đến chính các `Zone` vừa được tạo trong `ZoneManager`.
    - *Trước khi sửa lỗi*: `Map` tự tạo ra các Zone mới độc lập (bản sao).
    - *Sau khi sửa lỗi*: `Map` và `ZoneManager` cùng trỏ đến một đối tượng Zone duy nhất (Shared State).

### Bước 3: Khởi tạo Mob (`Map::init_mobs`)
- Hàm này được gọi ngay sau khi tạo Zone.
- Nó duyệt qua danh sách template Mob của Map.
- Với mỗi template, nó tạo một đối tượng Mob thực thể (`RtMob`) với đầy đủ HP, Level.
- `RtMob` được thêm vào danh sách `active_mobs` của các `Zone`.
- **Kết quả**: Khi Zone vừa được tạo xong, nó đã chứa sẵn danh sách Mob.

## 3. Luồng Người Chơi Vào Map (Login / Chuyển Map)

### Khi người chơi đăng nhập (`PlayerInfoService`):
1.  Gọi `ZONE_MANAGER.load_player_to_best_zone(player)`: Tìm khu vực còn chỗ trống tốt nhất.
2.  Trong `Zone::load_player_to_zone`:
    *   `add_player`: Thêm người chơi vào danh sách quản lý của Zone.
    *   `load_another_to_me`: Gửi thông tin các người chơi khác cho người vừa vào.
    *   `load_me_to_another`: Gửi thông tin người vừa vào cho những người khác.
    *   **`map_info`**: Gửi gói tin tham số map (-24) kèm danh sách Mob (`active_mobs`) về client.
        *   Do Mob đã được init ở Bước 3 (Khởi tạo), nên danh sách này có dữ liệu -> Client hiển thị Mob.

## 4. Refactoring ZoneService

### Vấn đề cũ:
- `ZoneService` là một struct hoạt động như "người trung gian" (wrapper).
- Hầu hết các hàm của nó chỉ đơn giản là gọi lại `ZoneManager`.
- Ví dụ: `ZoneService::load_player_to_best_zone` chỉ gọi `ZONE_MANAGER.load_player_to_best_zone`.

### Thay đổi:
- **Xóa bỏ `ZoneService`**: Loại bỏ file và module này hoàn toàn.
- **Truy cập trực tiếp**: Các service khác (`PlayerInfoService`, `ChangeMapService`) giờ đây gọi trực tiếp `ZONE_MANAGER`.
- **Lợi ích**: Giảm bớt sự phức tạp không cần thiết, code dễ đọc hơn, luồng dữ liệu trực quan hơn.

## 5. Cấu trúc thư mục liên quan
- `src/map/map.rs`: Định nghĩa struct `Map`, logic init zones và mob.
- `src/map/zone.rs`: Định nghĩa struct `Zone`, logic xử lý tương tác trong khu vực.
- `src/map/map_manager.rs`: Quản lý danh sách Map.
- `src/map/zone_manager.rs`: Quản lý danh sách Zone toàn cục.
