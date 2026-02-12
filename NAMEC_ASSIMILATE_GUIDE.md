# Hướng dẫn chi tiết Hiện thực Đồng hóa Namek (HTVV)

Tài liệu này giải thích chi tiết cách xử lý lệnh **Hợp thể vĩnh viễn (HTVV)** từ phía Client cho đến khi hoàn tất việc **Đồng hóa** sức mạnh đệ tử trong hệ thống Rust.

---

## 1. Xử lý lệnh từ Client (Controller)
Tại file `src/network/controller.rs`, khi nhận lệnh thay đổi trạng thái đệ tử (`PET_CHANGE_STATUS`), chúng ta cần bắt trường hợp `HTVV`.

### Mã nguồn:
```rust
// Trong match cmd::PET_CHANGE_STATUS
if let Ok(status) = PetStatus::try_from(status_byte) {
    if let Some(handle) = session.get_player_handle().await {
        if status == PetStatus::Fusion {
            // Hợp thể bình thường (Lưỡng long)
            handle.send_forget(PlayerMessage::Fusion {
                type_fusion: Fusion::LUONG_LONG_NHAT_THE, // 4
                template_id: 1,
            });
        } else if status == PetStatus::HTVV {
            // Gửi lệnh Đồng hóa (HTVV)
            handle.send_forget(PlayerMessage::Fusion {
                type_fusion: Fusion::HTVV, // 5
                template_id: 1,
            });
        } else {
            // Các trạng thái khác (Đi theo, Bảo vệ, Tấn công, Về nhà)
            handle.send_forget(PlayerMessage::Pet(PetMessage::ChangeStatus(status)));
        }
    }
}
```

### Giải thích:
- **`PetStatus::HTVV`**: Client gửi mã trạng thái là `5`.
- **`PlayerMessage::Fusion`**: Thay vì gửi lệnh đổi trạng thái đệ tử thông thường, chúng ta chuyển hướng sang lệnh `Fusion` với loại là `5`. Điều này giúp tập trung toàn bộ logic sát nhập (bình thường và vĩnh viễn) vào một hàm xử lý thống nhất trong `actor.rs`.

---

## 2. Logic xử lý Đồng hóa (Actor)
Tại file `src/player/player_actor/actor.rs`, hàm `handle_fusion` sẽ thực hiện quy trình đồng hóa.

### Mã nguồn xử lý:
```rust
// Phân đoạn xử lý trong handle_fusion
if type_fusion == Fusion::HTVV {
    // A. Kiểm tra hành tinh
    if self.player.gender != 1 {
        let _ = ServiceHandles::send_thong_bao_to_player(&self.player, "Chỉ Namek mới có thể đồng hóa!");
        return;
    }

    // B. Lấy sức mạnh đệ tử
    let pet_power = pet_snapshot.player.n_point.power;
    self.player.n_point.power += pet_power;
    self.player.n_point.tiem_nang += pet_power;

    // C. Hiệu ứng và thông báo
    ServiceHandles::send_fusion_effect(&self.player, Fusion::LUONG_LONG_NHAT_THE);
    let _ = ServiceHandles::send_thong_bao_to_player(&self.player, "Bạn đã đồng hóa đệ tử thành công!");

    // D. Xóa đệ tử vĩnh viễn
    let _ = pet_handle.send_forget(PlayerMessage::Logout);
    self.pet_handle = None;

    // E. Cập nhật Master
    self.player.n_point.cal_point();
    self.player.n_point.set_hp(self.player.n_point.hp_max);
    self.player.n_point.set_mp(self.player.n_point.mp_max);

    player_info_service::send_point_info_sync(&self.player);
    player_info_service::send_info_hp_mp_money(&self.player);
    ServiceHandles::send_cai_trang(&self.player);
    return;
}
```

### Giải thích chi tiết:
1.  **Kiểm tra hành tinh (`gender != 1`)**: Chỉ Namek (mã hành tinh là 1) mới có kỹ năng thiên phú là đồng hóa. Nếu người chơi Trái Đất hoặc Xayda gửi lệnh này, server sẽ từ chối.
2.  **Cộng dồn Sức mạnh (`power`)**: Toàn bộ điểm sức mạnh của đệ tử được cộng trực tiếp vào thuộc tính của sư phụ. Điều này khiến sư phụ mạnh lên vĩnh viễn ngay cả khi không hợp thể.
3.  **Hồi phục trạng thái**: Giống như trong truyện, sau khi đồng hóa, người được đồng hóa sẽ tràn đầy năng lượng (`set_hp`, `set_mp` lên tối đa).
4.  **Xóa đệ tử (`self.pet_handle = None`)**: Đây là bước quan trọng nhất. 
    - `pet_handle.send_forget(PlayerMessage::Logout)` yêu cầu tiến trình đệ tử (`PetActor`) tự đóng lại và thoát khỏi bản đồ.
    - `self.pet_handle = None` ngắt kết nối giữa Sư phụ và Đệ tử. Từ lúc này, Sư phụ sẽ không còn đệ tử đi theo nữa.
5.  **Cập nhật diện mạo (`send_cai_trang`)**: Để Client cập nhật lại hình ảnh nhân vật (vì khi không còn đệ tử, các chỉ số aura và ngoại trang có thể thay đổi).

---

## 3. Các lưu ý về Luồng dữ liệu (Data Flow)
- **Tính an toàn (Concurrency)**: Chúng ta dùng `pet_handle.send(PetMessage::GetSnapshot(tx))` để lấy dữ liệu đệ tử. Đây là cách an toàn nhất trong Rust (Actor Model) để tránh việc Master và Pet tranh chấp bộ nhớ (Deadlock).
- **Tính vĩnh viễn**: Để đệ tử biến mất hoàn toàn sau khi đổi map hoặc login lại, trong hàm lưu database (DAO), bạn cần xử lý: nếu `pet_handle` là `None`, hãy xóa dữ liệu đệ tử của tài khoản đó trong bảng `pet` hoặc `player`.

---

Hy vọng bản hướng dẫn này giúp bạn hiểu rõ và tự tin triển khai các tính năng tương tự!
