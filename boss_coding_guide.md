# Hướng Dẫn Thực Hành Code Hệ Thống Boss

Tài liệu này chứa các đoạn code mẫu và hướng dẫn từng bước để bạn tự tay triển khai.

## Bước 1: Khai báo Models (Data Mapping)
Vì dữ liệu trong Database có các cột JSON phức tạp (`stages`, `map_join`), ta cần tạo các Struct để Rust có thể parse dữ liệu này.

**File:** `src/models/boss.rs` (Bạn hãy tạo mới file này)

```rust
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct BossChat {
    pub s: Vec<String>, // Chat khi xuất hiện (Start)
    pub m: Vec<String>, // Chat ngẫu nhiên khi đánh (Middle)
    pub e: Vec<String>, // Chat khi chết hoặc chuyển stage (End)
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct BossStage {
    pub hp: i64,
    pub mp: i32,
    pub dame: i32,
    pub def: i32,
    pub outfit: Vec<i16>,      // [head, body, leg, flag, aura, eff]
    pub skills: Vec<Vec<i32>>, // [[id, level, cooldown], ...]
    pub chat: BossChat,
    pub together: Vec<String>,
}
```

---

## Bước 2: Cấu hình Entity (SeaORM)
Sau khi bạn chạy lệnh `./scripts/generate_entities.sh` thành công, hãy vào file `src/entities/boss_template.rs` và chỉnh sửa lại các kiểu dữ liệu để nó tự động parse JSON.

**File:** `src/entities/boss_template.rs`

```rust
// Thêm import này vào đầu file
use crate::models::boss::BossStage;
use sea_orm::entity::prelude::*;

// Tìm đến đoạn định nghĩa Model và sửa 2 cột này:
#[derive(Clone, Debug, PartialEq, DeriveEntityModel, Serialize, Deserialize)]
#[sea_orm(table_name = "boss_template")]
pub struct Model {
    #[sea_orm(primary_key, auto_increment = false)]
    pub id: String,
    pub name: String,
    pub r#type: String, // solo, group, sequence, scripted
    pub gender: i8,
    pub map_join: Vec<i32>, // Sửa từ String/Value thành Vec<i32>
    pub seconds_rest: i32,
    pub stages: Vec<BossStage>, // Sửa từ String/Value thành Vec<BossStage>
}
```

---

## Bước 3: Tạo BossTemplateManager
Boss cần được load lên bộ nhớ một lần duy nhất khi server chạy để truy cập nhanh.

**File:** `src/templates/boss_template_manager.rs`

```rust
use crate::entities::boss_template;
use crate::entities::prelude::BossTemplate;
use once_cell::sync::Lazy;
use sea_orm::*;
use std::collections::HashMap;
use std::sync::RwLock;

static BOSS_TEMPLATES: Lazy<RwLock<HashMap<String, boss_template::Model>>> =
    Lazy::new(|| RwLock::new(HashMap::new()));

pub async fn load(db: &DatabaseConnection) -> anyhow::Result<()> {
    let items = BossTemplate::find().all(db).await?;
    let mut lock = BOSS_TEMPLATES.write().unwrap();
    lock.clear();
    for item in items {
        lock.insert(item.id.clone(), item);
    }
    tracing::info!("Loaded {} boss templates", lock.len());
    Ok(())
}

pub fn get(id: &str) -> Option<boss_template::Model> {
    let lock = BOSS_TEMPLATES.read().unwrap();
    lock.get(id).cloned()
}
```

---

## Bước 4: Đăng ký Load Manager
Bạn hãy tìm file `src/services/manager.rs`, import `boss_template_manager` và gọi lệnh `load` trong hàm khởi tạo của server.

**Gợi ý:**
```rust
// Trong src/services/manager.rs
boss_template_manager::load(db).await?;
```

---

**Bạn hãy bắt đầu thực hiện Bước 1 và Bước 2 trước.** Nếu gặp lỗi không compile được do thiếu thư viện `serde`, hãy nhắn tôi để hỗ trợ!
