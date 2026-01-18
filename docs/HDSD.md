# Hướng Dẫn Sử Dụng GameNro Server

Tài liệu này hướng dẫn cách cài đặt, cấu hình và chạy server GameNro.

## 1. Yêu cầu hệ thống

Trước khi bắt đầu, đảm bảo máy của bạn đã cài đặt:

- **Rust**: Phiên bản mới nhất (Cài đặt qua [rustup.rs](https://rustup.rs/)).
- **MySQL (hoặc MariaDB)**: Cơ sở dữ liệu chính.
- **Git**: Để clone source code.

## 2. Cài đặt Cơ sở dữ liệu

Server sử dụng MySQL làm cơ sở dữ liệu chính.

1.  Tạo database mới có tên `nro`.
2.  Import file `nro.sql` nằm ở thư mục `database/` vào database vừa tạo.

```bash
mysql -u root -p nro < database/nro.sql
```

## 3. Cấu hình Server

File cấu hình chính nằm tại `config.toml` ở thư mục gốc. Bạn cần chỉnh sửa file này để khớp với thông tin database của bạn.

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
```

### Cấu hình SQLite (Tùy chọn)
Nếu bạn muốn dùng SQLite thay vì MySQL:
```toml
[database]
type_database="sqlite"
db_name="database/nro.sqlite"
# Các trường host, port, user, password sẽ bị bỏ qua với SQLite
```

[logging]
level = "info"
file = "logs/app.log"
```

## 4. Chạy Server

Sau khi cấu hình xong, mở terminal tại thư mục gốc dự án và chạy lệnh:

**Chạy chế độ Debug (trong quá trình phát triển):**
```bash
cargo run
```

**Chạy chế độ Release (hiệu năng cao, dùng cho production):**
```bash
cargo run --release
```

Khi thấy dòng log:
`Server listening on 127.0.0.1:14445`
Thì server đã khởi động thành công.

## 5. Kết nối Client

Client game nằm trong thư mục `client/ngocrongonline`.
Do đây là bản build cho Windows (`.exe`), trên Linux bạn cần sử dụng **Wine** để chạy.

1.  **Cài đặt Wine** (nếu chưa có):
    ```bash
    sudo apt update
    sudo apt install wine
    ```

2.  **Chạy Client**:
    Từ thư mục gốc dự án:
    ```bash
    wine "client/ngocrongonline/Ngoc Rong Online.exe"
    ```

Client sẽ kết nối tới server (mặc định 127.0.0.1:14445).

## 6. Các lệnh phát triển (Developer)

- **Kiểm tra lỗi biên dịch**: 
  ```bash
  cargo check
  ```
- **Build project binary**: 
  ```bash
  cargo build --release
  ```
- **Format code chuẩn Rust**: 
  ```bash
  cargo fmt
  ```
- **Xem tài liệu code**: 
  ```bash
  cargo doc --open
  ```
