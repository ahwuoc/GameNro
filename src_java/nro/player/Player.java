package nro.player;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/ifjict764
 *  sdt zalo: 0358176187
 * Chuyên chỉnh sữa mua bán source nro,...
 */
import Bot.DeTuAI;
import nro.services.FriendAndEnemyService;
import nro.services.Service;
import nro.services.TaskService;
import nro.services.InventoryService;
import nro.services.ClanService;
import nro.services.MapService;
import nro.services.SkillService;
import nro.services.ItemTimeService;
import nro.services.PlayerService;
import nro.services.EffectSkillService;
import nro.services.NgocRongNamecService;
import utils.Functions;
import boss.BossID;
import boss.BossManager;
import consts.ConstDailyGift;
import minigame.cost.LuckyNumberCost;
import minigame.LuckyNumber.LuckyNumberService;
import models.Training.TrainingService;
import nro.models.npc.NonInteractiveNPC;
import models.Card.Card;
import models.Card.RadarCard;
import models.Card.RadarService;
import models.MajinBuu.MajinBuuService;
import player.badges.Badges;
import player.badges.BadgesData;
import player.dailyGift.DailyGiftData;
import player.dailyGift.DailyGiftService;
import skill.PlayerSkill;

import java.util.Iterator;
import java.util.List;

import clan.Clan;
import intrinsic.IntrinsicPlayer;
import item.Item;
import item.ItemTime;
import npc.specialnpc.MagicTree;
import consts.ConstPlayer;
import consts.ConstTask;
import npc.specialnpc.MabuEgg;
import mob.MobMe;
import data.DataGame;
import clan.ClanMember;
import consts.ConstAchievement;
import item.CaiTrang;
import java.time.LocalDate;
import java.time.LocalDateTime;
import map.Zone;
import matches.IPVP;
import matches.TYPE_LOSE_PVP;
import skill.Skill;
import nro.server.io.MySession;
import task.Badges.BadgesTask;
import task.Badges.BadgesTaskService;
import task.TaskPlayer;
import network.Message;
import nro.server.Client;
import services.func.ChangeMapService;
import models.Combine.Combine;
import utils.Logger;
import utils.Util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import models.BlackBallWar.BlackBallWarService;
import models.The23rdMartialArtCongress.The23rdMartialArtCongressManager;
import map.ItemMap;
import map.MaBuHold;
import models.DragonNamecWar.TranhNgoc;
import models.DragonNamecWar.TranhNgocService;
import models.MajinBuu.MajinBuu14H;
import models.ShenronEvent.ShenronEvent;
import models.ShenronEvent_NOEL.ShenronEventnoel;
import models.SuperDivineWater.SuperDivineWaterService;
import models.The23rdMartialArtCongress.The23rdMartialArtCongress;
import nro.server.Maintenance;
import nro.server.Manager;
import utils.TimeUtil;

public class Player implements Runnable {

    public long diemmaydam;
    public long diemboss;
    public long lasttimeTD;
    public int cap;
    public int level;
    public long lastSaveTask = 0;
    public String tt = "";
    public long lastTimeEatPea;
    public boolean isCookingBanhDay = false;
    public boolean isCookingBanhTet = false;
    public boolean isCookingBanhChung = false;
    public boolean isCookingBanhChung2 = false;
    public boolean isCookingBanhTrungThuGaQuay = false;
    public boolean isCookingBanhTrungThuGaQuayHaoHan = false;
    public boolean isCookingBanhTrungThuHatSen = false;
    public long goldTai, goldXiu;
    public int kolQuestStage;
    public int kolVIPQuestStage;
    public long destronGas70CompletionCount;
    public long dailySuperHardQuestCompletionCount;
    public long bossBabyDefeatParticipationCount;
    public long monsterKillCountAutoTrain;
    public int vipPurchaseCount;
    public long lastClanCheckIn = 0;
    public LocalDateTime lastCheckIn;
    public long martialArtsTournamentWins;

    public PlayerSkill HocSkill;
    public List<Integer> CheckHocSkill = new ArrayList<>();

    @Setter
    @Getter
    private MySession session;
    public PlayerEffect effect;

    public long id;
    public String name;
    public byte gender;
    public boolean isNewMember;
    public short head;
    public int deltaTime;
    public int danap;
    public int bongmaster;
    public int hopquathang9vip;
    public int hopquathang9;
    public int hopquatrungthuvip;
    public int longdentreo;
    public int capsuvip;
    public int thiepchucvip;
    public int diemnoel;
    public int hopqua2010;
    public int hopkeomaquy;
    public int hoptrahoacuc;
    public int halloween_master;
    public int hopdiem;
    public int vongquayvang;
    public int vongquaydacbiet;
    public int phaobong;
    public int lixi;
    public int keo_halloween;
    public int thiep_halloween;
    public byte typePk;
    public boolean isNpc;
    public byte cFlag;
    public boolean haveTennisSpaceShip;
    public boolean isCopy;
    public boolean beforeDispose;
    public boolean isBot;
    public int mbv = 0;
    public boolean baovetaikhoan;
    //top kg
    public String nameClan;
    public int levelKhiGasDone;
    public long timeKhiGasDone;
    public long lastTimeUpdateTopKhiGas;
    //bkdb
    public int levelBDKBDone;
    public long timeBDKBDone;
    public long lastTimeUpdateTopBDKB;
    //cdrd
    public int levelCDRDDone;
    public long timeCDRDDone;
    public long lastTimeUpdateTopCDRD;
    public int point_maydam;
    public long total_damage_maydam;
    public int thachdauwhis = 0;
    public long mbvtime;
    public int timeGohome;
    public long lastUpdateGohomeTime;
    public boolean goHome;
    public long lastPkCommesonTime;
    public boolean callBossPocolo;
    public Zone zoneSieuThanhThuy;
    public boolean winSTT;
    public long lastTimeWinSTT;
    public int paramPorata = 0;
    public int idOtPorata = -1;
    public long lastTimeUpdateSTT;
    public MajinBuu14H maBu2H;
    public boolean isMabuHold;
    public MaBuHold maBuHold;
    public int precentMabuHold;
    public boolean isPhuHoMapMabu;
    public boolean danhanthoivang;
    public long lastRewardGoldBarTime;
    public int timesPerDayBDKB = 0;
    public long lastTimeJoinBDKB;
    public boolean joinCDRD;
    public long lastTimeJoinCDRD;
    public boolean talkToThuongDe;
    public boolean talkToThanMeo;
    public long timeChangeMap144;
    public long lastTimeJoinDT;
    public int typeChibi;
    public long lastTimeChibi;
    public long lastTimeUpdateChibi;
    public String captcha = "";
    public int spamcaptcha = 0;
    public long lasttimebotchat;
    public boolean doesNotAttack;
    public long lastTimePlayerNotAttack;
    public int timeNotAttack = 1800000;
    public boolean isPet;
    public boolean isNewPet;
    public boolean isNewPet1;
    public boolean isBoss;
    public boolean isPlayer;
    public PlayerClone clone;
    public boolean isClone;
    public IPVP pvp;
    public byte maxTime = 30;
    public byte type = 0;
    public boolean isOffline = false;
    public String notify = null;
    public int mapIdBeforeLogout;
    public List<Zone> mapBlackBall;
    public List<Zone> mapMaBu;
    public List<Player> temporaryEnemies = new ArrayList<>();
    public Date firstTimeLogin;
    public Zone zone;
    public Zone mapBeforeCapsule;
    public List<Zone> mapCapsule;
    public Pet pet;
    public NewPet newPet;
    public MobMe mobMe;
    public Location location;
    public SetClothes setClothes;
    public EffectSkill effectSkill;
    public MabuEgg mabuEgg;
    public TaskPlayer playerTask;
    public ItemTime itemTime;
    public Fusion fusion;
    public MagicTree magicTree;
    public IntrinsicPlayer playerIntrinsic;
    public Inventory inventory;
    public PlayerSkill playerSkill;
    public Combine combine;
    public IDMark iDMark;
    public Charms charms;
    public EffectSkin effectSkin;
    public NPoint nPoint;
    public RewardBlackBall rewardBlackBall;
    public FightMabu fightMabu;
    public NewSkill newSkill;
    public Satellite satellite;
    public Achievement achievement;
    public GiftCode giftCode;
    public Traning traning;
    public Badges badges;
    public Clan clan;
    public ClanMember clanMember;
    public List<Friend> friends;
    public List<Enemy> enemies;
    public boolean justRevived;
    public long lastTimeRevived;
    public long timeChangeZone;
    public long lastUseOptionTime;
    public short idNRNM = -1;
    public short idGo = -1;
    public long lastTimePickNRNM;
    public List<Card> Cards = new ArrayList<>();
    public int levelWoodChest;
    public long goldChallenge;
    public long rubyChallenge;
    public long lastTimeRewardWoodChest;
    public List<Item> itemsWoodChest = new ArrayList<>();
    public int indexWoodChest;
    public long lastTimePKDHVT23;
    public boolean lostByDeath;
    public boolean isPKDHVT;
    public int xSend;
    public int ySend;
    public boolean isFly;
    public long lastTimeDietQuy;
    // shenron event
    public long lastTimeShenronAppeared;
    public boolean isShenronAppear;
    public ShenronEvent shenronEvent;
    public ShenronEventnoel shenronEventnoel;
    // vo dai sinh tu
    public long lastTimePKVoDaiSinhTu;
    public boolean haveRewardVDST;
    public int thoiVangVoDaiSinhTu;
    public long timePKVDST;
    public int binhChonHatMit;
    public int binhChonPlayer;
    public Zone zoneBinhChon;
    public ItemEvent itemEvent;
    public int levelLuyenTap;
    public boolean isThachDau;
    public int tnsmLuyenTap;
    public boolean dangKyTapTuDong;
    public long lastTimeOffline;
    public int mapIdDangTapTuDong;
    public int lastMapOffline;
    public int lastZoneOffline;
    public int lastXOffline;
    public String thongBaoTapTuDong;
    public boolean teleTapTuDong;
    public int timesPerDayCuuSat;
    public long lastTimeCuuSat;
    public boolean nhanVangNangVIP;
    public boolean nhanDeTuNangVIP;
    public boolean nhanSKHVIP;
    public byte vip;
    public long timevip;
    public long totalDamageTaken;
    public boolean thongBaoChangeMap;
    public String textThongBaoChangeMap;
    public boolean thongBaoThua;
    public String textThongBaoThua;
    public SuperRank superRank;
    
//    public boolean canReward;

    public boolean canRewardBeNa;
    public boolean haveRewardBeNa;
    public boolean canRewardLanCon;
    public boolean haveRewardLanCon;
    public boolean canRewardTuanLoc;
    public boolean haveRewardTuanLoc;

    public boolean changeMapVIP;
//    public boolean haveReward;
    public int tayThong;
    public List<Item> itemsTradeWVP = new ArrayList<>();
    public boolean tradeWVP;
    private DropItem dropItem;
    public boolean isBattu = false;
    public List<BadgesData> dataBadges = new ArrayList<>();
    public List<BadgesTask> dataTaskBadges = new ArrayList<>();
    public long lastTimeChangeBadges;
    public List<DailyGiftData> dailyGiftData = new ArrayList<>();
    public int numUseSkill = 0;
    public int typeRecvieArchiment = 0;
    public PlayerEvent event;
    public PointFusion pointfusion;
    public List<Archivement> archivementList = new ArrayList<>();
    public List<ArchivementSucManh> archivementListSM = new ArrayList<>();
    public List<ArchivementSanBoss> archivementListDiem = new ArrayList<>();

    public boolean isHoldNamecBallTranhDoat;
    public int tempIdNamecBallHoldTranhDoat = -1;
    public long lastTimePickItem;
    public long lastTimeUpdateBallWar;
    public List<Integer> BoughtSkill = new ArrayList<>();
    public LearnSkill LearnSkill;
    public List<Integer> CheckLearnSkill = new ArrayList<>();
    public DeTuAI ai;
    @Setter
    @Getter
    private LocalDateTime timeCache;
    public Player master;

    public Player() {
        HocSkill = new PlayerSkill();
        LearnSkill = new LearnSkill();
        effect = new PlayerEffect(this);
        lastUseOptionTime = System.currentTimeMillis();
        lastTimeDietQuy = 0;
        location = new Location();
        nPoint = new NPoint(this);
        inventory = new Inventory();
        playerSkill = new PlayerSkill(this);
        setClothes = new SetClothes(this);
        effectSkill = new EffectSkill(this);
        fusion = new Fusion(this);
        playerIntrinsic = new IntrinsicPlayer();
        rewardBlackBall = new RewardBlackBall(this);
        fightMabu = new FightMabu(this);
        //----------------------------------------------------------------------
        iDMark = new IDMark();
        combine = new Combine();
        playerTask = new TaskPlayer();
        friends = new ArrayList<>();
        enemies = new ArrayList<>();
        itemTime = new ItemTime(this);
        charms = new Charms();
        effectSkin = new EffectSkin(this);
        newSkill = new NewSkill(this);
        satellite = new Satellite();
        achievement = new Achievement(this);
        giftCode = new GiftCode();
        traning = new Traning();
        itemEvent = new ItemEvent(this);
        superRank = new SuperRank(this);
        dropItem = new DropItem(this);
        badges = new Badges();
        event = new PlayerEvent(this);
        pointfusion = new PointFusion(this);
        archivementList = new ArrayList<>();
        archivementListSM = new ArrayList<>();
    }

    public PointFusion getPointfusion() {
        return this.pointfusion;
    }

    //--------------------------------------------------------------------------
    public boolean isDie() {
        if (this.nPoint != null) {
            return this.nPoint.hp <= 0;
        }
        return true;
    }

    public void sendMessage(Message msg) {
        if (this.session != null) {
            session.sendMessage(msg);
        }
    }

    public boolean isActive() {
        return (this.isPl() && this.session != null && this.session.actived) || (this.isPet && ((Pet) this).master.session != null && ((Pet) this).master.session.actived);
    }

    public boolean isPl() {
        return isPlayer && !isPet && !isNpc && !isBot && !isClone && !isBoss && !isNewPet && !isNewPet1 && !(this instanceof NonInteractiveNPC);
    }

    @Override
    public void run() {
        Functions.sleep(500);
        while (!Maintenance.isRunning && session != null && session.isConnected() && this.name != null) {
            long st = System.currentTimeMillis();
            update();
            long time = 1000 - (System.currentTimeMillis() - st);
            if (time > 0) {
                Functions.sleep(time);
            }
        }
    }

    public void start() {
        Thread.startVirtualThread(() -> {
            Thread.currentThread().setName("Update player " + this.name);
            this.run();
        });
    }

    public void update() {
        if (this.beforeDispose || isBot) {
            return;
        }

        try {
            // Cập nhật các đối tượng liên quan tới item và cây
            if (zone != null || (!isPl() && zone == null)) {
                if (itemTime != null) {
                    itemTime.update();
                }
                if (magicTree != null) {
                    magicTree.update();
                }

                if (isPl() && zone != null
                        && zone.map.mapId == gender + 21
                        && (TaskService.gI().getIdTask(this) == ConstTask.TASK_0_0
                        || TaskService.gI().getIdTask(this) == ConstTask.TASK_0_1)) {
                    playerTask.taskMain.index = 2;
                    TaskService.gI().sendTaskMain(this);
                }
            }

            // Điều kiện update chính khi không ở home map hoặc là NPC
            if ((zone != null && !MapService.gI().isHome(zone.map.mapId)) || (!isPl() && zone == null)) {

                // Kiểm tra và kick player bị ban
                if (isPl() && iDMark != null && iDMark.isBan()
                        && Util.canDoWithTime(iDMark.getLastTimeBan(), 5000)) {
                    Client.gI().kickSession(session);
                    return;
                }

                // Cập nhật các thuộc tính player và pet
                updateEntities();

                // Quản lý Chibi
                handleChibi();

                // Hoạt động thường nhật
                if (isPl()) {
                    handleDailyActivities();
                }

                // EffectSkill đặc biệt
                handleEffectSkill();

                // Điều chỉnh điểm và skin
                handleSkinEffects();

                // Fix map & check player
                if (!isBoss && !isPet) {
                    checkPlayerInMap();
                }

                // Mabu Egg & Map phụ hỗ trợ Mabu
                handleMabu();

                // Cập nhật drop item, MajinBuu, SuperDivineWater
                if (dropItem != null) {
                    dropItem.update();
                }
                MajinBuuService.gI().update(this);
                SuperDivineWaterService.gI().update(this);

                // Kiểm tra đi tới tương lai
                handleGoToFuture();

                // Kick player idle
                handleIdleKick();
            }

        } catch (Exception e) {
            Logger.logException(Player.class, e, "Lỗi tại player: " + name);
        }
    }

// --- Các phương thức phụ trợ ---
    private void updateEntities() {
        if (nPoint != null) {
            nPoint.update();
        }
        if (fusion != null) {
            fusion.update();
        }
        if (effectSkill != null) {
            effectSkill.update();
        }
        if (mobMe != null) {
            mobMe.update();
        }
        if (effectSkin != null) {
            effectSkin.update();
        }
        if (pet != null) {
            pet.update();
        }
        if (newPet != null) {
            newPet.update();
        }
        if (itemTime != null) {
            itemTime.update();
        }
        if (satellite != null) {
            satellite.update();
        }
        if (clone != null) {
            clone.update();
        }
    }

    private void handleChibi() {
        if (!isPl() || isDie() || effectSkill == null) {
            return;
        }

        long now = System.currentTimeMillis();

        // ===== TẠO CHIBI (25 phút / 1 lần) =====
        if (!effectSkill.isChibi
                && Util.canDoWithTime(lastTimeChibi, 25 * 60 * 1000)
                && !MapService.gI().isMapBlackBallWar(zone.map.mapId)) {

            EffectSkillService.gI().setChibi(this, 10 * 60 * 1000); // tồn tại 10 phút
            lastTimeChibi = now; // ✅ SET NGAY KHI CHIBI XUẤT HIỆN
            return;
        }

        // ===== UPDATE CHIBI =====
        if (effectSkill.isChibi && Util.canDoWithTime(lastTimeUpdateChibi, 1000)) {
            switch (typeChibi) {
                case 1 -> { // +MP
                    nPoint.mp = Math.min(nPoint.mp + nPoint.mpMax / 10, nPoint.mpMax);
                    PlayerService.gI().sendInfoMp(this);
                }
                case 3 -> { // +HP
                    nPoint.hp = Math.min(nPoint.hp + nPoint.hpMax / 10, nPoint.hpMax);
                    PlayerService.gI().sendInfoHp(this);
                }
            }
            lastTimeUpdateChibi = now;
            return;
        }

        // ===== CHIBI VỪA BIẾN MẤT → GỬI LẠI LINH THÚ =====
//        if (!effectSkill.isChibi && lastTimeUpdateChibi != 0) {
//            lastTimeUpdateChibi = 0;
//
//            Item linhThu = inventory.itemsBody.get(13);
//            if (linhThu != null && linhThu.isNotNullItem()) {
//                short smallId = (short) (linhThu.template.iconID - 1);
//                Service.gI().sendPetFollow(this, smallId);
//            }
//        }
    }

    private void handleDailyActivities() {
        if (achievement != null) {
            achievement.done(ConstAchievement.HOAT_DONG_CHAM_CHI, 1000);
        }

        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (!(hour >= 22 && hour <= 23) && zone.map.mapId == 126) {
            ChangeMapService.gI().changeMapNonSpaceship(this, 19, 1000 + Util.nextInt(-100, 100), 360);
        }

        updateCSMM();
        TaskService.gI().sendUpdateCountSubTask(this);
        autoSendBadges();
        BadgesTaskService.updateDoneTask(this);
        sendTextTimeDaiLyGift();
        TranhNgoc.gI().update(this);

        if (clan != null) {
            ClanService.gI().checkDoneTaskJoinClan(clan);
        }
    }

    private void handleEffectSkill() {
        if (!isPl() || effectSkill == null || !effectSkill.isMabuHold) {
            return;
        }

        nPoint.subHP(nPoint.hpMax / 100);
        if (Util.isTrue(1, 10)) {
            Service.gI().chat(this, "Cứu tôi với");
        }
        PlayerService.gI().sendInfoHp(this);

        if (precentMabuHold > 15) {
            EffectSkillService.gI().removeMabuHold(this);
        }
        if (nPoint.hp <= 0) {
            EffectSkillService.gI().removeMabuHold(this);
            setDie();
        }
    }

    private void handleSkinEffects() {
        if (zone == null || effectSkin == null) {
            return;
        }

        if (effectSkin.xHPKI > 1 && !MapService.gI().isMapBlackBallWar(zone.map.mapId)) {
            effectSkin.xHPKI = 1;
            nPoint.calPoint();
            Service.gI().point(this);
        }

        if (effectSkin.xDame > 1 && !MapService.gI().isMapBlackBallWar(zone.map.mapId)) {
            effectSkin.xDame = 1;
            nPoint.calPoint();
            Service.gI().point(this);
        }
    }

    private void handleMabu() {
        if (zone != null && zone.map.mapId == gender + 21 && mabuEgg != null) {
            mabuEgg.sendMabuEgg();
        }

        if (isPhuHoMapMabu && zone != null && !MapService.gI().isMapMabu2H(zone.map.mapId)) {
            isPhuHoMapMabu = false;
            nPoint.calPoint();
            Service.gI().point(this);
            Service.gI().Send_Info_NV(this);
            Service.gI().Send_Caitrang(this);
        }
    }

    private void handleGoToFuture() {
        if (!isBoss && iDMark != null && iDMark.isGotoFuture()
                && Util.canDoWithTime(iDMark.getLastTimeGoToFuture(), 60_000)) {
            ChangeMapService.gI().changeMapBySpaceShip(this, 102, -1, Util.nextInt(60, 200));
            iDMark.setGotoFuture(false);
        }
    }

    private void handleIdleKick() {
        if (isPl() && location != null
                && location.lastTimeplayerMove < System.currentTimeMillis() - 5 * 60 * 60 * 1000) {
            Client.gI().kickSession(session);
        }
    }

    public long lastTimeSendTextTime;

    public void sendTextTimeDaiLyGift() {
        if (Util.canDoWithTime(lastTimeSendTextTime, 300000)) {
            if (DailyGiftService.checkDailyGift(this, ConstDailyGift.NHAN_BUA_MIEN_PHI)) {
                ItemTimeService.gI().sendTextTime(this, itemTime.TEXT_NHAN_BUA_MIEN_PHI, "Nhận ngẫu nhiên bùa 1h mỗi ngày tại Bà Hạt Mít ở vách núi", 30);
            }
            lastTimeSendTextTime = System.currentTimeMillis();
        }
    }

    public void updateCSMM() {
        minigame.LuckyNumber.LuckyNumber.players.forEach((g) -> {
            if (this.id == g.id) {
                LuckyNumberService.showNumberPlayer(this, LuckyNumberService.strNumber(this.id));
                ItemTimeService.gI().sendItemTime(this, 2295, LuckyNumberCost.timeGame);
            }
        });
    }

    public void autoSendBadges() {
        Iterator<BadgesData> iterator = dataBadges.iterator();
        while (iterator.hasNext()) {
            BadgesData data = iterator.next();
            if (System.currentTimeMillis() >= data.timeofUseBadges) {
                iterator.remove();
            } else if (data.isUse) {
                badges.idBadges = data.idBadGes;
            }
        }

        if (badges.idBadges != -1 && Util.canDoWithTime(badges.lastTimeSendBadges, 10000)) {
            Service.gI().sendBadgesPlayer(this, 5, badges.idBadges);
            badges.lastTimeSendBadges = System.currentTimeMillis();
            this.nPoint.update();
            Service.gI().point(this);
        }
    }

    //--------------------------------------------------------------------------
    /*
     * {380, 381, 382}: ht lưỡng long nhất thể xayda trái đất
     * {383, 384, 385}: ht porata xayda trái đất
     * {391, 392, 393}: ht namếc
     * {870, 871, 872}: ht c2 trái đất
     * {873, 874, 875}: ht c2 namếc
     * {867, 878, 869}: ht c2 xayda
     */
    private static final short[][] idOutfitFusion = {
        {380, 381, 382}, {383, 384, 385}, {391, 392, 393},
        {870, 871, 872}, {873, 874, 875}, {867, 868, 869},
        {870, 871, 872}, {873, 874, 875}, {867, 868, 869},
        {1758, 1759, 1760}, {1761, 1762, 1763}, {1755, 1756, 1757}};

    public static final short[][] idOutfitGod = {
        {-1, 472, 473}, {-1, 476, 477}, {-1, 474, 475}
    };

    public static final short[][][] idOutfitHalloween = {
        {
            {545, 548, 549}, {547, 548, 549}, {546, 548, 549}
        },
        {
            {760, 761, 762}, {760, 761, 762}, {760, 761, 762}
        },
        {
            {654, 655, 656}, {654, 655, 656}, {654, 655, 656}
        },
        {
            {651, 652, 653}, {651, 652, 653}, {651, 652, 653}
        },
        {
            {651, 652, 653}, {651, 652, 653}, {651, 652, 653}
        }
    };

    public static final short[][] idOutfitMafuba = {
        {1221, 1222, 1223}, {-1, -1, -1}, {1218, 1219, 1220}
    };

    public int getHat() {
        return -1;
    }

    public byte getAura() {
        if (inventory != null && inventory.itemsBody.size() > 5 && inventory.itemsBody.get(5).isNotNullItem()) {
            Short equippedItemId = inventory.itemsBody.get(5).template.id;
            if (equippedItemId == 1275) {
                return 0;
            }
        }
        if (!isPl() || this.Cards.isEmpty()) {
            return -1;
        }

//        byte auraPower = auraPower();
//        if (auraPower != -1) {
//            return auraPower;
//        }

        // AURA BIẾN HÌNH
        if (this.effectSkill != null) {
            if (this.effectSkill.isBienHinh) {
                return ConstPlayer.AURABIENHINH[this.gender][this.effectSkill.levelBienHinh - 1];
            }
            if (this.effectSkill.isSuper) {
                return idAuraSuper[gender][(playerSkill.getSkillbyId(gender == 0 ? 27 : gender == 1 ? 28 : 29).point - 1) - numUseSkill];
            }
        }

        for (Card card : this.Cards) {
            if (card != null) {
                int cardId = card.Id;
                int level = card.Level;
                byte auraId = 0;

                switch (cardId) {
                    case 956:
                        if (level == 2) {
                            auraId = 0;
                        }
                        break;
                    case 1142:
                        if (level == 2) {
                            auraId = 1;
                        }
                        break;
                    case 1901:
                        if (level == 1) {
                            auraId = 2;
                        } else if (level == 2) {
                            auraId = 3;
                        } else if (level == 3) {
                            auraId = 4;
                        }
                        break;
                    default:
                        RadarCard radarTemplate = RadarService.gI().RADAR_TEMPLATE.stream()
                                .filter(r -> r.Id == cardId)
                                .findFirst()
                                .orElse(null);
                        if (radarTemplate != null) {
                            auraId = (byte) radarTemplate.AuraId;
                        }
                }

                if (auraId > 0) {
                    return auraId;
                }
            }
        }
        return -1;
    }

    private void checkPlayerInMap() {
        if (this.zone != null && this.zone.map != null && this.zone.map.mapId == 186) {
            if (this.nPoint.power < 80_000_000_000L) {
                ChangeMapService.gI().changeMapNonSpaceship(this, 5, 915, 408);
                InventoryService.gI().sendItemBag(this);
                Service.gI().sendBigMessage(this, 1139, "Không đủ sức mạnh yêu cầu, ngươi quay về đi!!");
                return;
            }

            if (!this.itemTime.isUsevevang) {
                ChangeMapService.gI().changeMapNonSpaceship(this, 5, 915, 408);
                Service.gI().sendBigMessage(this, 1139,
                        "Ngươi không có vé hợp lệ, hãy quay về!");
                return;
            }

            long timeUsed = System.currentTimeMillis() - this.itemTime.lastTimevevang;
            long timeLeft = this.itemTime.totalTimeVeVang - timeUsed;

            if (timeLeft <= 0) {
                this.itemTime.isUsevevang = false;
                this.itemTime.lastTimevevang = 0;
                this.itemTime.totalTimeVeVang = 0;

                ChangeMapService.gI().changeMapNonSpaceship(this, 5, 915, 408);
                Service.gI().sendBigMessage(this, 1139,
                        "Hết thời gian riêng tư rồi, ngươi quay về đi!!");
            }
        }
    }

    public byte auraPower() {
        if (nPoint == null) {
            return -1;
        }

        long p = nPoint.power;
        if (p >= 180_000_000_000L) {
            return 84;
        }
        if (p >= 120_000_000_000L) {
            return 83;
        }
        if (p >= 110_000_000_000L) {
            return 82;
        }
        if (p >= 80_000_000_000L) {
            return 6;
        }
        if (p >= 20_000_000_000L) {
            return 0;
        }

        return -1;
    }

    public byte getEffFront() {
        if (this.inventory == null) {
            return -1;
        }
        if (this.inventory.itemsBody.isEmpty() || this.inventory.itemsBody.size() < 10) {
            return -1;
        }
        int levelAo = 0;
        Item.ItemOption optionLevelAo = null;
        int levelQuan = 0;
        Item.ItemOption optionLevelQuan = null;
        int levelGang = 0;
        Item.ItemOption optionLevelGang = null;
        int levelGiay = 0;
        Item.ItemOption optionLevelGiay = null;
        int levelNhan = 0;
        Item.ItemOption optionLevelNhan = null;
        Item itemAo = this.inventory.itemsBody.get(0);
        Item itemQuan = this.inventory.itemsBody.get(1);
        Item itemGang = this.inventory.itemsBody.get(2);
        Item itemGiay = this.inventory.itemsBody.get(3);
        Item itemNhan = this.inventory.itemsBody.get(4);
        for (Item.ItemOption io : itemAo.itemOptions) {
            if (io.optionTemplate.id == 72) {
                levelAo = io.param;
                optionLevelAo = io;
                break;
            }
        }
        for (Item.ItemOption io : itemQuan.itemOptions) {
            if (io.optionTemplate.id == 72) {
                levelQuan = io.param;
                optionLevelQuan = io;
                break;
            }
        }
        for (Item.ItemOption io : itemGang.itemOptions) {
            if (io.optionTemplate.id == 72) {
                levelGang = io.param;
                optionLevelGang = io;
                break;
            }
        }
        for (Item.ItemOption io : itemGiay.itemOptions) {
            if (io.optionTemplate.id == 72) {
                levelGiay = io.param;
                optionLevelGiay = io;
                break;
            }
        }
        for (Item.ItemOption io : itemNhan.itemOptions) {
            if (io.optionTemplate.id == 72) {
                levelNhan = io.param;
                optionLevelNhan = io;
                break;
            }
        }
        if (optionLevelAo != null && optionLevelQuan != null && optionLevelGang != null && optionLevelGiay != null && optionLevelNhan != null
                && levelAo >= 8 && levelQuan >= 8 && levelGang >= 8 && levelGiay >= 8 && levelNhan >= 8) {
            return 8;
        } else if (optionLevelAo != null && optionLevelQuan != null && optionLevelGang != null && optionLevelGiay != null && optionLevelNhan != null
                && levelAo >= 7 && levelQuan >= 7 && levelGang >= 7 && levelGiay >= 7 && levelNhan >= 7) {
            return 7;
        } else if (optionLevelAo != null && optionLevelQuan != null && optionLevelGang != null && optionLevelGiay != null && optionLevelNhan != null
                && levelAo >= 6 && levelQuan >= 6 && levelGang >= 6 && levelGiay >= 6 && levelNhan >= 6) {
            return 6;
        } else if (optionLevelAo != null && optionLevelQuan != null && optionLevelGang != null && optionLevelGiay != null && optionLevelNhan != null
                && levelAo >= 5 && levelQuan >= 5 && levelGang >= 5 && levelGiay >= 5 && levelNhan >= 5) {
            return 5;
        } else if (optionLevelAo != null && optionLevelQuan != null && optionLevelGang != null && optionLevelGiay != null && optionLevelNhan != null
                && levelAo >= 4 && levelQuan >= 4 && levelGang >= 4 && levelGiay >= 4 && levelNhan >= 4) {
            return 4;
        } else {
            return -1;
        }
    }

    private static final short[][] idOutFitSuperEarth = {
        {1436, 1437, 1438}, // level 1

        {1436, 1437, 1438}, // level 2

        {1442, 1437, 1438}, // level 3

        {1440, 1437, 1438}, // level 4

        {1439, 1437, 1438}, // level 5

        {1441, 1437, 1438}, // level 6 
    };

    private static final short[][] idOutFitSuperNamec = {
        {1430, 1431, 1432}, // level 1

        {1443, 1431, 1432}, // level 2

        {1444, 1431, 1432}, // level 3

        {1445, 1431, 1432}, // level 4

        {1446, 1431, 1432}, // level 5

        {1447, 1431, 1432}, // level 6 
    };

    private static final short[][] idOutFitSuperSaiyan = {
        {1433, 1434, 1435}, // level 1

        {1433, 1434, 1435}, // level 2

        {1448, 1434, 1435}, // level 3

        {1449, 1434, 1435}, // level 4

        {1450, 1434, 1435}, // level 5

        {1451, 1434, 1435}, // level 6 
    };

    private static final byte[][] idAuraSuper = {
        {20, 21, 22, 23, 24, 25},// Trái đất

        {26, 27, 28, 29, 30, 31},// namec

        {32, 33, 34, 35, 36, 37},// xayda
    };

    public short getHeadThuCung() {
        if (this.isPl() && this.inventory != null && this.inventory.itemsBody.size() > 7 && this.inventory.itemsBody.get(7).isNotNullItem()) {
            return (short) (this.inventory.itemsBody.get(7).template.head);
        }
        return -1;
    }

    public short getBodyThuCung() {
        if (this.isPl() && this.inventory != null && this.inventory.itemsBody.size() > 7 && this.inventory.itemsBody.get(7).isNotNullItem()) {
            return (short) (this.inventory.itemsBody.get(7).template.body);
        }
        return -1;
    }

    public short getLegThuCung() {
        if (this.isPl() && this.inventory != null && this.inventory.itemsBody.size() > 7 && this.inventory.itemsBody.get(7).isNotNullItem()) {
            return (short) (this.inventory.itemsBody.get(7).template.leg);
        }
        return -1;
    }

    public short getHeadSuper() {
        switch (gender) {
            case 0:
                return idOutFitSuperEarth[(playerSkill.getSkillbyId(27).point - 1) - numUseSkill][0];
            case 1:
                return idOutFitSuperNamec[(playerSkill.getSkillbyId(28).point - 1) - numUseSkill][0];
            case 2:
                return idOutFitSuperSaiyan[(playerSkill.getSkillbyId(29).point - 1) - numUseSkill][0];
        }
        return -1;
    }

    public short getBodySuper() {
        switch (gender) {
            case 0:
                return idOutFitSuperEarth[(playerSkill.getSkillbyId(27).point - 1) - numUseSkill][1];
            case 1:
                return idOutFitSuperNamec[(playerSkill.getSkillbyId(28).point - 1) - numUseSkill][1];
            case 2:
                return idOutFitSuperSaiyan[(playerSkill.getSkillbyId(29).point - 1) - numUseSkill][1];
        }
        return -1;
    }

    public short getLegSuper() {
        switch (gender) {
            case 0:
                return idOutFitSuperEarth[(playerSkill.getSkillbyId(27).point - 1) - numUseSkill][2];
            case 1:
                return idOutFitSuperNamec[(playerSkill.getSkillbyId(28).point - 1) - numUseSkill][2];
            case 2:
                return idOutFitSuperSaiyan[(playerSkill.getSkillbyId(29).point - 1) - numUseSkill][2];
        }
        return -1;
    }

    public short getHead() {

        if (this.itemTime != null && this.itemTime.isUseKhauTrang) {
            if (this.gender == 0) {
                return 775;
            } else if (this.gender == 1) {
                return 777;
            } else if (this.gender == 2) {
                return 776;
            }
        }

        // Các trạng thái hóa thân Halloween
        if (this.itemTime != null) {
            if (this.itemTime.isBoXuong) {
                if (this.gender == 0) {
                    return 545;
                } else if (this.gender == 1) {
                    return 547;
                } else if (this.gender == 2) {
                    return 546;
                }
            }
            if (this.itemTime.isMaTroi) {
                return 651; // tất cả giới tính dùng chung
            }
            if (this.itemTime.isDoiNhi) {
                return 654; // tất cả giới tính dùng chung
            }
            if (this.itemTime.isBiMa) {
                return 760; // tất cả giới tính dùng chung
            }
        }
        if (this.itemTime.iscarot) {
            return 406; // tất cả giới tính dùng chung
        }

        if (effectSkill != null && effectSkill.isBienHinh) {
            return (short) ConstPlayer.HEADBIENHINH[this.gender][effectSkill.levelBienHinh - 1];
        }
        if (this.isPl() && this.pet != null && this.fusion.typeFusion == ConstPlayer.HOP_THE_PORATA || this.fusion.typeFusion == ConstPlayer.HOP_THE_PORATA2 || this.fusion.typeFusion == ConstPlayer.HOP_THE_PORATA3) {
            Item item = inventory.itemsBody.get(5);
            Item petItem = pet.inventory.itemsBody.get(5);

            boolean hasItem1 = item.isNotNullItem() && (item.template.id == 1693 || item.template.id == 1553);
            boolean hasItem2 = petItem.isNotNullItem() && (petItem.template.id == 1693 || petItem.template.id == 1553);
            boolean sameItem = item.isNotNullItem() && petItem.isNotNullItem() && item.template.id == petItem.template.id;
            if (hasItem1 && hasItem2 && !sameItem) {
                return 1578;
            }
        }

        if (effectSkill != null && effectSkill.isSuper) {
            return getHeadSuper();
        } else if (effectSkill != null && effectSkill.isBinh) {
            return idOutfitMafuba[effectSkill.typeBinh][0];
        }
        if (effectSkill != null && effectSkill.isStone) {
            return 454;
        }
        if (effectSkill != null && effectSkill.isMonkey) {
            return (short) ConstPlayer.HEADMONKEY[effectSkill.levelMonkey - 1];
        } else if (effectSkill != null && effectSkill.isSocola) {
            return 412;
        } else if (effectSkill != null && effectSkill.isSoHai) {
            return 882;
        } else if (effectSkill != null && effectSkill.isBang) {
            return 1210;
        } else if (fusion != null && fusion.typeFusion != ConstPlayer.NON_FUSION) {
            if (fusion.typeFusion == ConstPlayer.LUONG_LONG_NHAT_THE) {
                return idOutfitFusion[this.gender == ConstPlayer.NAMEC ? 2 : 0][0];
            } else if (fusion.typeFusion == ConstPlayer.HOP_THE_PORATA) {
                return idOutfitFusion[this.gender == ConstPlayer.NAMEC ? 2 : 1][0];
            } else if (fusion.typeFusion == ConstPlayer.HOP_THE_PORATA2) {
                return idOutfitFusion[6 + this.gender][0];
            } else if (fusion.typeFusion == ConstPlayer.HOP_THE_PORATA3) {
                return idOutfitFusion[9 + this.gender][0];
            }
        } else if (inventory != null && inventory.itemsBody.get(5).isNotNullItem()) {
            int headId = inventory.itemsBody.get(5).template.head;
            if (headId != -1) {
                return (short) headId;
            }
        }
        return this.head;
    }

    public short getBody() {
        if (this.itemTime != null) {
            if (this.itemTime.isBoXuong) {
                return 548;
            }
            if (this.itemTime.isMaTroi) {
                return 652;
            }
            if (this.itemTime.isDoiNhi) {
                return 655;
            }
            if (this.itemTime.isBiMa) {
                return 761;
            }
        }
        if (this.itemTime.iscarot) {
            return 407; // tất cả giới tính dùng chung
        }
        if (effectSkill != null && effectSkill.isBienHinh) {
            return (short) ConstPlayer.BODYBIENHINH[this.gender];
        }
        if (this.isPl() && this.pet != null && this.fusion.typeFusion == ConstPlayer.HOP_THE_PORATA || this.fusion.typeFusion == ConstPlayer.HOP_THE_PORATA2 || this.fusion.typeFusion == ConstPlayer.HOP_THE_PORATA3) {
            Item item = inventory.itemsBody.get(5);
            Item petItem = pet.inventory.itemsBody.get(5);

            boolean hasItem1 = item.isNotNullItem() && (item.template.id == 1693 || item.template.id == 1553);
            boolean hasItem2 = petItem.isNotNullItem() && (petItem.template.id == 1693 || petItem.template.id == 1553);
            boolean sameItem = item.isNotNullItem() && petItem.isNotNullItem() && item.template.id == petItem.template.id;
            if (hasItem1 && hasItem2 && !sameItem) {
                return 1581;
            }
        }
        if (effectSkill != null && effectSkill.isSuper) {
            return getBodySuper();
        } else if (effectSkill != null && effectSkill.isBinh) {
            return idOutfitMafuba[effectSkill.typeBinh][1];
        }
        if (effectSkill != null && effectSkill.isStone) {
            return 455;
        }
        if (effectSkill != null && effectSkill.isMonkey) {
            return 193;
        } else if (effectSkill != null && effectSkill.isSoHai) {
            return 883;
        } else if (effectSkill != null && effectSkill.isBang) {
            return 1211;
        } else if (effectSkill != null && effectSkill.isSocola) {
            return 413;
        } else if (isPhuHoMapMabu && fusion != null && fusion.typeFusion == ConstPlayer.NON_FUSION) {
            return idOutfitGod[this.gender][1];
        } else if (fusion != null && fusion.typeFusion != ConstPlayer.NON_FUSION) {
            if (fusion.typeFusion == ConstPlayer.LUONG_LONG_NHAT_THE) {
                return idOutfitFusion[this.gender == ConstPlayer.NAMEC ? 2 : 0][1];
            } else if (fusion.typeFusion == ConstPlayer.HOP_THE_PORATA) {
                return idOutfitFusion[this.gender == ConstPlayer.NAMEC ? 2 : 1][1];
            } else if (fusion.typeFusion == ConstPlayer.HOP_THE_PORATA2) {
                return idOutfitFusion[6 + this.gender][1];
            } else if (fusion.typeFusion == ConstPlayer.HOP_THE_PORATA3) {
                return idOutfitFusion[9 + this.gender][1];
            }
        } else if (inventory != null && inventory.itemsBody.get(5).isNotNullItem()) {
            int body = inventory.itemsBody.get(5).template.body;
            if (body != -1) {
                return (short) body;
            }
        }
        if (inventory != null && inventory.itemsBody.get(0).isNotNullItem()) {
            return inventory.itemsBody.get(0).template.part;
        }
        return (short) (gender == ConstPlayer.NAMEC ? 59 : 57);
    }

    public short getLeg() {
        if (this.itemTime != null) {
            if (this.itemTime.isBoXuong) {
                return 549;
            }
            if (this.itemTime.isMaTroi) {
                return 653;
            }
            if (this.itemTime.isDoiNhi) {
                return 656;
            }
            if (this.itemTime.isBiMa) {
                return 762;
            }
        }
        if (this.itemTime.iscarot) {
            return 408; // tất cả giới tính dùng chung
        }

        if (effectSkill != null && effectSkill.isBienHinh) {
            return (short) ConstPlayer.LEGBIENHINH[this.gender];
        }
        if (this.isPl() && this.pet != null && this.fusion.typeFusion == ConstPlayer.HOP_THE_PORATA || this.fusion.typeFusion == ConstPlayer.HOP_THE_PORATA2 || this.fusion.typeFusion == ConstPlayer.HOP_THE_PORATA3) {
            Item item = inventory.itemsBody.get(5);
            Item petItem = pet.inventory.itemsBody.get(5);

            boolean hasItem1 = item.isNotNullItem() && (item.template.id == 1693 || item.template.id == 1553);
            boolean hasItem2 = petItem.isNotNullItem() && (petItem.template.id == 1693 || petItem.template.id == 1553);
            boolean sameItem = item.isNotNullItem() && petItem.isNotNullItem() && item.template.id == petItem.template.id;
            if (hasItem1 && hasItem2 && !sameItem) {
                return 1582;
            }
        }
        if (effectSkill != null && effectSkill.isSuper) {
            return getLegSuper();
        } else if (effectSkill != null && effectSkill.isBinh) {
            return idOutfitMafuba[effectSkill.typeBinh][2];
        }
        if (effectSkill != null && effectSkill.isStone) {
            return 456;
        }
        if (effectSkill != null && effectSkill.isMonkey) {
            return 194;
        } else if (effectSkill != null && effectSkill.isSoHai) {
            return 884;
        } else if (effectSkill != null && effectSkill.isBang) {
            return 1212;
        } else if (effectSkill != null && effectSkill.isSocola) {
            return 414;
        } else if (isPhuHoMapMabu && fusion != null && fusion.typeFusion == ConstPlayer.NON_FUSION) {
            return idOutfitGod[this.gender][2];
        } else if (fusion != null && fusion.typeFusion != ConstPlayer.NON_FUSION) {
            if (fusion.typeFusion == ConstPlayer.LUONG_LONG_NHAT_THE) {
                return idOutfitFusion[this.gender == ConstPlayer.NAMEC ? 2 : 0][2];
            } else if (fusion.typeFusion == ConstPlayer.HOP_THE_PORATA) {
                return idOutfitFusion[this.gender == ConstPlayer.NAMEC ? 2 : 1][2];
            } else if (fusion.typeFusion == ConstPlayer.HOP_THE_PORATA2) {
                return idOutfitFusion[6 + this.gender][2];
            } else if (fusion.typeFusion == ConstPlayer.HOP_THE_PORATA3) {
                return idOutfitFusion[9 + this.gender][2];
            }
        } else if (inventory != null && inventory.itemsBody.get(5).isNotNullItem()) {
            int leg = inventory.itemsBody.get(5).template.leg;
            if (leg != -1) {
                return (short) leg;
            }
        }
        if (inventory != null && inventory.itemsBody.get(1).isNotNullItem()) {
            return inventory.itemsBody.get(1).template.part;
        }
        return (short) (gender == 1 ? 60 : 58);
    }

    public boolean checkSkinFusion() {
        if (inventory != null && inventory.itemsBody.get(5).isNotNullItem()) {
            Short idct = inventory.itemsBody.get(5).template.id;
            if (idct >= 601 && idct <= 603 || idct >= 639 && idct <= 641) {
                return true;
            }
        }
        return false;
    }

    public short getFlagBag() {
        if (this.iDMark.isHoldBlackBall()) {
            return 31;
        } else if (this.idNRNM >= 353 && this.idNRNM <= 359) {
            return 30;
        }
        if (TaskService.gI().getIdTask(this) == ConstTask.TASK_3_2) {
            return 28;
        }
        if (this.inventory.itemsBody.size() >= 11) {
            if (this.inventory.itemsBody.get(8).isNotNullItem()) {
                return this.inventory.itemsBody.get(8).template.part;
            }
        }
//        if (this.isPet && this.inventory.itemsBody.size() >= 8) {
//            if (this.inventory.itemsBody.get(7).isNotNullItem()) {
//                return this.inventory.itemsBody.get(7).template.part;
//            }
//        }
        if (this.clan != null) {
            return (short) this.clan.imgId;
        }
        return -1;
    }

    public short getMount() {
        if (this.inventory.itemsBody.isEmpty() || this.inventory.itemsBody.size() < 10) {
            return -1;
        }
        Item item = this.inventory.itemsBody.get(9);
        if (!item.isNotNullItem()) {
            return -1;
        }
        if (item.template.type == 24) {
            if (item.template.gender == 3 || item.template.gender == this.gender) {
                return item.template.id;
            } else {
                return -1;
            }
        } else {
            if (item.template.id < 500) {
                return item.template.id;
            } else {
                return (short) DataGame.MAP_MOUNT_NUM.get(item.template.id);
            }
        }
    }

    private void handlePetAttackByOwner(Player plAtt) {
        try {
            // Kiểm tra điều kiện hợp lệ
            if (plAtt == null || !(this instanceof Pet) || this.isClone || this.itemTime == null) {
                return;
            }

            Pet pet = (Pet) this;

            // Chỉ khi người tấn công là chủ của pet
            if (pet.master != null && pet.master.id == plAtt.id) {

                // Nếu pet đang có 1 trong 4 trạng thái thì không biến thêm
                if (this.itemTime.isBoXuong || this.itemTime.isMaTroi
                        || this.itemTime.isDoiNhi || this.itemTime.isBiMa) {
                    return;
                }

                // Random 1 trong 4 loại hóa thân
                int type = Util.nextInt(1, 4);
                int iconId = 0;
                String chatMsg = "";
                int duration = 1800000; // 30 phút mặc định

                switch (type) {
                    case 1: // Bộ Xương
                        this.itemTime.isBoXuong = true;
                        this.itemTime.lastTimeBoXuong = System.currentTimeMillis();
                        iconId = 5101;
                        chatMsg = "Á á á! Cơ thể ta chỉ còn bộ xương khô!";
                        break;

                    case 2: // Ma Trơi
                        this.itemTime.isMaTroi = true;
                        this.itemTime.lastTimeMaTroi = System.currentTimeMillis();
                        iconId = 6091;
                        chatMsg = "Chủ nhân... ta bay rồi... ta là Ma Trơi!";
                        break;

                    case 3: // Dơi Nhí
                        this.itemTime.isDoiNhi = true;
                        this.itemTime.lastTimeDoiNhi = System.currentTimeMillis();
                        iconId = 6094;
                        chatMsg = "Bay lên! Dơi Nhí xuất hiện!";
                        break;

                    case 4: // Bị Ma
                        this.itemTime.isBiMa = true;
                        this.itemTime.lastTimeBiMa = System.currentTimeMillis();
                        iconId = 7057;
                        chatMsg = "Huhuhu... Chủ nhân ơi, sao lại hóa ta thành ma thế này!";
                        break;
                }

                // Gửi icon + chat
                ItemTimeService.gI().sendItemTime(this, iconId, duration / 1000);
                Service.gI().chat(this, chatMsg);

                // Cập nhật ngoại hình
                Service.gI().Send_Caitrang(this);
                Service.gI().point(this);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //--------------------------------------------------------------------------
    public long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (plAtt != null && !plAtt.equals(this)) {
                setTemporaryEnemies(plAtt);
            }
            if (this.isBattu) {
                return 0;
            }

            handlePetAttackByOwner(plAtt);

            if (plAtt != null && plAtt.playerSkill.skillSelect != null && !plAtt.isBoss && MapService.gI().isMapMaBu(this.zone.map.mapId)) {
                switch (plAtt.playerSkill.skillSelect.template.id) {
                    case Skill.KAMEJOKO, Skill.MASENKO, Skill.ANTOMIC, Skill.DRAGON, Skill.DEMON, Skill.GALICK, Skill.LIEN_HOAN, Skill.KAIOKEN ->
                        damage = Util.maxIntValue(damage > this.nPoint.hpMax / 20 ? this.nPoint.hpMax / 20 : damage);
                }
            }
            if (plAtt != null && plAtt.isBoss) {
                this.effectSkin.isVoHinh = false;
                this.effectSkin.lastTimeVoHinh = System.currentTimeMillis();
            }
            if (plAtt != null && plAtt.effectSkill != null && plAtt.effectSkill.isBinh
                    && !Util.canDoWithTime(plAtt.effectSkill.lastTimeUpBinh, 3000)) {
                return 0;
            }
            if (plAtt != null && plAtt.isPl() && this.maBuHold != null && this.zone != null && this.zone.map.mapId == 128) {
                this.precentMabuHold++;
                damage = 1;
            }
            if (plAtt != null && this.nPoint.islinhthuydanhbac) {
                Service.gI().sendThongBao(plAtt, "Không thể tấn công! Vì người chơi này đã nạp lần đầu!");
                return 0;
            }
            if (plAtt != null && plAtt.idNRNM != -1 && (this.isBoss || this.isNewPet)) {
                return 1;
            }
            if (plAtt != null && (plAtt.idNRNM != -1 || this.idNRNM != -1) && plAtt.clan != null && this.clan != null && plAtt.clan == this.clan) {
                Service.gI().chatJustForMe(plAtt, this, "Ê cùng bang mà");
                return 0;
            }
            if (!Util.canDoWithTime(this.lastTimeRevived, 1500)) {
                return 0;
            }

            if (plAtt != null && plAtt.playerSkill.skillSelect != null) {
                switch (plAtt.playerSkill.skillSelect.template.id) {
                    case Skill.KAMEJOKO, Skill.MASENKO, Skill.ANTOMIC -> {
                        if (this.nPoint.voHieuChuong > 0) {
                            nro.services.PlayerService.gI().hoiPhuc(this, 0, Util.maxIntValue(damage * this.nPoint.voHieuChuong / 100));
                            return 0;
                        }
                    }

                }
            }

            int tlGiap = this.nPoint.tlGiap;
            int tlNeDon = this.nPoint.tlNeDon;

            if (plAtt != null && !isMobAttack && plAtt.playerSkill.skillSelect != null) {
                switch (plAtt.playerSkill.skillSelect.template.id) {
                    case Skill.KAMEJOKO, Skill.MASENKO, Skill.ANTOMIC, Skill.DRAGON, Skill.DEMON, Skill.GALICK, Skill.LIEN_HOAN, Skill.KAIOKEN, Skill.QUA_CAU_KENH_KHI, Skill.MAKANKOSAPPO, Skill.DICH_CHUYEN_TUC_THOI ->
                        tlNeDon -= plAtt.nPoint.tlchinhxac;
                    default ->
                        tlNeDon = 0;
                }

                switch (plAtt.playerSkill.skillSelect.template.id) {
                    case Skill.KAMEJOKO, Skill.MASENKO, Skill.ANTOMIC -> {
                        if (tlGiap - plAtt.nPoint.tlxgc >= 0) {
                            tlGiap -= plAtt.nPoint.tlxgc;
                        } else {
                            tlGiap = 0;
                        }
                    }
                    case Skill.DRAGON, Skill.DEMON, Skill.GALICK, Skill.LIEN_HOAN, Skill.KAIOKEN -> {
                        if (tlGiap - plAtt.nPoint.tlxgcc >= 0) {
                            tlGiap -= plAtt.nPoint.tlxgcc;
                        } else {
                            tlGiap = 0;
                        }
                    }
                }
            }

            if (piercing) {
                tlGiap = 0;
            }

            if (tlNeDon > 90) {
                tlNeDon = 90;
            }
            if (tlGiap > 86) {
                tlGiap = 86;
            }

            if (Util.isTrue(tlNeDon, 100)) {
                return 0;
            }

            damage -= Util.maxIntValue((damage / 100) * tlGiap);

            if (!piercing) {
                damage = this.nPoint.subDameInjureWithDeff(damage);
            }

            boolean isUseGX = false;
            if (!piercing && plAtt != null && plAtt.playerSkill.skillSelect != null) {
                switch (plAtt.playerSkill.skillSelect.template.id) {
                    case Skill.KAMEJOKO, Skill.MASENKO, Skill.ANTOMIC, Skill.DRAGON, Skill.DEMON, Skill.GALICK, Skill.LIEN_HOAN, Skill.KAIOKEN, Skill.QUA_CAU_KENH_KHI, Skill.MAKANKOSAPPO, Skill.DICH_CHUYEN_TUC_THOI ->
                        isUseGX = true;
                }
            }
            if ((isUseGX || isMobAttack) && this.itemTime != null) {
                if (this.itemTime.isUseGiapXen && !this.itemTime.isUseGiapXen2) {
                    damage /= 2;
                }
                if (this.itemTime.isUseGiapXen2) {
                    damage = damage / 100 * 40;
                }
            }

            if (!piercing && effectSkill.isShielding && !isMobAttack) {
                if (this.iDMark != null) {
                    this.iDMark.setDamePST(Util.maxIntValue(damage));
                }
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = 1;
                if (MapService.gI().isMapPhoBan(this.zone.map.mapId)) {
                    damage = 10;
                }
            }
//            damage = Math.min(damage, 2_000_000_000L); // Giới hạn damage không vượt quá 2 tỷ
            if (!piercing && plAtt == null && isMobAttack && (this.charms.tdBatTu > System.currentTimeMillis()) && damage >= this.nPoint.hp) {
                damage = this.nPoint.hp - 1;
            }

            if (isMobAttack && this.itemTime.isMaTroi && damage >= this.nPoint.hp) {
                damage = (this.nPoint.hp - 1);
            }
            if (isMobAttack && this.itemTime.isBoXuong && damage >= this.nPoint.hp) {
                damage = (this.nPoint.hp - 1);
            }
            if (isMobAttack && this.itemTime.isDoiNhi && damage >= this.nPoint.hp) {
                damage = (this.nPoint.hp - 1);
            }
            if (isMobAttack && this.itemTime.isBiMa && damage >= this.nPoint.hp) {
                damage = (this.nPoint.hp - 1);
            }
            if (this.zone.map.mapId == 129) {
                if (damage >= this.nPoint.hp) {
                    this.lostByDeath = true;
                    The23rdMartialArtCongress mc = The23rdMartialArtCongressManager.gI().getMC(zone);
                    if (mc != null) {
                        mc.die();
                    }
                    return 0;
                }
            }
            if (this.zone.map.mapId == 51) {
                this.totalDamageTaken += damage;
            }
            this.nPoint.subHP(Util.maxIntValue(damage));
            if ((plAtt != null || isMobAttack) && isDie() && !isBoss && !isNewPet && !isNewPet1) {
                if (Util.isTrue(this.nPoint.tlBom, 100)) {
                    setBom(plAtt);
                } else {
                    setDie(plAtt);
                }
            }

            return damage;
        } else {
            return 0;
        }
    }

    public void setTemporaryEnemies(Player pl) {
        if (!temporaryEnemies.contains(pl)) {
            temporaryEnemies.add(pl);
        }
    }

    protected void setBom(Player plAtt) {
        setDie(plAtt);
        SkillService.gI().sendPlayerPrepareBom(plAtt, 2000);
        //Service.gI().callClone(this);
    }

    public void kill(Player pl) {
        pl.injured(this, Util.maxIntValue(pl.nPoint.hpMax), false, false);
        PlayerService.gI().sendInfoHpMpMoney(this);
        Service.gI().Send_Info_NV(this);
    }

    public void setDie() {
        this.setDie(null);
    }

    protected void setDie(Player plAtt) {
        TaskService.gI().checkDoneTaskKillPlayer(plAtt);
        if (this.isPl()) {
            long vangtru = this.nPoint.power / 1000000;
            if (vangtru > 32000) {
                vangtru = 32000;
            }

            int vang = (int) vangtru - Util.nextInt(10, 100);

            if (this.inventory.gold >= vang && vang >= 1) {
                this.inventory.gold -= vang;
                Service.gI().sendMoney(this);
                vang = vang * 95 / 100;
                if (vang < 10000) {
                    Service.gI().dropItemMap(this.zone, new ItemMap(zone, 189, vang, this.location.x, this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), this.id));
                } else if (vang < 20000) {
                    Service.gI().dropItemMap(this.zone, new ItemMap(zone, 188, vang, this.location.x, this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), this.id));
                } else {
                    Service.gI().dropItemMap(this.zone, new ItemMap(zone, 190, vang, this.location.x, this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), this.id));
                }
            }
        }

        //xóa phù
        if (this.effectSkin.xHPKI > 1) {
            this.effectSkin.xHPKI = 1;
            Service.gI().point(this);
        }
        if (this.effectSkin.xDame > 1) {
            this.effectSkin.xDame = 1;
            Service.gI().point(this);
        }
        if (this.clone != null) {
            this.clone.setDie(plAtt);
        }
        //xóa tụ skill đặc biệt
        this.playerSkill.prepareQCKK = false;
        this.playerSkill.prepareLaze = false;
        this.playerSkill.prepareTuSat = false;
        //xóa hiệu ứng skill
        this.effectSkill.removeSkillEffectWhenDie();
        //
        nPoint.setHp(Util.maxIntValue(0));
        nPoint.setMp(Util.maxIntValue(0));
        //xóa trứng
        if (this.mobMe != null) {
            this.mobMe.mobMeDie();
            this.mobMe.dispose();
            this.mobMe = null;
        }
        Service.gI().charDie(this);
        //add kẻ thù
        if (!this.isPet && !this.isNewPet && !isBot && !this.isClone && !this.isNewPet1 && !this.isBoss && plAtt != null && !plAtt.isPet && !plAtt.isNewPet && !plAtt.isNewPet1 && !plAtt.isBoss) {
            if (!plAtt.itemTime.isUseAnDanh) {
                FriendAndEnemyService.gI().addEnemy(this, plAtt);
            }
        }
        //kết thúc pk

        this.typePk = 0;

        if (this.pvp != null && this.zone.map.mapId != 140) {
            this.pvp.lose(this, TYPE_LOSE_PVP.DEAD);
        }

        BlackBallWarService.gI().dropBlackBall(this);
        NgocRongNamecService.gI().dropNamekBall(this);
        if (isHoldNamecBallTranhDoat) {
            TranhNgocService.getInstance().dropBall(this, (byte) -1);
            TranhNgocService.getInstance().sendUpdateLift(this);
        }
    }

    //--------------------------------------------------------------------------
    public void setClanMember() {
        if (this.clanMember != null) {
            this.clanMember.powerPoint = this.nPoint.power;
            this.clanMember.head = this.getHead();
            this.clanMember.body = this.getBody();
            this.clanMember.leg = this.getLeg();
        }
    }

    public boolean isAdmin() {
        return this.session != null && this.session.isAdmin;
    }

    public void thuhoivp() {
        // Xử lý itemsBag
        Iterator<Item> bagIterator = inventory.itemsBag.iterator();
        while (bagIterator.hasNext()) {
            Item item = bagIterator.next();
            if (item.isNotNullItem() && item.itemOptions != null && !item.itemOptions.isEmpty()) {
                for (Item.ItemOption io : item.itemOptions) {
                    if (io.optionTemplate.id == 14 && io.param > 20) {
                        bagIterator.remove();
                        InventoryService.gI().removeItemBag(this, item);
                        InventoryService.gI().sendItemBag(this);
                        Service.gI().sendThongBao(this, "Đã thu hồi Vật phẩm vì gây lỗi game!");
                        break;  // Dừng vòng lặp khi đã xóa phần tử
                    }
                }
            }
        }

        // Xử lý itemsBody
        Iterator<Item> bodyIterator = inventory.itemsBody.iterator();
        while (bodyIterator.hasNext()) {
            Item item = bodyIterator.next();
            if (item.isNotNullItem() && item.itemOptions != null && !item.itemOptions.isEmpty()) {
                for (Item.ItemOption io : item.itemOptions) {
                    if (io.optionTemplate.id == 14 && io.param > 20) {
                        bodyIterator.remove();
                        InventoryService.gI().removeItem(inventory.itemsBody, item);
                        InventoryService.gI().sendItemBody(this);
                        Service.gI().sendThongBao(this, "Đã thu hồi Vật phẩm vì gây lỗi game!");
                        break;
                    }
                }
            }
        }

        // Xử lý itemsBox
        Iterator<Item> boxIterator = inventory.itemsBox.iterator();
        while (boxIterator.hasNext()) {
            Item item = boxIterator.next();
            if (item.isNotNullItem() && item.itemOptions != null && !item.itemOptions.isEmpty()) {
                for (Item.ItemOption io : item.itemOptions) {
                    if (io.optionTemplate.id == 14 && io.param > 20) {
                        boxIterator.remove();
                        InventoryService.gI().removeItem(inventory.itemsBox, item);
                        InventoryService.gI().sendItemBox(this);
                        Service.gI().sendThongBao(this, "Đã thu hồi Vật phẩm vì gây lỗi game!");
                        break;
                    }
                }
            }
        }

        // Xử lý pet.itemsBody
        if (pet != null) {
            Iterator<Item> petBodyIterator = pet.inventory.itemsBody.iterator();
            while (petBodyIterator.hasNext()) {
                Item item = petBodyIterator.next();
                if (item.isNotNullItem() && item.itemOptions != null && !item.itemOptions.isEmpty()) {
                    for (Item.ItemOption io : item.itemOptions) {
                        if (io.optionTemplate.id == 14 && io.param > 20) {
                            petBodyIterator.remove();
                            InventoryService.gI().removeItem(pet.inventory.itemsBody, item);
                            InventoryService.gI().sendItemBag(pet);
                            Service.gI().sendThongBao(this, "Đã thu hồi Vật phẩm vì gây lỗi game!");
                            break;
                        }
                    }
                }
            }
        }
    }

    public void setJustRevivaled() {
        this.justRevived = true;
        this.lastTimeRevived = System.currentTimeMillis();
    }

    public boolean actived() {
        return (this.isPl() && this.session != null && this.session.actived) || (this.isPet && !this.isClone && ((Pet) this).master.session != null && ((Pet) this).master.session.actived);
    }

//    public void sendNewPet() {
//        if (isPl() && inventory != null && inventory.itemsBody.get(7) != null) {
//            Item it = inventory.itemsBody.get(7);
//            if (it != null && it.isNotNullItem() && newPet == null) {
//                switch (it.template.id) {
//                    case 942 -> {
//                        PetService.Pet2(this, 966, 967, 968);
//                        Service.gI().point(this);
//                    }
//                    case 943 -> {
//                        PetService.Pet2(this, 969, 970, 971);
//                        Service.gI().point(this);
//                    }
//                    case 944 -> {
//                        PetService.Pet2(this, 972, 973, 974);
//                        Service.gI().point(this);
//                    }
//                    case 967 -> {
//                        PetService.Pet2(this, 1050, 1051, 1052);
//                        Service.gI().point(this);
//                    }
//                    case 968 -> {
//                        PetService.Pet2(this, 1183, 1184, 1185);
//                        Service.gI().point(this);
//                    }
//                }
//            }
//        }
//    }
    private void fixBlackBallWar() {
        int x = this.location.x;
        int y = this.location.y;
        switch (this.zone.map.mapId) {
            case 85, 86, 87, 88, 89, 90, 91 -> {
                if (this.isPl()) {
                    if (x < 24 || x > this.zone.map.mapWidth - 24 || y < 0 || y > this.zone.map.mapHeight - 24) {
                        if (MapService.gI().getWaypointPlayerIn(this) == null) {
                            Service.gI().resetPoint(this, x, this.zone.map.yPhysicInTop(this.location.x, 100));
                            this.nPoint.hp -= this.nPoint.hpMax / 10;
                            PlayerService.gI().sendInfoHp(this);
                            return;
                        }
                    }
                    int yTop = this.zone.map.yPhysicInTop(this.location.x, this.location.y);
                    if (yTop >= this.zone.map.mapHeight - 24) {
                        Service.gI().resetPoint(this, x, this.zone.map.yPhysicInTop(this.location.x, 100));
                        this.nPoint.hp -= this.nPoint.hpMax / 10;
                        PlayerService.gI().sendInfoHp(this);
                    }
                }
            }
        }
    }

    public void move(int _toX, int _toY) {
        if (_toX != this.location.x) {
            this.location.x = _toX;
        }
        if (_toY != this.location.y) {
            this.location.y = _toY;
        }
        MapService.gI().sendPlayerMove(this);
    }

    public long lastTimeAttack;

    public boolean UseLastTimeSkill() {
        // kiểm tra cooldown skill hiện tại
        if (this.playerSkill == null || this.playerSkill.skillSelect == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastTimeAttack >= this.playerSkill.skillSelect.coolDown) {
            this.lastTimeAttack = now;
            return true;
        }
        return false;
    }

    public void dispose() {
        if (itemsTradeWVP != null) {
            if (!itemsTradeWVP.isEmpty()) {
                for (Item item : itemsTradeWVP) {
                    InventoryService.gI().addItemBag(this, item);
                }
            }
            itemsTradeWVP.clear();
            itemsTradeWVP = null;
        }
        if (clone != null) {// thêm phân thân
            clone.dispose();
            clone = null;
        }
        if (pet != null) {
            pet.dispose();
            pet = null;
        }
        if (newPet != null) {
            newPet.dispose();
            newPet = null;
        }
        if (mapBlackBall != null) {
            mapBlackBall.clear();
            mapBlackBall = null;
        }
        zone = null;
        mapBeforeCapsule = null;
        if (mapMaBu != null) {
            mapMaBu.clear();
            mapMaBu = null;
        }
        mapBeforeCapsule = null;
        if (mapCapsule != null) {
            mapCapsule.clear();
            mapCapsule = null;
        }
        if (mobMe != null) {
            mobMe.dispose();
            mobMe = null;
        }
        location = null;
        if (setClothes != null) {
            setClothes.dispose();
            setClothes = null;
        }
        if (effectSkill != null) {
            effectSkill.dispose();
            effectSkill = null;
        }
        if (mabuEgg != null) {
            mabuEgg.dispose();
            mabuEgg = null;
        }
        if (playerTask != null) {
            playerTask.dispose();
            playerTask = null;
        }
        if (itemTime != null) {
            itemTime.dispose();
            itemTime = null;
        }
        if (fusion != null) {
            fusion.dispose();
            fusion = null;
        }
        if (magicTree != null) {
            magicTree.dispose();
            magicTree = null;
        }
        if (playerIntrinsic != null) {
            playerIntrinsic.dispose();
            playerIntrinsic = null;
        }
        if (inventory != null) {
            inventory.dispose();
            inventory = null;
        }
        if (playerSkill != null) {
            playerSkill.dispose();
            playerSkill = null;
        }
        if (combine != null) {
            combine.dispose();
            combine = null;
        }
        if (iDMark != null) {
            iDMark.dispose();
            iDMark = null;
        }
        if (charms != null) {
            charms.dispose();
            charms = null;
        }
        if (effectSkin != null) {
            effectSkin.dispose();
            effectSkin = null;
        }
        if (nPoint != null) {
            nPoint.dispose();
            nPoint = null;
        }
        if (rewardBlackBall != null) {
            rewardBlackBall.dispose();
            rewardBlackBall = null;
        }
        if (pvp != null) {
            pvp.dispose();
            pvp = null;
        }
        if (superRank != null) {
            superRank.dispose();
            superRank = null;
        }
        if (dropItem != null) {
            dropItem.dispose();
            dropItem = null;
        }
        if (satellite != null) {
            satellite = null;
        }
        if (achievement != null) {
            achievement.dispose();
            achievement = null;
        }
        if (giftCode != null) {
            giftCode.dispose();
            giftCode = null;
        }
        if (traning != null) {
            traning = null;
        }
        if (mapCapsule != null) {
            mapCapsule.clear();
            mapCapsule = null;
        }
        if (Cards != null) {
            Cards.clear();
            Cards = null;
        }
        if (itemsWoodChest != null) {
            itemsWoodChest.clear();
            itemsWoodChest = null;
        }
        if (friends != null) {
            friends.clear();
            friends = null;
        }
        if (enemies != null) {
            enemies.clear();
            enemies = null;
        }
        if (temporaryEnemies != null) {
            temporaryEnemies.clear();
            temporaryEnemies = null;
        }
        itemsWoodChest = null;
        Cards = null;
        itemEvent = null;
        maBu2H = null;
        maBuHold = null;
        zoneSieuThanhThuy = null;
        thongBaoTapTuDong = null;
        notify = null;
        clan = null;
        clanMember = null;
        friends = null;
        enemies = null;
        session = null;
        newSkill = null;
        name = null;
        textThongBaoChangeMap = null;
        textThongBaoThua = null;
    }
}
