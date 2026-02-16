package npc.npc_manifest;

import consts.ConstNpc;
import consts.ConstTask;
import consts.ConstVip;
import item.Item;
import jdbc.daos.PlayerDAO;
import npc.Npc;
import player.Player;
import services.InventoryService;
import services.ItemService;
import services.Service;
import services.TaskService;
import services.func.ChangeMapService;
import shop.ShopService;
import utils.Util;

public class BumaTH extends Npc {

    public BumaTH(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            switch (mapId) {

                case 5 -> {
                    int vip = player.vip;
                    String bacvip = "\b|3|chưa mỏ khóa vip";
                    String dmk = "\b|5|Đã mở khóa";
                    switch (vip) {
                        case 1:
                            bacvip = "\b|5| Mở Vip";
                            break;
                        case 2:
                            bacvip = "\b|5| Vip1";
                            break;
                        case 3:
                            bacvip = "\b|5| Vip 2";
                            break;
                        case 4:
                            bacvip = "\b|5| Vip 3";
                            break;

                    }
                    if (vip == 0) {
                        createOtherMenu(player, ConstNpc.BASE_MENU,
                                "Chan Quỹ Giúp duy trì game và giúp ngươi được buồng trưởng quan tâm hơn\n"
                                        + "\b|1|Cấp vip ngươi hiện tại: " + bacvip + " \n",

                                "Mua vip");
                    } else if (vip == 1) {
                        createOtherMenu(player, ConstNpc.BASE_MENU,
                                "Chan Quỹ Giúp duy trì game và giúp ngươi được buồng trưởng quan tâm hơn\n"
                                        + "\b|1|Cấp vip ngươi hiện tại: " + bacvip + " \n"
                                        + "\b|1| Tăng 20 TNSM Cho đệ tử và sư phụ" + dmk + " \n",
                                "Mua vip");
                    } else if (vip == 2) {
                        createOtherMenu(player, ConstNpc.BASE_MENU,
                                "Chan Quỹ Giúp duy trì game và giúp ngươi được buồng trưởng quan tâm hơn\n"
                                        + "\b|1|Cấp vip ngươi hiện tại: " + bacvip + " \n"
                                        + "\b|1| Tăng x2 thỏi vàng lụm được và tỷ lệ đập đồ Cho đệ tử và sư phụ " + dmk
                                        + " \n"
                                        + "\b|1| Tăng x2 sản thỏi vàng lụm được và tỷ lệ đập đồ  " + dmk + " \n",
                                "Mua vip");
                    } else if (vip == 3) {
                        createOtherMenu(player, ConstNpc.BASE_MENU,
                                "Chan Quỹ Giúp duy trì game và giúp ngươi được buồng trưởng quan tâm hơn\n"
                                        + "\b|1|Cấp vip ngươi hiện tại: " + bacvip + " \n"
                                        + "\b|1| Tăng 20 TNSM Cho đệ tử và sư phụ " + dmk + " \n"
                                        + "\b|1| Tăng x2 thỏi vàng lụm được và tỷ lệ đập đồ  " + dmk + " \n"
                                        + "\b|1| SHOP VIP " + dmk + " \n",
                                "Mua vip", "SHOP VIP");
                    } else {
                        createOtherMenu(player, ConstNpc.BASE_MENU,
                                "Chan Quỹ Giúp duy trì game và giúp ngươi được buồng trưởng quan tâm hơn\n"
                                        + "\b|1|Cấp vip ngươi hiện tại: " + bacvip + " \n"
                                        + "\b|1| Tăng 20 TNSM Cho đệ tử và sư phụ " + dmk + " \n"
                                        + "\b|1| Tăng x2 thỏi vàng lụm được và tỷ lệ đập đồ  " + dmk + " \n"
                                        + "\b|1| SHOP VIP " + dmk + " \n"
                                        + "\b|1| Máp Vùng Đất Hủy Diệt" + dmk + "\n",
                                "Mua vip", "SHOP VIP", "Vùng Đất Hủy Diệt");
                    }

                }
                case 169 -> {
                    createOtherMenu(player, ConstNpc.BASE_MENU, "Ra khỏi ngôi làng này sẽ gặp ngọn núi ngũ hành sơn",
                            "Về\n Đảo Kame", "Đóng");
                }

                default ->
                    super.openBaseMenu(player);
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.iDMark.isBaseMenu()) {
                switch (mapId) {
                    case 5 -> {
                        int vip = player.vip;
                        String Menu = "";
                        String bacvip = "\b|3|chưa mỏ khóa vip";
                        String dmk = "\b|5|Đã mở khóa";
                        switch (vip) {
                            case 1:
                                Menu = "Mâm 3";
                                bacvip = "\b|5| Mở Vip";
                                break;
                            case 2:
                                Menu = "Vip2";
                                bacvip = "\b|5|Vip1";
                                break;
                            case 3:
                                Menu = "Vip3";
                                bacvip = "\b|5| Vip2";
                                break;
                            case 4:
                                bacvip = "\b|5| Vip3";
                                Menu = "FULL Vip rồi";
                                break;
                            default:
                                Menu = "Mở Vip";
                                break;

                        }
                        if (select == 0) {

                            createOtherMenu(player, 1,
                                    "Chan Quỹ Giúp duy trì game và giúp ngươi được buồng trưởng quan tâm hơn\n"
                                            + "\b|1|Cấp vip ngươi hiện tại: " + bacvip + " \n",

                                    Menu, "Đóng");

                        }
                        if (select == 1) {
                            ShopService.gI().opendShop(player, "Shop_Vip", false);
                        }
                        if (select == 2) {
                            ChangeMapService.gI().changeMapNonSpaceship(player, 169, 50, 384);
                        }

                    }
                    case 169 -> {
                        if (select == 0) {
                            ChangeMapService.gI().changeMapNonSpaceship(player, 5, Util.nextInt(700, 800), 432);
                        }
                    }

                }
            } else if (player.iDMark.getIndexMenu() == 1) {
                int vip = player.vip;
                String bacvip = "\b|3|chưa mỏ khóa vip";
                String dmk = "\b|5|Đã mở khóa";
                switch (select) {
                    case 0 -> {
                        switch (vip) {
                            case 1:
                                bacvip = "\b|5|Mở Vip";
                                createOtherMenu(player, 12, "Đóng Vip 3 giúp ngươi:\n"

                                        + "\b|1| Hoặc ngươi có thể mở khóa free sau khi hoàn thành Nhiệm vụ Xên Hoàn Thiện\n"
                                        + "\n\b|7|Bạn đang có :" + player.getSession().cash + " VND\n|4|"
                                        + "\b|1|Cấp vip ngươi hiện tại: " + bacvip + " \n"
                                        + "\b|3| Mua Ngay: " + ConstVip.getDiscountedPrice(ConstVip.VIP_2, player.vip)
                                        + "\n"
                                        + "\b|1| Tăng 50% sát thương Kamejoko cho đệ\n"
                                        + "\b|5| Tăng x2 Tỷ lệ đập đồ và sản lượng vàng từ quái \n"
                                        + "\b|5| Mở Khóa Danh hiệu Vip 2 và nhận x1 Giáp luyện tập cấp 4\n",
                                        "Mua  Ngay ", "Mua Free");

                                break;
                            case 2:
                                bacvip = "\b|5| Vip1";
                                createOtherMenu(player, 13, "Đóng Vip 2 giúp ngươi:\n"
                                        + "\n\b|7|Bạn đang có :" + player.getSession().cash + " VND\n|4|"
                                        + "\b|1|Cấp vip ngươi hiện tại: " + bacvip + " \n"
                                        + "\b|3| Mua Ngay: " + ConstVip.getDiscountedPrice(ConstVip.VIP_3, player.vip)
                                        + "\n"
                                        + "\b|5|Mở Khóa Shop VIP\n"
                                        + "\b|5| Kháng lạnh cho đệ\n"
                                        + "\b|5| Mở Khóa Danh hiệu vip cho đệ và x10 Rương kích hoạt vải thô Random\n",
                                        "Mua    Ngay", "Đóng");
                                break;
                            case 3:
                                bacvip = "\b|5| vip2";
                                createOtherMenu(player, 14, "Đóng Vip 1 giúp ngươi:\n"
                                        + "\n\b|7|Bạn đang có :" + player.getSession().cash + " VND\n|4|"
                                        + "\b|1|Cấp vip ngươi hiện tại: " + bacvip + " \n"
                                        + "\b|3| Mua Ngay: " + ConstVip.getDiscountedPrice(ConstVip.VIP_4, player.vip)
                                        + "\n"
                                        + "\b|5|Mở Khóa Map Vip\n"
                                        + "\b|5|Tăng 10% SĐ,HP, KI cho đệ\n"
                                        + "\b|5| Mở Khóa Danh hiệu Vip cho đệ và nhận x1 Cánh Bán thần\n",
                                        "Mua Ngay ", "Đóng");

                                break;
                            case 4:
                                break;
                            default:
                                createOtherMenu(player, 11,
                                        "Đóng vé quan tâm giúp ngươi next tới nhiệm vụ Tiêu diệt KUKU\n"
                                                + "\n\b|7|Bạn đang có :" + player.getSession().cash + " VND\n|4|"
                                                + "\b|1|Cấp vip ngươi hiện tại: " + bacvip + " \n"
                                                + " \b|3| Mua ngay " + ConstVip.PRICE_VIP_1 + " Vnđ\n"
                                                + "\b|1| Hoặc ngươi có thể mở khóa free sau khi hoàn thành Nhiệm vụ Fide\n"
                                                + "\b|5| Tăng 20TNSM, 10% hút HpKI cho Đệ tử \n"
                                                + "\b|5| Mở Khóa Danh hiệu Vip cho đệ và nhận x10 3s\n",
                                        "Mua Vip ", "Mua Free");
                                break;
                        }

                    }

                }

            } else if (player.iDMark.getIndexMenu() == 11) {
                switch (select) {
                    case 0 -> {
                        if (player.vip > 0) {
                            Service.gI().sendThongBao(player, "Ngáo ngơ quan tâm rồi");
                            return;
                        }
                        if (player.getSession().cash < ConstVip.PRICE_VIP_1) {
                            Service.gI().sendThongBao(player, "20k của ta đâu!!");
                            return;
                        }
                        if (PlayerDAO.subcash(player, ConstVip.PRICE_VIP_1)) {
                            if (TaskService.gI().getIdTask(player) < ConstTask.TASK_20_0) {
                                player.playerTask.taskMain.id = 20;
                                player.playerTask.taskMain.index = 0;
                                TaskService.gI().sendNextTaskMain(player);
                            }
                            player.vip = 1;
                            Item bas = ItemService.gI().createNewItem((short) 16, 10);
                            InventoryService.gI().addItemBag(player, bas);
                            InventoryService.gI().sendItemBag(player);
                            ChangeMapService.gI().exitMap(player.pet);
                            Service.gI().sendThongBao(player, "Đóng quan tâm thành công và nhận x10 ngọc rồng 3 sao");

                        }
                    }
                    case 1 -> {
                        if (player.vip > 0) {
                            Service.gI().sendThongBao(player, "Ngáo ngơ quan tâm  rồi");
                            return;
                        }
                        if (TaskService.gI().getIdTask(player) < ConstTask.TASK_23_0) {
                            Service.gI().sendThongBao(player, "Cần hoàn thành nhiệm vụ fide để mở khóa free");
                            return;
                        } else {
                            player.vip = 1;
                            Item bas = ItemService.gI().createNewItem((short) 16, 110);

                            InventoryService.gI().addItemBag(player, bas);
                            InventoryService.gI().sendItemBag(player);
                            ChangeMapService.gI().exitMap(player.pet);
                            Service.gI().sendThongBao(player, "Đóng quan tâm thành công và nhận x10 ngọc rồng 3sao");
                        }

                    }
                }
            } else if (player.iDMark.getIndexMenu() == 12) {
                switch (select) {
                    case 0 -> {
                        if (player.vip > 1) {
                            Service.gI().sendThongBao(player, "Ngáo ngơ Mâm 3 rồi");
                            return;
                        }
                        if (player.getSession().cash < ConstVip.getDiscountedPrice(ConstVip.VIP_2, player.vip)) {
                            Service.gI().sendThongBao(player, "còn thiếu "
                                    + ConstVip.getDiscountedPrice(ConstVip.VIP_2, player.vip) + " Để nâng mâm");
                            return;
                        }
                        int cashsub = ConstVip.getDiscountedPrice(ConstVip.VIP_2, player.vip);
                        if (PlayerDAO.subcash(player, cashsub)) {

                            player.vip = 2;
                            Item bas = ItemService.gI().createNewItem((short) 1745, 1);
                            bas.itemOptions.add(new Item.ItemOption(9, 100));
                            InventoryService.gI().addItemBag(player, bas);
                            InventoryService.gI().sendItemBag(player);
                            ChangeMapService.gI().exitMap(player.pet);
                            Service.gI().sendThongBao(player, "Nâng Mâm 3 thành công và nhận x1 Giáp Luyện tập 4");

                        }
                    }
                    case 1 -> {
                        if (player.vip > 1) {
                            Service.gI().sendThongBao(player, "Ngáo ngơ Mâm 3 rồi");
                            return;
                        }
                        if (TaskService.gI().getIdTask(player) < ConstTask.TASK_28_3) {
                            Service.gI().sendThongBao(player,
                                    "Cần hoàn thành nhiệm vụ Xên Hoàn thiện để lên mâm 3 Free");
                            return;
                        } else {
                            player.vip = 2;
                            Item bas = ItemService.gI().createNewItem((short) 1745, 1);
                            bas.itemOptions.add(new Item.ItemOption(9, 100));
                            InventoryService.gI().addItemBag(player, bas);
                            InventoryService.gI().sendItemBag(player);
                            ChangeMapService.gI().exitMap(player.pet);
                            Service.gI().sendThongBao(player, "Nâng Mâm 3 thành công và nhận x1 Giáp Luyện tập 4");
                        }

                    }
                }
            } else if (player.iDMark.getIndexMenu() == 13) {
                switch (select) {
                    case 0 -> {
                        if (player.vip > 2) {
                            Service.gI().sendThongBao(player, "Ngáo ngơ Mâm 2 rồi");
                            return;
                        }
                        if (player.getSession().cash < ConstVip.getDiscountedPrice(ConstVip.VIP_3, player.vip)) {
                            Service.gI().sendThongBao(player, "còn thiếu "
                                    + ConstVip.getDiscountedPrice(ConstVip.VIP_3, player.vip) + " Để nâng mâm");
                            return;
                        }
                        int cashsub = ConstVip.getDiscountedPrice(ConstVip.VIP_3, player.vip);
                        if (PlayerDAO.subcash(player, cashsub)) {

                            player.vip = 3;
                            Item bas = ItemService.gI().createNewItem((short) 1536, 10);
                            InventoryService.gI().addItemBag(player, bas);
                            InventoryService.gI().sendItemBag(player);

                            Service.gI().sendThongBao(player,
                                    "Nâng Mâm 2 thành công và nhận x10 Hòm kích hoạt vải thô");
                            ChangeMapService.gI().exitMap(player.pet);
                        }
                    }

                }
            } else if (player.iDMark.getIndexMenu() == 14) {
                switch (select) {
                    case 0 -> {
                        if (player.vip > 3) {
                            Service.gI().sendThongBao(player, "Ngáo ngơ Mâm 1 rồi");
                            return;
                        }
                        if (player.getSession().cash < ConstVip.getDiscountedPrice(ConstVip.VIP_4, player.vip)) {
                            Service.gI().sendThongBao(player, "còn thiếu "
                                    + ConstVip.getDiscountedPrice(ConstVip.VIP_4, player.vip) + " Để nâng mâm");
                            return;
                        }
                        int cashsub = ConstVip.getDiscountedPrice(ConstVip.VIP_4, player.vip);
                        if (PlayerDAO.subcash(player, cashsub)) {

                            player.vip = 4;
                            Item bas = ItemService.gI().createNewItem((short) 1638, 1);
                            ChangeMapService.gI().exitMap(player.pet);

                            bas.itemOptions.add(new Item.ItemOption(50, 13));
                            bas.itemOptions.add(new Item.ItemOption(77, 13));
                            bas.itemOptions.add(new Item.ItemOption(103, 13));
                            bas.itemOptions.add(new Item.ItemOption(5, 10));
                            InventoryService.gI().addItemBag(player, bas);
                            InventoryService.gI().sendItemBag(player);
                            Service.gI().sendThongBao(player, "Nâng Mâm 1 thành công và nhận x1 Cánh bán thần");

                        }
                    }

                }

            }
        }
    }
}
