# Agent Instructions Directory

Thư mục này chứa tài liệu hướng dẫn cho AI agents khi làm việc với GameNro project.

## Cấu Trúc

### `/rules/` - Quy Tắc Kiến Trúc
- `actormodel.md` - Chi tiết về Actor Model pattern
- `deadlock.md` - Cách tránh deadlock trong actor system
- `coding_standards.md` - Coding conventions và best practices
- `boss_scripting.md` - Hướng dẫn viết boss AI scripts
- `workflow.md` - Development workflow chung

### `/memory/` - Ghi Chú Về Project
- `project_structure.md` - Cấu trúc thư mục và module
- `packet_protocol.md` - Giao thức packet
- `item_system.md` - Hệ thống vật phẩm
- `item_drop_system.md` - Cơ chế rơi đồ
- `combine_system.md` - Hệ thống nâng cấp trang bị

### `/workflows/` - Quy Trình Làm Việc
- `add_packet.md` - Workflow thêm packet handler mới

### `/struct_folder/` - Cấu Trúc Chi Tiết
- `folder.md` - Directory tree đầy đủ

## Sử Dụng

### Cho AI Agents
Các file này được thiết kế để AI agents (Kiro, Cursor, Copilot, v.v.) đọc và hiểu context của project.

### Cho Developers
Developers cũng nên đọc các file này để hiểu rõ kiến trúc và quy tắc của project.

## Tương Thích

Ngoài thư mục `.agent/`, project còn có:
- `.cursorrules` - Cho Cursor AI
- `.github/copilot-instructions.md` - Cho GitHub Copilot
- `.ai/context.md` - Cho các AI agents khác
- `.kiro/steering/` - Cho Kiro AI (auto-loaded)

Tất cả đều chứa thông tin tương tự nhưng format khác nhau để tương thích với từng AI.
