# Hướng Dẫn Sử Dụng GameNro Server

Tài liệu này hướng dẫn cách cài đặt, cấu hình và chạy server GameNro.

## 1. Yêu cầu hệ thống

Trước khi bắt đầu, đảm bảo máy của bạn đã cài đặt:

- **Rust**: Phiên bản mới nhất (Cài đặt qua [rustup.rs](https://rustup.rs/)).
- **MySQL (hoặc MariaDB)**: Cơ sở dữ liệu chính.
- **Git**: Để clone source code.
- **Wine** (Nếu dùng Linux): Để chạy client Windows.

## 2. Tải Source Code và Client

1.  **Clone Source Code**:
    ```bash
    git clone https://github.com/your-repo/GameNro.git
    cd GameNro
    ```

2.  **Dữ liệu Game (Client)**:
    Client game đã được tích hợp sẵn trong thư mục `client/ngocrongonline`.

## 3. Cài đặt Cơ sở dữ liệu

Server sử dụng MySQL làm cơ sở dữ liệu chính.

1.  Tạo database mới có tên `nro`.
2.  Import file `nro.sql` nằm ở thư mục `database/` vào database vừa tạo.

```bash
mysql -u root -p nro < database/nro.sql
```

## 4. Cấu hình Server

File cấu hình chính nằm tại `config_arc.toml` ở thư mục gốc. Bạn cần chỉnh sửa file này để khớp với thông tin database của bạn.

### Cấu hình MySQL (Mặc định)
```toml
[server]
listen_port = 14445
listen_host = "127.0.0.1"

[database]
type_database="mysql"
host="127.0.0.1"
port=3306
user="root"
password="your_password"
db_name="nro"
pool_size = 10
max_connections = 20
min_connections = 1

[logging]
level = "info"
file = "logs/app.log"
```

## 5. Chạy Server

Sau khi cấu hình xong, mở terminal tại thư mục gốc dự án và chạy lệnh:

**Chế độ Phát triển (Debug):**
```bash
RUST_LOG=info cargo run
```

**Chế độ Production (Release):**
```bash
cargo build --release
./target/release/arc_nro
```

Khi thấy dòng log:
`Server started successfully on 127.0.0.1:14445`
Thì server đã khởi động thành công.

## 6. Kết nối Client

Client game nằm trong thư mục `client/ngocrongonline`.

### Trên Windows:
Chạy file `Ngoc Rong Online.exe`.

### Trên Linux (Sử dụng Wine):
1.  Đảm bảo đã cài đặt Wine.
2.  Chạy lệnh từ thư mục gốc dự án:
    ```bash
    wine "client/ngocrongonline/Ngoc Rong Online.exe"
    ```

Client sẽ kết nối tới server (mặc định 127.0.0.1:14445).

## 7. Các lệnh phát triển (Developer)

- **Kiểm tra lỗi biên dịch**: 
  ```bash
  cargo check
  ```
- **Format code chuẩn Rust**: 
  ```bash
  cargo fmt
  ```
- **Xem tài liệu code**: 
  ```bash
  cargo doc --open
  ```

