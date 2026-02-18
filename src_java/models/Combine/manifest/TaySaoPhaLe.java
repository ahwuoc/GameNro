package models.Combine.manifest;

import consts.ConstNpc;
import item.Item;
import item.Item.ItemOption;
import models.Combine.CombineService;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.Service;
import utils.Util;
import java.util.Iterator; 

public class TaySaoPhaLe {

    // --- CẤU HÌNH ---
    private static final int BUA_TAY_THUONG_ID = 1998;
    private static final int BUA_TAY_CAO_CAP_ID = 1999;
    
    private static final int COST_THUONG = 200_000_000;
    private static final int COST_CAO_CAP = 500_000_000;

    // Danh sách các Option ID thuộc nhóm Pha Lê Thường
    private static final int[] OPTION_PHA_LE_THUONG = {77, 103, 80, 81, 50, 94, 108};

    // Danh sách các Option ID thuộc nhóm Pha Lê VIP
    private static final int[] OPTION_PHA_LE_VIP = {95, 96, 97, 98, 99, 100, 101, 153, 160};

    /**
     * Hiển thị thông tin trước khi tẩy
     */
    public static void showInfoCombine(Player player) {
        if (player.combine.itemsCombine.size() != 2) {
            Service.gI().sendDialogMessage(player, "Cần 1 trang bị đã ép pha lê và 1 bùa tẩy tương ứng.");
            return;
        }

        Item trangBi = null;
        Item buaTay = null;

        // --- LOGIC PHÂN LOẠI ITEM ---
        for (Item item : player.combine.itemsCombine) {
            if (item.template.id == BUA_TAY_THUONG_ID || item.template.id == BUA_TAY_CAO_CAP_ID) {
                buaTay = item;
            } else if (item.template.type >= 0 && item.template.type <= 5 || item.template.type == 32) {
                // Nhận Trang bị (Áo, Quần, Găng, Giày, Rada, Vũ khí) và Phụ kiện (32)
                trangBi = item;
            }
        }

        if (trangBi == null || buaTay == null) {
            Service.gI().sendDialogMessage(player, "Cần 1 trang bị và 1 bùa tẩy (1998 hoặc 1999).");
            return;
        }

        StringBuilder text = new StringBuilder();
        // Hiển thị text thuần, không dùng màu
        text.append("Tẩy Sao Pha Lê\n");
        text.append(trangBi.template.name).append("\n");

        int star = trangBi.getOptionParam(102); 

        // --- BÙA TẨY THƯỜNG ---
        if (buaTay.template.id == BUA_TAY_THUONG_ID) {
            if (star <= 0) {
                Service.gI().sendDialogMessage(player, "Trang bị chưa ép sao pha lê nào, không thể tẩy thường.");
                return;
            }
            if (star > 7) {
                 Service.gI().sendDialogMessage(player, "Trang bị đang có trên 7 sao. Vui lòng dùng Bùa Tẩy Cao Cấp để tẩy sao thứ 8 trở đi trước.");
                 return;
            }
            
            // Lấy thông tin dòng sẽ bị trừ
            String previewInfo = getOptionPreview(trangBi, OPTION_PHA_LE_THUONG);
            
            text.append("Sử dụng: ").append(buaTay.template.name).append("\n");
            text.append("Sẽ giảm: ").append(previewInfo).append("\n");
            text.append("Phí: ").append(Util.numberToMoney(COST_THUONG)).append(" vàng");

            if (player.inventory.gold < COST_THUONG) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, text.toString(), 
                        "Còn thiếu\n" + Util.numberToMoney(COST_THUONG - player.inventory.gold) + " vàng");
            } else {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, text.toString(), "Tẩy Sao", "Từ chối");
            }

        } 
        // --- BÙA TẨY CAO CẤP ---
        else if (buaTay.template.id == BUA_TAY_CAO_CAP_ID) {
            if (star < 8) {
                Service.gI().sendDialogMessage(player, "Trang bị chưa ép sao thứ 8, hãy dùng Bùa Tẩy Thường.");
                return;
            }

            // Lấy thông tin dòng sẽ bị trừ
            String previewInfo = getOptionPreview(trangBi, OPTION_PHA_LE_VIP);

            text.append("Sử dụng: ").append(buaTay.template.name).append("\n");
            text.append("Sẽ giảm: ").append(previewInfo).append("\n");
            text.append("Phí: ").append(Util.numberToMoney(COST_CAO_CAP)).append(" vàng");

            if (player.inventory.gold < COST_CAO_CAP) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, text.toString(), 
                        "Còn thiếu\n" + Util.numberToMoney(COST_CAO_CAP - player.inventory.gold) + " vàng");
            } else {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, text.toString(), "Tẩy Sao VIP", "Từ chối");
            }
        }
    }

    /**
     * Thực hiện hành động tẩy sao
     */
    public static void startCombine(Player player) {
        if (player.combine.itemsCombine.size() != 2) {
            return;
        }

        Item trangBi = null;
        Item buaTay = null;

        for (Item item : player.combine.itemsCombine) {
            if (item.template.id == BUA_TAY_THUONG_ID || item.template.id == BUA_TAY_CAO_CAP_ID) {
                buaTay = item;
            } else if (item.template.type >= 0 && item.template.type <= 5 || item.template.type == 32) {
                trangBi = item;
            }
        }

        if (trangBi == null || buaTay == null) {
            Service.gI().sendThongBao(player, "Thiếu nguyên liệu");
            return;
        }

        int star = trangBi.getOptionParam(102); 
        boolean success = false;

        // --- XỬ LÝ TẨY THƯỜNG ---
        if (buaTay.template.id == BUA_TAY_THUONG_ID) {
            if (star <= 0) {
                Service.gI().sendThongBao(player, "Không có sao để tẩy");
                return;
            }
            if (star > 7) {
                Service.gI().sendThongBao(player, "Cần tẩy sao cấp cao trước");
                return;
            }
            if (player.inventory.gold < COST_THUONG) {
                Service.gI().sendThongBao(player, "Không đủ " + Util.numberToMoney(COST_THUONG) + " vàng");
                return;
            }

            // Trừ vàng và bùa
            player.inventory.subGold(COST_THUONG);
            InventoryService.gI().subQuantityItemsBag(player, buaTay, 1);

            // 1. Tìm option và trừ chỉ số
            reduceOptionParamOrRemove(trangBi, OPTION_PHA_LE_THUONG);
            
            // 2. Giảm số sao hiển thị (102)
            decreaseStarOption(trangBi);
            
            success = true;
            Service.gI().sendThongBao(player, "Tẩy sao pha lê thành công!");
        } 
        
        // --- XỬ LÝ TẨY CAO CẤP ---
        else if (buaTay.template.id == BUA_TAY_CAO_CAP_ID) {
            if (star < 8) {
                Service.gI().sendThongBao(player, "Chưa ép sao thứ 8");
                return;
            }
            if (player.inventory.gold < COST_CAO_CAP) {
                Service.gI().sendThongBao(player, "Không đủ " + Util.numberToMoney(COST_CAO_CAP) + " vàng");
                return;
            }

            // Trừ vàng và bùa
            player.inventory.subGold(COST_CAO_CAP);
            InventoryService.gI().subQuantityItemsBag(player, buaTay, 1);

            // 1. Tìm option và trừ chỉ số VIP
            reduceOptionParamOrRemove(trangBi, OPTION_PHA_LE_VIP);

            // 2. Giảm số sao hiển thị (102)
            decreaseStarOption(trangBi);

            success = true;
            Service.gI().sendThongBao(player, "Tẩy sao pha lê cao cấp thành công!");
        }

        // --- KẾT THÚC ---
        if (success) {
            CombineService.gI().sendEffectSuccessCombine(player);
            InventoryService.gI().sendItemBag(player);
            Service.gI().sendMoney(player);
            CombineService.gI().reOpenItemCombine(player);
        }
    }

    /**
     * Tìm xem tẩy sẽ mất dòng nào để hiển thị lên Menu
     */
    private static String getOptionPreview(Item item, int[] optionsToScan) {
        if (item == null || item.itemOptions == null) return "Không tìm thấy";
        for (ItemOption io : item.itemOptions) {
            for (int id : optionsToScan) {
                if (io.optionTemplate.id == id) {
                    int paramReduce = getParamPerStar(id);
                    // Lọc bỏ các ký tự #, + để hiển thị đẹp hơn
                    String name = io.optionTemplate.name.replace("#", "").replace("+", "").trim();
                    return name + " -" + paramReduce;
                }
            }
        }
        return "Lỗi xác định chỉ số";
    }

    /**
     * LOGIC: Tìm option, trừ param, nếu <= 0 thì xóa
     */
    private static void reduceOptionParamOrRemove(Item item, int[] optionsToScan) {
        if (item == null || item.itemOptions == null) return;

        Iterator<ItemOption> iterator = item.itemOptions.iterator();
        while (iterator.hasNext()) {
            ItemOption io = iterator.next();
            
            boolean isMatch = false;
            for (int id : optionsToScan) {
                if (io.optionTemplate.id == id) {
                    isMatch = true;
                    break;
                }
            }
            
            if (isMatch) {
                int paramPerStar = getParamPerStar(io.optionTemplate.id);
                io.param -= paramPerStar;
                
                if (io.param <= 0) {
                    iterator.remove();
                }
                break; 
            }
        }
    }

    /**
     * CẤU HÌNH CHỈ SỐ CỦA 1 SAO
     */
    private static int getParamPerStar(int optionId) {
        switch (optionId) {
            case 77: return 5;  // HP
            case 103: return 5; // KI
            case 80: return 5;  // SD
            case 81: return 5;  // Giap
            case 50: return 3;  // Chi mang
            case 94: return 2;  // Giap %
            case 108: return 2; // Ne don %
            
            // Các option cao cấp/mới
            case 95: case 96: case 97: case 98: case 99: case 100: case 101:
            case 153: case 160:
                return 5;
                
            default: return 0;
        }
    }

    /**
     * Giảm số lượng sao (Option 102)
     */
    private static void decreaseStarOption(Item item) {
        Iterator<ItemOption> iterator = item.itemOptions.iterator();
        while (iterator.hasNext()) {
            ItemOption io = iterator.next();
            if (io.optionTemplate.id == 102) {
                io.param--;
                if (io.param <= 0) {
                    iterator.remove(); 
                }
                break;
            }
        }
    }
}