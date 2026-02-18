package nro.models.npc.npc_manifest;

/**
 * Box ZALO: https://zalo.me/g/ifjict764 SĐT Zalo: 0358176187 Chuyên chỉnh sửa,
 * mua bán source NRO
 */
import boss.BossID;
import consts.ConstNpc;
import item.Item;

import java.io.IOException;

import models.Combine.manifest.CheTaoTrangBiThienSu;
import network.Message;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.Service;
import nro.services.SkillService;
import services.func.ChangeMapService;
import models.Combine.CombineService;
import models.Training.TrainingService;
import nro.services.NpcService;
import services.func.TopService;
import shop.ShopService;
import skill.Skill;
import utils.SkillUtil;
import utils.Util;

public class Whis extends Npc {

    private static final int COST_HD = 50_000_000;

    public Whis(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) {
            return;
        }
        if (this.mapId == 154) {
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Thử đánh với ta xem nào.\nNgươi còn 1 lượt nữa cơ mà.",
                    "Nói chuyện",
                    "Học tuyệt kỹ",
                    "Top 100",
                    "[LV:" + (player.traning.getTop() + 1) + "]",
                    "Đến vùng đất hủy diệt");
        }
        if (this.mapId == 48) {
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Đã tìm đủ nguyên liệu cho tôi chưa?\nTôi sẽ giúp cậu mạnh lên kha khá đấy!",
                    "Từ Chối");
        }
        if (this.mapId == 169) {
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Cậu không chịu nổi khi ở đây sao?\nCậu sẽ khó mà mạnh lên được",
                    "Trốn về", "Ở lại");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }

        if (player.iDMark.isBaseMenu() && (this.mapId == 154 || this.mapId == 169)) {
            Item biKiep = InventoryService.gI().findItem(player.inventory.itemsBag, 1229);

            switch (select) {
                case 0 -> {
                    if (this.mapId == 154) {
                        if (!player.setClothes.checkSetDes()) {
                            createOtherMenu(player, ConstNpc.IGNORE_MENU,
                                    "Ngươi hãy trang bị đủ 5 món Hủy Diệt rồi quay lại.",
                                    "OK");
                        } else {
                            createOtherMenu(player, 5,
                                    "Ta sẽ giúp ngươi chế tạo trang bị Thiên Sứ",
                                    "Chế tạo", "Từ chối");
                        }
                    } else if (this.mapId == 164) {
                        ChangeMapService.gI().changeMapInYard(player, 154, -1, 758);
                    }
                }
                case 1 -> {
                    if (biKiep != null) {
                        handleHocTuyetKy(player, biKiep);
                    } else {
                        Service.gI().sendThongBao(player,
                                "Bạn chưa có Bí Kíp Tuyệt Kỹ trong hành trang!");
                    }
                }
                case 2 ->
                    TopService.showListTop(player, 3);
                case 3 ->
                    TrainingService.gI().callBoss(player, BossID.WHIS, false);
                case 4 -> {
                    if (biKiep != null) {
                        vaoMapHD(player);
                    }
                }
            }
        } else if (player.iDMark.getIndexMenu() == 5) {
            if (select == 0) {
                CombineService.gI().openTabCombine(player, CombineService.CHE_TAO_TRANG_BI_THIEN_SU);
            }
        } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_START_COMBINE) {
            if (player.combine.typeCombine == CombineService.CHE_TAO_TRANG_BI_THIEN_SU) {
                CombineService.gI().startCombine(player);
            }
        } else if (player.iDMark.getIndexMenu() == 6) {
            if (select == 0) {
                handleXacNhanHocSkill(player);
            }
        } else if (player.iDMark.isBaseMenu() && this.mapId == 169) {
            if (select == 0) {
                ChangeMapService.gI().changeMapBySpaceShip(player, 154, -1, 450);
            }
        }
    }

    private void handleHocTuyetKy(Player player, Item biKiep) {
        int skillId = switch (player.gender) {
            case 0 ->
                Skill.SUPER_KAME;
            case 2 ->
                Skill.LIEN_HOAN_CHUONG;
            default ->
                Skill.MA_PHONG_BA;
        };

        Skill curSkill = SkillUtil.getSkillbyId(player, skillId);
        boolean chuaHoc = (curSkill == null || curSkill.point == 0);
        
        long powerLimit = 60_000_000_000L;
        boolean duSucManh = player.nPoint.power >= powerLimit;
        boolean duSach = biKiep.quantity >= 9999;
        boolean duVang = player.inventory.gold >= 10_000_000;
        boolean duNgoc = player.inventory.gem >= 99;

        String skillName = switch (player.gender) {
            case 0 ->
                "Super Kamejoko";
            case 2 ->
                "Cái Đích Liên Hoàn Chưởng";
            default ->
                "Ma Phong Ba";
        };

        String title = "|1|Ta sẽ dạy ngươi tuyệt kỹ " + skillName + " "
                + (chuaHoc ? 1 : curSkill.point + 1);
        String chiTiet = "\n" + (duSach ? "|2|" : "|7|") + "Bí kíp tuyệt kỹ " + biKiep.quantity + "/9999"
                + "\n" + (duVang ? "|2|" : "|7|") + "Giá vàng: 10.000.000"
                + "\n" + (duNgoc ? "|2|" : "|7|") + "Giá ngọc: 99"
                + "\n" + (duSucManh ? "|2|Sức mạnh: Đã đủ" : "|7|Cần thêm: " + Util.numberToMoney(powerLimit - player.nPoint.power) + " sức mạnh");

        if (duSach && duVang && duNgoc && duSucManh) {
            createOtherMenu(player, 6, title + chiTiet, "Đồng ý", "Từ chối");
        } else {
            createOtherMenu(player, ConstNpc.IGNORE_MENU, title + chiTiet, "Từ chối");
        }
    }

    private void handleXacNhanHocSkill(Player player) {
        Item sach = InventoryService.gI().findItemBag(player, 1229);
        if (sach == null) {
            return;
        }

        if (player.nPoint.power < 60_000_000_000L) {
            Service.gI().sendThongBao(player, "Ngươi chưa đủ sức mạnh để học tuyệt kỹ.");
            return;
        }
        if (player.inventory.gold < 10_000_000) {
            Service.gI().sendThongBao(player, "Ngươi không đủ vàng.");
            return;
        }
        if (player.inventory.gem < 99) {
            Service.gI().sendThongBao(player, "Ngươi không đủ ngọc xanh.");
            return;
        }

        int skillId = switch (player.gender) {
            case 0 ->
                Skill.SUPER_KAME;
            case 2 ->
                Skill.LIEN_HOAN_CHUONG;
            default ->
                Skill.MA_PHONG_BA;
        };

        int iconSkill = (player.gender == 0) ? 11162 : (player.gender == 2 ? 11193 : 11194);
        Skill curSkill = SkillUtil.getSkillbyId(player, skillId);
        boolean chuaHoc = (curSkill == null || curSkill.point == 0);

        if (chuaHoc) {
            hocSkillMoi(player, sach, skillId, iconSkill);
        } else {
            nangSkill(player, sach, curSkill, iconSkill);
        }
    }

    private void hocSkillMoi(Player player, Item sach, int skillId, int iconSkill) {
        if (sach.quantity < 9999) {
            Service.gI().sendThongBao(player, "Ngươi còn thiếu " + (9999 - sach.quantity) + " bí kíp nữa.");
            return;
        }

        try {
            int matSach = 99;
            String msg = "Tư chất kém!";
            String msgChat = "Ngu dốt!";

            if (Util.isTrue(1, 15)) {
                matSach = 9999;
                msg = "Học skill thành công!";
                msgChat = "Chúc mừng ngươi!";

                switch (player.gender) {
                    case 0 ->
                        SkillService.gI().learSkillSpecial(player, Skill.SUPER_KAME);
                    case 2 ->
                        SkillService.gI().learSkillSpecial(player, Skill.LIEN_HOAN_CHUONG);
                    default ->
                        SkillService.gI().learSkillSpecial(player, Skill.MA_PHONG_BA);
                }
            } else {
                iconSkill = 15313;
            }

            guiThongTinSkill(player, sach, iconSkill, matSach == 99);
            npcChat(player, msgChat);
            Service.gI().sendThongBao(player, msg);

            InventoryService.gI().subQuantityItemsBag(player, sach, matSach);
            player.inventory.gold -= 10_000_000;
            player.inventory.gem -= 99;
            InventoryService.gI().sendItemBag(player);
        } catch (IOException ignored) {
        }
    }

    private void nangSkill(Player player, Item sach, Skill curSkill, int iconSkill) {
        if (sach.quantity < 999) {
            Service.gI().sendThongBao(player, "Ngươi còn thiếu " + (999 - sach.quantity) + " bí kíp nữa.");
            return;
        }
        if (curSkill.currLevel < 1000) {
            npcChat(player, "Ngươi chưa luyện skill đủ thành thạo. Hãy luyện thêm.");
            return;
        }
        if (curSkill.point >= 9) {
            npcChat(player, "Skill của ngươi đã đạt cấp tối đa.");
            return;
        }

        try {
            int matSach = 99;
            String msg = "Tư chất kém!";
            String msgChat = "Ngu dốt!";
            if (Util.isTrue(1, 30)) {
                matSach = 999;
                msg = "Nâng skill thành công!";
                msgChat = "Chúc mừng ngươi!";
                curSkill.point++;
                curSkill.currLevel = 0;
                SkillService.gI().sendCurrLevelSpecial(player, curSkill);
            } else {
                iconSkill = 15313;
            }

            guiThongTinSkill(player, sach, iconSkill, matSach == 99);
            npcChat(player, msgChat);
            Service.gI().sendThongBao(player, msg);

            InventoryService.gI().subQuantityItemsBag(player, sach, matSach);
            player.inventory.gold -= 10_000_000;
            player.inventory.gem -= 99;
            InventoryService.gI().sendItemBag(player);
        } catch (IOException ignored) {
        }
    }

    private void guiThongTinSkill(Player player, Item sach, int iconSkill, boolean fail) throws IOException {
        Message msg;

        msg = new Message(-81);
        msg.writer().writeByte(0);
        msg.writer().writeUTF("Skill 9");
        msg.writer().writeUTF("ENZEEFXNROxBARCOLL");
        msg.writer().writeShort(tempId);
        player.sendMessage(msg);
        msg.cleanup();

        msg = new Message(-81);
        msg.writer().writeByte(1);
        msg.writer().writeByte(1);
        msg.writer().writeByte(InventoryService.gI().getIndexItemBag(player, sach));
        player.sendMessage(msg);
        msg.cleanup();

        msg = new Message(-81);
        msg.writer().writeByte(fail ? 8 : 7);
        msg.writer().writeShort(iconSkill);
        player.sendMessage(msg);
        msg.cleanup();
    }

    private void vaoMapHD(Player player) {
        if (player.nPoint.power >= 80_000_000_000L && player.inventory.gold > COST_HD) {
            player.inventory.gold -= COST_HD;
            Service.gI().sendMoney(player);
            ChangeMapService.gI().changeMapBySpaceShip(player, 169, -1, 168);
        } else {
            npcChat(player, "Ngươi chưa đủ điều kiện để vào.");
            Service.gI().sendThongBao(player, "Cần sức mạnh > 80 Tỷ và 50 Tr vàng.");
        }
    }
}