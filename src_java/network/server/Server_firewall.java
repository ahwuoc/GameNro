package network.server;

import network.session.ISession;
import network.session.Session;
import network.session.SessionFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server_firewall implements Server_iskey, Runnable {

    private static Server_firewall I;

    private int port;
    private ServerSocket serverListen;
    private Class sessionClone;
    private boolean start;
    private boolean randomKey;
    private IServerClose serverClose;
    private ISessionAcceptHandler acceptHandler;
    private Thread loopServer;

    public static final HashMap<String, Integer> firewall = new HashMap<>();
    public static final HashMap<String, Integer> firewallDownDataGame = new HashMap<>();

    private Server_firewall() {
        this.port = -1;
        this.sessionClone = Session.class;
    }

    public static Server_firewall gI() {
        if (I == null) {
            I = new Server_firewall();
        }
        return I;
    }

    public Server_iskey init() {
        this.loopServer = Thread.ofVirtual()
                .name("Server_iskey-Loop")
                .unstarted(this);
        return this;
    }

    public Server_iskey start(int port) throws Exception {
        if (port < 0) {
            throw new Exception("Vui lòng khởi tạo port server!");
        }
        if (this.acceptHandler == null) {
            throw new Exception("AcceptHandler chưa được khởi tạo!");
        }
        if (!ISession.class.isAssignableFrom(this.sessionClone)) {
            throw new Exception("Type session clone không hợp lệ!");
        }
        try {
            this.port = port;
            this.serverListen = new ServerSocket(port);
        } catch (IOException ex) {
            System.out.println("Lỗi khởi tạo server tại port " + port);
            System.exit(0);
        }
        this.start = true;
        this.loopServer.start();
        System.out.println("Server Girlkun đang chạy tại port " + this.port);
        return this;
    }

    public Server_iskey close() {
        this.start = false;
        if (this.serverListen != null) {
            try {
                this.serverListen.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        if (this.serverClose != null) {
            this.serverClose.serverClose();
        }
        System.out.println("Server Girlkun đã đóng!");
        return this;
    }

    public Server_iskey dispose() {
        this.acceptHandler = null;
        this.loopServer = null;
        this.serverListen = null;
        return this;
    }

    public Server_iskey setAcceptHandler(ISessionAcceptHandler handler) {
        this.acceptHandler = handler;
        return this;
    }

    @Override
    public void run() {
        while (this.start) {
            try {
                Socket socket = this.serverListen.accept();
                String ip = socket.getInetAddress().getHostAddress();
                if (firewall.getOrDefault(ip, 0) > 21) {
                    socket.close();
                } else {
                    ISession session = SessionFactory.gI().cloneSession(this.sessionClone, socket);
                    this.acceptHandler.sessionInit(session);
                    SessionManager_network.gI().putSession(session);
                    firewall.put(ip, firewall.getOrDefault(ip, 0) + 1);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (Exception ex) {
                Logger.getLogger(Server_firewall.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public Server_iskey setDoSomeThingWhenClose(IServerClose serverClose) {
        this.serverClose = serverClose;
        return this;
    }

    public Server_iskey randomKey(boolean isRandom) {
        this.randomKey = isRandom;
        return this;
    }

    public boolean isRandomKey() {
        return this.randomKey;
    }

    public Server_iskey setTypeSessioClone(Class clazz) throws Exception {
        this.sessionClone = clazz;
        return this;
    }

    public ISessionAcceptHandler getAcceptHandler() throws Exception {
        if (this.acceptHandler == null) {
            throw new Exception("AcceptHandler chưa được khởi tạo!");
        }
        return this.acceptHandler;
    }

    public void stopConnect() {
        this.start = false;
    }
}
