# 🚀 Hướng Dẫn Deployment

## GitHub Actions CI/CD

Project đã được setup với 3 workflows:

### 1. CI Workflow (`.github/workflows/ci.yml`)
Chạy tự động khi push/PR vào `master`, `develop`:
- ✅ Check formatting (`cargo fmt`)
- ✅ Run linter (`cargo clippy`)
- ✅ Build project
- ✅ Run tests với MySQL test database
- ✅ Upload build artifacts

### 2. Deploy Workflow (`.github/workflows/deploy.yml`)
Chạy khi push vào `master` hoặc tạo tag `v*`:
- 📦 Build release binary
- 📦 Tạo deployment package
- 🚀 Deploy lên server (cần config)
- 🏷️ Tạo GitHub Release khi tag

### 3. Docker Workflow (`.github/workflows/docker.yml`)
Build và push Docker image lên GitHub Container Registry:
- 🐳 Build Docker image
- 📤 Push lên `ghcr.io`
- 🏷️ Tag theo branch/version

---

## Cách Sử Dụng

### Option 1: Deploy với Docker (Khuyến nghị)

1. **Clone repository**:
```bash
git clone <your-repo-url>
cd arc_nro
```

2. **Setup environment**:
```bash
cp .env.example .env
# Chỉnh sửa .env với thông tin của bạn
```

3. **Chạy với Docker Compose**:
```bash
docker-compose up -d
```

4. **Kiểm tra logs**:
```bash
docker-compose logs -f game_server
```

### Option 2: Deploy từ GitHub Container Registry

```bash
# Pull image từ GitHub
docker pull ghcr.io/<your-username>/arc_nro:master

# Chạy container
docker run -d \
  --name arc_nro \
  -p 14445:14445 \
  -e DATABASE_URL=mysql://user:pass@host:3306/nro \
  ghcr.io/<your-username>/arc_nro:master
```

### Option 3: Deploy thủ công lên VPS

1. **Download artifact từ GitHub Actions**
2. **Upload lên server**:
```bash
scp arc_nro-deploy.tar.gz user@server:/opt/arc_nro/
```

3. **Giải nén và chạy**:
```bash
ssh user@server
cd /opt/arc_nro
tar -xzf arc_nro-deploy.tar.gz
./arc_nro
```

---

## Setup Auto Deploy lên VPS

Để enable auto deploy trong `.github/workflows/deploy.yml`, cần thêm secrets vào GitHub:

1. Vào **Settings** → **Secrets and variables** → **Actions**
2. Thêm các secrets:
   - `DEPLOY_HOST`: IP hoặc domain của server
   - `DEPLOY_USER`: SSH username
   - `DEPLOY_KEY`: SSH private key

3. Uncomment phần deploy trong `deploy.yml`

---

## Tạo Release

```bash
# Tạo tag
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0
```

GitHub Actions sẽ tự động:
- Build release binary
- Tạo GitHub Release
- Upload binary vào Release

---

## Systemd Service (Production)

Tạo file `/etc/systemd/system/arc_nro.service`:

```ini
[Unit]
Description=Arc NRO Game Server
After=network.target mysql.service

[Service]
Type=simple
User=arc_nro
WorkingDirectory=/opt/arc_nro
ExecStart=/opt/arc_nro/arc_nro
Restart=always
RestartSec=10
Environment="RUST_LOG=info"

[Install]
WantedBy=multi-user.target
```

Enable và start:
```bash
sudo systemctl daemon-reload
sudo systemctl enable arc_nro
sudo systemctl start arc_nro
sudo systemctl status arc_nro
```

---

## Monitoring

### Xem logs
```bash
# Docker
docker-compose logs -f game_server

# Systemd
sudo journalctl -u arc_nro -f
```

### Health check
```bash
# Kiểm tra port
netstat -tulpn | grep 14445

# Test connection
telnet localhost 14445
```

---

## Troubleshooting

### Build failed
- Kiểm tra Rust version: `rustc --version`
- Clear cache: `cargo clean`
- Update dependencies: `cargo update`

### Database connection failed
- Kiểm tra MySQL đang chạy
- Verify DATABASE_URL trong config
- Check firewall rules

### Docker build slow
- Enable BuildKit: `export DOCKER_BUILDKIT=1`
- Use cache: GitHub Actions đã config sẵn

---

## Security Notes

⚠️ **Quan trọng**:
- Không commit file `.env` vào git
- Đổi password MySQL mặc định
- Sử dụng SSH key thay vì password
- Enable firewall và chỉ mở port cần thiết
- Thường xuyên update dependencies: `cargo update`
