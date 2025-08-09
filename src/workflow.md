# File Transfer Workflow Documentation

## Tổng quan

Dự án này bao gồm hai thành phần chính:
- **Client (C#)**: `src_client_c#/` - Ứng dụng Unity game client
- **Server (Java)**: `src_server_java/` - Server game Java

## Workflow Chuyển Tệp Giữa Client và Server

### 1. Kiến trúc Network

#### Client Side (C#)
- **Session Management**: Sử dụng `Session_ME.cs` và `Session_ME2.cs` để quản lý kết nối
- **Message Handling**: `Controller.cs` xử lý các message từ server
- **Service Layer**: `Service.cs` cung cấp các API để gửi request

#### Server Side (Java)
- **Network Layer**: Package `network/` chứa các class quản lý kết nối
- **Message Processing**: `MessageSendCollect.java` xử lý việc gửi/nhận message
- **Session Management**: `Session.java` và `Sender.java` quản lý session

### 2. Quy trình Chuyển Tệp

#### Bước 1: Client Request Resources
```csharp
// Service.cs - Client gửi request resource
public void getResource(sbyte action, MyVector vResourceIndex)
{
    Message message = new Message((sbyte)(-74));
    message.writer().writeByte(action);
    session.sendMessage(message);
}
```

#### Bước 2: Server Xử lý Request
```java
// Controller.java - Server nhận và xử lý command -74
case -74:
    byte type = _msg.reader().readByte();
    if (type == 1) {
        DataGame.sendSizeRes(_session);
    } else if (type == 2) {
        DataGame.sendRes(_session);
    }
    break;
```

#### Bước 3: Server Gửi Resource Data
```java
// DataGame.java - Server gửi resource files
public static void sendRes(MySession session) {
    for (final File fileEntry : new File("data/girlkun/res/x" + session.zoomLevel).listFiles()) {
        String original = fileEntry.getName();
        byte[] res = FileIO.readFile(fileEntry.getAbsolutePath());
        msg = new Message(-74);
        msg.writer().writeByte(2);
        msg.writer().writeUTF(original);
        msg.writer().writeInt(res.length);
        msg.writer().write(res);
        session.sendMessage(msg);
        Thread.sleep(5);
    }
}
```

#### Bước 4: Client Nhận và Lưu Resource
```csharp
// Controller.cs - Client xử lý nhận resource
case -74:
    sbyte b25 = msg.reader().readByte();
    if (b25 == 2) {
        string original = msg.reader().readUTF();
        string[] array4 = Res.split(original, "/", 0);
        string filename = "x" + mGraphics.zoomLevel + array4[array4.Length - 1];
        int num44 = msg.reader().readInt();
        sbyte[] data2 = new sbyte[num44];
        msg.reader().read(ref data2, 0, num44);
        Rms.saveRMS(filename, data2);
    }
    break;
```

### 3. Các Loại Resource Transfer

#### 3.1 Resource Files (-74 command)
- **Action 0**: Gửi version resource
- **Action 1**: Gửi số lượng files
- **Action 2**: Gửi từng file resource
- **Action 3**: Hoàn thành transfer

#### 3.2 Image Files (66 command)
```java
// Server gửi image theo tên
public static void sendImageByName(MySession session, String imgName) {
    byte[] data = FileIO.readFile("data/girlkun/img_by_name/x" + session.zoomLevel + "/" + imgName + ".png");
    msg.writer().writeUTF(imgName);
    msg.writer().writeInt(data.length);
    msg.writer().write(data);
}
```

#### 3.3 Icon Files (-67 command)
```java
// Server gửi icon theo ID
public static void sendIcon(MySession session, int id) {
    byte[] data = FileIO.readFile("data/girlkun/icon/x" + session.zoomLevel + "/" + id + ".png");
    msg.writer().writeInt(data.length);
    msg.writer().write(data);
}
```

### 4. Encryption và Security

#### Key Exchange
```csharp
// Client gửi key request
doSendMessage(new Message(-27));

// Server xử lý key exchange
if (msg.command == -27) {
    this.session.sendKey();
}
```

#### Message Encryption
```csharp
// Client encrypt/decrypt
if (getKeyComplete) {
    b = readKey(b);
    // Encrypt message data
}
```

### 5. File Storage

#### Client Storage
- Sử dụng `Rms.saveRMS()` để lưu files locally
- Files được lưu theo zoom level: `x{zoomLevel}filename`
- Version được lưu trong `ResVersion`

#### Server Storage
- Resource files: `data/girlkun/res/x{zoomLevel}/`
- Image files: `data/girlkun/img_by_name/x{zoomLevel}/`
- Icon files: `data/girlkun/icon/x{zoomLevel}/`

### 6. Error Handling

#### Client Side
```csharp
try {
    // File transfer logic
} catch (Exception) {
    GameCanvas.startOK(mResources.pls_restart_game_error, 8885, null);
}
```

#### Server Side
```java
try {
    // File transfer logic
} catch (Exception e) {
    Logger.logException(DataGame.class, e);
}
```

### 7. Performance Optimization

#### Client Side
- Sử dụng `Thread.Sleep(5)` để tránh spam
- Progress tracking với `ServerListScreen.percent`
- Batch processing với `MyVector`

#### Server Side
- Sử dụng `Thread.sleep(5)` giữa các file
- Buffer size optimization: `socket.setSendBufferSize(1_048_576)`
- Non-blocking message queue với `LinkedBlockingDeque`

### 8. Monitoring và Debug

#### Client Debug
```csharp
Res.outz("SEND MSG: " + message.command);
Debug.Log("GET DATA SERVER");
```

#### Server Debug
```java
System.out.println("🔍 JAVA DEBUG SEND: command=" + msg.command);
System.out.println("🔍 JAVA DEBUG RECEIVE: raw command=" + cmd);
```

## Kết luận

Workflow chuyển tệp giữa client C# và server Java được thiết kế với:
- **Reliability**: Error handling và retry mechanism
- **Security**: Encryption với key exchange
- **Performance**: Optimized buffer và threading
- **Scalability**: Modular design với separation of concerns
- **Monitoring**: Comprehensive logging và debug information

Hệ thống hỗ trợ nhiều loại resource khác nhau và có khả năng mở rộng để thêm các loại file transfer mới.

## Workflow Đăng Nhập

### 1. Client Side - Quy trình Đăng Nhập

#### Bước 1: Người dùng nhập thông tin đăng nhập
```csharp
// LoginScr.cs - Người dùng nhập username/password vào TField
public TField tfUser;  // Text field cho username
public TField tfPass;  // Text field cho password
```

#### Bước 2: Client xử lý thông tin đăng nhập
```csharp
// LoginScr.cs - doLogin() method
public void doLogin()
{
    // Đọc thông tin từ RMS (local storage)
    string text = Rms.loadRMSString("acc");
    string text2 = Rms.loadRMSString("pass");
    
    // Kiểm tra loại đăng nhập (normal vs login2)
    if (text != null && !text.Equals(string.Empty))
    {
        isLogin2 = false;
    }
    else if (Rms.loadRMSString("userAo" + ServerListScreen.ipSelect) != null)
    {
        isLogin2 = true;
    }
    
    // Validation
    if (text == null || text2 == null || GameMidlet.VERSION == null)
    {
        return;
    }
    
    // Kết nối và gửi request đăng nhập
    GameCanvas.connect();
    Service.gI().login(text, text2, GameMidlet.VERSION, (sbyte)(isLogin2 ? 1 : 0));
}
```

#### Bước 3: Client gửi request đăng nhập
```csharp
// Service.cs - login() method
public void login(string username, string pass, string version, sbyte type)
{
    Message message = messageNotLogin(0);  // Command -29 với sub-command 0
    message.writer().writeUTF(username);
    message.writer().writeUTF(pass);
    message.writer().writeUTF(version);
    message.writer().writeByte(type);
    session.sendMessage(message);
}
```

### 2. Server Side - Xử lý Đăng Nhập

#### Bước 1: Server nhận request đăng nhập
```java
// Controller.java - messageNotLogin() method
public void messageNotLogin(MySession session, Message msg) {
    byte cmd = msg.reader().readByte();
    switch (cmd) {
        case 0:  // Login request
            session.login(
                msg.reader().readUTF(),  // username
                msg.reader().readUTF()   // password
            );
            break;
    }
}
```

#### Bước 2: Server xử lý đăng nhập
```java
// MySession.java - login() method
public void login(String username, String password) {
    // Kiểm tra Anti-Login (chống spam)
    AntiLogin al = ANTILOGIN.get(this.ipAddress);
    if (!al.canLogin()) {
        Service.gI().sendThongBaoOK(this, al.getNotifyCannotLogin());
        return;
    }
    
    // Kiểm tra server maintenance
    if (Maintenance.isRuning) {
        Service.gI().sendThongBaoOK(this, "Server đang bảo trì");
        return;
    }
    
    // Kiểm tra số lượng player
    if (!this.isAdmin && Client.gI().getPlayers().size() >= Manager.MAX_PLAYER) {
        Service.gI().sendThongBaoOK(this, "Server quá tải");
        return;
    }
    
    // Thực hiện đăng nhập
    Player pl = GodGK.login(this, al);
    if (pl != null) {
        // Gửi dữ liệu game cho client
        DataGame.sendSmallVersion(this);
        DataGame.sendVersionGame(this);
        DataGame.sendDataItemBG(this);
        
        // Thiết lập player
        pl.setSession(this);
        Client.gI().put(pl);
        this.player = pl;
        
        // Gửi thông báo thành công
        Logger.warning("Successful login for player " + this.player.name);
    }
}
```

#### Bước 3: Database Authentication
```java
// GodGK.java - login() method
public static synchronized Player login(MySession session, AntiLogin al) {
    // Kiểm tra thông tin đăng nhập trong database
    // Tạo Player object nếu đăng nhập thành công
    // Trả về null nếu thất bại
}
```

### 3. Các Bước Bảo Mật

#### Anti-Login Protection
```java
// AntiLogin.java - Chống spam đăng nhập
public boolean canLogin() {
    // Kiểm tra số lần đăng nhập sai trong thời gian ngắn
    // Chặn IP nếu đăng nhập sai quá nhiều lần
}
```

#### Version Check
```java
// Kiểm tra version client
if (clientVersion != 999) {
    Service.gI().sendThongBaoOK(this, "Vui lòng cập nhật game!");
    return;
}
```

#### Server Capacity Check
```java
// Kiểm tra số lượng player hiện tại
if (Client.gI().getPlayers().size() >= Manager.MAX_PLAYER) {
    Service.gI().sendThongBaoOK(this, "Server quá tải");
    return;
}
```

### 4. Response Flow

#### Thành công:
1. Server gửi dữ liệu game (version, items, maps, etc.)
2. Client nhận và lưu dữ liệu
3. Chuyển sang màn hình chọn nhân vật

#### Thất bại:
1. Server gửi thông báo lỗi
2. Client hiển thị dialog lỗi
3. Người dùng có thể thử lại

### 5. Lưu trữ thông tin đăng nhập

#### Client Side
```csharp
// Lưu thông tin đăng nhập
public void savePass()
{
    if (isCheck) {
        Rms.saveRMSString("acc", tfUser.getText().ToLower().Trim());
        Rms.saveRMSString("pass", tfPass.getText().ToLower().Trim());
    }
}
```

#### Server Side
```java
// Cập nhật thông tin đăng nhập trong database
GirlkunDB.executeUpdate("update account set last_time_login = '" + 
    new Timestamp(System.currentTimeMillis()) + 
    "', ip_address = '" + session.ipAddress + "' where id = " + session.userId);
```

## Command Đăng Nhập

### Client → Server
- **-29,0**: Login
- **-29,2**: Set Client Type

### Server → Client (Success)
- **-77**: Small Version
- **-93**: Background Item Version
- **-28**: Game Version
- **-31**: Item Background Data
- **-87**: Game Data

### Server → Client (Error)
- **sendThongBaoOK**: Error Messages

### Flow
```
Client: -29,0
    ↓
Server: Validation
    ↓
Success: -77 → -93 → -28 → -31 → -87
    ↓
Error: sendThongBaoOK
```

## Command Sau Login

### Client → Server
- **13**: Client OK (Ready)

### Server → Client (After Client OK)
- **-3**: Player Info
- **-68**: Send Money
- **-69**: Send Money
- **-64**: Flag Bag
- **-113**: Skill Shortcut
- **-116**: Item Time
- **-106**: Item Time
- **-60**: Skill Data
- **-45**: Skill Effect
- **-124**: Map Effect
- **-112**: Map Effect
- **-95**: Zone Info
- **-105**: Zone Info
- **-6**: Map Info
- **-30**: Sub Command
- **-22**: Chat
- **-96**: Boss Info
- **-119**: Time Info

### Flow Sau Login
```
Client: 13 (Client OK)
    ↓
Server: Send Player Data
    ↓
Server: -3 → -68 → -69 → -64 → -113 → -116 → -106
    ↓
Server: -60 → -45 → -124 → -112 → -95 → -105 → -6
    ↓
Server: -30 → -22 → -96 → -119
```