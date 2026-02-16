package server;

import DucPro.FileRunner;
import DucPro.Functions;
import DucPro.FileRunner;
import DucPro.Functions;
import boss.AnTromManager;
import boss.BrolyManager;
import minigame.DecisionMaker.DecisionMaker;
import minigame.LuckyNumber.LuckyNumber;
import models.Consign.ConsignShopManager;
import jdbc.daos.HistoryTransactionDAO;
import bot.BotManager;
import boss.BossManager;
import boss.OtherBossManager;
import boss.TreasureUnderSeaManager;
import boss.SnakeWayManager;
import boss.RedRibbonHQManager;
import boss.GasDestroyManager;
import boss.YardartManager;
import boss.ChristmasEventManager;
import boss.FinalBossManager;
import boss.HalloweenEventManager;
import boss.HungVuongEventManager;
import boss.LunarNewYearEventManager;
import boss.SkillSummonedManager;
import boss.TrungThuEventManager;
import consts.ConstDataEventNAP;
import consts.ConstDataEventSM;

import java.io.IOException;

import network.inetwork.ISession;
import network.Network;
import server.io.MyKeyHandler;
import server.io.MySession;
import services.ClanService;
import services.NgocRongNamecService;
import utils.Logger;
import utils.TimeUtil;

import java.util.*;

import models.The23rdMartialArtCongress.The23rdMartialArtCongressManager;
import models.DeathOrAliveArena.DeathOrAliveArenaManager;
import event.EventManager;
import java.io.DataOutputStream;
import jdbc.daos.EventDAO;
import models.WorldMartialArtsTournament.WorldMartialArtsTournamentManager;
import network.MessageSendCollect;
import models.ShenronEvent.ShenronEventManager;
import models.SuperRank.SuperRankManager;
import network.Message;
import network.inetwork.ISessionAcceptHandler;

public class ServerManager {

    public static String timeStart;

    public static final Map CLIENTS = new HashMap();

    public static String NAME = "Local";
    public static String IP = "127.0.0.1";
    public static int PORT = 14445;

    private static ServerManager instance;

    public static boolean isRunning;

    public void init() {
        Manager.gI();
        HistoryTransactionDAO.deleteHistory();
    }

    public static ServerManager gI() {
        if (instance == null) {
            instance = new ServerManager();
            instance.init();
        }
        return instance;
    }

    public static void main(String[] args) {
        timeStart = TimeUtil.getTimeNow("dd/MM/yyyy HH:mm:ss");
        ServerManagerUI.startServer();
    }

    public void run() {
        isRunning = true;
        activeServerSocket();
        activeCommandLine();
        new Thread(NgocRongNamecService.gI(), "Update NRNM").start();
        new Thread(SuperRankManager.gI(), "Update Super Rank").start();
        new Thread(The23rdMartialArtCongressManager.gI(), "Update DHVT23").start();
        new Thread(DeathOrAliveArenaManager.gI(), "Update Võ Đài Sinh Tử").start();
        new Thread(WorldMartialArtsTournamentManager.gI(), "Update WMAT").start();
        new Thread(AutoMaintenance.gI(), "Update Bảo Trì Tự Động").start();
        new Thread(ShenronEventManager.gI(), "Update Shenron").start();
        BossManager.gI().loadBoss();
        Manager.MAPS.forEach(map.Map::initBoss);
        EventManager.gI().init();
        new Thread(BotManager.gI(), "Update Bot").start();
        new Thread(BossManager.gI(), "Update boss").start();
        new Thread(YardartManager.gI(), "Update yardart boss").start();
        new Thread(FinalBossManager.gI(), "Update final boss").start();
        new Thread(SkillSummonedManager.gI(), "Update Skill-summoned boss").start();
        new Thread(BrolyManager.gI(), "Update broly boss").start();
        new Thread(AnTromManager.gI(), "Update antrom boss").start();
        new Thread(OtherBossManager.gI(), "Update other boss").start();
        new Thread(RedRibbonHQManager.gI(), "Update reb ribbon hq boss").start();
        new Thread(TreasureUnderSeaManager.gI(), "Update treasure under sea boss").start();
        new Thread(SnakeWayManager.gI(), "Update snake way boss").start();
        new Thread(GasDestroyManager.gI(), "Update gas destroy boss").start();
        new Thread(TrungThuEventManager.gI(), "Update trung thu event boss").start();
        new Thread(HalloweenEventManager.gI(), "Update halloween event boss").start();
        new Thread(ChristmasEventManager.gI(), "Update christmas event boss").start();
        new Thread(HungVuongEventManager.gI(), "Update Hung Vuong event boss").start();
        new Thread(LunarNewYearEventManager.gI(), "Update lunar new year event boss").start();
        new Thread(LuckyNumber.gI(), "Update Lucky Number").start();
        new Thread(minigame.RubyGemGame.RubyGemGame.gI(), "Update RubyGemGame").start();
        new Thread(DecisionMaker.gI(), "Update Decision Maker").start();
        new Thread(() -> {
            while (isRunning) {
                try {
                    long st = System.currentTimeMillis();
                    ConstDataEventSM.isRunningSK = ConstDataEventSM.isActiveEvent();
                    ConstDataEventNAP.isRunningSK = ConstDataEventNAP.isActiveEvent();
                    Functions.sleep(Math.max(500 - (System.currentTimeMillis() - st), 10));

                    // TopService.gI().updateTop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "Update SK").start();

    }

    private void activeServerSocket() {
        try {
            Network.gI().init().setAcceptHandler(new ISessionAcceptHandler() {
                @Override
                public void sessionInit(ISession is) {
                    if (!canConnectWithIp(is.getIP())) {
                        is.disconnect();
                        return;
                    }
                    is.setMessageHandler(Controller.gI())
                            .setSendCollect(new MessageSendCollect() {
                                @Override
                                public void doSendMessage(ISession session, DataOutputStream dos, Message msg)
                                        throws Exception {
                                    try {
                                        byte[] data = msg.getData();
                                        if (session.sentKey()) {
                                            byte b = this.writeKey(session, msg.command);
                                            dos.writeByte(b);
                                        } else {
                                            dos.writeByte(msg.command);
                                        }
                                        if (data != null) {
                                            int size = data.length;
                                            if (msg.command == -32 || msg.command == -66 || msg.command == -74
                                                    || msg.command == 11 || msg.command == -67 || msg.command == -87
                                                    || msg.command == 66) {
                                                byte b2 = this.writeKey(session, (byte) size);
                                                dos.writeByte(b2 - 128);
                                                byte b3 = this.writeKey(session, (byte) (size >> 8));
                                                dos.writeByte(b3 - 128);
                                                byte b4 = this.writeKey(session, (byte) (size >> 16));
                                                dos.writeByte(b4 - 128);
                                            } else if (session.sentKey()) {
                                                byte byte1 = this.writeKey(session, (byte) (size >> 8));
                                                dos.writeByte(byte1);
                                                byte byte2 = this.writeKey(session, (byte) (size & 0xFF));
                                                dos.writeByte(byte2);
                                            } else {
                                                dos.writeShort(size);
                                            }
                                            if (session.sentKey()) {
                                                for (int i = 0; i < data.length; ++i) {
                                                    data[i] = this.writeKey(session, data[i]);
                                                }
                                            }
                                            dos.write(data);
                                        } else {
                                            dos.writeShort(0);
                                        }
                                        dos.flush();
                                        msg.cleanup();
                                    } catch (IOException iOException) {
                                        // empty catch block
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
                    .start(PORT);
        } catch (Exception e) {
        }
    }

    private boolean canConnectWithIp(String ipAddress) {
        Object o = CLIENTS.get(ipAddress);
        if (o == null) {
            CLIENTS.put(ipAddress, 1);
            return true;
        } else {
            int n = Integer.parseInt(String.valueOf(o));
            if (n < Manager.MAX_PER_IP) {
                n++;
                CLIENTS.put(ipAddress, n);
                return true;
            } else {
                return false;
            }
        }
    }

    private void activeCommandLine() {
        new Thread(() -> {
            Scanner sc = new Scanner(System.in);
            while (true) {
                String line = sc.nextLine();
                if (line.equals("baotri")) {
                    new Thread(() -> {
                        Maintenance.gI().start(5);
                    }).start();
                } else if (line.equals("athread")) {
                    System.out.println("Số thread hiện tại của Server DUCPRO: " + Thread.activeCount());
                } else if (line.equals("nplayer")) {
                    System.out.println(
                            "Số lượng người chơi hiện tại của Server DUCPRO: " + Client.gI().getPlayers().size());
                } else if (line.equals("shop")) {
                    Manager.gI().updateShop();
                    System.out.println("===========================DONE UPDATE SHOP===========================");
                } else if (line.equals("a")) {
                    new Thread(() -> {
                        Client.gI().close();
                    }).start();
                }
            }
        }, "Active line").start();
    }

    public void disconnect(MySession session) {
        Object o = CLIENTS.get(session.getIP());
        if (o != null) {
            int n = Integer.parseInt(String.valueOf(o));
            n--;
            if (n < 0) {
                n = 0;
            }
            CLIENTS.put(session.getIP(), n);
        }
    }

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
        Logger.success("SUCCESSFULLY MAINTENANCE!\n");

        try {
            String osName = System.getProperty("os.name").toLowerCase();
            String scriptFile = null;

            if (osName.contains("win")) {
                // Windows
                scriptFile = "run.bat";
                Logger.success("Detected Windows - Running " + scriptFile + "\n");
            } else if (osName.contains("linux") || osName.contains("unix")) {
                // Linux/Unix - skip restart, just exit
                Logger.success("Detected Linux - Skipping restart script\n");
            } else if (osName.contains("mac")) {
                // macOS
                scriptFile = "run.sh";
                Logger.success("Detected macOS - Running " + scriptFile + "\n");
            } else {
                Logger.error("Unknown OS: " + osName + "\n");
                System.exit(0);
                return;
            }

            if (scriptFile != null) {
                FileRunner.runBatchFile(scriptFile);
            }
        } catch (IOException e) {
            Logger.error("Error running restart script: " + e.getMessage() + "\n");
        }
        System.exit(0);
    }
}
