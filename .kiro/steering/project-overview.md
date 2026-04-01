---
inclusion: auto
---

# GameNro Project Overview

## Kiến Trúc
- **Actor Model**: PlayerActor và ZoneActor giao tiếp qua message passing
- **Async Runtime**: Tokio với tokio::mpsc channels
- **Database**: MySQL với SeaORM
- **Language**: Rust (stable toolchain)

## Cấu Trúc Module Chính

### Network Layer (`src/network/`)
- `session.rs`: Quản lý TCP connection
- `controller.rs`: Route packets đến handlers
- `handlers/`: Xử lý từng loại packet cụ thể

### Player System (`src/player/`)
- `player_actor/`: Core actor xử lý logic người chơi
- `components/`: Skill, effect, radar, task
- `player.rs`: Player state và data

### Map System (`src/map/`)
- `managers/zone_actor.rs`: Zone actor quản lý khu vực
- `models/`: Map, mob, item drop models
- `services/`: Business logic cho map

### Boss System (`src/boss/`)
- `boss_actor.rs`: Boss AI actor
- `scripts/`: Kịch bản cho từng boss cụ thể
- `manager.rs`: Quản lý spawn và lifecycle

### Item System (`src/item/`)
- `inventory.rs`: Quản lý hành trang
- `item_service.rs`: Logic vật phẩm
- `use_item_service.rs`: Xử lý sử dụng item

### Combat & Matches (`src/matches/`)
- `pvp.rs`: PvP logic
- `dhvt/`: Đại Hội Võ Thuật
- `pvp_manager.rs`: Matchmaking

## Quy Tắc Quan Trọng

### Actor Communication
- **KHÔNG BAO GIỜ** gọi `.await` chéo giữa các Actor
- Sử dụng message passing qua channels
- Actor chủ động push info, không pull

### Database Access
- Chỉ gọi DAO từ Actor hoặc Service layer
- Không gọi database trực tiếp từ handlers

### Error Handling
- Sử dụng `anyhow::Result` cho business logic
- Log errors với `tracing` crate

## File Quan Trọng
- `.agent/rules/actormodel.md`: Chi tiết về Actor Model
- `.agent/rules/deadlock.md`: Cách tránh deadlock
- `.agent/rules/coding_standards.md`: Coding conventions
- `.agent/workflows/add_packet.md`: Workflow thêm packet mới
