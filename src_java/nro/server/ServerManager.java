package nro.server;

/*
 * Box ZALO: https://zalo.me/g/ifjict764
 * SĐT Zalo: 0358176187
 * Chuyên chỉnh sửa, mua bán source NRO...
 */
import Bot.BotChatTG;
import Bot.BotManager;
import EMTI.FileRunner;
import utils.Functions;
import Top.TopKhiGas;
import boss.*;
import consts.ConstDataEventCHUCVIP;
import consts.ConstDataEventNAP;
import consts.ConstDataEventSM;
import consts.ConstDataEventTRANGSUCVIP;
import consts.ConstDataEventthangmuoi;
import consts.ConstSQL;
import event.EventManager;
import jdbc.daos.EventDAO;
import jdbc.daos.HistoryTransactionDAO;
import minigame.DecisionMaker.DecisionMaker;
import minigame.LuckyNumber.LuckyNumber;
import models.DeathOrAliveArena.DeathOrAliveArenaManager;
import models.ShenronEvent.ShenronEventManager;
import models.SuperRank.SuperRankManager;
import models.The23rdMartialArtCongress.The23rdMartialArtCongressManager;
import models.WorldMartialArtsTournament.WorldMartialArtsTournamentManager;
import models.kygui.ConsignShopManager;
import network.Message;
import network.MessageSendCollect;
import network.Network;
import network.inetwork.ISession;
import network.inetwork.ISessionAcceptHandler;
import nro.server.io.MyKeyHandler;
import nro.server.io.MySession;
import nro.services.ClanService;
import nro.services.NgocRongNamecService;
import utils.Logger;
import utils.TimeUtil;
import java.sql.Connection;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jdbc.DBConnecter;
import minigame.TX.TaiXiu;
import models.ShenronEvent_NOEL.ShenronEventManagernoel;
import nro.https.SimpleHttpServerThread;
import nro.player.Archivement;
import nro.player.ArchivementSanBoss;
import nro.player.ArchivementSucManh;
import nro.player.Player;

import static nro.server.AntiDDoS_BY_Barcoll.REAL_PORT;
import services.top.TopAutoService;
import services.top.TopService;
import zalo.server.Bot;

public class ServerManager {

    /**
     * Thời gian khởi động server
     */
    public static String timeStart;
    public static Player adminTarget;

    /**
     * Quản lý client theo IP
     */
    public static final Map<String, Integer> CLIENTS = new HashMap<>();

    /**
     * Thông tin VIP
     */
    public static final String TIME_VIP_START = "6/09/2025";
    public static final String TIME_VIP_END = "6/10/2025";
    public static final String LINK = "https://ngocrongbarcoll.com";

    public static String NAME;
    public static String IP;
    public static int PORT_REAL;

    private static ServerManager instance;

    /**
     * Trạng thái server
     */
    public static boolean isRunning;

    /**
     * Khởi tạo dữ liệu ban đầu
     */
    private void init() {
        Manager.gI();
        HistoryTransactionDAO.deleteHistory();
    }

    /**
     * Singleton
     */
    public static ServerManager gI() {
        if (instance == null) {
            instance = new ServerManager();
            instance.init();
        }
        return instance;
    }

    /**
     * Điểm bắt đầu server
     */
    
        /**
     * 🚀 Khởi động giao diện Web Dashboard song song server
     * Có thể truy cập qua http://localhost:8080
     */
//    private void startWebDashboard() {
//        Thread.startVirtualThread(() -> {
//            try {
//                io.netty.bootstrap.ServerBootstrap b = new io.netty.bootstrap.ServerBootstrap();
//                io.netty.channel.EventLoopGroup bossGroup = new io.netty.channel.nio.NioEventLoopGroup(1);
//                io.netty.channel.EventLoopGroup workerGroup = new io.netty.channel.nio.NioEventLoopGroup();
//
//                b.group(bossGroup, workerGroup)
//                 .channel(io.netty.channel.socket.nio.NioServerSocketChannel.class)
//                 .childHandler(new io.netty.channel.ChannelInitializer<io.netty.channel.socket.SocketChannel>() {
//                     @Override
//                     protected void initChannel(io.netty.channel.socket.SocketChannel ch) {
//                         io.netty.handler.codec.http.cors.CorsConfig cors = io.netty.handler.codec.http.cors.CorsConfigBuilder.forAnyOrigin().build();
//                         io.netty.channel.ChannelPipeline p = ch.pipeline();
//                         p.addLast(new io.netty.handler.codec.http.HttpServerCodec());
//                         p.addLast(new io.netty.handler.codec.http.HttpObjectAggregator(65536));
//                         p.addLast(new io.netty.handler.codec.http.cors.CorsHandler(cors));
//                         p.addLast(new HttpStaticFileServerHandler());
//                         p.addLast(new ServerAPIHandler());
//                         p.addLast(new io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler("/ws"));
//                         p.addLast(new WebSocketHandler());
//                     }
//                 });
//
//                b.bind(8080).sync();
//                System.out.println("🌐 Web Dashboard đang chạy tại: http://localhost:8080/");
//            } catch (Exception e) {
//                System.err.println("❌ Lỗi Web Dashboard: " + e.getMessage());
//            }
//        });
//    }

    
    public static void main(String[] args) {
        timeStart = TimeUtil.getTimeNow("dd/MM/yyyy HH:mm:ss");
//        ServerManager.gI().run();

        // UI quản lý server
        new nro.server.ui.ServerManagerUI().setVisible(true);//panel
        TopAutoService.gI().activeAuto();
//        new nro.server.ServerManager().startWebDashboard(); // chạy Web UI song song

        // AntiDDoS proxy (chưa bật)
//        Thread.startVirtualThread(() -> {
//            Thread.currentThread().setName("AntiDDoS-Proxy");
//            AntiDDoS_BY_Barcoll.runProxy();
//        });
    }

    /**
     * Chạy server
     */
    public void run() {
        long delay = 500;
        long delaySecond = 5000;
        isRunning = true;

        activeServerSocket();
        activeCommandLine();

        // ===============================
        // START ZALO BOT MANAGER
        // ===============================
//        try {
//            Bot zaloBot = new Bot();
//            zaloBot.start(); // chạy nền
//
//            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//                System.out.println("\nShutting down Zalo Bot...");
//                zaloBot.stop();
//            }));

//        } catch (Exception e) {
//            System.err.println("BOT ERROR: " + e.getMessage());
//            e.printStackTrace();
//        }

        // Service chạy nền
        Runnable[] services = {
            NgocRongNamecService.gI(),
            SuperRankManager.gI(),
            The23rdMartialArtCongressManager.gI(),
            DeathOrAliveArenaManager.gI(),
            WorldMartialArtsTournamentManager.gI(),
            AutoMaintenance.gI(),
            ShenronEventManager.gI(),
            ShenronEventManagernoel.gI()
        };
        for (Runnable service : services) {
            Thread.startVirtualThread(service);
        }

        // Khởi tạo boss, event, top
        new SimpleHttpServerThread().start();
        BossManager.gI().loadBoss();
        Manager.MAPS.forEach(map.Map::initBoss);
        EventManager.gI().init();
        TopKhiGas.getInstance().load();

        // Boss & event manager
        Runnable[] bosses = {
            BossManager.gI(),
            YardartManager.gI(),
            FinalBossManager.gI(),
            SkillSummonedManager.gI(),
            BrolyManager.gI(),
            AnTromManager.gI(),
            MatTroiManager.gI(),
            OtherBossManager.gI(),
            RedRibbonHQManager.gI(),
            TreasureUnderSeaManager.gI(),
            SnakeWayManager.gI(),
            GasDestroyManager.gI(),
            TrungThuEventManager.gI(),
            HalloweenEventManager.gI(),
            ChristmasEventManager.gI(),
            HungVuongEventManager.gI(),
            LunarNewYearEventManager.gI(),
            CatchpokemonEventManager.gI(),
            LuckyNumber.gI(),
            DecisionMaker.gI(),
            BabyManager.gI(),
            RongnhiManager.gI(),
            OdoManager.gI(),
            BotManager.gI(),
            TaiXiu.gI(),};
            Archivement.loadMocNapCache();
            ArchivementSucManh.loadMocSucManhCache();
            ArchivementSanBoss.loadMocSanBossCache();
            NroHttpServer.gI().start(); 
            new Thread(TopService.gI(), "Thread TOP").start();
        startTopUpdater();
        for (Runnable boss : bosses) {
            Thread.startVirtualThread(boss);
        }
        
        new Thread(() -> {
            while (!Maintenance.isRunning) {
                TopService.gI().loadListTop();
                try {
                    TimeUnit.SECONDS.sleep(60);
                } catch (InterruptedException e) {
                }
            }
        }, "Update Top").start();
        
        Thread.startVirtualThread(() -> {
            try {
                nro.recharge.RechargeHttp.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Check nạp tiền
        Thread.startVirtualThread(() -> {
            while (isRunning) {
                try {
                    long start = System.currentTimeMillis();
                    ChuyenKhoanManager.HandleTransactionAuto();
                    long elapsed = System.currentTimeMillis() - start;
                    if (elapsed < delaySecond) {
                        Thread.sleep(delaySecond - elapsed);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // Cộng quà nạp tiền
        Thread.startVirtualThread(() -> {
            while (isRunning) {
                try {
                    long start = System.currentTimeMillis();
                    ChuyenKhoanManager.HandleTransactionAddMoneyAuto();
                    long elapsed = System.currentTimeMillis() - start;
                    if (elapsed < delay) {
                        Thread.sleep(delay - elapsed);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

//         Virtual thread cho task nhẹ, chạy liên tục
//        Thread.startVirtualThread(() -> {
//            while (isRunning) {
//                try {
//                    long st = System.currentTimeMillis();
//
//                    ConstDataEventSM.isRunningSK = ConstDataEventSM.isActiveEvent();
//                    ConstDataEventNAP.isRunningSK = ConstDataEventNAP.isActiveEvent();
//                    ConstDataEventTRANGSUCVIP.isRunningSK = ConstDataEventTRANGSUCVIP.isActiveEvent();
//                    ConstDataEventCHUCVIP.isRunningSK = ConstDataEventCHUCVIP.isActiveEvent();
//                    ConstDataEventthangmuoi.isRunningSK = ConstDataEventthangmuoi.isActiveEvent();
//                    
//                    long elapsed = System.currentTimeMillis() - st;
//                    Functions.sleep(Math.max(500 - elapsed, 10));
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
    }
    private ScheduledExecutorService topUpdater;

    private void startTopUpdater() {
        topUpdater = Executors.newSingleThreadScheduledExecutor();
        topUpdater.scheduleAtFixedRate(() -> {
            if (shouldUpdateTop()) {
                updateTop();
                Manager.resetTopFlags();
            }
        }, 0, 3000, TimeUnit.MILLISECONDS);
    }

    private boolean shouldUpdateTop() {
        return Manager.isTopMaydamChanged || Manager.isTopWhisChanged;
    }

    private void updateTop() {
        try (Connection con = DBConnecter.getConnectionServer()) {
            if (Manager.isTopMaydamChanged) {
                Manager.Topmaydam = Manager.realTop(ConstSQL.queryTopmaydam, con);
            }
            if (Manager.isTopWhisChanged) {
                Manager.topWHIS = Manager.realTop(ConstSQL.TOP_WHIS, con);
            }

            Manager.resetTopFlags();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Kích hoạt server socket
     */
    private void activeServerSocket() {
        try {
            Network.gI().init().setAcceptHandler(new ISessionAcceptHandler() {
                @Override
                public void sessionInit(ISession is) {
                    // Kiểm tra IP
                    if (!canConnectWithIp(is.getIP())) {
                        is.disconnect();
                        return;
                    }

                    // Cấu hình session
                    is.setMessageHandler(Controller.gI())
                            .setSendCollect(new MessageSendCollect() {
                                @Override
                                public void doSendMessage(ISession session, DataOutputStream dos, Message msg) {
                                    try {
                                        byte[] data = msg.getData();

                                        // Ghi command
                                        if (session.sentKey()) {
                                            byte b = this.writeKey(session, msg.command);
                                            dos.writeByte(b);
                                        } else {
                                            dos.writeByte(msg.command);
                                        }

                                        // Ghi data
                                        if (data != null) {
                                            int size = data.length;
                                            if (msg.command == -32 || msg.command == -66 || msg.command == -74
                                                    || msg.command == 11 || msg.command == -67
                                                    || msg.command == -87 || msg.command == 66 || msg.command == 12 || msg.command == -28) {//conchongungoc
                                                dos.writeByte(this.writeKey(session, (byte) size) - 128);
                                                dos.writeByte(this.writeKey(session, (byte) (size >> 8)) - 128);
                                                dos.writeByte(this.writeKey(session, (byte) (size >> 16)) - 128);
                                            } else if (session.sentKey()) {
                                                dos.writeByte(this.writeKey(session, (byte) (size >> 8)));
                                                dos.writeByte(this.writeKey(session, (byte) (size & 0xFF)));
                                            } else {
                                                dos.writeShort(size);
                                            }

                                            // Mã hóa dữ liệu nếu đã gửi key
                                            if (session.sentKey()) {
                                                for (int i = 0; i < data.length; i++) {
                                                    data[i] = this.writeKey(session, data[i]);
                                                }
                                            }
                                            dos.write(data);
                                        } else {
                                            dos.writeShort(0);
                                        }

                                        dos.flush();
                                        msg.cleanup();
                                    } catch (IOException ignored) {
                                    }
                                }
                            })
                            .setKeyHandler(new MyKeyHandler())
                            .startCollect();
                }

                @Override
                public void sessionDisconnect(ISession session) {
                    Client.gI().kickSession((MySession) session);
                }
            }).setTypeSessioClone(MySession.class)
                    .setDoSomeThingWhenClose(() -> {
                        Logger.error("SERVER CLOSE\n");
                        System.exit(0);
                    })
                    .start(REAL_PORT);
        } catch (Exception ignored) {
        }
    }

    /**
     * Giới hạn kết nối theo IP
     */
    public boolean canConnectWithIp(String ipAddress) {
        Integer count = CLIENTS.get(ipAddress);
        if (count == null) {
            CLIENTS.put(ipAddress, 1);
            return true;
        } else if (count < Manager.MAX_PER_IP) {
            CLIENTS.put(ipAddress, count + 1);
            return true;
        }
        return false;
    }

    /**
     * Lệnh điều khiển server qua console (Command Line Interface)
     */
    private void activeCommandLine() {
        System.out.println("Type 'help' to see the list of commands");

        Thread.ofVirtual().name("Console-Command-Thread").start(() -> {
            try (Scanner sc = new Scanner(System.in)) {
                while (true) {
                    System.out.print("> ");
                    String line = sc.nextLine().trim();
                    if (line.isEmpty()) continue;

                    Runnable task = switch (line.toLowerCase()) {
                        case "bat" -> () -> {
                            AutoMaintenance.AutoMaintenance = true;
                            System.out.println("[AUTO] Da BAT che do bao tri tu dong.");
                        };
                        case "tat" -> () -> {
                            AutoMaintenance.AutoMaintenance = false;
                            System.out.println("[AUTO] Da TAT che do bao tri tu dong.");
                        };
                        case "baotri" -> () -> {
                            System.out.println("[SYSTEM] Bat dau quy trinh bao tri sau 5 giay...");
                            Maintenance.gI().start(5);
                        };
                        case "thot" -> () -> System.out.println("Active Threads: " + Thread.activeCount());
                        case "nplayer" -> () -> System.out.println("Online Players: " + Client.gI().getPlayers().size());
                        case "system" -> this::printSystemInfo;
                        case "listmaps" -> () -> {
                            System.out.println("=== MAP LIST ===");
                            Manager.MAPS.forEach(map -> System.out.printf("- ID: %d | Name: %s%n", map.mapId, map.mapName));
                            System.out.println("================");
                        };
                        case "reloadshop" -> () -> {
                            ConsignShopManager.gI().save();
                            System.out.println("[SHOP] Consign shop saved & reloaded.");
                        };
                        case "gc" -> () -> {
                            System.gc();
                            System.out.println("[SYSTEM] Garbage Collector executed.");
                        };
                        case "help" -> this::printCommandList;
                        case "exit", "quit" -> null;
                        default -> () -> System.out.println("Unknown command: '" + line + "'. Type 'help' for list.");
                    };

                    if (task != null) {
                        Thread.startVirtualThread(task);
                    } else if (line.equalsIgnoreCase("exit")) {
                        System.out.println("Console thread stopped.");
                        break;
                    }
                }
            } catch (Exception e) {
                Logger.logException(Manager.class, e, "Error in Console Command Line");
            }
        });
    }

    private void printSystemInfo() {
        System.out.println("=== SYSTEM METRICS ===");
        System.out.println("- Memory Usage : " + SystemMetrics.getMemoryInfo());
        System.out.println("- CPU Load     : " + SystemMetrics.getCpuInfo());
        System.out.println("======================");
    }

    private void printCommandList() {
        System.out.println("=== COMMAND LIST ===");
        System.out.format("%-15s : %s%n", "help", "Hien thi danh sach lenh");
        System.out.format("%-15s : %s%n", "baotri", "Bao tri server (dem nguoc 5s)");
        System.out.format("%-15s : %s%n", "bat/tat", "Bat/Tat che do bao tri tu dong");
        System.out.format("%-15s : %s%n", "nplayer", "Xem so luong nguoi choi online");
        System.out.format("%-15s : %s%n", "system", "Xem thong tin RAM/CPU");
        System.out.format("%-15s : %s%n", "thot", "Xem so luong Thread dang chay");
        System.out.format("%-15s : %s%n", "listmaps", "Liet ke danh sach ban do");
        System.out.format("%-15s : %s%n", "reloadshop", "Luu va tai lai Shop Ky Gui");
        System.out.format("%-15s : %s%n", "gc", "Don dep bo nho (Garbage Collection)");
        
        System.out.println("\n--- Quan ly Server / Admin [Chat Game] ---");
        System.out.format("%-15s : %s%n", "admin", "Mo menu Admin");
        System.out.format("%-15s : %s%n", "giftcode", "Quan ly Giftcode");
        System.out.format("%-15s : %s%n", "bot", "Quan ly Boss/Bot");
        
        System.out.println("\n--- Boss / Su kien ---");
        System.out.println("baby, rongnhi, odo, soihecquyn, xinbato, broly");
        System.out.println("antrom, mattroi, boss2, doanhtrai, bdkb, cdrd, kghd");
        System.out.println("trungthu, noel...");

        System.out.println("\n--- Buff / Skill / Test ---");
        System.out.println("hsk, battu, toado");
        System.out.println("hocskill, phanthan, dragon, daucatmoi, item, getitem");
        System.out.println("==============================================");
    }

    /**
     * Ngắt kết nối client
     */
    public void disconnect(MySession session) {
        Integer count = CLIENTS.get(session.getIP());
        if (count != null) {
            count = Math.max(0, count - 1);
            CLIENTS.put(session.getIP(), count);
        }
    }

    /**
     * Đóng server
     */
    public void close() {
        isRunning = false;
        try {
            ClanService.gI().close();
        } catch (Exception e) {
            Logger.error("Lỗi save clan!\n");
        }
        try {
            ConsignShopManager.gI().save();
        } catch (Exception e) {
            Logger.error("Lỗi save shop ký gửi!\n");
        }

        Client.gI().close();
        EventDAO.save();
        Logger.log("SUCCESSFULLY MAINTENANCE!\n");

        if (AutoMaintenance.isRunning) {
            AutoMaintenance.isRunning = false;
            try {
                FileRunner.runBatchFile("run.bat");
            } catch (IOException ignored) {
            }
        }
        System.exit(0);
    }
}
