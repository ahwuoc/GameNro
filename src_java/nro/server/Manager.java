package nro.server;

/*
 * Box ZALO: https://zalo.me/g/ifjict764
 * sdt zalo: 0358176187
 * Chuyên chỉnh sữa mua bán source nro,...
 */
import boss.boss_manifest.LuyenTap.NPC_MrPôPô;
import models.Card.OptionCard;
import models.Card.RadarService;
import models.Card.RadarCard;
import jdbc.DBConnecter;
import consts.ConstPlayer;
import consts.ConstMap;
import data.DataGame;
import jdbc.daos.ShopDAO;
import models.Template.*;
import clan.Clan;
import clan.ClanMember;
import consts.ConstDataEventCHUCVIP;
import consts.ConstDataEventNAP;
import consts.ConstDataEventSM;
import consts.ConstDataEventTOP;
import consts.ConstDataEventTRANGSUCVIP;
import consts.ConstDataEventthangmuoi;
import consts.ConstSQL;

import static data.DataGame.MAP_MOUNT_NUM;

import encrypt.ImageUtil;

import models.GiftCode.GiftCode;
import models.GiftCode.GiftCodeManager;
import intrinsic.Intrinsic;
import item.CaiTrang;
import item.Item;
import item.Item.ItemOption;

import java.io.BufferedReader;

import map.WayPoint;
import nro.models.npc.Npc;
import nro.models.npc.NpcFactory;
import player.badges.BagesTemplate;
import shop.Shop;
import skill.NClass;
import skill.Skill;
import task.Badges.BadgesTaskTemplate;
import task.SideTaskTemplate;
import task.SubTaskMain;
import task.TaskMain;
import nro.services.ItemService;
import nro.services.MapService;
import utils.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import map.EffectEventManager;
import map.EffectMap;

import matches.TOP;
import models.kygui.ConsignItem;
import models.kygui.ConsignShopManager;
import utils.Util;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import nro.models.npc.NonInteractiveNPC;
import nro.tambao.TamBaoService;
import power.CaptionManager;
import power.PowerLimitManager;
import services.top.TopManager;
import task.ClanTaskTemplate;
import task.KolTaskTemplate;

public final class Manager {

    private static Manager instance;

    public static String apiKey = "abcdef";
    public static int workerGroup = 10;
    public static String executeCommand;
    public static boolean debug;
    public static int apiPort = 8899;

    public static int bossGroup = 5;
    public static byte SERVER = 1;
    public static byte SECOND_WAIT_LOGIN = 5;
    public static int MAX_PER_IP = 10;
    public static int MAX_PLAYER = 2000;
    public static int EVENT_SEVER = 0;
    public static int RATE_EXP_SERVER = 1;
    public static boolean LOCAL = false;
    public static boolean TEST = false;
    public static boolean DAO_AUTO_UPDATER = false;
    public static final List<KolTaskTemplate> KOL_TASKS_TEMPLATE = new ArrayList<>();
    public static MapTemplate[] MAP_TEMPLATES;
    public static final List<map.Map> MAPS = new ArrayList<>();
    public static final List<ItemOptionTemplate> ITEM_OPTION_TEMPLATES = new ArrayList<>();
    public static final List<ArrHead2Frames> ARR_HEAD_2_FRAMES = new ArrayList<>();
    public static final Map<String, Byte> IMAGES_BY_NAME = new HashMap<>();
    public static final List<ItemTemplate> ITEM_TEMPLATES = new ArrayList<>();
    public static final List<MobTemplate> MOB_TEMPLATES = new ArrayList<>();
    public static final List<NpcTemplate> NPC_TEMPLATES = new ArrayList<>();
    public static final List<TaskMain> TASKS = new ArrayList<>();
    public static final List<SideTaskTemplate> SIDE_TASKS_TEMPLATE = new ArrayList<>();
    public static final List<ClanTaskTemplate> CLAN_TASKS_TEMPLATE = new ArrayList<>();
    public static final List<AchievementTemplate> ACHIEVEMENT_TEMPLATE = new ArrayList<>();
    public static int AUTO_MAINTENANCE = 1;
    public static int AUTO_MAINTENANCE_HOUR = 1;
    public static int AUTO_MAINTENANCE_MINUTE = 1;
    public static boolean LUNNAR_NEW_YEAR = false;
    public static boolean INTERNATIONAL_WOMANS_DAY = false;
    public static boolean CHRISTMAS = false;
    public static boolean HALLOWEEN = false;
    public static boolean HUNG_VUONG = false;
    public static boolean TRUNG_THU = false;
    public static boolean TOP_UP = false;

    public static final List<Intrinsic> INTRINSICS = new ArrayList<>();
    public static final List<Intrinsic> INTRINSIC_TD = new ArrayList<>();
    public static final List<Intrinsic> INTRINSIC_NM = new ArrayList<>();
    public static final List<Intrinsic> INTRINSIC_XD = new ArrayList<>();
    public static final List<HeadAvatar> HEAD_AVATARS = new ArrayList<>();
    public static final List<BgItem> BG_ITEMS = new ArrayList<>();
    public static final List<FlagBag> FLAGS_BAGS = new ArrayList<>();
    public static final List<NClass> NCLASS = new ArrayList<>();
    public static final List<Npc> NPCS = new ArrayList<>();
    public static List<Shop> SHOPS = new ArrayList<>();
    public static final List<Clan> CLANS = new ArrayList<>();
    public static final List<String> NOTIFY = new ArrayList<>();
    public static final List<BadgesTaskTemplate> TASKS_BADGES_TEMPLATE = new ArrayList<>();
    public static final List<BagesTemplate> BAGES_TEMPLATES = new ArrayList<>();
    public static final List<CaiTrang> CAI_TRANGS = new ArrayList<>();
    public static boolean isTopMaydamChanged = false;
    public static boolean isTopWhisChanged = false;
    public static List<TOP> Topmaydam;

    public static final short[] itemIds_Kaio_AWJ = {232, 236, 240, 244, 248, 252, 268, 272, 276};
    public static final short[] itemIds_tl_AWJ = {555, 557, 559, 556, 558, 560, 563, 565, 567};
    public static final short[] itemIds_tl_GN = {562, 564, 566, 561};
    public static final short[] itemIds_Kaio_GN = {256, 260, 264, 280};
    public static final short[] itemIds_LuongLong_AWJ = {233, 237, 241, 245, 249, 253, 269, 273, 277};
    public static final short[] itemIds_LuongLong_GN = {257, 261, 265, 281};

    public static final short[] aotd = {138, 139, 230, 231, 232, 233, 555};
    public static final short[] quantd = {142, 143, 242, 243, 244, 245, 556};
    public static final short[] gangtd = {146, 147, 254, 255, 256, 257, 562};
    public static final short[] giaytd = {150, 151, 266, 267, 268, 269, 563};
    public static final short[] aoxd = {170, 171, 238, 239, 240, 241, 559};
    public static final short[] quanxd = {174, 175, 250, 251, 252, 253, 560};
    public static final short[] gangxd = {178, 179, 262, 263, 264, 265, 566};
    public static final short[] giayxd = {182, 183, 274, 275, 276, 277, 567};
    public static final short[] aonm = {154, 155, 234, 235, 236, 237, 557};
    public static final short[] quannm = {158, 159, 246, 247, 248, 249, 558};
    public static final short[] gangnm = {162, 163, 258, 259, 260, 261, 564};
    public static final short[] giaynm = {166, 167, 270, 271, 272, 273, 565};
    public static final short[] radaSKHVip = {186, 187, 278, 279, 280, 281, 561};
    public static final short[][][] doSKHVip = {{aotd, quantd, gangtd, giaytd}, {aonm, quannm, gangnm, giaynm},
    {aoxd, quanxd, gangxd, giayxd}};

    public static List<TOP> topSM;
    public static List<TOP> topNap;
    public static List<TOP> topDuaSM;
    public static List<TOP> topDuaNap;
    public static List<TOP> topSD;
    public static List<TOP> topHP;
    public static List<TOP> topKI;
    public static List<TOP> topNV;
    public static List<TOP> topSK;
    public static List<TOP> topPVP;
    public static List<TOP> topNHS;
    public static List<TOP> topDC;
    public static List<TOP> topVDST;
    public static List<TOP> topWHIS;
    public static long timeRealTop = 0;
    public static final short[][] trangBiKichHoat = {{0, 6, 21, 27}, {1, 7, 22, 28}, {2, 8, 23, 29}};
    public static final short[][] trangBiKichHoatVip = {{555, 556, 562, 563}, {557, 558, 564, 565}, {559, 560, 566, 567}};

    public static Manager gI() {
        if (instance == null) {
            instance = new Manager();
        }
        return instance;
    }

    public static boolean hasNewTopScores() {
        return isTopMaydamChanged || isTopWhisChanged;
    }

    public static void resetTopFlags() {
        isTopMaydamChanged = false;
        isTopWhisChanged = false;
    }

    private Manager() {
        try {
            loadProperties();
        } catch (IOException ex) {
            Logger.logException(Manager.class, ex, "Lỗi load properites");
            System.exit(0);
        }
        ImageUtil.initImage();
        this.startEventStatusThread();
//        TamBaoService.loadItem();
        this.loadDatabase();
        NpcFactory.createNpcConMeo();
        NpcFactory.createNpcRongThieng();
        this.initMap();
        System.out.println("Finish connect Server: " + DBConnecter.DB_DATA);
    }

    private void initMap() {
        try {
            // Load cấu hình Tile chung
            int[][] tileTypeTop = readTileIndexTileType(ConstMap.TILE_TOP);
            if (tileTypeTop == null) {
                System.err.println("CRITICAL: Failed to load TileTypeTop!");
                System.exit(1);
                return;
            }

            // Load từng map
            for (MapTemplate mapTemp : MAP_TEMPLATES) {
                loadSingleMap(mapTemp, tileTypeTop);
            }

            // Khởi tạo NPC đặc biệt
            initSpecialNPCs();

            System.out.println("Init Maps Success: " + MAPS.size());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean loadSingleMap(MapTemplate mapTemp, int[][] tileTypeTop) {
        try {
            int tileIndex = mapTemp.tileId - 1;
            // Validation: Check Tile Index
            if (tileIndex < 0 || tileIndex >= tileTypeTop.length) {
                return false;
            }

            // Validation: Check Tile Map Data
            int[][] tileMap = readTileMap(mapTemp.id);
            if (tileMap == null) {
                return false;
            }

            // Khởi tạo Map
            map.Map map = new map.Map(
                    mapTemp.id, mapTemp.name, mapTemp.planetId, mapTemp.tileId,
                    mapTemp.bgId, mapTemp.bgType, mapTemp.type, tileMap, tileTypeTop[tileIndex],
                    mapTemp.zones, mapTemp.isMapOffline(), mapTemp.maxPlayerPerZone,
                    mapTemp.wayPoints, mapTemp.effectMaps
            );

            // Init Mob & NPC
            try {
                map.initMob(mapTemp.mobTemp, mapTemp.mobLevel, mapTemp.mobHp, mapTemp.mobX, mapTemp.mobY);
                map.initNpc(mapTemp.npcId, mapTemp.npcX, mapTemp.npcY);
            } catch (Exception ignored) {
            }

            MAPS.add(map);
            startMapWorker(map);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void startMapWorker(map.Map map) {
        Thread.ofVirtual()
                .name("Map-Worker-" + map.mapId)
                .start(() -> {
                    try {
                        map.run();
                    } catch (Exception e) {
                        System.out.println("CRITICAL ERROR in Map Worker [ID: " + map.mapId + "]");
                        e.printStackTrace();
                    }
                });
    }

    /**
     * Khởi tạo các NPC đặc biệt hệ thống
     */
    private void initSpecialNPCs() {
        try {
            new NonInteractiveNPC().initNonInteractiveNPC();
            new NPC_MrPôPô().initNPC_MrPôPô();
            System.out.println("-> Special NPCs initialized.");
        } catch (Exception e) {
            System.out.println("Error initializing special NPCs: " + e.getMessage());
        }
    }

    private void loadDatabase() {
        long st = System.currentTimeMillis();
        try (Connection con = DBConnecter.getConnectionServer()) {
            loadParts(con);
            loadImagesByName(con);
            loadShopsAndItems(con);
            loadGeneralTemplates(con);
            loadSkills(con);
            loadClans(con);
            loadGiftCodes(con);
            loadMapTemplates(con);
            loadTasks(con);
            loadRankings(con);
            PowerLimitManager.getInstance().load();
            CaptionManager.getInstance().load();
            EffectEventManager.gI().load();
            loadMaxSmallVersion();

        } catch (Exception e) {
            Logger.logException(Manager.class, e, "Error loading database");
            System.exit(0);
        }
        System.out.println("Total database loading time: " + (System.currentTimeMillis() - st) + " (ms)");
    }

    // =============================================================
    // CÁC HÀM CON (HELPER METHODS)
    // =============================================================
    // =============================================================
    // LUỒNG CẬP NHẬT TRẠNG THÁI SỰ KIỆN (CHẠY NGẦM)
    // =============================================================
    private void loadImagesByName(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("select name, n_frame from img_by_name"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                IMAGES_BY_NAME.put(rs.getString("name"), rs.getByte("n_frame"));
            }
        }
        System.out.println("Loaded Images By Name: " + IMAGES_BY_NAME.size());
    }

    private void loadClans(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("select * from clan"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Clan clan = new Clan();
                clan.id = rs.getInt("id");
                clan.name = rs.getString("name");
                clan.name2 = rs.getString("name_2");
                clan.slogan = rs.getString("slogan");
                clan.imgId = rs.getByte("img_id");
                clan.powerPoint = rs.getLong("power_point");
                clan.maxMember = rs.getByte("max_member");
                clan.capsuleClan = rs.getInt("clan_point");
                clan.level = rs.getByte("level");
                if (clan.level < 1) {
                    clan.level = 1;
                }
                clan.createTime = (int) (rs.getTimestamp("create_time").getTime() / 1000);

                JSONArray dataArray = (JSONArray) JSONValue.parse(rs.getString("members"));
                for (Object o : dataArray) {
                    JSONObject dataObject = (JSONObject) JSONValue.parse(String.valueOf(o));
                    ClanMember cm = new ClanMember();
                    cm.clan = clan;
                    cm.id = Integer.parseInt(String.valueOf(dataObject.get("id")));
                    cm.name = String.valueOf(dataObject.get("name"));
                    cm.head = Short.parseShort(String.valueOf(dataObject.get("head")));
                    cm.body = Short.parseShort(String.valueOf(dataObject.get("body")));
                    cm.leg = Short.parseShort(String.valueOf(dataObject.get("leg")));
                    cm.role = Byte.parseByte(String.valueOf(dataObject.get("role")));
                    cm.donate = Integer.parseInt(String.valueOf(dataObject.get("donate")));
                    cm.receiveDonate = Integer.parseInt(String.valueOf(dataObject.get("receive_donate")));
                    cm.memberPoint = Integer.parseInt(String.valueOf(dataObject.get("member_point")));
                    cm.clanPoint = Integer.parseInt(String.valueOf(dataObject.get("clan_point")));
                    cm.joinTime = Integer.parseInt(String.valueOf(dataObject.get("join_time")));
                    cm.timeAskPea = Long.parseLong(String.valueOf(dataObject.get("ask_pea_time")));
                    try {
                        cm.powerPoint = Long.parseLong(String.valueOf(dataObject.get("power")));
                    } catch (Exception ignored) {
                    }
                    clan.addClanMember(cm);
                }
                CLANS.add(clan);
            }
        }

        try (PreparedStatement ps = con.prepareStatement("select id from clan order by id desc limit 1"); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                Clan.NEXT_ID = rs.getInt("id") + 1;
            }
        }
        System.out.println("Loaded Clans: " + CLANS.size() + " (Next ID: " + Clan.NEXT_ID + ")");
    }

    private void loadGiftCodes(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM giftcode"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                GiftCode giftcode = new GiftCode();
                giftcode.code = rs.getString("code");
                giftcode.id = rs.getInt("id");
                giftcode.countLeft = rs.getInt("count_left");
                if (giftcode.countLeft == -1) {
                    giftcode.countLeft = 999999999;
                }
                giftcode.datecreate = rs.getTimestamp("datecreate");
                giftcode.dateexpired = rs.getTimestamp("expired");

                JSONArray jar = (JSONArray) JSONValue.parse(rs.getString("detail"));
                if (jar != null) {
                    for (Object o : jar) {
                        JSONObject jsonObj = (JSONObject) o;
                        int id = Integer.parseInt(jsonObj.get("temp_id").toString());
                        int quantity = Integer.parseInt(jsonObj.get("quantity").toString());
                        JSONArray option = (JSONArray) jsonObj.get("options");
                        ArrayList<ItemOption> optionList = new ArrayList<>();
                        if (option != null) {
                            for (Object opt : option) {
                                JSONObject jsonobject = (JSONObject) opt;
                                int optionId = Integer.parseInt(jsonobject.get("id").toString());
                                int param = Integer.parseInt(jsonobject.get("param").toString());
                                optionList.add(new Item.ItemOption(optionId, param));
                            }
                        }
                        giftcode.option.put(id, optionList);
                        giftcode.detail.put(id, quantity);
                    }
                }
                GiftCodeManager.gI().listGiftCode.add(giftcode);
            }
        }
        System.out.println("Loaded Giftcodes: " + GiftCodeManager.gI().listGiftCode.size());
    }

    private void loadParts(Connection con) throws Exception {
        List<Part> parts = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement("select * from part"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Part part = new Part();
                part.id = rs.getShort("id");
                part.type = rs.getByte("type");
                JSONArray dataArray = (JSONArray) JSONValue.parse(rs.getString("data").replaceAll("\\\"", ""));
                for (Object o : dataArray) {
                    JSONArray pd = (JSONArray) JSONValue.parse(String.valueOf(o));
                    part.partDetails.add(new PartDetail(Short.parseShort(String.valueOf(pd.get(0))),
                            Byte.parseByte(String.valueOf(pd.get(1))),
                            Byte.parseByte(String.valueOf(pd.get(2)))));
                }
                parts.add(part);
            }
        }
        // Write file cache
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream("data/update_data/part"))) {
            dos.writeShort(parts.size());
            for (Part part : parts) {
                dos.writeByte(part.type);
                for (PartDetail partDetail : part.partDetails) {
                    dos.writeShort(partDetail.iconId);
                    dos.writeByte(partDetail.dx);
                    dos.writeByte(partDetail.dy);
                }
            }
        }
        System.out.println("Loaded Parts: " + parts.size());
    }

    private void loadSkills(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("select * from skill_template order by nclass_id, slot"); ResultSet rs = ps.executeQuery()) {
            byte nClassId = -1;
            NClass nClass = null;
            while (rs.next()) {
                byte id = rs.getByte("nclass_id");
                if (id != nClassId) {
                    nClassId = id;
                    nClass = new NClass();
                    nClass.name = id == ConstPlayer.TRAI_DAT ? "Trái Đất" : id == ConstPlayer.NAMEC ? "Namếc" : "Xayda";
                    nClass.classId = nClassId;
                    NCLASS.add(nClass);
                }
                SkillTemplate skillTemplate = new SkillTemplate();
                skillTemplate.classId = nClassId;
                skillTemplate.id = rs.getByte("id");
                skillTemplate.name = rs.getString("name");
                skillTemplate.maxPoint = rs.getByte("max_point");
                skillTemplate.manaUseType = rs.getByte("mana_use_type");
                skillTemplate.type = rs.getByte("type");
                skillTemplate.iconId = rs.getShort("icon_id");
                skillTemplate.damInfo = rs.getString("dam_info");
                skillTemplate.description = rs.getString("description");
                nClass.skillTemplatess.add(skillTemplate);

                JSONArray dataArray = (JSONArray) JSONValue.parse(rs.getString("skills")
                        .replaceAll("\\[\"", "[").replaceAll("\"\\[", "[")
                        .replaceAll("\"\\]", "]").replaceAll("\\]\"", "]")
                        .replaceAll("\\}\",\"\\{", "},{"));

                for (Object o : dataArray) {
                    JSONObject dts = (JSONObject) JSONValue.parse(String.valueOf(o));
                    Skill skill = new Skill();
                    skill.template = skillTemplate;
                    skill.skillId = Short.parseShort(String.valueOf(dts.get("id")));
                    skill.point = Byte.parseByte(String.valueOf(dts.get("point")));
                    skill.powRequire = Long.parseLong(String.valueOf(dts.get("power_require")));
                    skill.manaUse = Integer.parseInt(String.valueOf(dts.get("mana_use")));
                    skill.coolDown = Integer.parseInt(String.valueOf(dts.get("cool_down")));
                    skill.dx = Integer.parseInt(String.valueOf(dts.get("dx")));
                    skill.dy = Integer.parseInt(String.valueOf(dts.get("dy")));
                    skill.maxFight = Integer.parseInt(String.valueOf(dts.get("max_fight")));
                    skill.damage = Short.parseShort(String.valueOf(dts.get("damage")));
                    skill.price = Short.parseShort(String.valueOf(dts.get("price")));
                    skill.moreInfo = String.valueOf(dts.get("info"));
                    skillTemplate.skillss.add(skill);
                }
            }
        }
        System.out.println("Loaded Skills: " + NCLASS.size() + " classes");
    }

    private void loadMapTemplates(Connection con) throws SQLException {
        // Load BG Item Template
        try (PreparedStatement ps = con.prepareStatement("select * from bg_item_template"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                BgItem bgItem = new BgItem();
                bgItem.id = rs.getInt("id");
                bgItem.layer = rs.getByte("layer");
                bgItem.dx = rs.getShort("dx");
                bgItem.dy = rs.getShort("dy");
                bgItem.idImage = rs.getShort("image_id");
                BG_ITEMS.add(bgItem);
            }
        }
        System.out.println("Loaded BG Items: " + BG_ITEMS.size());

        // Load Maps
        int countRow = 0;
        try (PreparedStatement ps = con.prepareStatement("select count(id) from map_template"); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                countRow = rs.getShort(1);
            }
        }

        MAP_TEMPLATES = new MapTemplate[countRow];
        try (PreparedStatement ps = con.prepareStatement("select * from map_template"); ResultSet rs = ps.executeQuery()) {
            short i = 0;
            while (rs.next()) {
                MapTemplate mapTemplate = new MapTemplate();
                mapTemplate.id = rs.getInt("id");
                mapTemplate.name = rs.getString("name");
                mapTemplate.type = rs.getByte("type");
                mapTemplate.planetId = rs.getByte("planet_id");
                mapTemplate.bgType = rs.getByte("bg_type");
                mapTemplate.tileId = rs.getByte("tile_id");
                mapTemplate.bgId = rs.getByte("bg_id");
                mapTemplate.zones = rs.getByte("zones");
                mapTemplate.maxPlayerPerZone = rs.getByte("max_player");

                // Waypoints
                JSONArray dataArray = (JSONArray) JSONValue.parse(rs.getString("waypoints")
                        .replaceAll("\\[\"\\[", "[[").replaceAll("\\]\"\\]", "]]").replaceAll("\",\"", ","));
                for (Object o : dataArray) {
                    WayPoint wp = new WayPoint();
                    JSONArray dtwp = (JSONArray) JSONValue.parse(String.valueOf(o));
                    wp.name = String.valueOf(dtwp.get(0));
                    wp.minX = Short.parseShort(String.valueOf(dtwp.get(1)));
                    wp.minY = Short.parseShort(String.valueOf(dtwp.get(2)));
                    wp.maxX = Short.parseShort(String.valueOf(dtwp.get(3)));
                    wp.maxY = Short.parseShort(String.valueOf(dtwp.get(4)));
                    wp.isEnter = Byte.parseByte(String.valueOf(dtwp.get(5))) == 1;
                    wp.isOffline = Byte.parseByte(String.valueOf(dtwp.get(6))) == 1;
                    wp.goMap = Short.parseShort(String.valueOf(dtwp.get(7)));
                    wp.goX = Short.parseShort(String.valueOf(dtwp.get(8)));
                    wp.goY = Short.parseShort(String.valueOf(dtwp.get(9)));
                    mapTemplate.wayPoints.add(wp);
                }

                // Mobs
                dataArray = (JSONArray) JSONValue.parse(rs.getString("mobs").replaceAll("\\\"", ""));
                mapTemplate.mobTemp = new byte[dataArray.size()];
                mapTemplate.mobLevel = new byte[dataArray.size()];
                mapTemplate.mobHp = new long[dataArray.size()];
                mapTemplate.mobX = new short[dataArray.size()];
                mapTemplate.mobY = new short[dataArray.size()];
                for (int j = 0; j < dataArray.size(); j++) {
                    JSONArray dtm = (JSONArray) JSONValue.parse(String.valueOf(dataArray.get(j)));
                    mapTemplate.mobTemp[j] = Byte.parseByte(String.valueOf(dtm.get(0)));
                    mapTemplate.mobLevel[j] = Byte.parseByte(String.valueOf(dtm.get(1)));
                    mapTemplate.mobHp[j] = ((Number) dtm.get(2)).longValue();
                    mapTemplate.mobX[j] = Short.parseShort(String.valueOf(dtm.get(3)));
                    mapTemplate.mobY[j] = Short.parseShort(String.valueOf(dtm.get(4)));
                }

                // NPCs
                dataArray = (JSONArray) JSONValue.parse(rs.getString("npcs").replaceAll("\\\"", ""));
                mapTemplate.npcId = new byte[dataArray.size()];
                mapTemplate.npcX = new short[dataArray.size()];
                mapTemplate.npcY = new short[dataArray.size()];
                for (int j = 0; j < dataArray.size(); j++) {
                    JSONArray dtn = (JSONArray) JSONValue.parse(String.valueOf(dataArray.get(j)));
                    mapTemplate.npcId[j] = Byte.parseByte(String.valueOf(dtn.get(0)));
                    mapTemplate.npcX[j] = Short.parseShort(String.valueOf(dtn.get(1)));
                    mapTemplate.npcY[j] = Short.parseShort(String.valueOf(dtn.get(2)));
                }

                // Effects
                EffectMap em = new EffectMap();
                em.setKey("beff");
                em.setValue("15");
                mapTemplate.effectMaps.add(em);

                MAP_TEMPLATES[i++] = mapTemplate;
            }
        }
        System.out.println("Loaded Map Templates: " + MAP_TEMPLATES.length);
    }

    private void loadGeneralTemplates(Connection con) throws SQLException {
        // Array Head 2 Frames
        try (PreparedStatement ps = con.prepareStatement("select * from array_head_2_frames"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ArrHead2Frames arr = new ArrHead2Frames();
                JSONArray data = (JSONArray) JSONValue.parse(rs.getString("data"));
                for (Object o : data) {
                    arr.frames.add(Integer.valueOf(o.toString()));
                }
                ARR_HEAD_2_FRAMES.add(arr);
            }
        }

        // Head Avatar
        try (PreparedStatement ps = con.prepareStatement("select * from head_avatar"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                HEAD_AVATARS.add(new HeadAvatar(rs.getInt("head_id"), rs.getInt("avatar_id")));
            }
        }

        // Flag Bag
        try (PreparedStatement ps = con.prepareStatement("select * from flag_bag"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                FlagBag flagBag = new FlagBag();
                flagBag.id = rs.getInt("id");
                flagBag.name = rs.getString("name");
                flagBag.gold = rs.getInt("gold");
                flagBag.gem = rs.getInt("gem");
                flagBag.iconId = rs.getShort("icon_id");
                String[] iconData = rs.getString("icon_data").split(",");
                flagBag.iconEffect = new short[iconData.length];
                for (int j = 0; j < iconData.length; j++) {
                    flagBag.iconEffect[j] = Short.parseShort(iconData[j].trim());
                }
                FLAGS_BAGS.add(flagBag);
            }
        }

        // Intrinsic
        try (PreparedStatement ps = con.prepareStatement("select * from intrinsic"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Intrinsic intrinsic = new Intrinsic();
                intrinsic.id = rs.getByte("id");
                intrinsic.name = rs.getString("name");
                intrinsic.paramFrom1 = rs.getShort("param_from_1");
                intrinsic.paramTo1 = rs.getShort("param_to_1");
                intrinsic.paramFrom2 = rs.getShort("param_from_2");
                intrinsic.paramTo2 = rs.getShort("param_to_2");
                intrinsic.icon = rs.getShort("icon");
                intrinsic.gender = rs.getByte("gender");
                switch (intrinsic.gender) {
                    case ConstPlayer.TRAI_DAT ->
                        INTRINSIC_TD.add(intrinsic);
                    case ConstPlayer.NAMEC ->
                        INTRINSIC_NM.add(intrinsic);
                    case ConstPlayer.XAYDA ->
                        INTRINSIC_XD.add(intrinsic);
                    default -> {
                        INTRINSIC_TD.add(intrinsic);
                        INTRINSIC_NM.add(intrinsic);
                        INTRINSIC_XD.add(intrinsic);
                    }
                }
                INTRINSICS.add(intrinsic);
            }
        }

        // Mob Templates
        try (PreparedStatement ps = con.prepareStatement("select * from mob_template"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                MobTemplate mob = new MobTemplate();
                mob.id = rs.getByte("id");
                mob.type = rs.getByte("type");
                mob.name = rs.getString("name");
                mob.hp = rs.getInt("hp");
                mob.rangeMove = rs.getByte("range_move");
                mob.speed = rs.getByte("speed");
                mob.dartType = rs.getByte("dart_type");
                mob.percentDame = rs.getByte("percent_dame");
                mob.percentTiemNang = rs.getByte("percent_tiem_nang");
                MOB_TEMPLATES.add(mob);
            }
        }

        // NPC Templates
        try (PreparedStatement ps = con.prepareStatement("select * from npc_template"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                NpcTemplate npc = new NpcTemplate();
                npc.id = rs.getByte("id");
                npc.name = rs.getString("name");
                npc.head = rs.getShort("head");
                npc.body = rs.getShort("body");
                npc.leg = rs.getShort("leg");
                npc.avatar = rs.getInt("avatar");
                NPC_TEMPLATES.add(npc);
            }
        }

        // Notify
        try (PreparedStatement ps = con.prepareStatement("select * from notify order by id desc"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                NOTIFY.add(rs.getString("name") + "<>" + rs.getString("text"));
            }
        }

        // Radar
        try (PreparedStatement ps = con.prepareStatement("select * from radar"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                RadarCard rd = new RadarCard();
                rd.Id = rs.getShort("id");
                rd.IconId = rs.getShort("iconId");
                rd.Rank = rs.getByte("rank");
                rd.Max = rs.getByte("max");
                rd.Type = rs.getByte("type");
                rd.Template = rs.getShort("mob_id");
                rd.Name = rs.getString("name");
                rd.Info = rs.getString("info");
                JSONArray arr = (JSONArray) JSONValue.parse(rs.getString("body"));
                for (Object o : arr) {
                    JSONObject ob = (JSONObject) o;
                    if (ob != null) {
                        rd.Head = Short.parseShort(ob.get("head").toString());
                        rd.Body = Short.parseShort(ob.get("body").toString());
                        rd.Leg = Short.parseShort(ob.get("leg").toString());
                        rd.Bag = Short.parseShort(ob.get("bag").toString());
                    }
                }
                rd.Options.clear();
                arr = (JSONArray) JSONValue.parse(rs.getString("options"));
                for (Object o : arr) {
                    JSONObject ob = (JSONObject) o;
                    if (ob != null) {
                        rd.Options.add(new OptionCard(Integer.parseInt(ob.get("id").toString()), Short.parseShort(ob.get("param").toString()), Byte.parseByte(ob.get("activeCard").toString())));
                    }
                }
                rd.AuraId = rs.getShort("aura_id");
                RadarService.gI().RADAR_TEMPLATE.add(rd);
            }
        }

        // Data Badges
        try (PreparedStatement ps = con.prepareStatement("select * from data_badges"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                BagesTemplate template = new BagesTemplate();
                template.id = rs.getInt("id");
                template.idEffect = rs.getInt("idEffect");
                template.idItem = rs.getInt("idItem");
                template.NAME = rs.getString("NAME");
                JSONArray option = (JSONArray) JSONValue.parse(rs.getString("Options"));
                if (option != null) {
                    for (Object o : option) {
                        JSONObject jsonobject = (JSONObject) o;
                        int optionId = Integer.parseInt(jsonobject.get("id").toString());
                        int param = Integer.parseInt(jsonobject.get("param").toString());
                        template.options.add(new Item.ItemOption(optionId, param));
                    }
                }
                BAGES_TEMPLATES.add(template);
            }
        }

        System.out.println("Loaded General Templates (Head, Flag, Intrinsic, Mob, NPC, Radar, Badges)");
    }

    private void loadTasks(Connection con) throws SQLException {
        // Task Main
        String sql = "SELECT id, task_main_template.name, detail, task_sub_template.name AS 'sub_name', max_count, notify, npc_id, map FROM task_main_template JOIN task_sub_template ON task_main_template.id = task_sub_template.task_main_id";
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            int taskId = -1;
            TaskMain task = null;
            while (rs.next()) {
                int id = rs.getInt("id");
                if (id != taskId) {
                    taskId = id;
                    task = new TaskMain();
                    task.id = taskId;
                    task.name = rs.getString("name");
                    task.detail = rs.getString("detail");
                    TASKS.add(task);
                }
                SubTaskMain subTask = new SubTaskMain();
                subTask.name = rs.getString("sub_name");
                subTask.maxCount = rs.getShort("max_count");
                subTask.notify = rs.getString("notify");
                subTask.npcId = rs.getByte("npc_id");
                subTask.mapId = rs.getShort("map");
                task.subTasks.add(subTask);
            }
        }

        // Side Task
        try (PreparedStatement ps = con.prepareStatement("select * from side_task_template"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                SideTaskTemplate sideTask = new SideTaskTemplate();
                sideTask.id = rs.getInt("id");
                sideTask.name = rs.getString("name");
                String[] mcs = {rs.getString("max_count_lv1"), rs.getString("max_count_lv2"), rs.getString("max_count_lv3"), rs.getString("max_count_lv4"), rs.getString("max_count_lv5")};
                for (int i = 0; i < 5; i++) {
                    String[] split = mcs[i].split("-");
                    sideTask.count[i][0] = Integer.parseInt(split[0]);
                    sideTask.count[i][1] = Integer.parseInt(split[1]);
                }
                SIDE_TASKS_TEMPLATE.add(sideTask);
            }
        }

        // Clan Task
        try (PreparedStatement ps = con.prepareStatement("select * from clan_task_template"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ClanTaskTemplate clanTask = new ClanTaskTemplate();
                clanTask.id = rs.getInt("id");
                clanTask.name = rs.getString("name");
                String[] mcs = {rs.getString("max_count_lv1"), rs.getString("max_count_lv2"), rs.getString("max_count_lv3"), rs.getString("max_count_lv4"), rs.getString("max_count_lv5")};
                for (int i = 0; i < 5; i++) {
                    String[] split = mcs[i].split("-");
                    clanTask.count[i][0] = Integer.parseInt(split[0]);
                    clanTask.count[i][1] = Integer.parseInt(split[1]);
                }
                CLAN_TASKS_TEMPLATE.add(clanTask);
            }
        }

        // Task Badges & Achievement & KOL
        try (PreparedStatement ps = con.prepareStatement("select * from task_badges_template"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                BadgesTaskTemplate t = new BadgesTaskTemplate();
                t.id = rs.getInt("id");
                t.name = rs.getString("NAME");
                t.count = rs.getInt("maxCount");
                t.idbadgesReward = rs.getInt("idbadgesReward");
                TASKS_BADGES_TEMPLATE.add(t);
            }
        }

        try (PreparedStatement ps = con.prepareStatement("select * from achievement_template"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ACHIEVEMENT_TEMPLATE.add(new AchievementTemplate(rs.getString("info1"), rs.getString("info2"), rs.getInt("money"), rs.getLong("max_count")));
            }
        }

        try (PreparedStatement ps = con.prepareStatement("select * from task_kol_template"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                KOL_TASKS_TEMPLATE.add(new KolTaskTemplate(rs.getInt("id"), rs.getString("info"), rs.getInt("max_count")));
            }
        }

        System.out.println("Loaded All Tasks (Main, Side, Clan, Badge, Achie, KOL)");
    }

    private void loadShopsAndItems(Connection con) throws SQLException {
        // Items Template
        try (PreparedStatement ps = con.prepareStatement("select * from item_template"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ItemTemplate item = new ItemTemplate();
                item.id = rs.getShort("id");
                item.type = rs.getByte("type");
                item.gender = rs.getByte("gender");
                item.name = rs.getString("name");
                item.description = rs.getString("description");
                item.level = rs.getByte("level");
                item.iconID = rs.getShort("icon_id");
                item.part = rs.getShort("part");
                item.isUpToUp = rs.getBoolean("is_up_to_up");
                item.strRequire = rs.getInt("power_require");
                item.gold = rs.getInt("gold");
                item.gem = rs.getInt("gem");
                item.head = rs.getInt("head");
                item.body = rs.getInt("body");
                item.leg = rs.getInt("leg");
                ITEM_TEMPLATES.add(item);

                // Mount check
                if (item.type == 23 && getNFrameImageByName("mount_" + item.part + "_0") != 0) {
                    MAP_MOUNT_NUM.put(item.id, (short) (item.part + 30000));
                }
            }
        }

        // Item Option Template
        try (PreparedStatement ps = con.prepareStatement("select id, name from item_option_template"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ItemOptionTemplate opt = new ItemOptionTemplate();
                opt.id = rs.getInt("id");
                opt.name = rs.getString("name");
                ITEM_OPTION_TEMPLATES.add(opt);
            }
        }

        // Shops
        SHOPS = ShopDAO.getShops(con);

        // Ký gửi (Consign)
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM shop_ky_gui"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                try {
                    List<Item.ItemOption> op = new ArrayList<>();
                    String itemOptionStr = rs.getString("itemOption");
                    if (itemOptionStr != null && !itemOptionStr.trim().isEmpty()) {
                        Object parsed = JSONValue.parse(itemOptionStr);
                        // Logic parse phức tạp ký gửi giữ nguyên
                        if (parsed instanceof JSONArray jsa2) {
                            for (Object obj : jsa2) {
                                if (obj instanceof JSONObject jso2) {
                                    op.add(new Item.ItemOption(Integer.parseInt(jso2.get("id").toString()), Integer.parseInt(jso2.get("param").toString())));
                                } else if (obj instanceof JSONArray arr && arr.size() >= 2) {
                                    op.add(new Item.ItemOption(Integer.parseInt(arr.get(0).toString()), Integer.parseInt(arr.get(1).toString())));
                                }
                            }
                        } else if (parsed instanceof JSONObject jsonObj) {
                            Object optObj = jsonObj.get("options");
                            if (optObj instanceof JSONArray arrOpts) {
                                for (Object o : arrOpts) {
                                    if (o instanceof JSONArray arr && arr.size() >= 2) {
                                        op.add(new Item.ItemOption(Integer.parseInt(arr.get(0).toString()), Integer.parseInt(arr.get(1).toString())));
                                    } else if (o instanceof JSONObject jso2) {
                                        op.add(new Item.ItemOption(Integer.parseInt(jso2.get("id").toString()), Integer.parseInt(jso2.get("param").toString())));
                                    }
                                }
                            }
                        }
                    }
                    ConsignShopManager.gI().listItem.add(new ConsignItem(
                            rs.getInt("id"), rs.getShort("item_id"), rs.getInt("player_id"),
                            rs.getByte("tab"), rs.getInt("gold"), rs.getInt("gem"),
                            rs.getInt("quantity"), rs.getByte("isUpTop"), op, rs.getByte("isBuy") == 1
                    ));
                } catch (Exception e) {
                    System.err.println("Error loading consign item " + rs.getInt("id") + ": " + e.getMessage());
                }
            }
        }
        System.out.println("Loaded Items, Options, Shops & Consign");
    }

    private void loadRankings(Connection con) {
        topNV = realTop(ConstSQL.TOP_NV, con);
        topSM = realTop(ConstSQL.TOP_SM, con);
        topNap = realTop(ConstSQL.TOP_NAP, con);
        topWHIS = realTop(ConstSQL.TOP_WHIS, con);
        topVDST = realTop(ConstSQL.TOP_VDST, con);
        topDuaSM = realTop(ConstSQL.TOP_DUA_SM, con);
        topDuaNap = realTop(ConstSQL.TOP_DUA_NAP, con);
        Topmaydam = realTop(ConstSQL.queryTopmaydam, con);
        Manager.timeRealTop = System.currentTimeMillis();
        TopManager.loadTop(con);
        System.out.println("Loaded Rankings");
    }

    private void loadMaxSmallVersion() {
        File directory = new File("data/icon/x4");
        if (directory.isDirectory()) {
            java.util.Optional<File> maxFile = java.util.Arrays.stream(directory.listFiles())
                    .filter(File::isFile).filter(file -> file.getName().endsWith(".png"))
                    .max(java.util.Comparator.comparingInt(file -> {
                        String name = file.getName();
                        return Integer.valueOf(name.substring(0, name.length() - 4));
                    }));
            if (maxFile.isPresent()) {
                String fileName = maxFile.get().getName();
                short maxVersion = Short.parseShort(fileName.substring(0, fileName.length() - 4));
                DataGame.maxSmallVersion = (short) (maxVersion + 1);
                System.out.println("Max Small Version: " + DataGame.maxSmallVersion);
            }
        }
    }

    public void updateShop() {
        try (Connection con = DBConnecter.getConnectionServer();) {
            SHOPS = ShopDAO.getShops(con);
        } catch (Exception ex) {

        }
    }

    public static List<TOP> realTop(String query, Connection con) {
        List<TOP> tops = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(query); ResultSet rs = ps.executeQuery()) {

            int index = 0; // dùng riêng cho TOP_WHIS và TOP_VDST
            while (rs.next()) {
                // mặc định head/body/leg theo giới tính
                byte gender = rs.getByte("gender");
                short head = Util.getHead(gender);
                short body = (short) (gender == 1 ? 59 : 57);
                short leg = (short) (gender == 1 ? 60 : 58);

                // đọc items_body
                String itemsBodyJson = rs.getString("items_body");
                if (itemsBodyJson != null) {
                    JSONArray dataArray = (JSONArray) JSONValue.parse(itemsBodyJson);
                    if (dataArray != null) {
                        // slot 0 → body
                        body = getItemPart(dataArray, 0, body, "body");

                        // slot 1 → leg
                        leg = getItemPart(dataArray, 1, leg, "leg");

                        // slot 5 → head/body/leg override
                        short[] parts = getItemParts(dataArray, 5, head, body, leg);
                        head = parts[0];
                        body = parts[1];
                        leg = parts[2];
                    }
                }

                // build object TOP
                TOP top = TOP.builder()
                        .name(rs.getString("name"))
                        .gender(gender)
                        .head(head)
                        .body(body)
                        .leg(leg)
                        .build();

                // set dữ liệu riêng theo từng top
                switch (query) {
                    case ConstSQL.TOP_NV -> {
                        top.setNv(rs.getByte("nv"));
                        top.setSubnv(rs.getByte("subnv"));
                        top.setLasttime(rs.getLong("lasttime"));
                    }
                    case ConstSQL.TOP_DC -> {
                        top.setDicanh(rs.getInt("dicanh"));
                        top.setJuventus(rs.getInt("juventus"));
                    }
                    case ConstSQL.TOP_SM, "TOP_DUA_SM" -> {
                        top.setPower(rs.getLong("sm"));
                    }
                    case ConstSQL.TOP_NAP -> {
                        top.setCash(rs.getInt("cash"));
                    }
                    case ConstSQL.TOP_DUA_NAP -> {
                        top.setCash(rs.getInt("danap"));
                    }
                    case ConstSQL.TOP_DUA_QUOC_VUONG -> {
                        top.setThoivang(rs.getInt("thoi_vang"));
                    }
                    case ConstSQL.TOP_WHIS -> {
                        top.setLasttime(rs.getLong("lasttime"));
                        top.setLevel(rs.getInt("top"));
                        top.setTime(rs.getInt("time"));
                        index++;
                    }
                    case ConstSQL.TOP_VDST -> {
                        top.setDivdst(rs.getInt("time"));
                        top.setLasttime(rs.getLong("lasttime"));
                        index++;
                    }
                    case ConstSQL.queryTopmaydam -> {
                        int maydam = rs.getInt("point_maydam");
                        long totalDame = rs.getLong("total_damage_maydam");
                        top.setId_player(rs.getInt("id")); // ⚡ quan trọng
                        top.setInfo1(maydam + " điểm");
                        top.setInfo2(Util.formatNumber(totalDame) + " sát thương");
                        index++;
                    }
                }

                tops.add(top);
            }
        } catch (Exception e) {
            System.err.println("Lỗi đọc realTop: " + e.getMessage());
            e.printStackTrace();
        }

        return tops;
    }

    /**
     * Lấy part item từ dataArray (slot body hoặc leg).
     */
    private static short getItemPart(JSONArray dataArray, int index, short defaultPart, String type) {
        try {
            JSONArray dataItem = (JSONArray) JSONValue.parse(dataArray.get(index).toString());
            if (dataItem != null && dataItem.get(0) != null) {
                short tempId = Short.parseShort(String.valueOf(dataItem.get(0)));
                if (tempId != -1) {
                    Item item = ItemService.gI().createNewItem(
                            tempId,
                            Integer.parseInt(String.valueOf(dataItem.get(1)))
                    );
                    return (short) item.template.part;
                }
            }
        } catch (Exception ignored) {
        }
        return defaultPart;
    }

    /**
     * Lấy head/body/leg override từ item slot 5.
     */
    private static short[] getItemParts(JSONArray dataArray, int index, short head, short body, short leg) {
        try {
            JSONArray dataItem = (JSONArray) JSONValue.parse(dataArray.get(index).toString());
            if (dataItem != null && dataItem.get(0) != null) {
                short tempId = Short.parseShort(String.valueOf(dataItem.get(0)));
                if (tempId != -1) {
                    Item item = ItemService.gI().createNewItem(
                            tempId,
                            Integer.parseInt(String.valueOf(dataItem.get(1)))
                    );
                    if (item.template.head != -1) {
                        head = (short) item.template.head;
                    }
                    if (item.template.body != -1) {
                        body = (short) item.template.body;
                    }
                    if (item.template.leg != -1) {
                        leg = (short) item.template.leg;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return new short[]{head, body, leg};
    }

    private void startEventStatusThread() {
        Thread.ofVirtual().name("Event-Status-Updater").start(() -> {
            // Đợi 5 giây để server khởi động ổn định trước khi bắt đầu check
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }

            System.out.println("Event Status Updater Started...");

            while (true) {
                try {
                    // Cập nhật trạng thái các sự kiện (True/False)
                    // Lưu ý: Lần chạy đầu tiên sẽ in thời gian Start/End ra console
                    ConstDataEventSM.isRunningSK = ConstDataEventSM.isActiveEvent();
                    ConstDataEventNAP.isRunningSK = ConstDataEventNAP.isActiveEvent();
                    ConstDataEventTRANGSUCVIP.isRunningSK = ConstDataEventTRANGSUCVIP.isActiveEvent();
                    ConstDataEventCHUCVIP.isRunningSK = ConstDataEventCHUCVIP.isActiveEvent();
                    ConstDataEventthangmuoi.isRunningSK = ConstDataEventthangmuoi.isActiveEvent();

                    // Nghỉ 1 giây để giảm tải CPU
                    Thread.sleep(1000);
                } catch (Exception e) {
                    Logger.error("Lỗi luồng cập nhật sự kiện: " + e.getMessage());
                }
            }
        });
    }

    public void loadProperties() throws IOException {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream("data/config/config.properties")) {
            properties.load(fis);
        }

        // ================= SERVER CONFIG =================
        SERVER = Byte.parseByte(properties.getProperty("server.sv", "1"));
        ServerManager.NAME = properties.getProperty("server.name", "NRO");

        // AntiDDoS Config
        AntiDDoS_BY_Barcoll.REAL_PORT = Integer.parseInt(properties.getProperty("server.port_real", "14445"));
        AntiDDoS_BY_Barcoll.PROXY_PORT = Integer.parseInt(properties.getProperty("server.port_proxy", "14445"));
        AntiDDoS_BY_Barcoll.REAL_HOST = properties.getProperty("server.ip_host", "localhost");

        // Link Server Config
        StringBuilder linkServer = new StringBuilder();
        if (properties.getProperty("server.ip") != null) {
            AntiDDoS_BY_Barcoll.REAL_HOST = properties.getProperty("server.ip");
            linkServer.append(ServerManager.NAME).append(":").append(AntiDDoS_BY_Barcoll.REAL_HOST)
                    .append(":").append(AntiDDoS_BY_Barcoll.REAL_PORT).append(":0,");
        }
        for (int i = 1; i <= 10; i++) {
            String svIp = properties.getProperty("server.sv" + i);
            if (svIp != null) {
                linkServer.append(svIp).append(":0,");
            }
        }
        if (linkServer.length() > 0) {
            DataGame.LINK_IP_PORT = linkServer.substring(0, linkServer.length() - 1);
        }

        // Game Settings
        SECOND_WAIT_LOGIN = Byte.parseByte(properties.getProperty("server.waitlogin", "5"));
        MAX_PER_IP = Integer.parseInt(properties.getProperty("server.maxperip", "10"));
        MAX_PLAYER = Integer.parseInt(properties.getProperty("server.maxplayer", "2000"));
        RATE_EXP_SERVER = Byte.parseByte(properties.getProperty("server.expserver", "1"));

        // Flags
        LOCAL = Boolean.parseBoolean(properties.getProperty("server.local", "false"));
        TEST = Boolean.parseBoolean(properties.getProperty("server.test", "false"));
        DAO_AUTO_UPDATER = Boolean.parseBoolean(properties.getProperty("server.daoautoupdater", "false"));

        // Maintenance
        AUTO_MAINTENANCE = Integer.parseInt(properties.getProperty("auto.maintenance", "1"));
        AUTO_MAINTENANCE_HOUR = Integer.parseInt(properties.getProperty("auto.maintenance.hour", "1"));
        AUTO_MAINTENANCE_MINUTE = Integer.parseInt(properties.getProperty("auto.maintenance.minute", "1"));

        try {
            File eventFile = new File("active_event.txt");
            Manager.ACTIVE_EVENTS.clear();
            if (eventFile.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(eventFile))) {
                    String line = br.readLine();
                    if (line != null && !line.isEmpty()) {
                        String[] ids = line.split("-");
                        for (String id : ids) {
                            try {
                                Manager.ACTIVE_EVENTS.add(Integer.parseInt(id.trim()));
                            } catch (NumberFormatException e) {
                                System.err.println("[EventManager] ID sự kiện không hợp lệ: " + id);
                            }
                        }
                    }
                }
            } else {
                System.out.println(">> [Startup] Không tìm thấy active_event.txt. Sử dụng mặc định.");
                Manager.ACTIVE_EVENTS.add(7);
            }

            System.out.println(">> [Server] Active Events Loaded: " + Manager.ACTIVE_EVENTS);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(">> [Error] Lỗi khi tải cấu hình sự kiện!");
        }

        ConstDataEventSM.YEAR_EVENT = Short.parseShort(properties.getProperty("event.year", "2025"));

        // Event Sức Mạnh
        loadEventSM(properties);

        // Event Nạp
        loadEventNap(properties);

        // Event Thiệp Chúc VIP
        loadEventChucVip(properties);

        // Event Trang Sức VIP
        loadEventTrangSucVip(properties);

        // Event 20/10 (Tháng Mười)
        loadEventThangMuoi(properties);

        // Event Đua Top
        loadEventDuaTop(properties);

        // Init Flags
        ConstDataEventSM.initsukien = false;
        ConstDataEventSM.isTraoQua = true;
        ConstDataEventNAP.initsukien = false;
        ConstDataEventNAP.isTraoQua = true;
    }

    // ================= HELPER METHODS FOR LOADING EVENTS =================
    private void loadEventSM(Properties prop) {
        ConstDataEventSM.MONTH_OPEN = Byte.parseByte(prop.getProperty("event.sm.month_open", "1"));
        ConstDataEventSM.DATE_OPEN = Byte.parseByte(prop.getProperty("event.sm.date_open", "1"));
        ConstDataEventSM.HOUR_OPEN = Byte.parseByte(prop.getProperty("event.sm.hour_open", "0"));
        ConstDataEventSM.MIN_OPEN = Byte.parseByte(prop.getProperty("event.sm.minute_open", "0"));

        ConstDataEventSM.MONTH_END = Byte.parseByte(prop.getProperty("event.sm.month_end", "1"));
        ConstDataEventSM.DATE_END = Byte.parseByte(prop.getProperty("event.sm.date_end", "1"));
        ConstDataEventSM.HOUR_END = Byte.parseByte(prop.getProperty("event.sm.hour_end", "0"));
        ConstDataEventSM.MIN_END = Byte.parseByte(prop.getProperty("event.sm.minute_end", "0"));
    }

    private void loadEventNap(Properties prop) {
        ConstDataEventNAP.MONTH_OPEN = Short.parseShort(prop.getProperty("event.nap.month_open", "1"));
        ConstDataEventNAP.DATE_OPEN = Short.parseShort(prop.getProperty("event.nap.date_open", "1"));
        ConstDataEventNAP.HOUR_OPEN = Short.parseShort(prop.getProperty("event.nap.hour_open", "0"));
        ConstDataEventNAP.MIN_OPEN = Short.parseShort(prop.getProperty("event.nap.minute_open", "0"));

        ConstDataEventNAP.MONTH_END = Short.parseShort(prop.getProperty("event.nap.month_end", "1"));
        ConstDataEventNAP.DATE_END = Short.parseShort(prop.getProperty("event.nap.date_end", "1"));
        ConstDataEventNAP.HOUR_END = Short.parseShort(prop.getProperty("event.nap.hour_end", "0"));
        ConstDataEventNAP.MIN_END = Short.parseShort(prop.getProperty("event.nap.minute_end", "0"));
    }

    private void loadEventChucVip(Properties prop) {
        ConstDataEventCHUCVIP.MONTH_OPEN = Short.parseShort(prop.getProperty("event.chucvip.month_open", "1"));
        ConstDataEventCHUCVIP.DATE_OPEN = Short.parseShort(prop.getProperty("event.chucvip.date_open", "1"));
        ConstDataEventCHUCVIP.HOUR_OPEN = Short.parseShort(prop.getProperty("event.chucvip.hour_open", "0"));
        ConstDataEventCHUCVIP.MIN_OPEN = Short.parseShort(prop.getProperty("event.chucvip.minute_open", "0"));

        ConstDataEventCHUCVIP.MONTH_END = Short.parseShort(prop.getProperty("event.chucvip.month_end", "1"));
        ConstDataEventCHUCVIP.DATE_END = Short.parseShort(prop.getProperty("event.chucvip.date_end", "1"));
        ConstDataEventCHUCVIP.HOUR_END = Short.parseShort(prop.getProperty("event.chucvip.hour_end", "0"));
        ConstDataEventCHUCVIP.MIN_END = Short.parseShort(prop.getProperty("event.chucvip.minute_end", "0"));
    }

    private void loadEventTrangSucVip(Properties prop) {
        ConstDataEventTRANGSUCVIP.MONTH_OPEN = Short.parseShort(prop.getProperty("event.trangsucvip.month_open", "1"));
        ConstDataEventTRANGSUCVIP.DATE_OPEN = Short.parseShort(prop.getProperty("event.trangsucvip.date_open", "1"));
        ConstDataEventTRANGSUCVIP.HOUR_OPEN = Short.parseShort(prop.getProperty("event.trangsucvip.hour_open", "0"));
        ConstDataEventTRANGSUCVIP.MIN_OPEN = Short.parseShort(prop.getProperty("event.trangsucvip.minute_open", "0"));

        ConstDataEventTRANGSUCVIP.MONTH_END = Short.parseShort(prop.getProperty("event.trangsucvip.month_end", "1"));
        ConstDataEventTRANGSUCVIP.DATE_END = Short.parseShort(prop.getProperty("event.trangsucvip.date_end", "1"));
        ConstDataEventTRANGSUCVIP.HOUR_END = Short.parseShort(prop.getProperty("event.trangsucvip.hour_end", "0"));
        ConstDataEventTRANGSUCVIP.MIN_END = Short.parseShort(prop.getProperty("event.trangsucvip.minute_end", "0"));
    }

    private void loadEventThangMuoi(Properties prop) {
        ConstDataEventthangmuoi.MONTH_OPEN = Short.parseShort(prop.getProperty("event.thangmuoi.month_open", "1"));
        ConstDataEventthangmuoi.DATE_OPEN = Short.parseShort(prop.getProperty("event.thangmuoi.date_open", "1"));
        ConstDataEventthangmuoi.HOUR_OPEN = Short.parseShort(prop.getProperty("event.thangmuoi.hour_open", "0"));
        ConstDataEventthangmuoi.MIN_OPEN = Short.parseShort(prop.getProperty("event.thangmuoi.minute_open", "0"));

        ConstDataEventthangmuoi.MONTH_END = Short.parseShort(prop.getProperty("event.thangmuoi.month_end", "1"));
        ConstDataEventthangmuoi.DATE_END = Short.parseShort(prop.getProperty("event.thangmuoi.date_end", "1"));
        ConstDataEventthangmuoi.HOUR_END = Short.parseShort(prop.getProperty("event.thangmuoi.hour_end", "0"));
        ConstDataEventthangmuoi.MIN_END = Short.parseShort(prop.getProperty("event.thangmuoi.minute_end", "0"));
    }

    private void loadEventDuaTop(Properties prop) {
        ConstDataEventTOP.MONTH_OPEN = Short.parseShort(prop.getProperty("event.top.month_open", "1"));
        ConstDataEventTOP.DATE_OPEN = Short.parseShort(prop.getProperty("event.top.date_open", "1"));
        ConstDataEventTOP.HOUR_OPEN = Short.parseShort(prop.getProperty("event.top.hour_open", "0"));
        ConstDataEventTOP.MIN_OPEN = Short.parseShort(prop.getProperty("event.top.minute_open", "0"));

        ConstDataEventTOP.MONTH_END = Short.parseShort(prop.getProperty("event.top.month_end", "1"));
        ConstDataEventTOP.DATE_END = Short.parseShort(prop.getProperty("event.top.date_end", "1"));
        ConstDataEventTOP.HOUR_END = Short.parseShort(prop.getProperty("event.top.hour_end", "0"));
        ConstDataEventTOP.MIN_END = Short.parseShort(prop.getProperty("event.top.minute_end", "0"));

        ConstDataEventTOP.MONTH_REWARD = Short.parseShort(prop.getProperty("event.top.month_reward", "1"));
        ConstDataEventTOP.DATE_REWARD = Short.parseShort(prop.getProperty("event.top.date_reward", "1"));
        ConstDataEventTOP.HOUR_REWARD = Short.parseShort(prop.getProperty("event.top.hour_reward", "0"));
        ConstDataEventTOP.MIN_REWARD = Short.parseShort(prop.getProperty("event.top.minute_reward", "0"));
    }
    public static final List<Integer> ACTIVE_EVENTS = new ArrayList<>();

    /**
     * @param tileTypeFocus tile type: top, bot, left, right...
     * @return [tileMapId][tileType]
     */
    private int[][] readTileIndexTileType(int tileTypeFocus) {
        int[][] tileIndexTileType = null;
        try {
            DataInputStream dis = new DataInputStream(new FileInputStream("data/map/tile_set_info"));
            int numTileMap = dis.readByte();
            tileIndexTileType = new int[numTileMap][];
            for (int i = 0; i < numTileMap; i++) {
                int numTileOfMap = dis.readByte();
                for (int j = 0; j < numTileOfMap; j++) {
                    int tileType = dis.readInt();
                    int numIndex = dis.readByte();
                    if (tileType == tileTypeFocus) {
                        tileIndexTileType[i] = new int[numIndex];
                    }
                    for (int k = 0; k < numIndex; k++) {
                        int typeIndex = dis.readByte();
                        if (tileType == tileTypeFocus) {
                            tileIndexTileType[i][k] = typeIndex;

                        }
                    }
                }
            }
        } catch (IOException e) {
            Logger.logException(MapService.class, e);
        }
        return tileIndexTileType;
    }

    /**
     * @param mapId mapId
     * @return tile map for paint
     */
    private int[][] readTileMap(int mapId) {
        int[][] tileMap = null;
        try {
            try (DataInputStream dis = new DataInputStream(new FileInputStream("data/map/tile_map_data/" + mapId))) {
                int w = dis.readByte();
                int h = dis.readByte();
                tileMap = new int[h][w];
                for (int[] tm : tileMap) {
                    for (int j = 0; j < tm.length; j++) {
                        tm[j] = dis.readByte();
                    }
                }
            }
        } catch (IOException e) {
        }
        return tileMap;
    }

    public static Clan getClanById(int id) throws Exception {
        for (Clan clan : CLANS) {
            if (clan.id == id) {
                return clan;
            }
        }
        throw new Exception("Không tìm thấy clan id: " + id);
    }

    public static void addClan(Clan clan) {
        CLANS.add(clan);
    }

    public static int getNumClan() {
        return CLANS.size();

    }

    public static MobTemplate getMobTemplateByTemp(int mobTempId) {
        for (MobTemplate mobTemp : MOB_TEMPLATES) {
            if (mobTemp.id == mobTempId) {
                return mobTemp;
            }
        }
        return null;
    }

    public static CaiTrang getCaiTrangByItemId(int itemId) {
        for (CaiTrang caiTrang : CAI_TRANGS) {
            if (caiTrang.tempId == itemId) {
                return caiTrang;
            }
        }
        return null;
    }

    public static byte getNFrameImageByName(String name) {
        Object n = IMAGES_BY_NAME.get(name);
        if (n != null) {
            return Byte.parseByte(String.valueOf(n));
        } else {
            return 0;
        }
    }
    public static Timestamp timeSuKienDuaTop = Timestamp.valueOf("2025-09-21 23:59:59");
    public static String timeStartDuaTop = "10h ngày 25/5/2025";
    public static String timeEndDuaTop = "23h59 ngày 10/6/2025";
    public static String timeEndNhanGiai = "20h20 ngày 24/11/2025";

    public static String demTimeSuKien() {
        return demConLai(timeSuKienDuaTop);
    }

    public static long demTimeSuKien2() {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime eventTime = timeSuKienDuaTop.toLocalDateTime();
        long daysRemaining = ChronoUnit.DAYS.between(currentTime, eventTime);
        return Math.max(daysRemaining, 0);
    }

    // 🌕 SỰ KIỆN TRUNG THU VIP & MA QUỶ & TRÀ HOA CÚC
    public static Timestamp timeSuKienDuaTopTrungThuVip = Timestamp.valueOf("2025-10-10 23:59:59");
    public static Timestamp timeSuKienDuaTopmaquy = Timestamp.valueOf("2025-10-10 23:59:59");
    public static Timestamp timeSuKienDuaToptrahoacuc = Timestamp.valueOf("2025-10-10 23:59:59");

    public static String demTimeSuKienTrungThuVip() {
        return demConLai(timeSuKienDuaTopTrungThuVip);
    }

    public static String demTimeSuKienmaquy() {
        return demConLai(timeSuKienDuaTopmaquy);
    }

    public static String demTimeSuKientrahoacuc() {
        return demConLai(timeSuKienDuaToptrahoacuc);
    }

    // 🎁 THỜI GIAN NHẬN GIẢI (TRUNG THU VIP, MA QUỶ, TRÀ HOA CÚC)
    public static Timestamp timeSuKienDuaTopTrungThuVipNhanGiai = Timestamp.valueOf("2025-10-15 23:59:59");
    public static Timestamp timeSuKienDuaTopmaquyNhanGiai = Timestamp.valueOf("2025-10-15 23:59:59");
    public static Timestamp timeSuKienDuaToptrahoacucNhanGiai = Timestamp.valueOf("2025-10-15 23:59:59");

    public static String demTimeSuKienTrungThuVipNhanGiai() {
        return demConLai(timeSuKienDuaTopTrungThuVipNhanGiai);
    }

    public static String demTimeSuKienmaquyNhanGiai() {
        return demConLai(timeSuKienDuaTopmaquyNhanGiai);
    }

    public static String demTimeSuKientrahoacucNhanGiai() {
        return demConLai(timeSuKienDuaToptrahoacucNhanGiai);
    }

    // 🏮 SỰ KIỆN LỒNG ĐÈN TREO
    public static Timestamp timeSuKienDualongdentreo = Timestamp.valueOf("2025-10-25 23:59:59");
    public static Timestamp timeSuKienDuaToplongdentreoNhanGiai = Timestamp.valueOf("2025-10-27 23:59:59");

    public static String demTimeSuKienlongdentreo() {
        return demConLai(timeSuKienDualongdentreo);
    }

    public static String demTimeSuKienlongdentreoNhanGiai() {
        return demConLai(timeSuKienDuaToplongdentreoNhanGiai);
    }

    // 💎 CÁC SỰ KIỆN VIP KHÁC (CAPSULE, THIỆP VIP, 20/10)
    public static Timestamp timeSuKiencapsuvipNhanGiai = Timestamp.valueOf("2025-10-27 23:59:59");
    public static Timestamp timeSuKienthiepvipNhanGiai = Timestamp.valueOf("2025-10-27 23:59:59");
    public static Timestamp timeSuKien2010NhanGiai = Timestamp.valueOf("2025-10-27 23:59:59");

    public static String demTimeSuKiencapsuvip() {
        return getEventCountDownFromConfig(ConstDataEventTRANGSUCVIP.MONTH_END, ConstDataEventTRANGSUCVIP.DATE_END, ConstDataEventTRANGSUCVIP.HOUR_END, ConstDataEventTRANGSUCVIP.MIN_END);
    }

    public static String demTimeSuKiencapsuvipNhanGiai() {
        return demConLai(timeSuKiencapsuvipNhanGiai);
    }

    public static String demTimeSuKienthiepchucvip() {
        return getEventCountDownFromConfig(ConstDataEventCHUCVIP.MONTH_END, ConstDataEventCHUCVIP.DATE_END, ConstDataEventCHUCVIP.HOUR_END, ConstDataEventCHUCVIP.MIN_END);
    }

    public static String demTimeSuKienthiepvipNhanGiai() {
        return demConLai(timeSuKienthiepvipNhanGiai);
    }

    public static String demTimeSuKien2010() {
        return getEventCountDownFromConfig(ConstDataEventthangmuoi.MONTH_END, ConstDataEventthangmuoi.DATE_END, ConstDataEventthangmuoi.HOUR_END, ConstDataEventthangmuoi.MIN_END);
    }

    public static String demTimeSuKien2010NhanGiai() {
        return demConLai(timeSuKien2010NhanGiai);
    }

    // 🎃 HALLOWEEN EVENTS
    // Hộp Kẹo Ma Quỷ
    public static Timestamp timeSuKienKeoMaQuyEnd = Timestamp.valueOf("2025-11-10 23:59:59");
    public static Timestamp timeSuKienKeoMaQuyNhanGiai = Timestamp.valueOf("2025-11-12 23:59:59");

    public static String demTimeKeoMaQuy() {
        return demConLai(timeSuKienKeoMaQuyEnd);
    }

    public static String demTimeKeoMaQuyNhanGiai() {
        return demConLai(timeSuKienKeoMaQuyNhanGiai);
    }

    // Thiệp Halloween
    public static Timestamp timeSuKienThiepHalloweenEnd = Timestamp.valueOf("2025-11-15 23:59:59");
    public static Timestamp timeSuKienThiepHalloweenNhanGiai = Timestamp.valueOf("2025-11-17 23:59:59");

    public static String demTimeThiepHalloween() {
        return demConLai(timeSuKienThiepHalloweenEnd);
    }

    public static String demTimeThiepHalloweenNhanGiai() {
        return demConLai(timeSuKienThiepHalloweenNhanGiai);
    }

    // Hộp Điểm, Vòng Quay Vàng, Vòng Quay Đặc Biệt (Chung thời gian Halloween)
    public static Timestamp timeSuKienhopdiemEnd = Timestamp.valueOf("2025-11-15 23:59:59");
    public static Timestamp timeSuKienhopdiemNhanGiai = Timestamp.valueOf("2025-11-17 23:59:59");
    public static Timestamp timeSuKienvongquayvangEnd = Timestamp.valueOf("2025-11-15 23:59:59");
    public static Timestamp timeSuKienvongquayvangNhanGiai = Timestamp.valueOf("2025-11-17 23:59:59");
    public static Timestamp timeSuKienvongquaydacbietEnd = Timestamp.valueOf("2025-11-15 23:59:59");
    public static Timestamp timeSuKienvongquaydacbietNhanGiai = Timestamp.valueOf("2025-11-17 23:59:59");
    public static Timestamp timeSuKienphaobongEnd = Timestamp.valueOf("2025-11-15 23:59:59");
    public static Timestamp timeSuKienphaobongNhanGiai = Timestamp.valueOf("2025-11-17 23:59:59");
    public static Timestamp timeSuKienlixi = Timestamp.valueOf("2025-11-17 23:59:59");
    public static Timestamp timeSuKienlixiNhanGiai = Timestamp.valueOf("2025-11-17 23:59:59");

    public static String demTimehopdiem() {
        return demConLai(timeSuKienhopdiemEnd);
    }

    public static String demTimehopdiemNhanGiai() {
        return demConLai(timeSuKienhopdiemNhanGiai);
    }

    public static String demTimevongquayvang() {
        return demConLai(timeSuKienvongquayvangEnd);
    }

    public static String demTimevongquayvangNhanGiai() {
        return demConLai(timeSuKienvongquayvangNhanGiai);
    }

    public static String demTimevongquaydacbiet() {
        return demConLai(timeSuKienvongquaydacbietEnd);
    }

    public static String demTimephaobong() {
        return demConLai(timeSuKienphaobongEnd);
    }

    public static String demTimephaobongNhanGiai() {
        return demConLai(timeSuKienphaobongNhanGiai);
    }

    public static String demTimevongquaydacbietNhanGiai() {
        return demConLai(timeSuKienvongquaydacbietNhanGiai);
    }

    public static String demTimelixi() {
        return demConLai(timeSuKienlixi);
    }

    public static String demTimelixiNhanGiai() {
        return demConLai(timeSuKienlixiNhanGiai);
    }

    // Túi Mù Halloween
    public static Timestamp timeSuKienTuiMuHalloweenEnd = Timestamp.valueOf("2025-11-05 23:59:59");
    public static Timestamp timeSuKienTuiMuHalloweenNhanGiai = Timestamp.valueOf("2025-11-07 23:59:59");

    public static String demTimeTuiMuHalloween() {
        return demConLai(timeSuKienTuiMuHalloweenEnd);
    }

    public static String demTimeTuiMuHalloweenNhanGiai() {
        return demConLai(timeSuKienTuiMuHalloweenNhanGiai);
    }

    private static String demConLai(Timestamp eventTimeStamp) {
        return demConLai(eventTimeStamp.toLocalDateTime());
    }

    private static String demConLai(LocalDateTime eventTime) {
        LocalDateTime currentTime = LocalDateTime.now();
        long daysRemaining = ChronoUnit.DAYS.between(currentTime, eventTime);
        if (daysRemaining > 0) {
            return "(" + daysRemaining + " ngày nữa)";
        } else {
            return "(Đã kết thúc)";
        }
    }

    private static String getEventCountDownFromConfig(int month, int date, int hour, int minute) {
        LocalDateTime eventTime = LocalDateTime.of(ConstDataEventSM.YEAR_EVENT, month, date, hour, minute);
        return demConLai(eventTime);
    }

    public void reloadTopWhis() {
        try (Connection con = DBConnecter.getConnectionServer()) {
            Manager.topWHIS = realTop(ConstSQL.TOP_WHIS, con);
        } catch (Exception e) {
            Logger.error("Lỗi cập nhật Top Whis: " + e.getMessage());
        }
    }
}
