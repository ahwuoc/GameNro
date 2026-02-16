/*     */ package network.server;

/*     */
 /*     */ import network.session.ISession;
/*     */ import network.session.Session;
/*     */ import network.session.SessionFactory;
/*     */ import java.io.IOException;
/*     */ import java.net.ServerSocket;
/*     */ import java.net.Socket;
import java.util.HashMap;
/*     */ import java.util.logging.Level;
/*     */ import java.util.logging.Logger;

/*     */
 /*     */
 /*     */ public class TeaGameServer
        /*     */ implements InDucProServer /*     */ {

    /*     */ private static TeaGameServer I;
    /*     */    private int port;
    /*     */    private ServerSocket serverListen;
    /*     */    private Class sessionClone;

    /*     *//*     */
 /*     */ public static TeaGameServer gI() {
        /*  22 */ if (I == null) {
            /*  23 */ I = new TeaGameServer();
            /*     */        }
        /*  25 */ return I;
        /*     */    }
    /*     */    private boolean start;
    private boolean randomKey;
    private IServerClose serverClose;
    /*     */    private ISessionAcceptHandler acceptHandler;
    /*     */    private Thread loopServer;

    /*     *//*     */
 /*     */ private TeaGameServer() {
        /*  32 */ this.port = -1;
        /*     */ this.sessionClone = Session.class;
        /*     */    }
    /*     */
 /*     */
 /*     */ public static HashMap<String, Integer> firewall = new HashMap<>();
    public static HashMap<String, Integer> firewallDownDataGame = new HashMap<>();

    /*     */
 /*     */
 /*     */
 /*     */
 /*     *//*     */
 /*     */
 /*     */
 /*     */
 /*     */
 /*     */ public InDucProServer init() {
        /*  44 */ this.loopServer = new Thread(this);
        /*  45 */ return this;
        /*     */    }

    /*     */
 /*     *//*     */
 /*     */
 /*     */ public InDucProServer start(int port) throws Exception {
        /*  50 */ if (port < 0) {
            /*  51 */ throw new Exception("Vui lòng khởi tạo port server!");
            /*     */        }
        /*  53 */ if (this.acceptHandler == null) {
            /*  54 */ throw new Exception("AcceptHandler chưa được khởi tạo!");
            /*     */        }
        /*  56 */ if (!ISession.class.isAssignableFrom(this.sessionClone)) {
            /*  57 */ throw new Exception("Type session clone không hợp lệ!");
            /*     */        }
        /*     */ try {
            /*  60 */ this.port = port;
            /*  61 */ this.serverListen = new ServerSocket(port);
            /*  62 */        } catch (IOException ex) {
            /*  63 */ System.out.println("Lỗi khởi tạo server tại port " + port);
            /*  64 */ System.exit(0);
            /*     */        }
        /*  66 */ this.start = true;
        /*  67 */ this.loopServer.start();
        /*  68 */ System.out.println("Server Girlkun đang chạy tại port " + this.port);
        /*  69 */ return this;
        /*     */    }

    /*     */
 /*     *//*     */
 /*     */
 /*     */ public InDucProServer close() {
        /*  74 */ this.start = false;
        /*  75 */ if (this.serverListen != null) {
            /*     */ try {
                /*  77 */ this.serverListen.close();
                /*  78 */            } catch (IOException ex) {
                /*  79 */ ex.printStackTrace();
                /*     */            }
            /*     */        }
        /*  82 */ if (this.serverClose != null) {
            /*  83 */ this.serverClose.serverClose();
            /*     */        }
        /*  85 */ System.out.println("Server Girlkun đã đóng!");
        /*  86 */ return this;
        /*     */    }

    /*     */
 /*     *//*     */
 /*     */
 /*     */ public InDucProServer dispose() {
        /*  91 */ this.acceptHandler = null;
        /*  92 */ this.loopServer = null;
        /*  93 */ this.serverListen = null;
        /*  94 */ return this;
        /*     */    }

    /*     */
 /*     *//*     */
 /*     */
 /*     */ public InDucProServer setAcceptHandler(ISessionAcceptHandler handler) {
        /*  99 */ this.acceptHandler = handler;
        /* 100 */ return this;
        /*     */    }

    /*     */
 /*     */
    public void run() {
        while (this.start) {
            try {
                Socket socket = this.serverListen.accept();
                String ip = socket.getInetAddress().getHostAddress();
                if (firewall.containsKey(ip) && firewall.get(ip).intValue() > 21) {
                    socket.close();
                } else {
                    ISession session = SessionFactory.gI().cloneSession(this.sessionClone, socket);
                    this.acceptHandler.sessionInit(session);
                    TeaGameSessionManager.gI().putSession(session);
                    if (firewall.containsKey(ip)) {
                        int value = firewall.get(ip).intValue();
                        firewall.put(ip, value += 1);
                    } else {
                        firewall.put(ip, 1);
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (Exception ex) {
                Logger.getLogger(TeaGameServer.class.getName()).log(Level.SEVERE, (String) null, ex);
            }
        }
    }

 /*     */ public InDucProServer setDoSomeThingWhenClose(IServerClose serverClose) {
        /* 121 */ this.serverClose = serverClose;
        /* 122 */ return this;
        /*     */    }

    /*     */
 /*     *//*     */
 /*     */
 /*     */ public InDucProServer randomKey(boolean isRandom) {
        /* 127 */ this.randomKey = isRandom;
        /* 128 */ return this;
        /*     */    }

    /*     */
 /*     */
 /*     */ public boolean isRandomKey() {
        /* 133 */ return this.randomKey;
        /*     */    }

    /*     */
 /*     *//*     */
 /*     */
 /*     */ public InDucProServer setTypeSessioClone(Class clazz) throws Exception {
        /* 138 */ this.sessionClone = clazz;
        /* 139 */ return this;
        /*     */    }

    /*     */
 /*     */
 /*     */ public ISessionAcceptHandler getAcceptHandler() throws Exception {
        /* 144 */ if (this.acceptHandler == null) {
            /* 145 */ throw new Exception("AcceptHandler chưa được khởi tạo!");
            /*     */        }
        /* 147 */ return this.acceptHandler;
        /*     */    }

    /*     */
 /*     */
 /*     */ public void stopConnect() {
        /* 152 */ this.start = false;
        /*     */    }
    /*     */ }


