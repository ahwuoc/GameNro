# Hướng dẫn sử dụng SeaORM Entities

## Tổng quan

Project sử dụng SeaORM làm ORM để tương tác với MySQL database. Entities được generate từ database schema và nằm trong thư mục `src/entities/`.

## Cấu trúc

```
src/entities/
├── mod.rs          # Export tất cả entities
├── prelude.rs      # Re-export entities với tên ngắn gọn
├── account.rs      # Entity cho bảng account
├── player.rs       # Entity cho bảng player
└── ...             # Các entities khác
```

## Sử dụng cơ bản

### 1. Import entities

```rust
use crate::entities::{account, player};
use sea_orm::*;
```

### 2. Query dữ liệu

#### Tìm theo ID
```rust
let account = account::Entity::find_by_id(1)
    .one(&db)
    .await?;
```

#### Tìm theo điều kiện
```rust
let account = account::Entity::find()
    .filter(account::Column::Username.eq("username"))
    .one(&db)
    .await?;
```

#### Tìm tất cả
```rust
let accounts = account::Entity::find()
    .all(&db)
    .await?;
```

#### Tìm với limit và offset
```rust
let accounts = account::Entity::find()
    .limit(10)
    .offset(0)
    .all(&db)
    .await?;
```

### 3. Tạo mới (Insert)

```rust
let new_account = account::ActiveModel {
    username: Set("newuser".to_owned()),
    password: Set("hashed_password".to_owned()),
    email: Set("email@example.com".to_owned()),
    ban: Set(false),
    is_admin: Set(false),
    active: Set(1),
    vnd: Set(0),
    vang: Set(0),
    ..Default::default()
};

let result = account::Entity::insert(new_account)
    .exec(&db)
    .await?;

let account_id = result.last_insert_id;
```

### 4. Cập nhật (Update)

#### Cập nhật từ Model
```rust
// Lấy account từ database
let account_model = account::Entity::find_by_id(1)
    .one(&db)
    .await?
    .unwrap();

// Chuyển sang ActiveModel để update
let mut account_active = account_model.into_active_model();

// Thay đổi giá trị
account_active.last_time_login = Set(chrono::Local::now().naive_local());
account_active.vang = Set(1000);

// Lưu vào database
let updated = account_active.update(&db).await?;
```

#### Update trực tiếp
```rust
account::Entity::update_many()
    .col_expr(account::Column::Vang, Expr::value(1000))
    .filter(account::Column::Id.eq(1))
    .exec(&db)
    .await?;
```

### 5. Xóa (Delete)

```rust
// Xóa theo ID
account::Entity::delete_by_id(1)
    .exec(&db)
    .await?;

// Xóa theo điều kiện
account::Entity::delete_many()
    .filter(account::Column::Ban.eq(true))
    .exec(&db)
    .await?;
```

### 6. Relations (Quan hệ)

#### Lấy player từ account
```rust
let account = account::Entity::find_by_id(1)
    .one(&db)
    .await?
    .unwrap();

// Lấy tất cả players của account này
let players = account.find_related(player::Entity)
    .all(&db)
    .await?;
```

#### Eager loading với join
```rust
let accounts_with_players = account::Entity::find()
    .find_with_related(player::Entity)
    .all(&db)
    .await?;
```

### 7. Transactions

```rust
let txn = db.begin().await?;

// Thực hiện các operations
let account = account::ActiveModel {
    username: Set("user1".to_owned()),
    password: Set("pass".to_owned()),
    ..Default::default()
};
let account_result = account::Entity::insert(account).exec(&txn).await?;

let player = player::ActiveModel {
    account_id: Set(Some(account_result.last_insert_id)),
    name: Set("Player1".to_owned()),
    ..Default::default()
};
player::Entity::insert(player).exec(&txn).await?;

// Commit transaction
txn.commit().await?;
```

## Generate lại entities

Khi database schema thay đổi, bạn cần generate lại entities:

### Cách 1: Sử dụng script
```bash
./scripts/generate_entities.sh
```

### Cách 2: Chạy trực tiếp CLI
```bash
sea-orm-cli generate entity \
  -u "mysql://root:password@localhost:3306/nro_black" \
  -o src/entities \
  --with-serde both \
  --expanded-format
```

### Cách 3: Generate từng bảng cụ thể
```bash
sea-orm-cli generate entity \
  -u "mysql://root:password@localhost:3306/nro_black" \
  -o src/entities \
  --tables account,player
```

## Lưu ý quan trọng

### DateTime fields
- MySQL `TIMESTAMP` và `DATETIME` được map sang `chrono::NaiveDateTime`
- Khi set giá trị: `Set(chrono::Local::now().naive_local())`
- Không dùng `chrono::Utc::now()` trực tiếp vì nó trả về `DateTime<Utc>`

### Boolean fields
- MySQL `TINYINT(1)` được map sang `bool` trong Rust
- So sánh: `if account.ban { ... }` thay vì `if account.ban == 1`

### Optional fields
- Các column có `NULL` trong database được map sang `Option<T>`
- Set giá trị: `Set(Some(value))` hoặc `Set(None)`

### Default values
- Sử dụng `..Default::default()` để bỏ qua các field không cần set
- Các field có `auto_increment` hoặc `default` trong DB không cần set

## Ví dụ thực tế

### Login và update last login time
```rust
pub async fn login(
    db: &DatabaseConnection,
    username: &str,
    password: &str,
) -> Result<account::Model, DbErr> {
    // Tìm account
    let account = account::Entity::find()
        .filter(account::Column::Username.eq(username))
        .one(db)
        .await?
        .ok_or(DbErr::Custom("Account not found".to_string()))?;
    
    // Kiểm tra password
    if account.password != password {
        return Err(DbErr::Custom("Wrong password".to_string()));
    }
    
    // Kiểm tra ban
    if account.ban {
        return Err(DbErr::Custom("Account banned".to_string()));
    }
    
    // Update last login time
    let mut account_active = account.into_active_model();
    account_active.last_time_login = Set(chrono::Local::now().naive_local());
    
    let updated = account_active.update(db).await?;
    Ok(updated)
}
```

### Tạo player mới
```rust
pub async fn create_player(
    db: &DatabaseConnection,
    account_id: i32,
    name: &str,
    gender: i32,
) -> Result<player::Model, DbErr> {
    let player = player::ActiveModel {
        account_id: Set(Some(account_id)),
        name: Set(name.to_string()),
        gender: Set(gender),
        head: Set(102),
        clan_id: Set(-1),
        create_time: Set(chrono::Local::now().naive_local()),
        first_time_login: Set(chrono::Local::now().naive_local()),
        // Các field JSON
        data_inventory: Set(r#"{"gold": 0}"#.to_string()),
        items_bag: Set(r#"[]"#.to_string()),
        ..Default::default()
    };
    
    let result = player::Entity::insert(player).exec(db).await?;
    
    // Lấy player vừa tạo
    player::Entity::find_by_id(result.last_insert_id)
        .one(db)
        .await?
        .ok_or(DbErr::Custom("Failed to get created player".to_string()))
}
```

## Tài liệu tham khảo

- [SeaORM Documentation](https://www.sea-ql.org/SeaORM/)
- [SeaORM Tutorial](https://www.sea-ql.org/sea-orm-tutorial/)
- [SeaORM Cookbook](https://www.sea-ql.org/sea-orm-cookbook/)
