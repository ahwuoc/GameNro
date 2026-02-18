package nro.models.npc.npc_manifest;

/**
 * Box ZALO:https://zalo.me/g/ifjict764 sdt zalo: 0358176187 Chuyên chỉnh sữa
 * mua bán source nro,...
 */
import consts.ConstDailyGift;
import consts.ConstMenu;
import consts.ConstNpc;
import event.EventManager;
import item.Item;
import item.Item.ItemOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import jdbc.daos.PlayerDAO;
import models.Combine.Combine;

import models.Combine.CombineService;
import models.Combine.manifest.CheTaoCuonSachCu;
import models.Combine.manifest.DoiSachTuyetKy;
import models.Combine.manifest.NangCapVatPham;
import models.DeathOrAliveArena.DeathOrAliveArena;
import models.DeathOrAliveArena.DeathOrAliveArenaManager;
import models.DeathOrAliveArena.DeathOrAliveArenaService;
import network.Message;
import nro.models.npc.Npc;
import nro.player.Player;
import player.dailyGift.DailyGiftService;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.NpcService;
import nro.services.Service;
import services.func.ChangeMapService;
import services.func.TopService;
import shop.ShopService;
import utils.Util;

public class BaHatMit extends Npc {

    public BaHatMit(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            switch (this.mapId) {
                case 5 -> {
                    // --- Nếu sự kiện 20-11 (Ngày Nhà Giáo Việt Nam) đang bật ---
                    if (EventManager.TEACHERS_DAY) {
                        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                "Ngươi tìm ta có việc gì?",
                                "Sự kiện\n20-11",
                                "Chức năng\nPha lê",
                                "Chuyển hóa\nTrang bị",
                                "Nâng cấp\nChân mệnh",
                                "Nâng cấp\nKích hoạt",
//                                "Nâng Cấp\n Đệ Tử",
                                "Di chuyển"
                        );
                    } else if (EventManager.HALLOWEEN) {
                        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                "Ngươi tìm ta có việc gì?",
                                "Sự kiện\nHalloween",
                                "Chức năng\nPha lê",
                                "Chuyển hóa\nTrang bị",
                                "Nâng cấp\nChân mệnh",
                                "Nâng cấp\nKích hoạt",
//                                "Nâng Cấp\n Đệ Tử",
                                "Di chuyển"
                        );
                    } else {
                        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                "Ngươi tìm ta có việc gì?",
                                "Chức năng\nPha lê",
                                "Chuyển hóa\nTrang bị",
                                "Nâng cấp\nChân mệnh",
                                "Nâng cấp\nKích hoạt",
                                "Di chuyển"
                        );
                    }
                }
                case 112 -> {
                    if (Util.isAfterMidnight(player.lastTimePKVoDaiSinhTu)) {
                        player.haveRewardVDST = false;
                        player.thoiVangVoDaiSinhTu = 0;
                    }
                    if (player.haveRewardVDST) {
                        this.createOtherMenu(player, ConstNpc.BASE_MENU, "Đây là phần thưởng cho con.", "1 ngọc bí\nbất kì", "1 bí ngô");
                        return;
                    }
                    if (DeathOrAliveArenaManager.gI().getVDST(player.zone) != null) {
                        if (DeathOrAliveArenaManager.gI().getVDST(player.zone).getPlayer().equals(player)) {
                            this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ngươi muốn hủy đăng ký thi đấu võ đài?", "Top 100", "Đồng ý\n" + player.thoiVangVoDaiSinhTu + " thỏi vàng", "Từ chối", "Về\nđảo rùa");
                            return;
                        }
                        this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ngươi muốn đăng ký thi đấu võ đài?\nnhiều phần thưởng giá trị đang đợi ngươi đó", "Top 100", "Bình chọn", "Đồng ý\n" + player.thoiVangVoDaiSinhTu + " thỏi vàng", "Từ chối", "Về\nđảo rùa");
                        return;
                    }
                    this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ngươi muốn đăng ký thi đấu võ đài?\nnhiều phần thưởng giá trị đang đợi ngươi đó", "Top 100", "Đồng ý\n" + player.thoiVangVoDaiSinhTu + " thỏi vàng", "Từ chối", "Về\nđảo rùa");
                }
                case 174 ->
                    this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ngươi tìm ta có việc gì?", "Quay về", "Từ chối");
                case 181 ->
                    this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ngươi tìm ta có việc gì?", "Quay về", "Từ chối");
                default -> {
                    List<String> menu = new ArrayList<>(Arrays.asList("Sách\nTuyệt Kỹ", "Cửa hàng\nBùa", "Nâng cấp\nVật phẩm", "Làm phép\nNhập đá", "Nhập\nNgọc Rồng"));
                    if (InventoryService.gI().findItem(player, 454) || InventoryService.gI().findItem(player, 921)) {
                        menu = new ArrayList<>(Arrays.asList("Sách\nTuyệt Kỹ", "Cửa hàng\nBùa", "Nâng cấp\nVật phẩm", InventoryService.gI().findItemBongTaiCap2(player) ? "Mở chỉ số\nBông tai\nPorata cấp\n2" : "Nâng cấp\nBông tai\nPorata", "Làm phép\nNhập đá", "Nhập\nNgọc Rồng"));
                    }
                    if (InventoryService.gI().findItem(player, 921) || InventoryService.gI().findItem(player, 1810)) {
                        menu = new ArrayList<>(Arrays.asList("Sách\nTuyệt Kỹ", "Cửa hàng\nBùa", "Nâng cấp\nVật phẩm", InventoryService.gI().findItemBongTaiCap2(player) ? "Mở chỉ số\nBông tai\nPorata cấp\n2" : "Nâng cấp\nBông tai\nPorata", InventoryService.gI().findItemBongTaiCap3(player) ? "Mở chỉ số\nBông tai\nPorata cấp\n3" : "Nâng cấp\nBông tai\nPorata cấp\n3", "Làm phép\nNhập đá", "Nhập\nNgọc Rồng"));
                    }
                    if (DailyGiftService.checkDailyGift(player, ConstDailyGift.NHAN_BUA_MIEN_PHI)) {
                        menu.add(0, "Thưởng\nBùa 1h\nngẫu nhiên");
                    }
                    String[] menus = menu.toArray(new String[0]);
                    this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ngươi tìm ta có việc gì?", menus);
                }
            }
        }
    }
    
    private void handleuoc1000ngoc(Player player) {
        if (InventoryService.gI().getCountEmptyBag(player) <= 0) {
            Service.gI().sendThongBao(player, "Hành trang của bạn đã đầy.");
            return;
        }

        // kiểm tra ruby
        if (player.inventory.ruby < 1000) {
            Service.gI().sendThongBao(player, "Bạn cần 1000 hồng ngọc để ước.");
            return;
        }

        // trừ ruby
        player.inventory.ruby -= 1000;
        Service.gI().sendMoney(player);

        // --- Tỉ lệ item ---
        Map<Short, Integer> itemWeight = Map.of((short) 1970, 80);
        short itemId = getRandomItemByWeight(itemWeight);

        Item newItem = ItemService.gI().createNewItem(itemId, 1);

        // --- Option theo item ---
        Map<Short, List<ItemOptionConfig>> optionMap = Map.of(
                (short) 1970, List.of(
                        new ItemOptionConfig(50, 7, 12, true),
                        new ItemOptionConfig(77, 7, 12, true),
                        new ItemOptionConfig(103, 7, 12, true),
                        new ItemOptionConfig(30, 0, false)
                )
        );

        applyOptions(newItem, optionMap.get(itemId));

        InventoryService.gI().addItemBag(player, newItem);
        InventoryService.gI().sendItemBag(player);
        Service.gI().sendThongBao(player, "Bạn đã nhận được " + newItem.template.name);
    }
    
    private void applyOptions(Item item, List<ItemOptionConfig> configs) {
        if (configs == null) {
            return;
        }
        for (ItemOptionConfig cfg : configs) {
            int value = cfg.isRandom
                    ? ThreadLocalRandom.current().nextInt(cfg.min, cfg.max + 1)
                    : cfg.min;
            item.itemOptions.add(new ItemOption(cfg.id, value));
        }
    }
    
    private short getRandomItemByWeight(Map<Short, Integer> weights) {
        int total = weights.values().stream().mapToInt(i -> i).sum();
        int r = ThreadLocalRandom.current().nextInt(total);
        for (var e : weights.entrySet()) {
            if (r < e.getValue()) {
                return e.getKey();
            }
            r -= e.getValue();
        }
        return 0;
    }
    
    private static class ItemOptionConfig {

        int id, min, max;
        boolean isRandom;

        ItemOptionConfig(int id, int min, int max, boolean isRandom) {
            this.id = id;
            this.min = min;
            this.max = max;
            this.isRandom = isRandom;
        }

        ItemOptionConfig(int id, int value, boolean isRandom) {
            this(id, value, value, isRandom);
        }
    }
    
    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            Item dathuctinh = InventoryService.gI().findItemBag(player, 1278);
            Item xuvang = InventoryService.gI().findItemBag(player, 1703);
            int DaThucTinh = dathuctinh != null ? dathuctinh.quantity : 0;
            int Xu = xuvang != null ? xuvang.quantity : 0;
            int levelPet = (player.pet != null) ? player.level : 0;
            int da = 300 * (levelPet + 1);
            int xu = 3 * (levelPet + 1);
            switch (this.mapId) {
                case 5 -> {
                    switch (player.iDMark.getIndexMenu()) {
                        case ConstNpc.BASE_MENU -> {
                            // 🔹 Nếu chọn dòng đầu tiên (Sự kiện)
                            // ✅ CHUẨN HÓA PHẦN MENU SỰ KIỆN
                            if (select == 0) {
                                // Ưu tiên Halloween trước (nếu đang bật)
                                if (EventManager.HALLOWEEN) {
                                    createOtherMenu(player, ConstMenu.MENU_ENVENT_HALLOWEEN,
                                            "Halloween vui vẻ!\nTa có thể giúp gì cho ngươi?",
                                            "Làm\nHộp Kẹo\nMa Quỷ",
                                            "Làm\n10xHộp Kẹo\nMa Quỷ",
                                            "Ước bằng\n1000 ngọc",
                                            "Từ chối");
                                    return;
                                }

                                // Nếu không phải Halloween mà là sự kiện 20/11
                                if (EventManager.TEACHERS_DAY) {
                                    createOtherMenu(player, ConstMenu.MENU_ENVENT_20_11,
                                            "Ngày Nhà Giáo Việt Nam 20/11!\nTa có thể giúp gì cho con?",
                                            "Làm\nTúi Trà Khô",
                                            "Làm\nHộp Trà",
                                            "Làm\nHộp Trà Hoa\nCúc",
                                            "Làm\nThiệp Chúc\nThường",
                                            "Làm\nThiệp Chúc\nĐặc Biệt",
                                            "Hướng dẫn",
                                            "Từ chối");
                                    return;
                                }
                            }

                            // 🔹 Nếu event đang tắt → bỏ qua dòng “Sự kiện 20-11” và dồn chỉ số lại
                            int index = select; // Giữ nguyên chỉ số khi có sự kiện đặc biệt
                            if (!EventManager.HALLOWEEN && !EventManager.TEACHERS_DAY) {
                                index = select + 1; // Khi KHÔNG có event nào thì dồn lại
                            }

                            switch (index) {
                                case 1 -> {
                                    createOtherMenu(player, ConstMenu.MENU_PHA_LE,
                                            "Ta có thể giúp gì cho ngươi ?",
                                            "Ép sao\ntrang bị",
                                            "Pha lê\nhoá\ntrang bị",
                                            "Nâng cấp\nSao pha lê",
                                            "Đánh bóng\nSao pha lê",
                                            "Cường hoá\nLỗ sao\npha lê",
                                            "Tạo đá Hematite",
                                            "Tẩy Sao\nPha Lê");
                                }
//                                case 1 -> //Chuyển hoá trang bị
//                                    createOtherMenu(player, ConstMenu.MENU_NANG_CAP_TRANG_BI, "Ta sẽ biến trang bị của ngươi thành trang bị Kích Hoạt", "Kích hoạt\nThường", "Kích hoạt\nVIP");
                                case 2 -> {//Chuyển hoá trang bị
                                    createOtherMenu(player, ConstMenu.MENU_CHUYEN_HOA_TRANG_BI,
                                            "Ta sẽ biến trang bị mới cao cấp hơn của người\n"
                                            + "thành trang bị có cấp độ và sao pha lê của trang bị cũ",
                                            "Chuyển hóa\nDùng vàng", "Chuyển hóa\nDùng ngọc");
                                }
                                case 3 -> {
                                    CombineService.gI().openTabCombine(player, CombineService.NANG_CAP_CHAN_MENH);
                                }
                                case 4 -> {//Chuyển hoá trang bị
                                    createOtherMenu(player, ConstMenu.MENU_KICH_HOAT,
                                            "Ngươi cần ta giúp gì?\n",
                                            "Phân rã\nTrang bị\nKích hoạt",
                                            "Tái tạo\nCapsule\nKích hoạt");
                                }
//                                case 5 -> {
//                                    this.createOtherMenu(player, 11233, player.pet != null
//                                            ? (player.cap >= 10 && player.level >= 10
//                                                    ? "Pet của bạn đã đạt cấp và level tối đa!\nKhông thể nâng cấp thêm." : (player.level >= 10
//                                                            ? "Pet hiện tại: " + player.pet.name.replaceAll("\\$|Cấp \\d+ Level \\d+", "").trim()
//                                                            + " Cấp " + player.cap + " Level " + player.level
//                                                            + "\nSau nâng cấp: " + player.pet.name.replaceAll("\\$|Cấp \\d+ Level \\d+", "").trim()
//                                                            + " Cấp " + (player.cap < 10 ? player.cap + 1 : player.cap) + " Level 0"
//                                                            + "\nĐiều kiện: Pet Level 10 trở lên"
//                                                            + "\nNgươi cần Đá Thức Tỉnh [" + DaThucTinh + "/" + da + "]"
//                                                            + "\nVà " + Xu + "/" + xu + " Cái nịt"
//                                                            + "\nBạn có muốn nâng cấp Cấp cho đệ tử không?"
//                                                            : "Pet hiện tại: " + player.pet.name.replaceAll("\\$|Cấp \\d+ Level \\d+", "").trim()
//                                                            + " Cấp " + player.cap + " Level " + player.level
//                                                            + "\nSau nâng cấp: " + player.pet.name.replaceAll("\\$|Cấp \\d+ Level \\d+", "").trim()
//                                                            + " Cấp " + player.cap + " Level " + (player.level + 1)
//                                                            + "\nĐiều kiện: " + da + " điểm Boss + " + xu + " Cái nịt để tăng 1 Level"
//                                                            + "\n(Bạn còn " + player.getSession().diemboss + " điểm Boss và " + Xu + " Cái nịt)"
//                                                            + "\nBạn có muốn nâng cấp Level cho đệ tử không?")) : "Bạn chưa có Pet!", player.pet != null
//                                                    ? (player.cap >= 10 && player.level >= 10 ? "Đã Nâng\nToàn Bộ"
//                                                            : (player.level >= 10 ? "Nâng cấp\nĐệ" : "Nâng cấp\nLevel"))
//                                                    : "Đã Nâng\nToàn Bộ",
//                                            "Từ chối"
//                                    );
//                                }
                                case 5 -> {//Chuyển hoá trang bị
                                    createOtherMenu(player, ConstMenu.MRNU_DI_CHUYEN,
                                            "Ngươi muốn di chuyển đến đâu?\n",
                                            "Xuống\nĐịa Ngục",
                                            "Võ đài\nSinh Tử");
                                }
//                                case 4 ->
//                                    ChangeMapService.gI().changeMapNonSpaceship(player,
//                                            174,
//                                            110 + Util.nextInt(0, 100),
//                                            408);
//                                case 5 ->
//                                    ChangeMapService.gI().changeMapNonSpaceship(player, 112, 200 + Util.nextInt(-100, 100), 408);
//                                case 5 ->
//                                    CombineService.gI().openTabCombine(player, CombineService.PHAN_RA_TRANG_BI_KH);
//                                case 6 ->
//                                    CombineService.gI().openTabCombine(player, CombineService.TAI_TAO_CAPSULE_KH);
//                                case 3 ->
//                                    createOtherMenu(player, ConstMenu.CHUC_NANG_BHM_KHAC, "Ngươi tìm ta có việc gì?\n",
//                                            "Đập đồ\nẢo Hóa",
//                                            "Build Đồ",
//                                            "Pháp sư hoá",
//                                            "Siêu hóa\n Cải trang",
//                                            "Tinh ấn\ntrang bị",
//                                            "Tinh thạch\ntrang bị",
//                                            "Nâng cấp\nGiáp LT");
//                                case 4 ->
//                                    this.createOtherMenu(player, ConstMenu.SHOP_BHM, "|7|CHÂN MỆNH"
//                                            + "\n\n|1|Bạn đang có: " + (player.event.getEventPointBHM()) + " Điểm Săn Boss"
//                                            + "\n|3| Lưu ý: Chỉ được nhận Chân mệnh 1 lần (Hành trang chỉ tồn tại 1 Chân mệnh)"
//                                            + "\nNếu đã có Chân mệnh. Ta sẽ giúp ngươi nâng cấp bậc lên với các dòng chỉ số cao hơn",
//                                            player.event.getEventPointBHM() >= 1000 ? "Nhận Chân mệnh" : "Cần 1000 điểm\n để nhận", "Nâng cấp Chân mệnh", "Shop\nĐIỂM", "Shop\nBà Hạt Mít"
//                                    );
//                                case 5->
//                                    CombineService.gI().openTabCombine(player, CombineService.TAI_TAO_TRANG_BI_KICH_HOAT);    
                            }
                        }
                        case 11233 -> {
                            if (player.pet == null) {
                                Service.gI().sendThongBao(player, "Bạn chưa có Pet!");
                                return;
                            }

                            if (player.cap == 10 && player.level == 10) {
                                Service.gI().sendThongBao(player, "Toàn bộ đã đạt cấp tối đa!");
                                return;
                            }

                            while (player.level < 10) {
                                if (player.getSession().diemboss >= da && Xu >= xu) {
                                    player.getSession().diemboss -= da;
                                    player.level++;
                                    String rawName = player.pet.name != null ? player.pet.name.trim() : "Đệ tử";
                                    rawName = rawName.replaceAll("Cấp \\d+ Level \\d+", "").trim();
                                    if (rawName.startsWith("$")) {
                                        rawName = rawName.substring(1);
                                    }
                                    player.pet.name = "$" + rawName + " Cấp " + player.cap + " Level " + player.level;
                                    Service.gI().sendThongBao(player, "Nâng cấp thành công! Pet đã lên Level " + player.level);
                                    InventoryService.gI().subQuantityItemsBag(player, xuvang, xu);
                                    PlayerDAO.updatePlayer(player);
                                } else {
                                    Service.gI().sendThongBao(player, "Không đủ điểm Boss hoặc Cái nịt để nâng Level cho Pet!");
                                    return;
                                }
                            }

                            if (player.level >= 10 && player.cap < 10) {
                                if (DaThucTinh >= da && Xu >= xu) {
                                    int cap = player.cap;
                                    int tile = switch (cap) {
                                        case 0 ->
                                            100;
                                        case 1 ->
                                            80;
                                        case 2 ->
                                            50;
                                        case 3 ->
                                            40;
                                        case 4, 5, 7 ->
                                            35;
                                        case 6 ->
                                            30;
                                        case 8 ->
                                            20;
                                        case 9 ->
                                            10;
                                        default ->
                                            5;
                                    };
                                    InventoryService.gI().subQuantityItemsBag(player, dathuctinh, da);
                                    InventoryService.gI().subQuantityItemsBag(player, xuvang, xu);
                                    if (Util.isTrue(tile, 100)) {
                                        player.cap++;
                                        player.level = 0;
                                        String rawName = player.pet.name != null ? player.pet.name.trim() : "Đệ tử";
                                        rawName = rawName.replaceAll("Cấp \\d+ Level \\d+", "").trim();
                                        if (rawName.startsWith("$")) {
                                            rawName = rawName.substring(1);
                                        }
                                        player.pet.name = "$" + rawName + " Cấp " + player.cap + " Level " + player.level;
                                        Service.gI().sendThongBao(player, "Nâng cấp thành công! Pet đã lên cấp " + player.cap + "!");
                                        player.pet.nPoint.setFullHpMp();
                                        PlayerDAO.updatePlayer(player);
                                        ChangeMapService.gI().exitMap(player.pet);
                                    } else {
                                        String message = "Nâng cấp thất bại! Đệ tử vẫn giữ nguyên cấp " + player.cap;
                                        if (Util.isTrue(40, 100)) {
                                            if (player.level > 0) {
                                                player.level--;
                                                String rawName = player.pet.name != null ? player.pet.name.trim() : "Đệ tử";
                                                rawName = rawName.replaceAll("Cấp \\d+ Level \\d+", "").trim();
                                                if (rawName.startsWith("$")) {
                                                    rawName = rawName.substring(1);
                                                }
                                                player.pet.name = "$" + rawName + " Cấp " + player.cap + " Level " + player.level;
                                                message += "\nKhông may! Level đệ tử giảm xuống còn " + player.level;
                                            }
                                        }
                                        Service.gI().sendThongBao(player, message);
                                    }
                                } else {
                                    Service.gI().sendThongBao(player, "Bạn không đủ Đá Thức Tỉnh hoặc Cái nịt để nâng cấp!");
                                }
                            }
                        }
                        case ConstMenu.MENU_ENVENT_HALLOWEEN -> {
                            switch (select) {

                                case 0 -> {
                                    Item keobantay = InventoryService.gI().findItemBagByTemp(player, 901);
                                    Item giayhalloween = InventoryService.gI().findItemBagByTemp(player, 1354);
                                    Item keotraibi = InventoryService.gI().findItemBagByTemp(player, 1355);
                                    int sokeo = keobantay != null ? keobantay.quantity : 0;
                                    boolean dukeo = sokeo >= 99;
                                    int sogiay = giayhalloween != null ? giayhalloween.quantity : 0;
                                    boolean dugiay = sogiay >= 1;
                                    int sotrai = keotraibi != null ? keotraibi.quantity : 0;
                                    boolean dutrai = sotrai >= 3;

                                    String mauLa = dukeo ? "|2|" : "|7|";
                                    String mauVang = dugiay ? "|2|" : "|7|";
                                    String mauDO = dutrai ? "|2|" : "|7|";

                                    String NpcSay = "|1|Hộp Kẹo Ma Quỷ\n"
                                            + mauLa + "Kẹo bàn tay " + sokeo + "/99\n"
                                            + mauVang + "Giỏ đựng kẹo trái bí " + sogiay + "/1\n"
                                            + mauDO + "Giấy trang trí Halloween " + sotrai + "/3\n";

                                    if (dukeo && dugiay && dutrai) {
                                        createOtherMenu(player, ConstNpc.LAM_HOP_KEO_MA_QUY, NpcSay, "Đồng ý", "Từ chối");
                                    } else {
                                        createOtherMenu(player, ConstNpc.LAM_HOP_KEO_MA_QUY_2, NpcSay, "Từ chối");
                                    }
                                }
                                case 1 -> {
                                    Item keobantay = InventoryService.gI().findItemBagByTemp(player, 901);
                                    Item giayhalloween = InventoryService.gI().findItemBagByTemp(player, 1354);
                                    Item keotraibi = InventoryService.gI().findItemBagByTemp(player, 1355);
                                    int sokeo = keobantay != null ? keobantay.quantity : 0;
                                    boolean dukeo = sokeo >= 990;
                                    int sogiay = giayhalloween != null ? giayhalloween.quantity : 0;
                                    boolean dugiay = sogiay >= 10;
                                    int sotrai = keotraibi != null ? keotraibi.quantity : 0;
                                    boolean dutrai = sotrai >= 30;

                                    String mauLa = dukeo ? "|2|" : "|7|";
                                    String mauVang = dugiay ? "|2|" : "|7|";
                                    String mauDO = dutrai ? "|2|" : "|7|";

                                    String NpcSay = "|1|Hộp Kẹo Ma Quỷ\n"
                                            + mauLa + "Kẹo bàn tay " + sokeo + "/990\n"
                                            + mauVang + "Giỏ đựng kẹo trái bí " + sogiay + "/10\n"
                                            + mauDO + "Giấy trang trí Halloween " + sotrai + "/30\n";

                                    if (dukeo && dugiay && dutrai) {
                                        createOtherMenu(player, ConstNpc.LAM_HOP_KEO_MA_QUY_x10, NpcSay, "Đồng ý", "Từ chối");
                                    } else {
                                        createOtherMenu(player, ConstNpc.LAM_HOP_KEO_MA_QUY_x10_2, NpcSay, "Từ chối");
                                    }
                                }
                                case 2 -> {
                                    createOtherMenu(player, 121,
                                            "1000 ngọc cũng được, hãy ước đi thể hiện mình là đại gia.",
                                            "Đồng ý", "Từ chối");
                                }
                            }
                        }
                        case 121 -> {
                            handleuoc1000ngoc(player);
                        }
                        case ConstNpc.LAM_HOP_KEO_MA_QUY -> {
                            Item keoBanTay = InventoryService.gI().findItemBagByTemp(player, 901);
                            Item gioTraiBi = InventoryService.gI().findItemBagByTemp(player, 1354);
                            Item giayTrangTri = InventoryService.gI().findItemBagByTemp(player, 1355);

                            if (keoBanTay != null && keoBanTay.quantity >= 99
                                    && gioTraiBi != null && gioTraiBi.quantity >= 1
                                    && giayTrangTri != null && giayTrangTri.quantity >= 3) {

                                if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                    // 🧧 Tạo vật phẩm “Hộp Kẹo Ma Quỷ”
                                    Item hopKeo = ItemService.gI().createNewItem((short) 1356); // ID bạn có thể đổi theo ý

                                    // Trừ nguyên liệu
                                    InventoryService.gI().subQuantityItemsBag(player, keoBanTay, 99);
                                    InventoryService.gI().subQuantityItemsBag(player, gioTraiBi, 1);
                                    InventoryService.gI().subQuantityItemsBag(player, giayTrangTri, 3);

                                    // Thêm vật phẩm
                                    InventoryService.gI().addItemBag(player, hopKeo);
                                    InventoryService.gI().sendItemBag(player);

                                    // Thông báo
                                    Service.gI().sendThongBao(player, "Bạn đã tạo thành công Hộp Kẹo Ma Quỷ!");
                                    Service.gI().chat(player, "Hộp Kẹo Ma Quỷ đã hoàn thành!");
                                    Service.gI().sendEffAllPlayer(player, (short) 13, 1, -1, 1); // hiệu ứng khi chế thành công (tùy chọn)
                                } else {
                                    Service.gI().sendThongBao(player, "Hành trang không còn chỗ trống!");
                                }
                            } else {
                                Service.gI().sendThongBao(player, "Thiếu nguyên liệu để tạo Hộp Kẹo Ma Quỷ!");
                            }
                        }

                        case ConstNpc.LAM_HOP_KEO_MA_QUY_x10 -> {
                            Item keoBanTay = InventoryService.gI().findItemBagByTemp(player, 901);
                            Item gioTraiBi = InventoryService.gI().findItemBagByTemp(player, 1354);
                            Item giayTrangTri = InventoryService.gI().findItemBagByTemp(player, 1355);

                            if (keoBanTay != null && keoBanTay.quantity >= 990
                                    && gioTraiBi != null && gioTraiBi.quantity >= 10
                                    && giayTrangTri != null && giayTrangTri.quantity >= 30) {

                                if (InventoryService.gI().getCountEmptyBag(player) >= 1) { // 1 slot đủ, vì add nối tiếp
                                    // 🧧 Tạo 10 hộp
                                    for (int i = 0; i < 10; i++) {
                                        Item hopKeo = ItemService.gI().createNewItem((short) 1368); // ID hộp kẹo ma quỷ
                                        InventoryService.gI().addItemBag(player, hopKeo);
                                    }

                                    // Trừ nguyên liệu
                                    InventoryService.gI().subQuantityItemsBag(player, keoBanTay, 990);
                                    InventoryService.gI().subQuantityItemsBag(player, gioTraiBi, 10);
                                    InventoryService.gI().subQuantityItemsBag(player, giayTrangTri, 30);

                                    // Cập nhật túi
                                    InventoryService.gI().sendItemBag(player);

                                    // Thông báo & hiệu ứng
                                    Service.gI().sendThongBao(player, "Bạn đã làm thành công 10 Hộp Kẹo Ma Quỷ!");
                                    Service.gI().chat(player, "Quá trình chế tạo 10 Hộp Kẹo Ma Quỷ hoàn tất!");
                                    Service.gI().sendEffAllPlayer(player, (short) 13, 1, -1, 1); // hiệu ứng chế tạo
                                } else {
                                    Service.gI().sendThongBao(player, "Hành trang không đủ chỗ trống!");
                                }
                            } else {
                                Service.gI().sendThongBao(player, "Thiếu nguyên liệu để chế 10 Hộp Kẹo Ma Quỷ!");
                            }
                        }

                        case ConstMenu.MENU_ENVENT_20_11 -> {
                            switch (select) {

                                case 0 -> {
                                    Item latratuoi = InventoryService.gI().findItemBagByTemp(player, 1364);
                                    int soLa = latratuoi != null ? latratuoi.quantity : 0;
                                    boolean duLa = soLa >= 99;
                                    boolean duVang = player.inventory.getGold() >= 100_000_000;

                                    String mauLa = duLa ? "|2|" : "|7|";
                                    String mauVang = duVang ? "|2|" : "|7|";
                                    String mauMoTa = (duLa && duVang) ? "|2|" : "|7|";

                                    String NpcSay = "|1|Để làm ra Túi trà khô cần:\n"
                                            + mauLa + "Lá trà tươi " + soLa + "/99\n"
                                            + mauVang + "Giá vàng: 100.000.000\n"
                                            + mauMoTa + "Với công thức gia truyền nhà ta\n"
                                            + mauMoTa + "Đảm bảo thơm ngon và...\n"
                                            + mauMoTa + "Giòn luôn cả sâu!";

                                    if (duLa && duVang) {
                                        createOtherMenu(player, ConstNpc.LAM_TUI_TRA_KHO, NpcSay, "Đồng ý", "Từ chối");
                                    } else {
                                        createOtherMenu(player, ConstNpc.LAM_TUI_TRA_KHO_2, NpcSay, "Từ chối");
                                    }
                                }

                                case 1 -> {
                                    Item tuitrakho = InventoryService.gI().findItemBagByTemp(player, 1368);
                                    Item quetre = InventoryService.gI().findItemBagByTemp(player, 1366);
                                    Item niatra = InventoryService.gI().findItemBagByTemp(player, 1365);

                                    int soTui = tuitrakho != null ? tuitrakho.quantity : 0;
                                    int soQue = quetre != null ? quetre.quantity : 0;
                                    int soNia = niatra != null ? niatra.quantity : 0;

                                    boolean duTui = soTui >= 1;
                                    boolean duQue = soQue >= 1;
                                    boolean duNia = soNia >= 1;
                                    boolean duVang = player.inventory.getGold() >= 100_000_000;
                                    boolean duHet = duTui && duQue && duNia && duVang;

                                    String mauTui = duTui ? "|2|" : "|7|";
                                    String mauQue = duQue ? "|2|" : "|7|";
                                    String mauNia = duNia ? "|2|" : "|7|";
                                    String mauVang = duVang ? "|2|" : "|7|";
                                    String mauMoTa = duHet ? "|2|" : "|7|";

                                    String NpcSay = "|1|Để làm ra 1 Hộp trà cần:\n"
                                            + mauTui + "Túi trà khô " + soTui + "/1\n"
                                            + mauQue + "Que tre " + soQue + "/1\n"
                                            + mauNia + "Nia tre " + soNia + "/1\n"
                                            + mauVang + "Giá vàng: 100.000.000\n"
                                            + mauMoTa + "Thơm ngon đậm vị, tinh hoa Việt!";

                                    if (duHet) {
                                        createOtherMenu(player, ConstNpc.HOP_TRA, NpcSay, "Đồng ý", "Từ chối");
                                    } else {
                                        createOtherMenu(player, ConstNpc.HOP_TRA_2, NpcSay, "Từ chối");
                                    }
                                }

                                case 2 -> {
                                    Item hoptra = InventoryService.gI().findItemBagByTemp(player, 1369);
                                    Item tuitrakho1 = InventoryService.gI().findItemBagByTemp(player, 1368);
                                    Item quetre1 = InventoryService.gI().findItemBagByTemp(player, 1366);
                                    Item niatra1 = InventoryService.gI().findItemBagByTemp(player, 1365);
                                    Item hoacuc = InventoryService.gI().findItemBagByTemp(player, 1367);

                                    int soHop = hoptra != null ? hoptra.quantity : 0;
                                    int soTui = tuitrakho1 != null ? tuitrakho1.quantity : 0;
                                    int soQue = quetre1 != null ? quetre1.quantity : 0;
                                    int soNia = niatra1 != null ? niatra1.quantity : 0;
                                    int soHoa = hoacuc != null ? hoacuc.quantity : 0;

                                    boolean duHop = soHop >= 1;
                                    boolean duTui = soTui >= 5;
                                    boolean duQue = soQue >= 1;
                                    boolean duNia = soNia >= 1;
                                    boolean duHoa = soHoa >= 1;
                                    boolean duVang = player.inventory.getGold() >= 100_000_000;

                                    boolean duHet = duHop && duTui && duQue && duNia && duHoa && duVang;

                                    String mauHop = duHop ? "|2|" : "|7|";
                                    String mauTui = duTui ? "|2|" : "|7|";
                                    String mauQue = duQue ? "|2|" : "|7|";
                                    String mauNia = duNia ? "|2|" : "|7|";
                                    String mauHoa = duHoa ? "|2|" : "|7|";
                                    String mauVang = duVang ? "|2|" : "|7|";
                                    String mauMoTa = duHet ? "|2|" : "|7|";

                                    String NpcSay = "|1|Để làm ra 1 Hộp trà hoa cúc cần:\n"
                                            + mauHop + "Hộp trà " + soHop + "/1\n"
                                            + mauTui + "Túi trà khô " + soTui + "/5\n"
                                            + mauQue + "Que tre " + soQue + "/1\n"
                                            + mauNia + "Nia tre " + soNia + "/1\n"
                                            + mauHoa + "Hoa cúc " + soHoa + "/1\n"
                                            + mauVang + "Giá vàng: 100.000.000\n"
                                            + mauMoTa + "Với công thức đặc biệt, đảm bảo thơm ngon và...\n"
                                            + mauMoTa + "Giòn luôn cả sâu!";

                                    if (duHet) {
                                        createOtherMenu(player, ConstNpc.HOP_TRA_HOA_CUC, NpcSay, "Đồng ý", "Từ chối");
                                    } else {
                                        createOtherMenu(player, ConstNpc.HOP_TRA_HOA_CUC_2, NpcSay, "Từ chối");
                                    }
                                }

                                case 3 -> {
                                    Item manhgiay = InventoryService.gI().findItemBagByTemp(player, 1373);
                                    Item baobithiep = InventoryService.gI().findItemBagByTemp(player, 1372);
                                    Item keodan = InventoryService.gI().findItemBagByTemp(player, 1374);

                                    int soGiay = manhgiay != null ? manhgiay.quantity : 0;
                                    int soBithi = baobithiep != null ? baobithiep.quantity : 0;
                                    int soKeo = keodan != null ? keodan.quantity : 0;

                                    boolean duGiay = soGiay >= 99;
                                    boolean duBithi = soBithi >= 1;
                                    boolean duKeo = soKeo >= 1;
                                    boolean duVang = player.inventory.getGold() >= 100_000_000;

                                    boolean duHet = duGiay && duBithi && duKeo && duVang;

                                    String mauGiay = duGiay ? "|2|" : "|7|";
                                    String mauBithi = duBithi ? "|2|" : "|7|";
                                    String mauKeo = duKeo ? "|2|" : "|7|";
                                    String mauVang = duVang ? "|2|" : "|7|";
                                    String mauMoTa = duHet ? "|2|" : "|7|";

                                    String NpcSay = "|1|Để làm ra 1 Thiệp chúc thường cần:\n"
                                            + mauGiay + "Mảnh giấy " + soGiay + "/99\n"
                                            + mauBithi + "Bao bì thiệp " + soBithi + "/1\n"
                                            + mauKeo + "Keo dán " + soKeo + "/1\n"
                                            + mauVang + "Giá vàng: 100.000.000\n"
                                            + mauMoTa + "Viết nên lời chúc chân thành nhất!";

                                    if (duHet) {
                                        createOtherMenu(player, ConstNpc.THIEP_CHUC_THUONG, NpcSay, "Đồng ý", "Từ chối");
                                    } else {
                                        createOtherMenu(player, ConstNpc.THIEP_CHUC_THUONG_2, NpcSay, "Từ chối");
                                    }
                                }

                                case 4 -> {
                                    Item manhgiay1 = InventoryService.gI().findItemBagByTemp(player, 1373);
                                    Item baobithiep1 = InventoryService.gI().findItemBagByTemp(player, 1372);
                                    Item keodan1 = InventoryService.gI().findItemBagByTemp(player, 1374);
                                    Item loichuc = InventoryService.gI().findItemBagByTemp(player, 1375);

                                    int soGiay = manhgiay1 != null ? manhgiay1.quantity : 0;
                                    int soBithi = baobithiep1 != null ? baobithiep1.quantity : 0;
                                    int soKeo = keodan1 != null ? keodan1.quantity : 0;
                                    int soLoi = loichuc != null ? loichuc.quantity : 0;

                                    boolean duGiay = soGiay >= 99;
                                    boolean duBithi = soBithi >= 1;
                                    boolean duKeo = soKeo >= 1;
                                    boolean duLoi = soLoi >= 1;
                                    boolean duVang = player.inventory.getGold() >= 100_000_000;

                                    boolean duHet = duGiay && duBithi && duKeo && duLoi && duVang;

                                    String mauGiay = duGiay ? "|2|" : "|7|";
                                    String mauBithi = duBithi ? "|2|" : "|7|";
                                    String mauKeo = duKeo ? "|2|" : "|7|";
                                    String mauLoi = duLoi ? "|2|" : "|7|";
                                    String mauVang = duVang ? "|2|" : "|7|";
                                    String mauMoTa = duHet ? "|2|" : "|7|";

                                    String NpcSay = "|1|Để làm ra 1 Thiệp chúc đặc biệt cần:\n"
                                            + mauGiay + "Mảnh giấy " + soGiay + "/99\n"
                                            + mauBithi + "Bao bì thiệp " + soBithi + "/1\n"
                                            + mauKeo + "Keo dán " + soKeo + "/1\n"
                                            + mauLoi + "Lời chúc " + soLoi + "/1\n"
                                            + mauVang + "Giá vàng: 100.000.000\n"
                                            + mauMoTa + "Một tấm thiệp đầy ý nghĩa, trân trọng gửi yêu thương!";

                                    if (duHet) {
                                        createOtherMenu(player, ConstNpc.THIEP_CHUC_DAC_BIET, NpcSay, "Đồng ý", "Từ chối");
                                    } else {
                                        createOtherMenu(player, ConstNpc.THIEP_CHUC_DAC_BIET_2, NpcSay, "Từ chối");
                                    }
                                }

                                case 5 ->
                                    NpcService.gI().createTutorial(player, this.avartar, ConstNpc.HUONG_DAN_2010);
                            }
                        }
                        case ConstMenu.MENU_KICH_HOAT -> {
                            switch (select) {
                                case 0 -> {
                                    CombineService.gI().openTabCombine(player, CombineService.PHAN_RA_TRANG_BI_KH);
                                }
                                case 1 -> {
                                    CombineService.gI().openTabCombine(player, CombineService.TAI_TAO_CAPSULE_KH);
                                }
                            }
                        }
                        case ConstNpc.LAM_TUI_TRA_KHO -> {
                            switch (select) {
                                case 0 -> {
                                    Item latratuoi = InventoryService.gI().findItemBagByTemp(player, 1364);
                                    if (latratuoi != null && latratuoi.quantity >= 99) {
                                        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                            Item tuitrakho = ItemService.gI().createNewItem((short) 1368);
                                            if (player.inventory.getGold() >= 100_000_000) {
                                                player.inventory.subGold(100_000_000);
                                                Service.gI().sendMoney(player);
                                                InventoryService.gI().subQuantityItemsBag(player, latratuoi, 99);
                                                InventoryService.gI().addItemBag(player, tuitrakho);
                                                InventoryService.gI().sendItemBag(player);
                                                Service.gI().sendThongBao(player, "Bạn đã làm thành công Túi trà khô!");
                                            } else {
                                                Service.gI().sendThongBao(player, "Bạn không đủ 100.000.000 vàng!");
                                            }
                                        } else {
                                            Service.gI().sendThongBao(player, "Hành trang không còn chỗ trống!");
                                        }
                                    } else {
                                        Service.gI().sendThongBao(player, "Thiếu Lá trà tươi x99!");
                                    }
                                }
                            }
                        }

                        case ConstNpc.HOP_TRA -> {
                            switch (select) {
                                case 0 -> {
                                    Item tuitrakho = InventoryService.gI().findItemBagByTemp(player, 1368);
                                    Item quetre = InventoryService.gI().findItemBagByTemp(player, 1366);
                                    Item niatre = InventoryService.gI().findItemBagByTemp(player, 1365);

                                    if ((tuitrakho != null && tuitrakho.quantity >= 1)
                                            && (quetre != null && quetre.quantity >= 1)
                                            && (niatre != null && niatre.quantity >= 1)) {

                                        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                            if (player.inventory.getGold() >= 100_000_000) {
                                                player.inventory.subGold(100_000_000);
                                                Service.gI().sendMoney(player);
                                                Item hoptra = ItemService.gI().createNewItem((short) 1369);
                                                InventoryService.gI().addItemBag(player, hoptra);
                                                InventoryService.gI().subQuantityItemsBag(player, tuitrakho, 1);
                                                InventoryService.gI().subQuantityItemsBag(player, quetre, 1);
                                                InventoryService.gI().subQuantityItemsBag(player, niatre, 1);
                                                InventoryService.gI().sendItemBag(player);
                                                Service.gI().sendThongBao(player, "Bạn đã làm thành công Hộp trà!");
                                            } else {
                                                Service.gI().sendThongBao(player, "Bạn không đủ 100.000.000 vàng!");
                                            }
                                        } else {
                                            Service.gI().sendThongBao(player, "Hành trang không còn chỗ trống!");
                                        }
                                    } else {
                                        Service.gI().sendThongBao(player, "Thiếu nguyên liệu: Túi trà khô, Que tre, Nia tre!");
                                    }
                                }
                            }
                        }

                        case ConstNpc.HOP_TRA_HOA_CUC -> {
                            switch (select) {
                                case 0 -> {
                                    Item hoptra = InventoryService.gI().findItemBagByTemp(player, 1369);
                                    Item tuitrakho = InventoryService.gI().findItemBagByTemp(player, 1368);
                                    Item quetre = InventoryService.gI().findItemBagByTemp(player, 1366);
                                    Item niatre = InventoryService.gI().findItemBagByTemp(player, 1365);
                                    Item hoacuc = InventoryService.gI().findItemBagByTemp(player, 1367);

                                    if ((hoptra != null && hoptra.quantity >= 1)
                                            && (tuitrakho != null && tuitrakho.quantity >= 5)
                                            && (quetre != null && quetre.quantity >= 1)
                                            && (niatre != null && niatre.quantity >= 1)
                                            && (hoacuc != null && hoacuc.quantity >= 1)) {

                                        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                            if (player.inventory.getGold() >= 100_000_000) {
                                                player.inventory.subGold(100_000_000);
                                                Service.gI().sendMoney(player);
                                                Item hoptrahoacuc = ItemService.gI().createNewItem((short) 1370);
                                                InventoryService.gI().addItemBag(player, hoptrahoacuc);
                                                InventoryService.gI().subQuantityItemsBag(player, hoptra, 1);
                                                InventoryService.gI().subQuantityItemsBag(player, tuitrakho, 5);
                                                InventoryService.gI().subQuantityItemsBag(player, quetre, 1);
                                                InventoryService.gI().subQuantityItemsBag(player, niatre, 1);
                                                InventoryService.gI().subQuantityItemsBag(player, hoacuc, 1);
                                                InventoryService.gI().sendItemBag(player);
                                                Service.gI().sendThongBao(player, "Bạn đã làm thành công Hộp trà hoa cúc!");
                                            } else {
                                                Service.gI().sendThongBao(player, "Bạn không đủ 100.000.000 vàng!");
                                            }
                                        } else {
                                            Service.gI().sendThongBao(player, "Hành trang không còn chỗ trống!");
                                        }
                                    } else {
                                        Service.gI().sendThongBao(player, "Thiếu nguyên liệu: Hộp trà, Túi trà khô x5, Que tre, Nia tre, Hoa cúc!");
                                    }
                                }
                            }
                        }

                        case ConstNpc.THIEP_CHUC_THUONG -> {
                            switch (select) {
                                case 0 -> {
                                    Item manhgiay = InventoryService.gI().findItemBagByTemp(player, 1373);
                                    Item baobithiep = InventoryService.gI().findItemBagByTemp(player, 1372);
                                    Item keodan = InventoryService.gI().findItemBagByTemp(player, 1374);

                                    if ((manhgiay != null && manhgiay.quantity >= 99)
                                            && (baobithiep != null && baobithiep.quantity >= 1)
                                            && (keodan != null && keodan.quantity >= 1)) {

                                        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                            if (player.inventory.getGold() >= 100_000_000) {
                                                player.inventory.subGold(100_000_000);
                                                Service.gI().sendMoney(player);
                                                Item thiepchucthuong = ItemService.gI().createNewItem((short) 1376);
                                                InventoryService.gI().addItemBag(player, thiepchucthuong);
                                                InventoryService.gI().subQuantityItemsBag(player, manhgiay, 99);
                                                InventoryService.gI().subQuantityItemsBag(player, baobithiep, 1);
                                                InventoryService.gI().subQuantityItemsBag(player, keodan, 1);
                                                InventoryService.gI().sendItemBag(player);
                                                Service.gI().sendThongBao(player, "Bạn đã làm thành công Thiệp chúc thường!");
                                            } else {
                                                Service.gI().sendThongBao(player, "Bạn không đủ 100.000.000 vàng!");
                                            }
                                        } else {
                                            Service.gI().sendThongBao(player, "Hành trang không còn chỗ trống!");
                                        }
                                    } else {
                                        Service.gI().sendThongBao(player, "Thiếu nguyên liệu: Mảnh giấy x99, Bao bì thiệp, Keo dán!");
                                    }
                                }
                            }
                        }

                        case ConstNpc.THIEP_CHUC_DAC_BIET -> {
                            switch (select) {
                                case 0 -> {
                                    Item manhgiay = InventoryService.gI().findItemBagByTemp(player, 1373);
                                    Item baobithiep = InventoryService.gI().findItemBagByTemp(player, 1372);
                                    Item keodan = InventoryService.gI().findItemBagByTemp(player, 1374);
                                    Item loichuc = InventoryService.gI().findItemBagByTemp(player, 1375);

                                    if ((manhgiay != null && manhgiay.quantity >= 99)
                                            && (baobithiep != null && baobithiep.quantity >= 1)
                                            && (keodan != null && keodan.quantity >= 1)
                                            && (loichuc != null && loichuc.quantity >= 1)) {

                                        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                            if (player.inventory.getGold() >= 100_000_000) {
                                                player.inventory.subGold(100_000_000);
                                                Service.gI().sendMoney(player);
                                                Item thiepchucdacbiet = ItemService.gI().createNewItem((short) 1609);
                                                InventoryService.gI().addItemBag(player, thiepchucdacbiet);
                                                InventoryService.gI().subQuantityItemsBag(player, manhgiay, 99);
                                                InventoryService.gI().subQuantityItemsBag(player, baobithiep, 1);
                                                InventoryService.gI().subQuantityItemsBag(player, keodan, 1);
                                                InventoryService.gI().subQuantityItemsBag(player, loichuc, 1);
                                                InventoryService.gI().sendItemBag(player);
                                                Service.gI().sendThongBao(player, "Bạn đã làm thành công Thiệp chúc đặc biệt!");
                                            } else {
                                                Service.gI().sendThongBao(player, "Bạn không đủ 100.000.000 vàng!");
                                            }
                                        } else {
                                            Service.gI().sendThongBao(player, "Hành trang không còn chỗ trống!");
                                        }
                                    } else {
                                        Service.gI().sendThongBao(player, "Thiếu nguyên liệu: Mảnh giấy x99, Bao bì thiệp, Keo dán, Lời chúc!");
                                    }
                                }
                            }
                        }

                        case ConstMenu.MRNU_DI_CHUYEN -> {
                            switch (select) {
                                case 0 -> {
                                    ChangeMapService.gI().changeMapNonSpaceship(player, 174, 110 + Util.nextInt(0, 100), 408);
                                }
                                case 1 -> {
                                    ChangeMapService.gI().changeMapNonSpaceship(player, 112, 200 + Util.nextInt(-100, 100), 408);
                                }
                            }
                        }
                        case ConstMenu.CHUC_NANG_BHM_KHAC -> {
                            switch (select) {
                                case 0: //Đập đồ ảo hóa
                                    CombineService.gI().openTabCombine(player, CombineService.DAP_DO_AO_HOA);
                                    break;
                                case 1: //Build Đồ
                                    this.createOtherMenu(player, ConstMenu.BUILD_DO_BHM, "|7|\bChi Tiết:\n|0|"
                                            + "\nMở Khóa GD: tỉ lệ thành công 30%"
                                            + "\nGia hạn Vật Phẩm: tỉ lệ thành công 30% + 3 - 7 ngày, 70% + 1 ngày"
                                            + "\nTẩy đồ: tẩy sao pha lê, chỉ số đặc biệt một số trang bị",
                                            "Mở Khóa GD", "Gia hạn\n Vật Phẩm", "Tẩy Đồ"
                                    );
                                    break;//Zalo: 0358124452  
                                case 2: //Pháp sư hóa trang bị
                                    CombineService.gI().openTabCombine(player, CombineService.PS_HOA_TRANG_BI);
                                    break;
                                case 3: //Siêu Hóa
                                    CombineService.gI().openTabCombine(player, CombineService.SIEU_HOA);
                                    break;
                                case 4:
                                    CombineService.gI().openTabCombine(player, CombineService.AN_TRANG_BI);
                                    break;//Name: EMTI 
                                case 5:
                                    CombineService.gI().openTabCombine(player, CombineService.TINH_THACH_HOA);
                                    break;//Name: EMTI  
                                case 6:
                                    CombineService.gI().openTabCombine(player, CombineService.NANG_GIAP_LUYEN_TAP);
                                    break;//Name: EMTI 
                            }
                        }
                        case ConstMenu.BUILD_DO_BHM -> {
                            switch (select) {
                                case 0: //Mở khóa Item
                                    CombineService.gI().openTabCombine(player, CombineService.MO_KHOA_ITEM);
                                    break;
                                case 1: //Gia hạn vật phẩm
                                    CombineService.gI().openTabCombine(player, CombineService.GIA_HAN_VAT_PHAM);
                                    break;
                                case 2: //Tẩy đồ
                                    CombineService.gI().openTabCombine(player, CombineService.TAY_PS_HOA_TRANG_BI);
                                    break;
                            }
                        }
                        case ConstMenu.SHOP_BHM -> {
                            switch (select) {
                                case 0: //Nhận chân mệnh
//                                    if (player.event.getEventPointBHM() >= 1000) {
//                                         Item item
//                                    }
                                    Service.gI().sendThongBao(player, "Chưa mở");
                                    break;
                                case 1: //Nâng cấp chân mệnh
                                    CombineService.gI().openTabCombine(player, CombineService.NANG_CAP_CHAN_MENH);
                                    break;
                                case 2: //Tẩy đồ
                                    ShopService.gI().opendShop(player, "SHOP_BHM", false);
                                    break;
                                case 3: //Tẩy đồ
                                    ShopService.gI().opendShop(player, "SHOP_THOI_BHM", false);
                                    break;
                            }
                        }
                        case ConstMenu.MENU_PHA_LE -> {
                            switch (select) {
                                case 0: //Ép sao trang bị
                                    CombineService.gI().openTabCombine(player, CombineService.EP_SAO_TRANG_BI);
                                    break;
                                case 1: //Pha lê hoá trang bị
                                    createOtherMenu(player, ConstMenu.MENU_PHA_LE_HOA_TRANG_BI, "Ngươi muốn pha lê hoá trang bị bằng cách nào?", "Bằng ngọc", "Từ chối");
                                    break;
                                case 2: //Nâng cấp sao pha lê
                                    CombineService.gI().openTabCombine(player, CombineService.NANG_CAP_SAO_PHA_LE);
                                    break;
                                case 3: //Đánh bóng sao pha lê
                                    CombineService.gI().openTabCombine(player, CombineService.DANH_BONG_SAO_PHA_LE);
                                    break;
                                case 4: //Cường hoá lỗ sao pha lê
                                    CombineService.gI().openTabCombine(player, CombineService.CUONG_HOA_LO_SAO_PHA_LE);
                                    break;
                                case 5: //Tạo đá Hematite
                                    CombineService.gI().openTabCombine(player, CombineService.TAO_DA_HEMATITE);
                                    break;
                                case 6: //Tẩy sao
                                    CombineService.gI().openTabCombine(player, CombineService.TAY_SAO_PHA_LE);
                                    break;
                            }
                        }
                        case ConstMenu.MENU_NANG_CAP_TRANG_BI -> {
                            switch (select) {
                                case 0:
                                    CombineService.gI().openTabCombine(player, CombineService.NANG_CAP_KICH_HOAT);
                                    break;
                                case 1:
                                    CombineService.gI().openTabCombine(player, CombineService.NANG_CAP_KICH_HOAT_VIP);
                                    break;
                            }
                        }
                        case ConstMenu.MENU_CHUYEN_HOA_TRANG_BI -> {
                            switch (select) {
                                case 0 -> {
                                    CombineService.gI().openTabCombine(player, CombineService.CHUYEN_HOA_TRANG_BI_DUNG_VANG);
                                }
                                case 1 -> {
                                    CombineService.gI().openTabCombine(player, CombineService.CHUYEN_HOA_TRANG_BI_DUNG_NGOC);
                                }
                            }
                        }
                        case ConstMenu.MENU_PHA_LE_HOA_TRANG_BI -> {
                            if (select == 0) {
                                CombineService.gI().openTabCombine(player, CombineService.PHA_LE_HOA_TRANG_BI);
                            }
                        }
                        case ConstNpc.MENU_START_COMBINE -> {
                            switch (player.combine.typeCombine) {
                                case CombineService.PHA_LE_HOA_TRANG_BI -> {
                                    switch (select) {
                                        case 0 ->
                                            CombineService.gI().startCombine(player, 100);
                                        case 1 ->
                                            CombineService.gI().startCombine(player, 10);
                                        case 2 ->
                                            CombineService.gI().startCombine(player);
                                    }
                                }

                                case CombineService.NANG_CAP_KICH_HOAT_VIP, CombineService.NANG_CAP_KICH_HOAT, CombineService.NANG_CAP_SAO_PHA_LE, CombineService.DANH_BONG_SAO_PHA_LE, CombineService.CUONG_HOA_LO_SAO_PHA_LE, CombineService.TAO_DA_HEMATITE, CombineService.EP_SAO_TRANG_BI, CombineService.DAP_DO_AO_HOA, CombineService.PS_HOA_TRANG_BI, CombineService.TAY_PS_HOA_TRANG_BI, CombineService.SIEU_HOA, CombineService.AN_TRANG_BI, CombineService.TINH_THACH_HOA, CombineService.NANG_GIAP_LUYEN_TAP, CombineService.MO_KHOA_ITEM, CombineService.GIA_HAN_VAT_PHAM, CombineService.NANG_CAP_CHAN_MENH, CombineService.TAI_TAO_TRANG_BI_KICH_HOAT, CombineService.PHAN_RA_TRANG_BI_KH, CombineService.TAI_TAO_CAPSULE_KH, CombineService.TAY_SAO_PHA_LE -> {
                                    switch (select) {
                                        case 0:
                                            CombineService.gI().startCombine(player);
                                            break;
                                    }
                                }
                            }
                        }
                    }
                }
                case 112 -> {
                    if (player.iDMark.isBaseMenu()) {
                        if (player.haveRewardVDST) {
                            switch (select) {
                                case 0 -> {
                                    if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                        Item item = ItemService.gI().createNewItem((short) (Util.nextInt(705, 708)));
                                        item.itemOptions.add(new Item.ItemOption(93, 30));
                                        InventoryService.gI().addItemBag(player, item);
                                        InventoryService.gI().sendItemBag(player);
                                        Service.gI().sendThongBao(player, "Bạn nhận được " + item.template.name);
                                        player.haveRewardVDST = false;
                                    } else {
                                        Service.gI().sendThongBao(player, "Hành trang không còn chỗ trống, không thể nhặt thêm");
                                    }
                                }
                                case 1 -> {
                                    if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                        Item item = ItemService.gI().createNewItem((short) 585);
                                        item.itemOptions.add(new Item.ItemOption(93, 30));
                                        InventoryService.gI().addItemBag(player, item);
                                        InventoryService.gI().sendItemBag(player);
                                        Service.gI().sendThongBao(player, "Bạn nhận được " + item.template.name);
                                        player.haveRewardVDST = false;
                                    } else {
                                        Service.gI().sendThongBao(player, "Hành trang không còn chỗ trống, không thể nhặt thêm");
                                    }
                                }
                            }
                            return;
                        }
                        if (DeathOrAliveArenaManager.gI().getVDST(player.zone) != null) {
                            if (DeathOrAliveArenaManager.gI().getVDST(player.zone).getPlayer().equals(player)) {
                                switch (select) {
                                    case 0 -> {
                                        TopService.showListTop(player, 5);
                                    }
                                    case 1 ->
                                        this.npcChat("Không thể thực hiện");
                                    case 2 -> {
                                    }
                                    case 3 ->
                                        ChangeMapService.gI().changeMapBySpaceShip(player, 5, -1, 1156);
                                }
                                return;
                            }
                            switch (select) {
                                case 0 -> {
                                    TopService.showListTop(player, 5);
                                }
                                case 1 ->
                                    this.createOtherMenu(player, ConstNpc.DAT_CUOC_HAT_MIT, "Phí bình chọn là 1 triệu vàng\nkhi trận đấu kết thúc\n90% tổng tiền bình chọn sẽ chia đều cho phe bình chọn chính xác", "Bình chọn cho " + DeathOrAliveArenaManager.gI().getVDST(player.zone).getPlayer().name + " (" + DeathOrAliveArenaManager.gI().getVDST(player.zone).getCuocPlayer() + ")", "Bình chọn cho hạt mít (" + DeathOrAliveArenaManager.gI().getVDST(player.zone).getCuocBaHatMit() + ")");
                                case 2 ->
                                    DeathOrAliveArenaService.gI().startChallenge(player);
                                case 3 -> {
                                }
                                case 4 ->
                                    ChangeMapService.gI().changeMapBySpaceShip(player, 5, -1, 1156);
                            }
                            return;
                        }
                        switch (select) {
                            case 0 -> {
                                TopService.showListTop(player, 5);
                            }
                            case 1 ->
                                DeathOrAliveArenaService.gI().startChallenge(player);
                            case 2 -> {
                            }
                            case 3 ->
                                ChangeMapService.gI().changeMapBySpaceShip(player, 5, -1, 1156);
                        }
                    } else if (player.iDMark.getIndexMenu() == ConstNpc.DAT_CUOC_HAT_MIT) {
                        if (DeathOrAliveArenaManager.gI().getVDST(player.zone) != null) {
                            switch (select) {
                                case 0 -> {
                                    if (player.inventory.gold >= 1_000_000) {
                                        DeathOrAliveArena vdst = DeathOrAliveArenaManager.gI().getVDST(player.zone);
                                        vdst.setCuocPlayer(vdst.getCuocPlayer() + 1);
                                        vdst.addBinhChon(player);
                                        player.binhChonPlayer++;
                                        player.zoneBinhChon = player.zone;
                                        player.inventory.gold -= 1_000_000;
                                        Service.gI().sendMoney(player);
                                    } else {
                                        Service.gI().sendThongBao(player, "Bạn không đủ vàng, còn thiếu " + Util.numberToMoney(1_000_000 - player.inventory.gold) + " vàng nữa");
                                    }
                                }
                                case 1 -> {
                                    if (player.inventory.gold >= 1_000_000) {
                                        DeathOrAliveArena vdst = DeathOrAliveArenaManager.gI().getVDST(player.zone);
                                        vdst.setCuocBaHatMit(vdst.getCuocBaHatMit() + 1);
                                        vdst.addBinhChon(player);
                                        player.binhChonHatMit++;
                                        player.zoneBinhChon = player.zone;
                                        player.inventory.gold -= 1_000_000;
                                        Service.gI().sendMoney(player);
                                    } else {
                                        Service.gI().sendThongBao(player, "Bạn không đủ vàng, còn thiếu " + Util.numberToMoney(1_000_000 - player.inventory.gold) + " vàng nữa");
                                    }
                                }
                            }
                        }
                    }
                }
                case 174, 181 -> {
                    if (player.iDMark.isBaseMenu()) {
                        switch (select) {
                            case 0 ->
                                ChangeMapService.gI().changeMapBySpaceShip(player, 5, -1, 1156);
                        }
                    }
                }
                case 42, 43, 44, 84 -> {
                    if (player.iDMark.isBaseMenu()) {
                        if (!DailyGiftService.checkDailyGift(player, ConstDailyGift.NHAN_BUA_MIEN_PHI)) {
                            select++;
                        }
                        if (!InventoryService.gI().findItem(player, 454) && !InventoryService.gI().findItem(player, 921)) {
                            if (select >= 4) {
                                select++;
                            }
                        }
                        if (!InventoryService.gI().findItem(player, 921) && !InventoryService.gI().findItem(player, 1810)) {
                            if (select >= 5) {
                                select++;
                            }
                        }
                        switch (select) {
                            case 0:
                                if (DailyGiftService.checkDailyGift(player, ConstDailyGift.NHAN_BUA_MIEN_PHI)) {
                                    int idItem = Util.nextInt(213, 219);
                                    player.charms.addTimeCharms(idItem, 60);
                                    Item bua = ItemService.gI().createNewItem((short) idItem);
                                    Service.gI().sendThongBao(player, "Bạn vừa nhận thưởng " + bua.template.name);
                                    DailyGiftService.updateDailyGift(player, ConstDailyGift.NHAN_BUA_MIEN_PHI);
                                } else {
                                    Service.gI().sendThongBao(player, "Hôm nay bạn đã nhận bùa miễn phí rồi!!!");
                                }
                                break;
                            case 1:
                                createOtherMenu(player, ConstNpc.MENU_SACH_TUYET_KY, "Ta có thể giúp gì cho ngươi ?",
                                        "Đóng thành\nSách cũ",
                                        "Đổi Sách\nTuyệt kỹ",
                                        "Giám định\nSách",
                                        "Tẩy\nSách",
                                        "Nâng cấp\nSách\nTuyệt kỹ",
                                        "Hồi phục\nSách",
                                        "Phân rã\nSách");
                                break;
                            case 2:
                                createOtherMenu(player, ConstNpc.MENU_OPTION_SHOP_BUA, "Bùa của ta rất lợi hại, nhìn ngươi yếu đuối thế này, chắc muốn mua bùa để " + "mạnh mẽ à, mua không ta bán cho, xài rồi lại thích cho mà xem.",
                                        "Bùa\n1 giờ",
                                        "Bùa\n8 giờ",
                                        "Bùa\n1 tháng", "Đóng");
                                break;
                            case 3:
                                CombineService.gI().openTabCombine(player, CombineService.NANG_CAP_VAT_PHAM);
                                break;
                            case 4:
                                if (InventoryService.gI().findItemBongTaiCap2(player)) {
                                    CombineService.gI().openTabCombine(player, CombineService.NANG_CHI_SO_BONG_TAI);
                                } else {
                                    CombineService.gI().openTabCombine(player, CombineService.NANG_CAP_BONG_TAI);
                                }
                                break;
                            case 5:
                                if (InventoryService.gI().findItemBongTaiCap3(player)) {
                                    CombineService.gI().openTabCombine(player, CombineService.NANG_CHI_SO_BONG_TAI_CAP_3);
                                } else {
                                    CombineService.gI().openTabCombine(player, CombineService.NANG_CAP_BONG_TAI_CAP_3);
                                }
                                break;
                            case 6:
                                CombineService.gI().openTabCombine(player, CombineService.LAM_PHEP_NHAP_DA);
                                break;
                            case 7:
                                CombineService.gI().openTabCombine(player, CombineService.NHAP_NGOC_RONG);
                                break;
                        }
                    } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_SACH_TUYET_KY) {
                        switch (select) {
                            case 0:
                                CheTaoCuonSachCu.showCombine(player);
                                break;
                            case 1:
                                DoiSachTuyetKy.showCombine(player);
                                break;
                            case 2:
                                CombineService.gI().openTabCombine(player, CombineService.GIAM_DINH_SACH);
                                break;
                            case 3:
                                CombineService.gI().openTabCombine(player, CombineService.TAY_SACH);
                                break;
                            case 4:
                                CombineService.gI().openTabCombine(player, CombineService.NANG_CAP_SACH_TUYET_KY);
                                break;
                            case 5:
                                CombineService.gI().openTabCombine(player, CombineService.HOI_PHUC_SACH);
                                break;
                            case 6:
                                CombineService.gI().openTabCombine(player, CombineService.PHAN_RA_SACH);
                                break;
                        }
                    } else if (player.iDMark.getIndexMenu() == ConstNpc.DONG_THANH_SACH_CU) {
                        CheTaoCuonSachCu.cheTaoCuonSachCu(player);
                    } else if (player.iDMark.getIndexMenu() == ConstNpc.DOI_SACH_TUYET_KY) {
                        DoiSachTuyetKy.doiSachTuyetKy(player);
                    } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_OPTION_SHOP_BUA) {
                        switch (select) {
                            case 0 ->
                                ShopService.gI().opendShop(player, "BUA_1H", true);
                            case 1 ->
                                ShopService.gI().opendShop(player, "BUA_8H", true);
                            case 2 ->
                                ShopService.gI().opendShop(player, "BUA_1M", true);
                        }
                    } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_START_COMBINE) {
                        switch (player.combine.typeCombine) {
                            case CombineService.NANG_CAP_BONG_TAI, CombineService.NANG_CHI_SO_BONG_TAI, CombineService.LAM_PHEP_NHAP_DA, CombineService.NHAP_NGOC_RONG, CombineService.GIAM_DINH_SACH, CombineService.TAY_SACH, CombineService.NANG_CAP_SACH_TUYET_KY, CombineService.HOI_PHUC_SACH, CombineService.PHAN_RA_SACH -> {
                                if (select == 0) {
                                    CombineService.gI().startCombine(player);
                                }
                            }
                            case CombineService.NANG_CAP_VAT_PHAM -> {
                                if (select == 0) {
                                    CombineService.gI().startCombine(player);
                                } else if (select == 1) {
                                    NangCapVatPham.nangCapVatPham(player, true);
                                }
                            }
                        }
                    }
                }
                default -> {
                }
            }
        }
    }
}
