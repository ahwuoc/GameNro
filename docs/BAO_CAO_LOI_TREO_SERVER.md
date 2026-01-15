# Báo Cáo Sự Cố: Treo Server Khi Chuyển Item & Lỗi Inventory

## 1. Mô tả sự cố (Issue Description)
Khi người chơi thực hiện hành động chuyển item từ Body (trang bị) sang Bag (hành trang), server bị treo (hang) tại dòng log `test call` và không phản hồi tiếp. Client cũng không nhận được cập nhật.

## 2. Nguyên nhân (Root Cause)

### A. Lỗi Deadlock (Kẹt khóa - Nguyên nhân chính gây treo)

Có thể hình dung sự cố này bằng ví dụ đơn giản sau:

**Ví dụ "Căn Phòng Session" (The Room Analogy):**
1.  **Bước 1:** `AsyncController` (Người quản lý) nhận được tin nhắn từ Client. Để xử lý, nó phải **Bước vào Căn Phòng Session** và khóa cửa lại (Giữ `Write Lock`).
2.  **Bước 2:** Trong lúc đang ở trong phòng, Controller gọi hàm `UseItem::get_item` -> gọi tiếp `pl.send_message()`.
3.  **Bước 3 - Vấn đề:** Hàm `pl.send_message()` được thiết kế độc lập. Khi chạy, nó lại yêu cầu: **"Hãy đưa tôi chìa khóa để tôi mở cửa vào phòng Session gửi tin nhắn!"** (`session.write().await`).
4.  **Kết quả (Deadlock):**
    *   `pl.send_message()` đứng chờ ở cửa, đợi ai đó đưa chìa khóa.
    *   Nhưng người giữ chìa khóa là `Controller` thì đang đứng "chờ" `pl.send_message()` làm xong việc mới đi tiếp.
    *   => **Hai bên chờ nhau mãi mãi. Server bị treo.**

## 3. So Sanh Code (Code Comparison)

### ❌ Code Cũ (Gây Deadlock & Lỗi Inventory)
**File:** `src/item/use_item.rs`

```rust
// Code này chạy bên trong Controller (đang giữ session lock)
pub async fn get_item(session: &mut AsyncSession, ...) {
    // [MƯỢN] Lấy mutable reference của Player
    let Some(pl) = session.get_player_mut() else { return Ok(()); };

    match type_item_inventory {
        TypeItemInventory::BodyToBag => {
            // ... Logic lấy item ...

            // LỖI 1: Insert làm tăng size túi
            pl.inventory.items_bag.insert(idx, it.clone());

            // LỖI 2: Gọi send_message() trong khi đang giữ 'pl' (vốn đang mượn từ session)
            // Việc này gây xung đột quyền truy cập
            pl.send_message(msg).await?; 
            
            InventoryService::send_item_bag_to_client(pl).await?;
        }
    }
}
```

### ✅ Code Mới (Đã Khắc Phục) - Giải Thích Cơ Chế Mượn/Nhả (Borrow/Drop)
**File:** `src/item/use_item.rs`

```rust
pub async fn get_item(session: &mut AsyncSession, ...) {
    // [BẮT ĐẦU MƯỢN] Tạo scope block {} để giới hạn phạm vi mượn Player
    let messages = {
        // [MƯỢN] session.get_player_mut() trả về reference 'pl'.
        // Theo luật Rust: Khi đang mượn mutable (pl), ta KHÔNG được dùng 'session' làm việc khác.
        let Some(pl) = session.get_player_mut() else { return Ok(()); };

        match type_item_inventory {
            TypeItemInventory::BodyToBag => {
                // ... Xử lý logic thay đổi dữ liệu Player (dùng pl) ...
                if idx < pl.inventory.items_bag.len() {
                    pl.inventory.items_bag[idx] = it;
                }

                // Thay vì gửi tin (cần session), ta chỉ TẠO tin nhắn
                let mut msg = Message::new(cmd::SEND_ALTER_MESSAGE);
                msg.write_utf("Đã chuyển item")?;
                let bag_msg = InventoryService::create_item_bag_message(pl)?;
                
                vec![msg, bag_msg] // Trả dữ liệu ra ngoài
            }
            _ => Vec::new(),
        }
    }; // [KẾT THÚC MƯỢN] Ra khỏi dấu ngoặc này:
       // -> Biến 'pl' bị hủy (Drop).
       // -> Quyền mượn kết thúc. 'session' được "Nhả" (Release) tự do.

    // [THỰC HIỆN GỬI] Bây giờ ta có thể dùng 'session' an toàn để gửi tin
    for msg in messages {
        session.send_message(&msg).await?;
    }
    Ok(())
}
```

## 4. Giải pháp chi tiết khác (Logic Inventory)
- **Vấn đề:** Code cũ sử dụng `insert` chèn phần tử và đẩy mảng.
- **Giải pháp:** Chuyển sang gán chỉ số (`items_bag[idx] = it`) để thay thế ô trống (null item) bằng item thật, giữ nguyên cấu trúc hành trang.

## 5. Bài học kinh nghiệm (Best Practices)
1. **Tránh Nested Locks:** Cẩn thận khi gọi các hàm `async` có side-effect lên cùng một tài nguyên đang được lock (như Session, Player) từ bên trong một lock đã tồn tại.
2. **Reuse Session:** Nếu đã có tham chiếu `session` (mutable reference) trong context hiện tại, hãy dùng nó trực tiếp để gửi tin.
3. **Tách Logic và IO:** Tách phần xử lý logic (tính toán, create message) ra khỏi phần IO (gửi tin, ghi database) để dễ quản lý luồng dữ liệu.
4. **Scope Management:** Sử dụng Block Scope `{...}` trong Rust để kiểm soát vòng đời biến mượn (Borrow Lifetime), giúp tránh lỗi Borrow Checker và Deadlock.
