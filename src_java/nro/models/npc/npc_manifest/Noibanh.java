/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package nro.models.npc.npc_manifest;

import consts.ConstNpc;
import event.EventManager;
import item.Item;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.Service;
import utils.Util;

/**
 *
 * @author Administrator
 */
public class Noibanh extends Npc {
    
    // Map lưu thời gian nấu xong của từng người chơi (Key: ID Player, Value: Thời gian xong)
    private static final Map<Long, Long> cookingTime = new ConcurrentHashMap<>();
    
    // Map lưu trạng thái Auto (Key: ID Player, Value: Số bánh đã nấu xong / 10)
    private static final Map<Long, Integer> autoCookingProgress = new ConcurrentHashMap<>();
    
    // ID Menu
    private static final int MENU_NHAN_BANH = 5050;
    private static final int MENU_AUTO_COOK = 5051;

    public Noibanh(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) {
            return;
        }
        
        // --- KIỂM TRA ĐANG AUTO (VIP) ---
        if (autoCookingProgress.containsKey(player.id)) {
            int finished = autoCookingProgress.get(player.id);
            createOtherMenu(player, ConstNpc.IGNORE_MENU, 
                    "|1|TRẠNG THÁI AUTO NẤU (VIP)\n"
                    + "|2|Tiến độ: " + finished + "/10 nồi.\n"
                    + "|7|Hệ thống đang tự động nấu và chuyển bánh vào hành trang mỗi phút.\n"
                    + "Bạn có thể đi làm việc khác.", 
                    "Đóng");
            return;
        }

        // --- KIỂM TRA ĐANG NẤU THƯỜNG ---
        if (checkIsCooking(player)) {
            long now = System.currentTimeMillis();
            if (!cookingTime.containsKey(player.id)) {
                // Trường hợp mất dữ liệu Ram do bảo trì, cho nhận luôn
                createOtherMenu(player, MENU_NHAN_BANH, "Bánh đã chín (do bảo trì). Nhận ngay!", "Nhận Bánh");
                return;
            }
            long timeDone = cookingTime.get(player.id);
            long timeRemain = (timeDone - now) / 1000;

            if (timeRemain > 0) {
                createOtherMenu(player, ConstNpc.IGNORE_MENU, 
                        "|7|Bánh chưa chín!\n|2|Vui lòng quay lại sau: " + timeRemain + " giây.", "Đóng");
            } else {
                createOtherMenu(player, MENU_NHAN_BANH, 
                        "|1|Bánh đã chín thơm phức!\n|7|Bạn có muốn nhận bánh ngay không?", 
                        "Nhận Bánh", "Để tí nữa");
            }
            return;
        }

        // --- MENU CHÍNH ---
        if (EventManager.TRUNG_THU) {
            player.iDMark.setIndexMenu(ConstNpc.BASE_MENU);
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Xin chào, mình là nồi bánh, bạn muốn nấu bánh gì?",
                    "Bánh Trung Thu\nGà Quay", "Bánh Trung Thu\nGà Quay Hảo Hạng", "Bánh Trung Thu\nHạt Sen", "Từ chối");
            return;
        }
        if (EventManager.HUNG_VUONG) {
            player.iDMark.setIndexMenu(ConstNpc.BASE_MENU);
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Xin chào " + player.name + "\nTôi là nồi nấu bánh\nTôi có thể giúp gì cho bạn?",
                    "Tự nấu bánh", "Từ chối");
        }
        if (EventManager.LUNAR_NEW_YEAR) {
            player.iDMark.setIndexMenu(ConstNpc.BASE_MENU);
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Hãy tìm đủ nguyên liệu và loại bánh muốn nấu",
                    "Nấu\nBánh tét", "Nấu\nBánh chưng", "Auto Nấu\n(VIP)", "Từ chối");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) return;
        if (this.mapId != 0 && this.mapId != 5 && this.mapId != 7 && this.mapId != 14) return;
        
        // Nhận bánh thường
        if (player.iDMark.getIndexMenu() == MENU_NHAN_BANH) {
            if (select == 0) receiveCake(player);
            return;
        }
        
        // Menu chọn Auto
        if (player.iDMark.getIndexMenu() == MENU_AUTO_COOK) {
            switch (select) {
                case 0: autoCookSequence(player, 1); break; // Auto Tét
                case 1: autoCookSequence(player, 2); break; // Auto Chưng
            }
            return;
        }

        // Menu cấp 1
        if (player.iDMark.isBaseMenu()) {
            if (EventManager.TRUNG_THU) {
                switch (select) {
                    case 0: showBanhtrunthuGaQuayMenu(player); break;
                    case 1: showBanhtrunthuGaQuayHaoHanMenu(player); break;
                    case 2: showBanhtrunthuHatSenMenu(player); break;
                }
            } else if (EventManager.HUNG_VUONG) {
                if (select == 0) createOtherMenu(player, 1, "Chọn loại bánh", "Nấu Bánh Dầy", "Nấu Bánh Chưng", "Từ chối");
            } else if (EventManager.LUNAR_NEW_YEAR) {
                switch (select) {
                    case 0: showBanhTetMenu(player); break;
                    case 1: showBanhChungTetMenu(player); break;
                    case 2: // Mở menu Auto
                        createOtherMenu(player, MENU_AUTO_COOK, 
                                 "|1|DỊCH VỤ AUTO NẤU BÁNH (VIP)\n"
                                 + "|2|Cơ chế: Tự động nấu và nhận 10 lần liên tiếp.\n"
                                 + "Thời gian: 10 phút (1 phút/cái).\n"
                                 + "|7|Phí dịch vụ: 1 Thỏi vàng + Nguyên liệu x10.", 
                                 "Auto 10\nBánh Tét", "Auto 10\nBánh Chưng", "Đóng");
                        break;
                }
            }
            return;
        }

        // Menu con Hùng Vương
        if (player.iDMark.getIndexMenu() == 1) {
            switch (select) {
                case 0: showBanhDayMenu(player); break;
                case 1: showBanhChungMenu(player); break;
            }
            return;
        }

        // Bắt đầu nấu thường
        if (EventManager.LUNAR_NEW_YEAR && player.iDMark.getIndexMenu() == ConstNpc.BANH_TET) startCooking(player, "Bánh Tét", 5_000_000, 1);
        if (EventManager.LUNAR_NEW_YEAR && player.iDMark.getIndexMenu() == ConstNpc.BANH_CHUNG) startCooking(player, "Bánh Chưng (Tết)", 5_000_000, 2);
        if (EventManager.HUNG_VUONG && player.iDMark.getIndexMenu() == ConstNpc.MENU_BANH_TET) startCooking(player, "Bánh Dầy", 1_000_000, 3);
        if (EventManager.HUNG_VUONG && player.iDMark.getIndexMenu() == ConstNpc.MENU_BANH_CHUNG) startCooking(player, "Bánh Chưng", 5_000_000, 4);
        if (EventManager.TRUNG_THU && player.iDMark.getIndexMenu() == ConstNpc.MENU_BANH_TRUNG_THU_GA_QUAY) startCooking(player, "Trung Thu Gà Quay", 20_000_000, 5);
        if (EventManager.TRUNG_THU && player.iDMark.getIndexMenu() == ConstNpc.MENU_BANH_TRUNG_THU_GA_QUAY_HAO_HAN) startCooking(player, "Trung Thu HH", 1_000, 6);
        if (EventManager.TRUNG_THU && player.iDMark.getIndexMenu() == ConstNpc.MENU_BANH_TRUNG_THU_HAT_SEN) startCooking(player, "Trung Thu Hạt Sen", 1_000, 7);
    }
    
    // ================== LOGIC AUTO NẤU (VIP) ==================
    
    private void autoCookSequence(Player player, int type) {
        if (autoCookingProgress.containsKey(player.id) || checkIsCooking(player)) {
            this.npcChat(player, "Bạn đang nấu bánh rồi, không thể nhận thêm đơn hàng!");
            return;
        }

        // 1. Kiểm tra phí dịch vụ (1 Thỏi vàng)
        Item thoiVang = InventoryService.gI().findItemBag(player, 457);
        if (thoiVang == null || thoiVang.quantity < 1) {
            this.npcChat(player, "Bạn cần 1 Thỏi vàng để thuê người nấu hộ!");
            return;
        }

        // 2. Cấu hình nguyên liệu cho 10 cái
        int[][] materials = null;
        short rewardId = -1;
        String name = "";
        
        if (type == 1) { // 10 Bánh Tét
            name = "Bánh Tét";
            rewardId = 752;
            materials = new int[][]{{748, 100}, {749, 100}, {750, 100}, {751, 100}}; // 10 x 10
        } else if (type == 2) { // 10 Bánh Chưng Tết
            name = "Bánh Chưng";
            rewardId = 753;
            materials = new int[][]{{748, 100}, {749, 100}, {750, 100}, {751, 100}, {886, 10}}; 
        }

        // 3. Kiểm tra đủ nguyên liệu không
        for (int[] mat : materials) {
            Item it = InventoryService.gI().findItemBag(player, mat[0]);
            if (it == null || it.quantity < mat[1]) {
                 this.npcChat(player, "Hành trang không đủ nguyên liệu để nấu 10 cái " + name + "!");
                 return;
            }
        }
        
        // 4. Trừ phí + Nguyên liệu (Trừ hết 1 lần luôn để tránh bug)
        InventoryService.gI().subQuantityItemsBag(player, thoiVang, 1);
        for (int[] mat : materials) {
            Item it = InventoryService.gI().findItemBag(player, mat[0]);
            InventoryService.gI().subQuantityItemsBag(player, it, mat[1]);
        }
        Service.gI().sendMoney(player);
        InventoryService.gI().sendItemBag(player);
        
        // 5. Bắt đầu luồng chạy tự động
        final short finalRewardId = rewardId;
        final String finalName = name;
        
        // Đánh dấu đang auto
        autoCookingProgress.put(player.id, 0);
        this.npcChat(player, "Đã nhận đơn hàng Auto!\nCứ mỗi phút ta sẽ gửi 1 " + name + " vào hành trang của bạn.");
        
        // Chạy Thread ngầm
        Thread.startVirtualThread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    // Chờ 60 giây (1 phút)
                    Thread.sleep(60000);
                    
                    // Kiểm tra nếu player thoát game thì dừng (để tránh lỗi)
                    // Hoặc vẫn chạy nhưng phải check null. Ở đây check đơn giản.
                    if (player.zone == null) {
                        break; 
                    }
                    
                    // Add 1 cái bánh vào hành trang
                    Item cake = ItemService.gI().createNewItem(finalRewardId);
                    InventoryService.gI().addItemBag(player, cake);
                    InventoryService.gI().sendItemBag(player);
                    
                    // Thông báo
                    Service.gI().sendThongBao(player, "Auto: Bạn nhận được " + cake.template.name + " (" + i + "/10)");
                    
                    // Cập nhật tiến độ
                    autoCookingProgress.put(player.id, i);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // Xóa trạng thái auto khi xong hoặc lỗi
                autoCookingProgress.remove(player.id);
                if (player.zone != null) {
                    Service.gI().sendThongBao(player, "Đã hoàn thành đơn hàng Auto nấu bánh!");
                }
            }
        });
    }

    // ================== LOGIC NẤU THƯỜNG ==================

    private boolean checkIsCooking(Player player) {
        return player.isCookingBanhTrungThuGaQuay || player.isCookingBanhTrungThuGaQuayHaoHan 
                || player.isCookingBanhTrungThuHatSen || player.isCookingBanhDay
                || player.isCookingBanhChung || player.isCookingBanhTet || player.isCookingBanhChung2;
    }

    private void startCooking(Player player, String name, int cost, int type) {
        if (autoCookingProgress.containsKey(player.id)) {
            this.npcChat(player, "Đang chạy Auto nấu, không thể nấu thủ công!");
            return;
        }
        
        boolean success = false;
        switch(type) {
            case 1: success = checkAndSub(player, cost, false, new int[][]{{748, 10}, {749, 10}, {750, 10}, {751, 10}}); if(success) player.isCookingBanhTet=true; break;
            case 2: success = checkAndSub(player, cost, false, new int[][]{{748, 10}, {749, 10}, {750, 10}, {751, 10}, {886, 1}}); if(success) player.isCookingBanhChung2=true; break;
            case 3: success = checkAndSub(player, cost, false, new int[][]{{1214, 99}, {1547, 5}, {1545, 2}, {1544, 1}}); if(success) player.isCookingBanhDay=true; break;
            case 4: success = checkAndSub(player, cost, false, new int[][]{{1214, 99}, {1548, 2}, {1549, 2}}); if(success) player.isCookingBanhChung=true; break;
            case 5: success = checkAndSub(player, cost, false, new int[][]{{888, 99}, {889, 5}, {886, 2}, {887, 1}}); if(success) player.isCookingBanhTrungThuGaQuay=true; break;
            case 6: success = checkAndSub(player, cost, true, new int[][]{{888, 99}, {889, 5}, {886, 2}, {887, 1}}); if(success) player.isCookingBanhTrungThuGaQuayHaoHan=true; break;
            case 7: success = checkAndSub(player, cost, true, new int[][]{{888, 99}, {889, 5}, {886, 2}, {1312, 1}}); if(success) player.isCookingBanhTrungThuHatSen=true; break;
        }

        if (success) {
            cookingTime.put(player.id, System.currentTimeMillis() + 60000);
            this.npcChat(player, "Bắt đầu nấu " + name + ".\n|7|Quay lại sau 60 giây để nhận bánh.");
            Service.gI().sendThongBao(player, "Bắt đầu nấu " + name + " (60s)");
        } else {
            this.npcChat(player, "Không đủ nguyên liệu hoặc tiền!");
        }
    }

    private void receiveCake(Player player) {
        Item itemNhan = null;
        String tenBanh = "";
        try {
            if (player.isCookingBanhTet) { itemNhan = ItemService.gI().createNewItem((short) 752); tenBanh = "Bánh Tét"; player.isCookingBanhTet = false; }
            else if (player.isCookingBanhChung2) { itemNhan = ItemService.gI().createNewItem((short) 753); tenBanh = "Bánh Chưng (Tết)"; player.isCookingBanhChung2 = false; }
            else if (player.isCookingBanhDay) { itemNhan = ItemService.gI().createNewItem((short) 1542); tenBanh = "Bánh Dầy"; player.isCookingBanhDay = false; }
            else if (player.isCookingBanhChung) { itemNhan = ItemService.gI().createNewItem((short) 1556); tenBanh = "Bánh Chưng"; player.isCookingBanhChung = false; }
            else if (player.isCookingBanhTrungThuGaQuay) { itemNhan = ItemService.gI().createNewItem((short) 890); tenBanh = "TT Gà Quay"; player.isCookingBanhTrungThuGaQuay = false; }
            else if (player.isCookingBanhTrungThuGaQuayHaoHan) { 
                itemNhan = ItemService.gI().createNewItem((short) 890); tenBanh = "TT Hảo Hạng"; 
                if (Util.isTrue(30, 100)) InventoryService.gI().addItemBag(player, ItemService.gI().createNewItem((short) 891));
                player.isCookingBanhTrungThuGaQuayHaoHan = false; 
            }
            else if (player.isCookingBanhTrungThuHatSen) { itemNhan = ItemService.gI().createNewItem((short) 1313); tenBanh = "TT Hạt Sen"; player.isCookingBanhTrungThuHatSen = false; }

            if (itemNhan != null) {
                InventoryService.gI().addItemBag(player, itemNhan);
                InventoryService.gI().sendItemBag(player);
                cookingTime.remove(player.id);
                this.npcChat(player, "Của bạn đây: " + tenBanh);
                createOtherMenu(player, ConstNpc.IGNORE_MENU, "|1|Đã nhận " + tenBanh, "Đóng");
            } else {
                cookingTime.remove(player.id);
                this.npcChat(player, "Lỗi: Không tìm thấy bánh!");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private boolean checkAndSub(Player player, int cost, boolean isRuby, int[][] items) {
        if (isRuby) { if (player.inventory.ruby < cost) return false; } else { if (player.inventory.gold < cost) return false; }
        for (int[] item : items) {
            Item it = InventoryService.gI().findItemBag(player, item[0]);
            if (it == null || it.quantity < item[1]) return false;
        }
        if (isRuby) player.inventory.ruby -= cost; else player.inventory.gold -= cost;
        for (int[] item : items) {
            Item it = InventoryService.gI().findItemBag(player, item[0]);
            InventoryService.gI().subQuantityItemsBag(player, it, item[1]);
        }
        Service.gI().sendMoney(player);
        InventoryService.gI().sendItemBag(player);
        return true;
    }

    // ================== HIỂN THỊ CÔNG THỨC ==================

    private void showBanhtrunthuGaQuayMenu(Player player) {
        Item botmi = InventoryService.gI().findItemBag(player, 888);
        Item dauxanh = InventoryService.gI().findItemBag(player, 889);
        Item trungvitmuoi = InventoryService.gI().findItemBag(player, 886);
        Item gaquyanguyencon = InventoryService.gI().findItemBag(player, 887);
        if (checkItem(botmi, 99) && checkItem(dauxanh, 5) && checkItem(trungvitmuoi, 2) && checkItem(gaquyanguyencon, 1) && player.inventory.gold >= 20_000_000) {
            createOtherMenu(player, ConstNpc.MENU_BANH_TRUNG_THU_GA_QUAY, "|2|Bánh trung thu Gà Quay\n" + getInfoItem(botmi, 99, "Bột mì") + getInfoItem(dauxanh, 5, "Đậu xanh") + getInfoItem(trungvitmuoi, 2, "Trứng vịt muối") + getInfoItem(gaquyanguyencon, 1, "Gà quay nguyên con") + "Giá vàng: 20tr", "Đồng ý", "Từ chối");
        } else {
             createOtherMenu(player, ConstNpc.IGNORE_MENU, "|2|Bánh trung thu Gà Quay\n" + getInfoItem(botmi, 99, "Bột mì") + getInfoItem(dauxanh, 5, "Đậu xanh") + getInfoItem(trungvitmuoi, 2, "Trứng vịt muối") + getInfoItem(gaquyanguyencon, 1, "Gà quay nguyên con") + (player.inventory.gold < 20_000_000 ? "|7|Thiếu vàng" : "|1|Giá vàng: 20tr"), "Đóng");
        }
    }
    private void showBanhtrunthuGaQuayHaoHanMenu(Player player) {
        Item botmi = InventoryService.gI().findItemBag(player, 888);
        Item dauxanh = InventoryService.gI().findItemBag(player, 889);
        Item trungvitmuoi = InventoryService.gI().findItemBag(player, 886);
        Item gaquyanguyencon = InventoryService.gI().findItemBag(player, 887);
        if (checkItem(botmi, 99) && checkItem(dauxanh, 5) && checkItem(trungvitmuoi, 2) && checkItem(gaquyanguyencon, 1) && player.inventory.ruby >= 1_000) {
            createOtherMenu(player, ConstNpc.MENU_BANH_TRUNG_THU_GA_QUAY_HAO_HAN, "|2|Bánh trung thu Gà Quay Hảo Hạng\n30% nhận thêm bánh thập cẩm\n" + getInfoItem(botmi, 99, "Bột mì") + getInfoItem(dauxanh, 5, "Đậu xanh") + getInfoItem(trungvitmuoi, 2, "Trứng vịt muối") + getInfoItem(gaquyanguyencon, 1, "Gà quay nguyên con") + "Giá ngọc: 1k", "Đồng ý", "Từ chối");
        } else {
            createOtherMenu(player, ConstNpc.IGNORE_MENU, "|2|Bánh trung thu Gà Quay Hảo Hạng\n" + getInfoItem(botmi, 99, "Bột mì") + getInfoItem(dauxanh, 5, "Đậu xanh") + getInfoItem(trungvitmuoi, 2, "Trứng vịt muối") + getInfoItem(gaquyanguyencon, 1, "Gà quay nguyên con") + (player.inventory.ruby < 1_000 ? "|7|Thiếu ngọc" : "|1|Giá ngọc: 1k"), "Đóng");
        }
    }
    private void showBanhtrunthuHatSenMenu(Player player) {
        Item botmi = InventoryService.gI().findItemBag(player, 888);
        Item dauxanh = InventoryService.gI().findItemBag(player, 889);
        Item trungvitmuoi = InventoryService.gI().findItemBag(player, 886);
        Item hatsen = InventoryService.gI().findItemBag(player, 1312);
        if (checkItem(botmi, 99) && checkItem(dauxanh, 5) && checkItem(trungvitmuoi, 2) && checkItem(hatsen, 1) && player.inventory.ruby >= 1_000) {
            createOtherMenu(player, ConstNpc.MENU_BANH_TRUNG_THU_HAT_SEN, "|2|Bánh trung thu Hạt Sen\n" + getInfoItem(botmi, 99, "Bột mì") + getInfoItem(dauxanh, 5, "Đậu xanh") + getInfoItem(trungvitmuoi, 2, "Trứng vịt muối") + getInfoItem(hatsen, 1, "Hạt sen") + "Giá ngọc: 1k", "Đồng ý", "Từ chối");
        } else {
             createOtherMenu(player, ConstNpc.IGNORE_MENU, "|2|Bánh trung thu Hạt Sen\n" + getInfoItem(botmi, 99, "Bột mì") + getInfoItem(dauxanh, 5, "Đậu xanh") + getInfoItem(trungvitmuoi, 2, "Trứng vịt muối") + getInfoItem(hatsen, 1, "Hạt sen") + (player.inventory.ruby < 1_000 ? "|7|Thiếu ngọc" : "|1|Giá ngọc: 1k"), "Đóng");
        }
    }
    private void showBanhDayMenu(Player player) {
        Item comNep = InventoryService.gI().findItemBag(player, 1214);
        Item botGao = InventoryService.gI().findItemBag(player, 1547);
        Item muoiTieu = InventoryService.gI().findItemBag(player, 1545);
        Item chaLua = InventoryService.gI().findItemBag(player, 1544);
        if (checkItem(comNep, 99) && checkItem(botGao, 5) && checkItem(muoiTieu, 2) && checkItem(chaLua, 1) && player.inventory.gold >= 1_000_000) {
            createOtherMenu(player, ConstNpc.MENU_BANH_TET, "|2|Bạn muốn nấu bánh dầy?\n" + getInfoItem(comNep, 99, "Cơm nếp") + getInfoItem(botGao, 5, "Bột gạo") + getInfoItem(muoiTieu, 2, "Muối tiêu") + getInfoItem(chaLua, 1, "Chả lụa") + "Giá vàng: 1tr", "Đồng ý", "Từ chối");
        } else {
            createOtherMenu(player, ConstNpc.IGNORE_MENU, "|2|Bạn muốn nấu bánh dầy\n" + getInfoItem(comNep, 99, "Cơm nếp") + getInfoItem(botGao, 5, "Bột gạo") + getInfoItem(muoiTieu, 2, "Muối tiêu") + getInfoItem(chaLua, 1, "Chả lụa") + (player.inventory.gold < 1_000_000 ? "|7|Thiếu vàng" : "|1|Giá vàng: 1tr"), "Đóng");
        }
    }
    private void showBanhChungMenu(Player player) {
        Item comNep = InventoryService.gI().findItemBag(player, 1214);
        Item dauXanh = InventoryService.gI().findItemBag(player, 1548);
        Item thitTuoi = InventoryService.gI().findItemBag(player, 1549);
        if (checkItem(comNep, 99) && checkItem(dauXanh, 2) && checkItem(thitTuoi, 2) && player.inventory.gold >= 5_000_000) {
            createOtherMenu(player, ConstNpc.MENU_BANH_CHUNG, "|2|Bạn muốn nấu bánh chưng?\n" + getInfoItem(comNep, 99, "Cơm nếp") + getInfoItem(dauXanh, 2, "Đậu xanh") + getInfoItem(thitTuoi, 2, "Thịt tươi") + "Giá vàng: 5tr", "Đồng ý", "Từ chối");
        } else {
            createOtherMenu(player, ConstNpc.IGNORE_MENU, "|2|Bạn muốn nấu bánh chưng\n" + getInfoItem(comNep, 99, "Cơm nếp") + getInfoItem(dauXanh, 2, "Đậu xanh") + getInfoItem(thitTuoi, 2, "Thịt tươi") + (player.inventory.gold < 5_000_000 ? "|7|Thiếu vàng" : "|1|Giá vàng: 5tr"), "Đóng");
        }
    }
    private void showBanhTetMenu(Player player) {
        Item thittheo = InventoryService.gI().findItemBag(player, 748);
        Item thungnep = InventoryService.gI().findItemBag(player, 749);
        Item thungdauxanh = InventoryService.gI().findItemBag(player, 750);
        Item ladong = InventoryService.gI().findItemBag(player, 751);
        if (checkItem(thittheo, 10) && checkItem(thungnep, 10) && checkItem(thungdauxanh, 10) && checkItem(ladong, 10) && player.inventory.gold >= 5_000_000) {
            createOtherMenu(player, ConstNpc.BANH_TET, "|2|Bạn muốn nấu Bánh Tét?\n" + getInfoItem(thittheo, 10, "Thịt heo") + getInfoItem(thungnep, 10, "Thúng nếp") + getInfoItem(thungdauxanh, 10, "Thúng đậu xanh") + getInfoItem(ladong, 10, "Lá dong") + "Giá vàng: 5tr", "Đồng ý", "Từ chối");
        } else {
            createOtherMenu(player, ConstNpc.IGNORE_MENU, "|2|Bạn muốn nấu Bánh Tét\n" + getInfoItem(thittheo, 10, "Thịt heo") + getInfoItem(thungnep, 10, "Thúng nếp") + getInfoItem(thungdauxanh, 10, "Thúng đậu xanh") + getInfoItem(ladong, 10, "Lá dong") + (player.inventory.gold < 5_000_000 ? "|7|Thiếu vàng" : "|1|Giá vàng: 5tr"), "Đóng");
        }
    }
    private void showBanhChungTetMenu(Player player) {
        Item thittheo = InventoryService.gI().findItemBag(player, 748);
        Item thungnep = InventoryService.gI().findItemBag(player, 749);
        Item thungdauxanh = InventoryService.gI().findItemBag(player, 750);
        Item ladong = InventoryService.gI().findItemBag(player, 751);
        Item trungvitmuoi = InventoryService.gI().findItemBag(player, 886);
        if (checkItem(thittheo, 10) && checkItem(thungnep, 10) && checkItem(thungdauxanh, 10) && checkItem(ladong, 10) && checkItem(trungvitmuoi, 1) && player.inventory.gold >= 5_000_000) {
            createOtherMenu(player, ConstNpc.BANH_CHUNG, "|2|Bạn muốn nấu Bánh Chưng (Tết)?\n" + getInfoItem(thittheo, 10, "Thịt heo") + getInfoItem(thungnep, 10, "Thúng nếp") + getInfoItem(thungdauxanh, 10, "Thúng đậu xanh") + getInfoItem(ladong, 10, "Lá dong") + getInfoItem(trungvitmuoi, 1, "Trứng muối") + "Giá vàng: 5tr", "Đồng ý", "Từ chối");
        } else {
            createOtherMenu(player, ConstNpc.IGNORE_MENU, "|2|Bạn muốn nấu Bánh Chưng (Tết)\n" + getInfoItem(thittheo, 10, "Thịt heo") + getInfoItem(thungnep, 10, "Thúng nếp") + getInfoItem(thungdauxanh, 10, "Thúng đậu xanh") + getInfoItem(ladong, 10, "Lá dong") + getInfoItem(trungvitmuoi, 1, "Trứng muối") + (player.inventory.gold < 5_000_000 ? "|7|Thiếu vàng" : "|1|Giá vàng: 5tr"), "Đóng");
        }
    }

    private boolean checkItem(Item item, int quan) { return item != null && item.quantity >= quan; }
    private String getInfoItem(Item item, int quan, String name) { return (item != null && item.quantity >= quan) ? "|1|" + name + " " + item.quantity + "/" + quan + "\n" : "|7|" + name + " " + (item != null ? item.quantity : 0) + "/" + quan + "\n"; }
}