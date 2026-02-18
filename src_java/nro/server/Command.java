package nro.server;

/**
 * Box ZALO:https://zalo.me/g/ifjict764 sdt zalo: 0358176187 Chuyên chỉnh sữa
 * mua bán source nro,...
 */
import Bot.BotManager;
import EMTI.SystemMetrics;
import boss.AnTromManager;
import boss.BabyManager;
import boss.BossManager;
import boss.BrolyManager;
import boss.ChristmasEventManager;
import boss.GasDestroyManager;
import boss.HalloweenEventManager;
import boss.MatTroiManager;
import boss.OdoManager;
import boss.OtherBossManager;
import boss.RedRibbonHQManager;
import boss.RongnhiManager;
import boss.SnakeWayManager;
import boss.SoiHecQuynManager;
import boss.TreasureUnderSeaManager;
import boss.TrungThuEventManager;
import boss.XinBaToManager;
import consts.ConstNpc;
import item.Item;
import java.util.HashMap;

import java.util.List;
import java.util.Map;

import minigame.LuckyNumber.LuckyNumber;
import models.GiftCode.GiftCodeManager;
import models.ShenronEvent.ShenronEvent;
import models.ShenronEvent.ShenronEventManager;
import models.ShenronEvent_NOEL.ShenronEventManagernoel;
import models.ShenronEvent_NOEL.ShenronEventnoel;
import network.SessionManager;
import nro.player.Pet;
import nro.player.Player;
import player.badges.BadgesData;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.NpcService;
import nro.services.PetService;
import nro.services.Service;
import nro.services.SkillService;
import nro.services.TaskService;
import services.func.ChangeMapService;
import services.func.Input;
import services.top.TopService;
import skill.Skill;

public class Command {

    private static Command instance;

    public static Command gI() {
        if (instance == null) {
            instance = new Command();
        }
        return instance;
    }

    public void chat(Player player, String text) {
        if (!check(player, text)) {
            Service.gI().chat(player, text);
        }
    }

    public boolean check(Player player, String text) {
        try {
            // ==========================
            // 1. ADMIN COMMANDS
            // ==========================
            if (player.isAdmin()) {
                Map<String, Runnable> commands = Map.ofEntries(
                        Map.entry("admin", () -> showAdminMenu(player)),
                        Map.entry("giftcode", () -> GiftCodeManager.gI().checkInfomationGiftCode(player)),
                        // Boss
                        Map.entry("baby", () -> BabyManager.gI().showListBoss(player)),
                        Map.entry("rongnhi", () -> RongnhiManager.gI().showListBoss(player)),
                        Map.entry("odo", () -> OdoManager.gI().showListBoss(player)),
                        Map.entry("soihecquyn", () -> SoiHecQuynManager.gI().showListBoss(player)),
                        Map.entry("xinbato", () -> XinBaToManager.gI().showListBoss(player)),
                        Map.entry("boss", () -> BossManager.gI().showListBoss(player)),
                        Map.entry("broly", () -> BrolyManager.gI().showListBoss(player)),
                        Map.entry("antrom", () -> AnTromManager.gI().showListBoss(player)),
                        Map.entry("mattroi", () -> MatTroiManager.gI().showListBoss(player)),
                        Map.entry("boss2", () -> OtherBossManager.gI().showListBoss(player)),
                        Map.entry("doanhtrai", () -> RedRibbonHQManager.gI().showListBoss(player)),
                        Map.entry("bdkb", () -> TreasureUnderSeaManager.gI().showListBoss(player)),
                        Map.entry("cdrd", () -> SnakeWayManager.gI().showListBoss(player)),
                        Map.entry("kghd", () -> GasDestroyManager.gI().showListBoss(player)),
                        Map.entry("trungthu", () -> TrungThuEventManager.gI().showListBoss(player)),
                        Map.entry("noel", () -> ChristmasEventManager.gI().showListBoss(player)),
                        Map.entry("halowen", () -> HalloweenEventManager.gI().showListBoss(player)),
                        // Buff / hỗ trợ
                        Map.entry("hsk", () -> Service.gI().releaseCooldownSkill(player)),
                        Map.entry("battu", () -> toggleBattu(player)),
                        Map.entry("toado", () -> Service.gI().sendThongBao(player, player.location.x + " - " + player.location.y)),
                        // Test / debug
                        Map.entry("hocskill", () -> learnTestSkill(player)),
                        Map.entry("phanthan", () -> SkillService.gI().learSkillSpecial(player, Skill.PHAN_THAN, 1)),
                        Map.entry("dragon", () -> spawnDragon(player)),
                        Map.entry("dragonnoel", () -> spawnDragonnoel(player)),
                        Map.entry("daucatmoi", () -> repeatNotify("BOSS Nro vừa xuất hiện tại nhà anh ấy", 10)),
                        // Menu Bot
                        Map.entry("bot", () -> showBotMenu(player)),
                        // Item
                        Map.entry("item", () -> Input.gI().createFormGiveItem(player)),
                        Map.entry("getitem", () -> Input.gI().createFormGetItem(player)),
                        // Di chuyển / position
                        Map.entry("d", () -> Service.gI().setPos(player, player.location.x, player.location.y + 10))
                );

                if (commands.containsKey(text)) {
                    commands.get(text).run();
                    return true;
                }

                // ==========================
                // Prefix commands
                // ==========================
                if (text.equals("loadtop")) {
                    TopService.gI().loadListTop();
                    Service.gI().sendThongBao(player, "Reload Top!");
                    return true;
                }
                if (text.startsWith("sp")) {
                    return parseAndAddSM(player, text.substring(2), false);
                }
                if (text.startsWith("dt")) {
                    return parseAndAddSM(player, text.substring(2), true);
                }
                if (text.startsWith("m")) {
                    return changeMap(player, text);
                }
                if (text.startsWith("dmg")) {
                    return setPoint(player, "dmg", text);
                }
                if (text.startsWith("hpg")) {
                    return setPoint(player, "hpg", text);
                }
                if (text.startsWith("kig")) {
                    return setPoint(player, "kig", text);
                }
                if (text.startsWith("smg")) {
                    return setPoint(player, "smg", text);
                }
                if (text.startsWith("defg")) {
                    return setPoint(player, "defg", text);
                }
                if (text.startsWith("crg")) {
                    return setPoint(player, "crg", text);
                }
                if (text.startsWith("ntask")) {
                    return setTask(player, text);
                }
                if (text.startsWith("badges_")) {
                    player.badges.idBadges = Integer.parseInt(text.substring(7));
                }
                if (text.startsWith("kq")) {
                    Service.gI().sendThongBao(player, "Kết quả Lucky Round tiếp theo là: " + LuckyNumber.RESULT);
                    return true;
                }
                if (text.startsWith("danhhieu_")) {
                    new BadgesData(player, Integer.parseInt(text.substring(10)), 5);
                }
                if (text.startsWith("gender_")) {
                    player.gender = Byte.parseByte(text.substring(7));
                }
                if (text.startsWith("i")) {
                    return giveItem(player, text);
                }
            }

            // ==========================
            // Pet commands
            // ==========================
            if (text.startsWith("ten con la ")) {
                PetService.gI().changeNamePet(player, text.substring(11));
            }

            if (player.pet != null) {
                switch (text) {
                    case "di theo", "follow" ->
                        player.pet.changeStatus(Pet.FOLLOW);
                    case "bao ve", "protect" ->
                        player.pet.changeStatus(Pet.PROTECT);
                    case "tan cong", "attack" ->
                        player.pet.changeStatus(Pet.ATTACK);
                    case "ve nha", "go home" ->
                        player.pet.changeStatus(Pet.GOHOME);
                    case "bien hinh" ->
                        player.pet.transform();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

// ----------------- HÀM HỖ TRỢ -----------------
    private void toggleBattu(Player player) {
        player.isBattu = !player.isBattu;
        Service.gI().sendThongBao(player, "Bất tử" + (player.isBattu ? ": ON" : ": OFF"));
    }

    private void learnTestSkill(Player player) {
        switch (player.gender) {
            case 0 -> {
                SkillService.gI().learSkillSpecial(player, Skill.DRAGON, 7);
                SkillService.gI().learSkillSpecial(player, Skill.KAMEJOKO, 7);
                SkillService.gI().learSkillSpecial(player, Skill.THAI_DUONG_HA_SAN, 7);
                SkillService.gI().learSkillSpecial(player, Skill.KAIOKEN, 7);
                SkillService.gI().learSkillSpecial(player, Skill.QUA_CAU_KENH_KHI, 7);
                SkillService.gI().learSkillSpecial(player, Skill.DICH_CHUYEN_TUC_THOI, 7);
                SkillService.gI().learSkillSpecial(player, Skill.THOI_MIEN, 1);
                SkillService.gI().learSkillSpecial(player, Skill.KHIEN_NANG_LUONG, 7);
                SkillService.gI().learSkillSpecial(player, Skill.SUPER_KAME, 1);
                SkillService.gI().learSkillSpecial(player, Skill.BIEN_HINH, 5);
                SkillService.gI().learSkillSpecial(player, Skill.PHAN_THAN, 7);
            }
            case 2 -> {
                SkillService.gI().learSkillSpecial(player, Skill.GALICK, 7);
                SkillService.gI().learSkillSpecial(player, Skill.ANTOMIC, 7);
                SkillService.gI().learSkillSpecial(player, Skill.TAI_TAO_NANG_LUONG, 7);
                SkillService.gI().learSkillSpecial(player, Skill.BIEN_KHI, 7);
                SkillService.gI().learSkillSpecial(player, Skill.TU_SAT, 7);
                SkillService.gI().learSkillSpecial(player, Skill.HUYT_SAO, 7);
                SkillService.gI().learSkillSpecial(player, Skill.TROI, 1);
                SkillService.gI().learSkillSpecial(player, Skill.KHIEN_NANG_LUONG, 7);
                SkillService.gI().learSkillSpecial(player, Skill.LIEN_HOAN_CHUONG, 1);
                SkillService.gI().learSkillSpecial(player, Skill.BIEN_HINH, 5);
                SkillService.gI().learSkillSpecial(player, Skill.PHAN_THAN, 7);
            }
            default -> {
                SkillService.gI().learSkillSpecial(player, Skill.DEMON, 7);
                SkillService.gI().learSkillSpecial(player, Skill.MASENKO, 7);
                SkillService.gI().learSkillSpecial(player, Skill.TRI_THUONG, 7);
                SkillService.gI().learSkillSpecial(player, Skill.MAKANKOSAPPO, 7);
                SkillService.gI().learSkillSpecial(player, Skill.DE_TRUNG, 7);
                SkillService.gI().learSkillSpecial(player, Skill.LIEN_HOAN, 7);
                SkillService.gI().learSkillSpecial(player, Skill.SOCOLA, 1);
                SkillService.gI().learSkillSpecial(player, Skill.KHIEN_NANG_LUONG, 7);
                SkillService.gI().learSkillSpecial(player, Skill.MA_PHONG_BA, 1);
                SkillService.gI().learSkillSpecial(player, Skill.BIEN_HINH, 5);
                SkillService.gI().learSkillSpecial(player, Skill.PHAN_THAN, 7);
            }
        }
    }

    private void spawnDragon(Player player) {
        ShenronEvent shenron = new ShenronEvent();
        shenron.setPlayer(player);
        ShenronEventManager.gI().add(shenron);
        player.shenronEvent = shenron;
        shenron.setZone(player.zone);
        shenron.activeShenron(true, ShenronEvent.DRAGON_EVENT);
        shenron.sendWhishesShenron();
    }
    
    private void spawnDragonnoel(Player player) {
        ShenronEventnoel shenron = new ShenronEventnoel();
        shenron.setPlayer(player);
        ShenronEventManagernoel.gI().add(shenron);
        player.shenronEventnoel = shenron;
        shenron.setZone(player.zone);
        shenron.activeShenronoel(true, ShenronEventnoel.DRAGON_EVENT_NOEL);
        shenron.sendWhishesShenron();
    }

    private void showAdminMenu(Player player) {
        StringBuilder info = new StringBuilder()
                .append("|0|--- QUẢN LÝ SERVER ---\n")
                .append("Time Start : ").append(ServerManager.timeStart).append("\n")
                .append("Online     : ").append(Client.gI().getPlayers().size()).append(" người chơi\n")
                .append("Sessions   : ").append(SessionManager.gI().getNumSession()).append("\n")
                .append("Threads    : ").append(Thread.activeCount()).append(" luồng\n")
                .append(SystemMetrics.ToString());

        NpcService.gI().createMenuConMeo(
                player,
                ConstNpc.MENU_ADMIN,
                -1,
                info.toString(),
                "Ngọc Rồng",
                "Đệ Tử",
                "Bảo Trì",
                "Tìm Kiếm\nNgười Chơi",
                "Boss",
                "Call Broly",
                "Buff VND",
                "Buff\nHộp Thư",
                "Đóng"
        );
    }

    private void repeatNotify(String message, int times) {
        for (int i = 0; i < times; i++) {
            ServerNotify.gI().notify(message);
        }
    }

    private void showBotMenu(Player player) {
        StringBuilder info = new StringBuilder()
                .append("|0|--- QUẢN LÝ BOT ---\n")
                .append("Player Online : ").append(Client.gI().getPlayers().size()).append("\n")
                .append("Threads       : ").append(Thread.activeCount()).append("\n")
                .append("Bot Online    : ").append(BotManager.gI().bot.size()).append("\n");

        NpcService.gI().createMenuConMeo(
                player,
                ConstNpc.MENU_BOT,
                -1,
                info.toString(),
                "Bot\nPem Quái",
                "Bot\nPem Nappa",
                "Bot\nPem Tương Lai",
                "Bot\nPem Cold",
                "Bot\nBán Item",
                "Bot\nSăn Boss",
                "Bot\nUp Đệ",
                "Bot\nChatTG",
                "Đóng"
        );
    }

    private boolean parseAndAddSM(Player player, String value, boolean isPet) {
        try {
            long power = Long.parseLong(value.replaceAll("[^0-9]", ""));
            Service.gI().addSMTN(isPet ? player.pet : player, (byte) 2, power, false);
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    private boolean changeMap(Player player, String text) {
        try {
            int mapId = Integer.parseInt(text.replace("m", ""));
            ChangeMapService.gI().changeMapInYard(player, mapId, -1, -1);
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    private boolean setPoint(Player player, String type, String text) {
        try {
            String numberPart = text.substring(type.length()).trim(); // lấy phần số sau type
            long value = Long.parseLong(numberPart);

            switch (type) {
                case "dmg" ->
                    player.nPoint.dameg = value;
                case "hpg" ->
                    player.nPoint.hpg = value;
                case "smg" ->
                    player.nPoint.power = value;
                case "kig" ->
                    player.nPoint.mpg = value;
                case "defg" ->
                    player.nPoint.defg = (int) value;
                case "crg" ->
                    player.nPoint.critg = (int) value;
                default -> {
                    return false; // loại prefix không hợp lệ
                }
            }

            Service.gI().point(player);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean setTask(Player player, String text) {
        try {
            int idTask = Integer.parseInt(text.replace("ntask", ""));
            player.playerTask.taskMain.id = idTask - 1;
            player.playerTask.taskMain.index = 0;
            TaskService.gI().sendNextTaskMain(player);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean giveItem(Player player, String text) {
        try {
            String args = text.substring(1).trim();
            String[] parts = args.split("\\s+");
            short id;
            int quantity = 1;

            if (parts.length == 1) {
                String digits = parts[0].replaceAll("[^0-9]", "");
                if (!digits.isEmpty()) {
                    id = Short.parseShort(digits);
                } else {
                    Service.gI().sendThongBao(player, "ID không hợp lệ!");
                    return true;
                }
            } else if (parts.length >= 2) {
                id = Short.parseShort(parts[0]);
                quantity = Integer.parseInt(parts[1]);
            } else {
                Service.gI().sendThongBao(player, "Sai cú pháp!");
                return true;
            }

            Item item = ItemService.gI().createNewItem(id, quantity);
            List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop(id);
            if (!ops.isEmpty()) {
                item.itemOptions = ops;
            }

            InventoryService.gI().addItemBag(player, item);
            InventoryService.gI().sendItemBag(player);
            Service.gI().sendThongBao(player, "GET " + item.template.name + " [" + item.template.id + "] SUCCESS !");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Service.gI().sendThongBao(player, "Có lỗi xảy ra!");
            return true;
        }
    }
}
