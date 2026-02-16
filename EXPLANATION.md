# Giải Thích Cơ Chế Task: Tại sao đánh sai quái vẫn tính điểm? (Phần 3)

Đây là tài liệu giải thích lỗi "Tính điểm sai quái" bạn vừa gặp phải.

## 1. Vấn đề: Cơ chế "Của ai người nấy đánh"
Trong Database, dữ liệu quái vật nhiệm vụ 4 của 3 hành tinh được gộp chung một dòng:
*   Sub-task 14: `mob_id = '4,5,6'`

**Thứ tự mặc định:**
*   Chỉ số 0: Khủng long (Trái Đất)
*   Chỉ số 1: Lợn lòi (Namếc)
*   Chỉ số 2: Quỷ đất (Xayda)

### ❌ Lỗi do dùng logic "Chứa trong danh sách" (Contains)
Trước khi sửa, code dùng lệnh `sub_targets.contains(&target_id)`. 
Điều này nghĩa là: **Nếu bạn là người Trái Đất, bạn đánh con Quỷ đất (Xayda) Server vẫn tính điểm cho bạn.**

Nghiêm trọng hơn, khi bạn sang bước 2 (Sub-task 15), danh sách quái là `'5,6,4'`. Vì con số `6` vẫn nằm trong danh sách này, nên nếu bạn lười không đi tìm quái mới mà quay lại đánh con quái cũ (ID 6), Server vẫn thấy nó "hợp lệ" và cộng điểm.

---

## 2. Giải pháp: Ép buộc ID theo đúng hành tinh (Resolve ID)
Tôi đã thay đổi logic Check Task từ `contains` sang `resolve_id`.

**Cách hoạt động mới:**
1. Server lấy `mob_id` từ Database (ví dụ: `'4,5,6'`).
2. Server check `player.gender` (Ví dụ: Xayda = 2).
3. Server ép buộc bạn **chỉ được phép** đánh con quái ở vị trí số 2 (ID = 6).
4. Nếu bạn đánh con ID 4 hay ID 5, Server sẽ so sánh `target_id == "6"` -> Kết quả là **FALSE** và không cộng điểm.

---

## 3. Tại sao tên quái trong UI thỉnh thoảng hiện sai?
Nếu bạn thấy UI hiện "Đánh 3 con Khủng long mẹ" trong khi bạn là Xayda, hãy kiểm tra hàm `TaskUtils`. 

Placeholder `{mob_mother}`, `{mob_mother_1}`, `{mob_mother_2}` hiện đang lấy tên dựa trên `target_id` (ID quái của bước hiện tại). Để tên hiển thị chuẩn nhất cho cả 3 bước cùng lúc, bạn nên sử dụng các placeholder riêng biệt cho từng hành tinh hoặc điền ID cụ thể vào từng hành tinh trong SQL.

**Tóm lại:** Lỗi cộng điểm sai đã được sửa triệt để bằng cách ép buộc ID theo Gender.
