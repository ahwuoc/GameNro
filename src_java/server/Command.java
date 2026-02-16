package server;

import DucPro.SystemMetrics;
import boss.AnTromManager;
import boss.BossManager;
import boss.BrolyManager;
import boss.GasDestroyManager;
import boss.OtherBossManager;
import boss.RedRibbonHQManager;
import boss.SnakeWayManager;
import boss.TreasureUnderSeaManager;
import boss.TrungThuEventManager;
import consts.ConstNpc;
import item.Item;
import models.Template.SkillTemplate;
import utils.SkillUtil;

import java.util.List;

import minigame.LuckyNumber.LuckyNumber;
import models.GiftCode.GiftCodeManager;
import models.ShenronEvent.ShenronEvent;
import models.ShenronEvent.ShenronEventManager;
import network.SessionManager;
import player.Pet;
import player.Player;
import player.badges.BadgesData;
import services.InventoryService;
import services.ItemService;
import services.NpcService;
import services.PetService;
import services.Service;
import services.SkillService;
import services.TaskService;
import services.func.ChangeMapService;
import services.func.Input;
import skill.Skill;
import bot.Bot;
import bot.BotManager;
import bot.BotType;

public class Command {

    private static Command instance;

    public static Command gI() {
        if (instance == null) {
            instance = new Command();
        }
        return instance;
    }

    public void NhanChat(Player player, String text) {
        if (!check(player, text)) {
            Service.gI().chat(player, text);
        }
    }

    public boolean check(Player player, String text) {
        if (player.isAdmin()) {
            if (text.equals("giftcode")) {
                GiftCodeManager.gI().checkInfomationGiftCode(player);
                return true;
            } else if (text.equals("mapboss")) {
                BossManager.gI().showListBoss(player);
                return true;
            } else if (text.equals("mapbroly")) {
                BrolyManager.gI().showListBoss(player);
                return true;
            } else if (text.equals("mapantrom")) {
                AnTromManager.gI().showListBoss(player);
                return true;
            } else if (text.equals("mapboss2")) {
                OtherBossManager.gI().showListBoss(player);
                return true;
            } else if (text.equals("mapdt")) {
                RedRibbonHQManager.gI().showListBoss(player);
                return true;
            } else if (text.equals("mapbdkb")) {
                TreasureUnderSeaManager.gI().showListBoss(player);
                return true;
            } else if (text.equals("mapcdrd")) {
                SnakeWayManager.gI().showListBoss(player);
                return true;
            } else if (text.equals("mapkghd")) {
                GasDestroyManager.gI().showListBoss(player);
                return true;
            } else if (text.equals("lb")) {

                Service.gI().sendThongBaoFromAdmin(player, "<10>Countdown 10 giây\n" +
                        "Ban nhan duoc item {{457-1}} {{457-2}}" +
                        "\\nBan nhan duoc item {{1125-10}}" +
                        "\\nBan nhan duoc item {{1126-10}}");

                return true;
            } else if (text.equals("maptrungthu")) {
                TrungThuEventManager.gI().showListBoss(player);
                return true;
            } else if (text.equals("hsk")) {
                Service.gI().releaseCooldownSkill(player);
                return true;
            } else if (text.startsWith("sp")) {
                try {
                    long power = Long.parseLong(text.substring(3));
                    Service.gI().addSMTN(player, (byte) 2, power, false);
                    return true;
                } catch (Exception e) {
                }
            } else if (text.equals("battu")) {
                if (player.isBattu) {
                    player.isBattu = false;
                } else {
                    player.isBattu = true;
                }
                Service.gI().sendThongBao(player, "Bất tử" + (player.isBattu ? ": ON" : ": OFF"));
                return true;
            } else if (text.startsWith("dt ")) {
                try {
                    long power = Long.parseLong(text.substring(3)); // cắt "dt "
                    Service.gI().addSMTN(player.pet, (byte) 2, power, false);
                    return true;
                } catch (NumberFormatException e) {
                    Service.gI().sendThongBao(player, "Sai format  Ví dụ: dt 100000");
                }
            } else if (text.equals("fullskill")) {
                player.playerSkill.skills.clear();
                List<SkillTemplate> templates = Manager.NCLASS.get(player.gender).skillTemplatess;
                for (SkillTemplate template : templates) {
                    Skill skill = SkillUtil.createSkill(template.id, 7);
                    if (skill != null) {
                        player.playerSkill.skills.add(skill);
                    }
                }
                player.nPoint.setFullHpMp();
                Service.gI().point(player);
                Service.gI().player_loader_info(player);
                Service.gI().sendThongBao(player, "Đã học full skill!");
                return true;
            } else if (text.equals("test")) {
                switch (player.gender) {
                    case 0 ->
                        SkillService.gI().learSkillSpecial(player, Skill.SUPER_KAME, 1);
                    case 2 ->
                        SkillService.gI().learSkillSpecial(player, Skill.LIEN_HOAN_CHUONG, 1);
                    default ->
                        SkillService.gI().learSkillSpecial(player, Skill.MA_PHONG_BA, 1);
                }
                return true;

            } else if (text.equals("broly")) {

                SkillService.gI().learSkillSpecial(player, Skill.SUPER_BROLY, 1);

                return true;
            } else if (text.equals("test2")) {
                switch (player.gender) {
                    case 0 -> {
                        SkillService.gI().learSkillSpecial(player, Skill.PHAN_THAN, 6);
                    }
                    case 2 -> {
                        SkillService.gI().learSkillSpecial(player, Skill.PHAN_THAN, 6);
                    }
                    default -> {
                        SkillService.gI().learSkillSpecial(player, Skill.PHAN_THAN, 6);
                    }
                }
                return true;
            } else if (text.equals("dragon")) {
                ShenronEvent shenron = new ShenronEvent();
                shenron.setPlayer(player);
                ShenronEventManager.gI().add(shenron);
                player.shenronEvent = shenron;
                shenron.setZone(player.zone);
                shenron.activeShenron(true, ShenronEvent.DRAGON_EVENT);
                shenron.sendWhishesShenron();
                return true;
            } else if (text.equals("admin")) {
                NpcService.gI().createMenuConMeo(player, ConstNpc.MENU_ADMIN, -1,
                        "|0|Time start: " + ServerManager.timeStart + "\nClients: " + Client.gI().getPlayers().size()
                                + " người chơi\n Sessions: " + SessionManager.gI().getNumSession() + "\nThreads: "
                                + Thread.activeCount() + " luồng" + "\n" + SystemMetrics.ToString(),
                        "Ngọc rồng", "Đệ tử", "Bảo trì", "Tìm kiếm\nngười chơi", "Boss", "Call Super Bư",
                        "Call BROLYSUPERVIP", "Buff VND", "Buff\nhộp thư", "Bot Manager");
                return true;
            } else if (text.equals("bot") || text.equals("botmanager")) {
                // Show Bot Manager Menu
                showBotManagerMenu(player);
                return true;

            } else if (text.equals("vnd")) {
                Input.gI().createFormBuffVND(player);
                return true;
            } else if (text.equals("daucatmoi")) {
                for (int i = 0; i < 10; i++) {
                    ServerNotify.gI().notify("BOSS Nro vừa xuất hiện tại nhà anh ấy");
                }
                return true;
            } else if (text.startsWith("m ")) {
                int mapId = Integer.parseInt(text.replace("m ", ""));
                ChangeMapService.gI().changeMapInYard(player, mapId, -1, -1);
                return true;
            }
            if (text.startsWith("dmg_")) {
                try {
                    long dameg = Long.parseLong(text.replaceAll("dmg_", ""));
                    player.nPoint.dameg = dameg;
                    Service.gI().point(player);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (text.startsWith("hpg_")) {
                try {
                    long hpg = Long.parseLong(text.replaceAll("hpg_", ""));
                    player.nPoint.hpg = hpg;
                    Service.gI().point(player);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (text.startsWith("mpg_")) {
                try {
                    long mpg = Integer.parseInt(text.replaceAll("mpg", ""));
                    player.nPoint.mpg = mpg;
                    Service.gI().point(player);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (text.startsWith("defg")) {
                try {
                    int defg = Integer.parseInt(text.replaceAll("defg", ""));
                    player.nPoint.defg = defg;
                    Service.gI().point(player);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (text.startsWith("crg")) {
                try {
                    int critg = Integer.parseInt(text.replaceAll("crg", ""));
                    player.nPoint.critg = critg;
                    Service.gI().point(player);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (text.startsWith("ntask")) {
                try {
                    int idTask = Integer.parseInt(text.replaceAll("ntask", ""));
                    player.playerTask.taskMain.id = idTask - 1;
                    player.playerTask.taskMain.index = 0;
                    TaskService.gI().sendNextTaskMain(player);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (text.startsWith("badges_")) {
                int idBadges = Integer.parseInt(text.replaceAll("badges_", ""));
                player.badges.idBadges = idBadges;
            }
            if (text.startsWith("kq")) {
                Service.gI().sendThongBao(player, "Kết quả Lucky Round tiếp theo là: " + LuckyNumber.RESULT);
                return true;
            }
            if (text.startsWith("danhhieu_")) {
                int idGender = Integer.parseInt(text.replaceAll("danhhieu_", ""));
                BadgesData data = new BadgesData(player, idGender, 5);
                return true;
            }
            if (text.startsWith("gender_")) {
                byte idGender = Byte.parseByte(text.replaceAll("gender_", ""));
                player.gender = idGender;
                return true;
            }
            if (text.startsWith("i")) {
                String[] parts = text.split(" ");
                if (parts.length >= 3) {
                    short id = Short.parseShort(parts[1]);
                    int quantity = Integer.parseInt(parts[2]);
                    Item item = ItemService.gI().createNewItem(id, quantity);
                    List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop((short) id);
                    if (!ops.isEmpty()) {
                        item.itemOptions = ops;
                    }
                    InventoryService.gI().addItemBag(player, item);
                    InventoryService.gI().sendItemBag(player);
                    Service.gI().sendThongBao(player,
                            "GET " + item.template.name + " [" + item.template.id + "] SUCCESS !");
                    return true;
                } else {
                    Service.gI().sendThongBao(player, "Lỗi");
                    return true;
                }
            } else if (text.equals("item")) {
                Input.gI().createFormGiveItem(player);
                return true;
            } else if (text.equals("getitem")) {
                Input.gI().createFormGetItem(player);
                return true;
            } else if (text.equals("d")) {
                Service.gI().setPos(player, player.location.x, player.location.y + 10);
                return true;
            }
        }
        if (text.startsWith("ten con la ")) {
            PetService.gI().changeNamePet(player, text.replaceAll("ten con la ", ""));
        }
        // else if (text.equals("rsp")) { // hồi all skill, Ki
        // Service.gI().releaseCooldownSkill(player.pet);
        // return true;
        // }

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
        return false;
    }

    // ==================== Bot Manager ====================

    /**
     * Hiển thị menu quản lý Bot
     */
    public void showBotManagerMenu(Player player) {
        if (!player.isAdmin()) {
            Service.gI().sendThongBao(player, "Không đủ quyền hạn!");
            return;
        }

        java.util.List<Bot> allBots = BotManager.gI().getAllBots();
        int totalBots = allBots.size();
        int farmMobCount = (int) allBots.stream().filter(b -> b.getData().getBotType() == BotType.FARM_MOB).count();
        int farmBossCount = (int) allBots.stream().filter(b -> b.getData().getBotType() == BotType.FARM_BOSS).count();
        int npcCount = (int) allBots.stream().filter(b -> b.getData().getBotType() == BotType.NPC).count();
        int farmDeTuCount = (int) allBots.stream().filter(b -> b.getData().getBotType() == BotType.FARM_DE_TU).count();

        String info = "|0|=== BOT MANAGER ==="
                + "\nTổng số Bot: " + totalBots
                + "\n- Farm Quái: " + farmMobCount
                + "\n- Farm Boss: " + farmBossCount
                + "\n- Bot NPC: " + npcCount
                + "\n- Farm Đệ Tử: " + farmDeTuCount;

        NpcService.gI().createMenuConMeo(player, ConstNpc.MENU_BOT_MANAGER, -1, info,
                "Tạo Bot\nFarm Quái", "Tạo Bot\nFarm Boss", "Tạo Bot\nNPC", "Tạo Bot\nFarm Đệ Tử", "Xóa tất cả\nBot",
                "Đóng");
    }

    /**
     * Xử lý menu Bot Manager
     */
    public void handleBotManagerMenu(Player player, int select) {
        if (!player.isAdmin()) {
            return;
        }

        switch (select) {
            case 0 -> Input.gI().createFormSpawnBotFarmMob(player);
            case 1 -> Input.gI().createFormSpawnBotFarmBoss(player);
            case 2 -> Input.gI().createFormSpawnBotNPC(player);
            case 3 -> Input.gI().createFormSpawnBotFarmDeTu(player);
            case 4 -> Input.gI().createFormRemoveAllBots(player);
        }
    }
}
