# Project Structure & Module Map

## 1. Directory Tree
(Dựa trên file folder.md bạn đã cung cấp)

## 2. Module Responsibilities (Draft)
Dựa trên phân tích sơ bộ, tôi phân loại các module như sau:
- **`src/network`**: Giao thức mạng, xử lý Packet, Session.
- **`src/player/player_actor`**: Logic cốt lõi của người chơi (Actor Model).
- **`src/boss`**: Quản lý Boss và các kịch bản (scripts) cho từng Boss cụ thể.
- **`src/map`**: Quản lý Zone, Map, Waypoint và các Actor liên quan đến khu vực.
- **`src/item` & `src/shop`**: Hệ thống vật phẩm, hành trang và cửa hàng.
- **`src/npc`**: Xử lý logic hội thoại và menu của NPC.
- **`src/matches`**: Các hoạt động PK, Đại Hội Võ Thuật (DHVT), thách đấu.
- **`src/database` & `src/entities`**: Tương tác với Database (SQLx/SeaORM).

## 3. Usage guidelines
Khi cần tìm một tính năng:
- Tìm logic kỹ năng -> `src/player/components/player_skill.rs`.
- Tìm logic vật phẩm -> `src/item/`.
- Tìm kịch bản Boss -> `src/boss/scripts/`.
- Tìm lệnh Admin -> `src/npc/handlers/admin.rs` hoặc `src/services/command.rs`.
