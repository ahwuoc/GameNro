package npc.npc_manifest;

import boss.BossID;
import consts.ConstNpc;
import consts.ConstTaskBadges;
import consts.ConstTaskPet;
import consts.cn;
import item.Item;
import static java.lang.Math.pow;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import jdbc.daos.PlayerDAO;
import models.Combine.CombineService;
import models.Training.TrainingService;
import npc.Npc;
import player.Player;
import services.InventoryService;
import services.ItemService;
import services.NpcService;
import services.Service;
import services.func.ChangeMapService;
import shop.ShopService;
import task.Badges.BadgesTask;
import task.Badges.BadgesTaskService;
import task.Pet.PetTaskService;
import utils.Util;

public class NangDe extends Npc {

    public NangDe(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (player.pet != null) {
                this.createOtherMenu(player, ConstNpc.BASE_MENU, ""
                        + "Làm Nhiệm vụ đệ tử để nhận Đá Nâng cấp đệ\n"
                        + "Đệ tử đạt level tối đa là level 10\n"
                        + "Mỗi level sẽ tăng thêm 1% chỉ số hợp thể\n"
                        + "\b|7|Map nghĩa địa nơi up Hồn Bông tai cấp 3, cần đệ 40 tỷ hãy cẩn trọng trong đó, nếu chết sẽ ko thể hồi sinh và boss rất nguy hiểm\n",
                        "Nhiệm Vụ\n Đệ Tử",
                        "Nâng cấp Đệ tử",
                        "Shop đệ tử",
                        "Đổi Skill Đệ", "Tới Nghĩa Địa");
            } else {
                Service.gI().sendThongBao(player, "Cần có đệ tử để mở khóa tính năng này");
                return;
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.iDMark.isBaseMenu()) {
                switch (select) {
                    case 0 -> {

                        if (player.PetTask.id <= 0) {
                            player.PetTask.id = 1;
                            PetTaskService.UpdateTask(player, 1);
                        }

                        this.createOtherMenu(player, 900, "Hoàn Thành nhiệm vụ cho đệ nhận Đá Nâng Cấp đệ"
                                + "\b|1| Nếu là Nhiệm vụ tiêu diệt boss thì vào lúc 18-19H hàng ngày Chỉ Đệ tử mới có thể gây sát thương lên boss\n"
                                + "\b|5|Nhiệm vụ hiện tại: " + player.PetTask.id + ": "
                                + PetTaskService.nameTask(player.PetTask.id) + "\n"
                                + " \b|3|Tiến Độ: " + player.PetTask.count + "/" + player.PetTask.countMax + " ("
                                + player.PetTask.getPercentProcess() + "%)"
                                + "Hoàn Thành Nhận: " + PetTaskService.Reward(player.PetTask.id) + " Đá Nâng Cấp Đệ\n",

                                "Trả Nhiệm vụ");
                    }

                    case 1 -> {
                        int type = player.pet.typePet;
                        int lv = player.pet.level;
                        int csht = 0;
                        if (type == 1) {
                            csht = 10;
                        }
                        if (type >= 2) {
                            csht = 20;
                        }
                        csht += lv;

                        if (type >= 2) {
                            if (lv < 5) {
                                this.createOtherMenu(player, 887, "\b|7| Thông tin đệ tử:\n"
                                        + "\b|5| Tên Đệ: " + player.pet.name + "\n"
                                        + "\b|1| Level tiếp theo cần: " + (int) pow(2, lv) + " Đá Nâng cấp\n"
                                        + "\b|3| Tăng chỉ số hợp thể: " + (csht + lv) + " %\n"
                                        + "\b|5| Cấp hiện tại " + lv + "\n"
                                        + "\b|7| Nâng chỉ sô " + lv + "/5 để mở khóa"
                                        + "\b|7| Nâng chỉ số đặc biệt " + lv + "/10 để mở khóa",
                                        "Nâng cấp"

                                );
                            } else if (lv < 10) {
                                this.createOtherMenu(player, 887, "\b|7| Thông tin đệ tử:\n"
                                        + "\b|3| Tên Đệ: " + player.pet.name + "\n"
                                        + "\b|1| Level tiếp theo cần: " + (int) pow(2, lv) + " Đá Nâng cấp\n"
                                        + "\b|3| Tăng chỉ số hợp thể: " + csht + " %\n"
                                        + "\b|5| Cấp hiện tại " + lv + "\n"

                                        + "\b|5| Chọn Option Đã mở khỏa"
                                        + "\b|7| Nâng chỉ số đặc biệt " + lv + "/10 để mở khóa",
                                        "Nâng cấp",

                                        "Chọn Option");
                            } else {
                                this.createOtherMenu(player, 887, "\b|7| Thông tin đệ tử:\n"
                                        + "\b|3| Tên Đệ: " + player.pet.name + "\n"
                                        + "\b|3| Tăng chỉ số hợp thể: " + csht + " %\n"
                                        + "\b|5| Cấp hiện tại " + lv + "\n"
                                        + "\b|5| Chọn Option Đã mở khỏa"
                                        + "\b|5| Nâng chỉ số đặc biệt Đã mở khóa",
                                        "Nâng cấp Đệ tử",
                                        "Chọn Option",
                                        "Nâng Option");
                            }
                        } else {
                            if (player.pet.level < 10) {
                                this.createOtherMenu(player, 887, "\b|7| Thông tin đệ tử:\n"

                                        + "\b|5| Tên Đệ: " + player.pet.name + "\n"
                                        + "\b|1| Level tiếp theo cần: " + (int) pow(2, lv) + " Đá Nâng cấp\n"
                                        + "\b|3| Tăng chỉ số hợp thể: " + csht + " %\n"
                                        + "\b|5| Cấp hiện tại " + lv + "\n"

                                        ,
                                        "Nâng cấp");
                            } else {
                                this.createOtherMenu(player, 887, "\b|7| Thông tin đệ tử:\n"
                                        + "\b|5| Tên Đệ: " + player.pet.name + "\n"
                                        + "\b|1| Level tiếp theo cần: " + (int) pow(2, lv) + " Đá Nâng cấp\n"
                                        + "\b|3| Tăng chỉ số hợp thể: " + csht + " %\n"
                                        + "\b|5| Cấp hiện tại " + lv + "\n"

                                        ,
                                        "Nâng cấp");
                            }
                        }
                        break;
                    }

                    case 2 -> {
                        ShopService.gI().opendShop(player, "SHOP_DE", false);
                        break;
                    }
                    case 3 -> {
                        if (player.getSession() != null) {
                            this.createOtherMenu(player, 888,
                                    "|0|Lưu ý: Đổi Skill đệ bằng tiền nạp sẽ mất VND\n|7|"
                                            + "Bạn có: " + player.getSession().cash + " VND",
                                    // Menu CHọn
                                    "Đổi skill 2-3\n <" + cn.skill23 + ">", "Đổi skill 2-4\n <" + cn.skill24 + ">",
                                    "Đổi skill 5\n <" + cn.skill5);

                        }
                        break;
                    }
                    case 4 -> {
                        if (player.pet.nPoint.power < 40_000_000_000L) {
                            Service.gI().sendThongBao(player, "Cố mẹ gắng đi e, đệ 40 tỷ đã");
                            return;
                        }

                        ChangeMapService.gI().changeMapNonSpaceship(player, 181, 615, 288);
                        break;
                    }

                }
            } else if (player.iDMark.getIndexMenu() == 900) {
                switch (select) {
                    case 0: {
                        if (PetTaskService.isDoneTask(player)) {
                            int sl = PetTaskService.Reward(player.PetTask.id);
                            Item danc = ItemService.gI().createNewItem((short) 1739, sl);
                            InventoryService.gI().addItemBag(player, danc);
                            InventoryService.gI().sendItemBag(player);
                            Service.gI().sendThongBao(player,
                                    "Chúc mừng Đệ tử Bạn đã hoàn thành nhiệm vụ ,Bạn nhận được x" + sl + " Đá nâng Đệ");
                            player.PetTask.id++;
                            PetTaskService.UpdateTask(player, player.PetTask.id);
                            Service.gI().sendThongBao(player,
                                    "Nhiệm vụ Đệ tử tiếp theo là: " + PetTaskService.nameTask(player.PetTask.id));
                        } else {
                            Service.gI().sendThongBao(player, "Cần Hoàn Thành Nhiệm vụ để trả Nhiệm vụ");
                        }
                    }
                }

            } else if (player.iDMark.getIndexMenu() == 887) {
                switch (select) {
                    case 0:
                        int lv = player.pet.level;
                        int type = player.pet.typePet;
                        int sl = (int) pow(2, lv);
                        int id = 1739;

                        Item dancde = InventoryService.gI().findItemBag(player, id);
                        if (player.inventory.gem < sl) {
                            Service.gI().sendThongBao(player, "Bạn không có đủ ngọc, còn thiếu "
                                    + (sl - player.inventory.gem) + " ngọc nữa");
                            return;
                        }
                        if (dancde == null || dancde.quantity < sl) {
                            Service.gI().sendThongBao(player, "Cần Đá nâng, Để nâng cấp đệ");
                            return;
                        }
                        if (player.pet.level >= 10) {
                            Service.gI().sendThongBao(player, "Pet đã đạt cấp tối đa");
                            return;
                        }

                        player.pet.level++;
                        String newName = player.pet.name.replaceAll("\\[.*?\\]", "") + "[Cấp " + player.pet.level + "]";
                        player.pet.name = newName;
                        player.inventory.gem -= sl;
                        InventoryService.gI().subQuantityItemsBag(player, dancde, sl);
                        InventoryService.gI().sendItemBag(player);
                        ChangeMapService.gI().exitMap(player.pet);
                        if (dancde.quantity >= sl) {
                            this.createOtherMenu(player, 887, "\b|7| Thông tin đệ tử:\n"
                                    + "\b|5|Nâng cấp thành công!!!\n\n"
                                    + "\b|5| Cấp hiện tại " + player.pet.level + "\n"

                                    + "\b|1| Cấp tiếp theo cần: x" + sl + " " + dancde.template.name
                                    + " và Ngọc xanh \n",
                                    "Nâng cấp");
                        }
                        break;
                    case 1:
                        int op = player.optde;
                        if (player.pet.typePet == 3) {
                            this.createOtherMenu(player, 890, "\b|7| Lựa chọn option tăng thêm khi hợp thể\n"
                                    + "\b|7| Chỉ số tăng thêm hiện tại " + op + "%\n"
                                    + "\b|3| Lựa chọn hiện tại, Option: "
                                    + (player.choice > 0 ? player.choice : "Chưa lựa chọn") + "\n"
                                    + "\b|5|Option 1 tăng Sát thương đấm galick \n "
                                    + "\b|5|Option 2 tăng HP,SĐ khi biến khỉ\n "
                                    + "\b|5|Option 3 tăng  sát thương bom\n ",
                                    "Option 1", "Option 2", "Option 3");
                        } else if (player.pet.typePet == 4) {
                            this.createOtherMenu(player, 890, "\b|7| Lựa chọn option tăng thêm khi hợp thể\n"
                                    + "\b|7| Chỉ số tăng thêm hiện tại " + op + "%\n"
                                    + "\b|3| Lựa chọn hiện tại, Option: "
                                    + (player.choice > 0 ? player.choice : "Chưa lựa chọn") + "\n"
                                    + "\b|5|Option 1 tăng Sát thương Liên Hoàn \n "
                                    + "\b|5|Option 2 tăng Sát thương đẻ trứng\n "
                                    + "\b|5|Option 3 tăng  sát thương laze\n ",
                                    "Option 1", "Option 2", "Option 3");
                        } else {
                            this.createOtherMenu(player, 890, "\b|7| Lựa chọn option tăng thêm khi hợp thể\n"
                                    + "\b|7| Chỉ số tăng thêm hiện tại " + op + "%\n"
                                    + "\b|3| Lựa chọn hiện tại, Option: "
                                    + (player.choice > 0 ? player.choice : "Chưa lựa chọn") + "\n"
                                    + "\b|5|Option 1 tăng Sát KameJoko \n "
                                    + "\b|5|Option 2 tăng Sát thương Kaioken\n "
                                    + "\b|5|Option 3 tăng  sát thương Quả cầu kinh khi\n ",
                                    "Option 1", "Option 2", "Option 3");
                        }
                        break;
                    case 2:
                        this.createOtherMenu(player, 891, "\b|7| Nâng cấp option sẽ tiêu tốn Tinh Thạch\n"
                                + "\b|5|Chỉ số tăng thêm hiện Tại " + player.optde + "/"
                                + (player.pet.nPoint.limitPower + 1) + "%\n"
                                + "\b|1| Cấp tiếp theo cần x" + (player.optde + 1) * 2 + " Tinh thạch và thỏi vàng",
                                "Tăng chỉ số", "Đóng");
                        break;

                }

            } else if (player.iDMark.getIndexMenu() == 891) {
                switch (select) {
                    case 0:
                        int sl = (player.optde + 1) * 2;
                        Item tv = InventoryService.gI().findItemBag(player, 457);
                        Item dancde = InventoryService.gI().findItemBag(player, 1823);
                        if (tv == null || tv.quantity < sl) {
                            Service.gI().sendThongBao(player, "Cần " + sl + " tv");
                            return;
                        }
                        if (dancde == null || dancde.quantity < sl) {
                            Service.gI().sendThongBao(player, "Cần Tinh Thạch để nâng option");
                            return;
                        }
                        if (player.optde >= player.pet.nPoint.limitPower) {
                            Service.gI().sendThongBao(player, "Đã đạt cấp tối đa, hãy up đệ thêm để mở thêm giới hạn");
                            return;
                        }
                        player.optde++;
                        Service.gI().sendThongBao(player, "Nâng cấp thành công");
                        InventoryService.gI().subQuantityItemsBag(player, tv, sl);
                        InventoryService.gI().subQuantityItemsBag(player, dancde, sl);
                        InventoryService.gI().sendItemBag(player);

                        break;

                }
            } else if (player.iDMark.getIndexMenu() == 890) {

                switch (select) {

                    case 0:
                        player.choice = 1;
                        if (player.pet.typePet == 3) {
                            Service.gI().sendThongBao(player,
                                    "\b|7| Đổi thành công Option 1 tăng Sát thương đấm galick");
                        } else if (player.pet.typePet == 4) {
                            Service.gI().sendThongBao(player,
                                    "\b|7| Đổi thành công Option 1 tăng Sát thương Liên Hoàn");
                        } else {
                            Service.gI().sendThongBao(player, "\b|7| Đổi thành công Option 1 tăng Sát thương Kamejoko");
                        }
                        break;
                    case 1:
                        player.choice = 2;
                        if (player.pet.typePet == 3) {
                            Service.gI().sendThongBao(player, "\b|5| Đổi thành công Option 2 tăng HP,SĐ khi biến khỉ");
                        } else if (player.pet.typePet == 4) {
                            Service.gI().sendThongBao(player, "\b|5| Đổi thành công Option 2 tăng Sát thương đẻ trứng");
                        } else {
                            Service.gI().sendThongBao(player, "\b|5| Đổi thành công Option 2 tăng Sát thương Kaioken");
                        }
                        break;
                    case 2:
                        player.choice = 3;
                        if (player.pet.typePet == 3) {
                            Service.gI().sendThongBao(player, "\b|3| Đổi thành công Option 3 tăng  sát thương bom");
                        } else if (player.pet.typePet == 4) {
                            Service.gI().sendThongBao(player, "\b|3| Option 3 tăng  sát thương laze");
                        } else {
                            Service.gI().sendThongBao(player,
                                    "\b|3| Đổi thành côngOption 3 tăng  sát thương Quả cầu kinh khi");
                        }
                        break;
                }
            }

            else if (player.iDMark.getIndexMenu() == 888) {
                switch (select) {
                    case 0: // thay chiêu 2-3 đệ tử
                        if (player.getSession() != null && player.getSession().cash < cn.skill23) {
                            Service.gI().sendThongBao(player, "Bạn không đủ " + cn.skill23 + " VND");
                            return;
                        }

                        if (PlayerDAO.subcash(player, cn.skill23)) {
                            if (player.pet != null) {
                                if (player.pet.playerSkill.skills.get(1).skillId != -1) {
                                    player.pet.openSkill2();
                                    if (player.pet.playerSkill.skills.get(2).skillId != -1) {
                                        player.pet.openSkill3();
                                    }
                                    Service.gI().sendThongBao(player, "Đổi skill 2-3 đệ thành công");
                                } else {
                                    Service.gI().sendThongBao(player, "Ít nhất đệ tử ngươi phải có chiêu 2 chứ!");

                                }
                            } else {
                                Service.gI().sendThongBao(player, "Ngươi làm gì có đệ tử?");

                            }
                        }
                        break;
                    case 1: // thay chiêu 2-4 đệ tử
                        if (player.getSession() != null && player.getSession().cash < cn.skill24) {
                            Service.gI().sendThongBao(player, "Bạn không đủ " + cn.skill24 + " VND");
                            return;
                        }

                        if (PlayerDAO.subcash(player, cn.skill24)) {
                            if (player.pet != null) {
                                if (player.pet.playerSkill.skills.get(1).skillId != -1) {
                                    player.pet.openSkill2();
                                    if (player.pet.playerSkill.skills.get(3).skillId != -1) {
                                        player.pet.openSkill4();
                                    }
                                    Service.gI().sendThongBao(player, "Đổi skill 2-4 đệ thành công");

                                } else {
                                    Service.gI().sendThongBao(player, "Ít nhất đệ tử ngươi phải có chiêu 2 chứ!");

                                }
                            } else {
                                Service.gI().sendThongBao(player, "Ngươi làm gì có đệ tử?");

                            }
                        }
                        break;
                    case 2: // thay chiêu 5đệ tử
                        if (player.getSession() != null && player.getSession().cash < cn.skill5) {
                            Service.gI().sendThongBao(player, "Bạn không đủ " + cn.skill5 + " VND");
                            return;
                        }

                        if (PlayerDAO.subcash(player, cn.skill5)) {
                            if (player.pet != null) {
                                if (player.pet.playerSkill.skills.get(4).skillId != -1) {
                                    player.pet.openSkill5();

                                    Service.gI().sendThongBao(player, "Đổi skill 5 đệ thành công");

                                } else {
                                    Service.gI().sendThongBao(player, "Ít nhất đệ tử ngươi phải có chiêu 5 chứ!");

                                }
                            } else {
                                Service.gI().sendThongBao(player, "Ngươi làm gì có đệ tử?");

                            }
                        }
                        break;

                }
            }
        }
    }
}
