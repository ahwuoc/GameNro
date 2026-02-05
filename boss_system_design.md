# Thiết Kế Hệ Thống Boss - GameNro (Rust Version) - FINAL

Tài liệu này là bản thiết kế cuối cùng sau khi đã rà soát toàn bộ source Java.

## 1. Cấu Trúc Database (Bảng `boss_template`)

| Column | Type | Ý nghĩa |
| :--- | :--- | :--- |
| **id** | VARCHAR(100) (PK) | ID định danh (Ví dụ: `boss_black_goku`, `frieza_dai_ca`) |
| **name** | VARCHAR | Tên hiển thị trên đầu Boss |
| **type** | VARCHAR(50) | `solo`, `group`, `sequence`, `scripted` |
| **gender** | TINYINT | Hành tinh (0: TD, 1: NM, 2: XD) |
| **map_join** | JSON | Mảng map xuất hiện `[1, 2, 3]` |
| **seconds_rest** | INT | Thời gian hồi sinh (giây) |
| **stages** | **JSON** | Mảng chứa dữ liệu các giai đoạn (Chi tiết bên dưới) |

### Cấu trúc JSON `stages` (Chi tiết):
```json
[
  {
    "hp": 1000000,
    "mp": 31072002,
    "dame": 5000,
    "def": 1000, // Chỉ số phòng thủ/né đòn
    "outfit": [282, 283, 284, -1, 0, 0], // [head, body, leg, flag, aura, eff]
    "skills": [[0, 7, 1000], [1, 7, 2000]],
    "chat": {
      "s": ["|-1|Chào cưng", "|-2|Á! Fide kìa!"], // -1: Boss, -2: Player
      "m": ["|-1|Yếu quá", "|-1|Cố lên"],
      "e": ["|-1|Ta sẽ trở lại"]
    },
    "together": ["boss_con_1", "boss_con_2"]
  }
]
```

---

## 2. Logic Xử Lý Trong Rust

### Cơ chế Chat Hội Thoại
Khi parse chuỗi chat nếu gặp prefix:
- `|-1|`: Server gửi Message chat cho Boss.
- `|-2|`: Server tìm 1 Player ngẫu nhiên trong map và gửi Message chat thay cho họ.
- `|n|`: Tìm Boss phụ có index `n` trong nhóm để chat.

### 2. Phân Loại Xử Lý (Logic Type)

### `solo`: Boss Đơn Lẻ
- Boss xuất hiện một mình tại một khu vực ngẫu nhiên trong mảng `map_join`.
- Chỉ sử dụng dữ liệu tại `stages[0]`.

### `group`: Boss Đồng Bọn
- Khi Boss chính xuất hiện, hệ thống tự động gọi các Boss phụ trong mảng `together` của stage đó xuất hiện cùng khu vực.
- Phù hợp cho: Tiểu đội sát thủ, Số 19-20, Anh em sên bọ hung.

### `sequence`: Boss Nối Tiếp (Multi-stage)
- Khi một Stage bị tiêu diệt, Boss không mất đi mà thực hiện hiệu ứng biến hình và chuyển sang Stage tiếp theo trong mảng `stages`.
- Phù hợp cho: Frieza (4 dạng), Cell (3 dạng), Buu.

### `scripted`: Boss Code Tay
- Sử dụng các chỉ số từ DB nhưng AI chiến đấu được viết riêng (Raw Code).
- Phù hợp cho các Boss có cơ chế đặc thù không thể mô tả bằng Template (Broly, Boss sự kiện có mini-game).
---

## 3. Quản Lý Boss Scripted (Type 3)
Các Boss như **Broly** hoặc **Cell** sẽ có thêm một trường `script_id` trong DB (nếu cần). Trong code Rust, chúng ta sẽ map:
```rust
match template.id.as_str() {
    "boss_broly" => BrolyAI::update(boss),
    "boss_cell" => CellAI::update(boss),
    _ => DefaultAI::update(boss),
}
```

---
**Đây là cấu trúc mạnh nhất và bao quát nhất, bạn có đồng ý chốt cấu trúc này để tôi bắt đầu tạo bảng không?**
