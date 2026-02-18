package nro.models.npc.npc_manifest;

/**
 *
 * Box ZALO:https://zalo.me/g/ifjict764 sdt zalo: 0358176187 Chuyên chỉnh sữa
 * mua bán source nro,...
 */
import consts.ConstNpc;
import consts.ConstNpcConfig;
import item.Item;
import jdbc.daos.PlayerDAO;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.NpcService;
import nro.services.PetService;
import nro.services.Service;
import nro.services.TaskService;
import services.func.Input;
import shop.ShopService;

public class Brolysama extends Npc {

    public Brolysama(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                switch (mapId) {
                    case 5 ->
                        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                "Chào ngài, ta có thể giúp gì cho ngài"
                                + "\b|7|Bạn đang có :" + player.getSession().cash + " VND\n|4|",
                                "Cửa hàng",
                                "Menu VND",
                                "Nạp Tiền",
                                "Đổi đệ",
                                " Mở thành viên",
                                "Đổi skill đệ");
                    default ->
                        super.openBaseMenu(player);
                }
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.iDMark.isBaseMenu()) {
                switch (select) {
                    case 0:
                        ShopService.gI().opendShop(player, "BARDOCK_SHOP", false);
                        break;
                    case 1:
                        if (player.getSession() != null) {
                            this.createOtherMenu(player, 777,
                                    "\b|1|Có tiền rồi đổi thôi!"
                                    + "|6|\bCó thể nhận mốc nạp khi nạp ở quy lão Kame nha"
                                    + "\b|7|Bạn đang có :" + player.getSession().cash + " VND",
                                    "Đổi thỏi vàng", "Đổi ngọc xanh", "Đổi hồng ngọc");
                        }
                        break;
                    case 2:
                        NpcService.gI().createBigMessage(player, avartar, "Nhớ đăng nhập xong sau đó bấm NẠP!!!", (byte) 1, "NẠP", "https://dragonball.online/nap-atm.php");
                        break;
                    case 3:
                        if (player.getSession() != null) {
                            this.createOtherMenu(player, 13000,
                                    "\b|1|Có tiền rồi đổi thôi!\n|3|"
                                    + "Đổi đệ thì tháo cái đồ đệ ra, mất tự chịu nha!\n|7|"
                                    + "Thông tin từng loại đệ:\n|2|"
                                    + "Fide nhí:+ 20 - 30% chỉ số khi hợp thể\n|2|"
                                    + "Cell nhí:+ 25 - 45% chỉ số khi hợp thể\n|2|"
                                    + "Bưu nhí:+ 30 - 55% chỉ số khi hợp thể\n|2|"
                                    + "Adr bãi biển:+ 45 - 60% chỉ số khi hợp thể\n|2|"
                                    + "Bưu gầy:+ 70% chỉ số khi hợp thể\n|2|"
                                    + "Berrus nhí:+ 45 - 80% chỉ số khi hợp thể\n|1|"
                                    + "Bạn đang có :" + player.getSession().cash + " VND\n|6|"
                                    + "\nChỉ số hợp thể đệ hiện tại :\n|7|"
                                    + "-HP:" + player.pointfusion.getHpFusion() + " ,KI:" + player.pointfusion.getMpFusion() + " ,DAME:" + player.pointfusion.getDameFusion() + "",
                                    "Mua đệ\nFide nhí\n<" + ConstNpcConfig.de31 + ">", "Mua đệ\nCell nhí\n<" + ConstNpcConfig.de32 + ">", "Mua đệ\nBưu nhí\n<" + ConstNpcConfig.de33 + ">", "Mua đệ\nAdr bãi biển\n<" + ConstNpcConfig.de34 + ">", "Mua đệ\nBưu gầy\n<" + ConstNpcConfig.de35 + ">", "Mua đệ\nBerrus nhí\n<" + ConstNpcConfig.de36 + ">");
                        }
                        break;

                    case 4:
                        if (player.getSession() != null) {
                            this.createOtherMenu(player, 782,
                                    "\b|2|Mở thành viên giá 20000 VND \n \b|7|Bạn đã nạp :"
                                    + "" + player.getSession().cash + " đồng\n|4|"
                                    + " + \"Giftcode mở thành viên: codethanhvien1 - codethanhvien2 - codethanhvien3\"",
                                    "Mở", "Đóng");
                        }
                        break;
                    case 5:
                        if (player.getSession() != null) {
                            this.createOtherMenu(player, 888,
                                    "|0|Lưu ý: Đổi Skill đệ bằng tiền nạp sẽ mất VND\n|7|"
                                    + "Bạn có: " + player.getSession().cash + " VND",
                                    //Menu CHọn
                                    "Đổi skill 2-3\n <" + ConstNpcConfig.skill23 + ">", "Đổi skill 2-4\n <" + ConstNpcConfig.skill24 + ">");

                        }
                        break;

                }
            } else if (player.iDMark.getIndexMenu() == 13000) {
                switch (select) {
                    case 0:
                        if (player.pet == null) {
                            Service.gI().sendThongBao(player, "Ngươi cần phải có đệ mới sử dụng được chức năng này?");
                            return;
                        }
                        for (Item item : player.pet.inventory.itemsBody) {
                            if (item.isNotNullItem()) {
                                Service.gI().sendThongBao(player, "Cần bỏ đồ đệ tử đang mặc để sử dụng chức năng?");
                                return;
                            }
                        }
                        if (player.getSession() != null && player.getSession().cash < ConstNpcConfig.de31) {
                            Service.gI().sendThongBao(player, "Bạn không đủ " + ConstNpcConfig.de31 + " VND");
                            return;
                        }

                        if (PlayerDAO.subcash(player, ConstNpcConfig.de31)) {
//                            PetService.gI().createPetFideNhi(player, player.pet != null, player.gender);
                            Service.gI().sendThongBao(player, "Bạn đã nhận được đệ Fide Nhí");
                        } else {
                            Service.gI().sendThongBao(player, "Đã có lỗi xảy ra !!");
                        }

                        break;
                    case 1:
                        if (player.pet == null) {
                            Service.gI().sendThongBao(player, "Ngươi cần phải có đệ mới sử dụng được chức năng này?");
                            return;
                        }
                        for (Item item : player.pet.inventory.itemsBody) {
                            if (item.isNotNullItem()) {
                                Service.gI().sendThongBao(player, "Cần bỏ đồ đệ tử đang mặc để sử dụng chức năng?");
                                return;
                            }
                        }
                        if (player.getSession() != null && player.getSession().cash < ConstNpcConfig.de32) {
                            Service.gI().sendThongBao(player, "Bạn không đủ " + ConstNpcConfig.de32 + " VND");
                            return;
                        }

                        if (PlayerDAO.subcash(player, ConstNpcConfig.de32)) {
//                            PetService.gI().createPetCellNhi(player, player.pet != null, player.gender);
                            Service.gI().sendThongBao(player, "Bạn đã nhận được đệ Cell Nhí");
                        } else {
                            Service.gI().sendThongBao(player, "Đã có lỗi xảy ra !!");
                        }
                        break;
                    case 2:
                        if (player.pet == null) {
                            Service.gI().sendThongBao(player, "Ngươi cần phải có đệ mới sử dụng được chức năng này?");
                            return;
                        }

                        for (Item item : player.pet.inventory.itemsBody) {
                            if (item.isNotNullItem()) {
                                Service.gI().sendThongBao(player, "Cần bỏ đồ đệ tử đang mặc để sử dụng chức năng?");
                                return;
                            }
                        }
                        if (player.getSession() != null && player.getSession().cash < ConstNpcConfig.de33) {
                            Service.gI().sendThongBao(player, "Bạn không đủ " + ConstNpcConfig.de33 + " VND");
                            return;
                        }

                        if (PlayerDAO.subcash(player, ConstNpcConfig.de33)) {
//                            PetService.gI().createPetBuuNhi(player, player.pet != null, player.gender);
                            Service.gI().sendThongBao(player, "Bạn đã nhận được đệ Bưu Nhí");
                        } else {
                            Service.gI().sendThongBao(player, "Đã có lỗi xảy ra !!");
                        }

                        break;
                    case 3:
                        if (player.pet == null) {
                            Service.gI().sendThongBao(player, "Ngươi cần phải có đệ mới sử dụng được chức năng này?");
                            return;
                        }
                        for (Item item : player.pet.inventory.itemsBody) {
                            if (item.isNotNullItem()) {
                                Service.gI().sendThongBao(player, "Cần bỏ đồ đệ tử đang mặc để sử dụng chức năng?");
                                return;
                            }
                        }
                        if (player.getSession() != null && player.getSession().cash < ConstNpcConfig.de34) {
                            Service.gI().sendThongBao(player, "Bạn không đủ " + ConstNpcConfig.de34 + " VND");
                            return;
                        }

                        if (PlayerDAO.subcash(player, ConstNpcConfig.de34)) {
//                            PetService.gI().createPetAdrBeach(player, player.pet != null, player.gender);
                            Service.gI().sendThongBao(player, "Bạn đã nhận được đệ Adr Bãi biển");
                        } else {
                            Service.gI().sendThongBao(player, "Đã có lỗi xảy ra !!");
                        }

                        break;
                    case 4:
                        if (player.pet == null) {
                            Service.gI().sendThongBao(player, "Ngươi cần phải có đệ mới sử dụng được chức năng này?");
                            return;
                        }
                        for (Item item : player.pet.inventory.itemsBody) {
                            if (item.isNotNullItem()) {
                                Service.gI().sendThongBao(player, "Cần bỏ đồ đệ tử đang mặc để sử dụng chức năng?");
                                return;
                            }
                        }
                        if (player.getSession() != null && player.getSession().cash < ConstNpcConfig.de35) {
                            Service.gI().sendThongBao(player, "Bạn không đủ " + ConstNpcConfig.de35 + " VND");
                            return;
                        }

                        if (PlayerDAO.subcash(player, ConstNpcConfig.de35)) {
//                            PetService.gI().createPetMabuGay(player, player.pet != null, player.gender);
                            Service.gI().sendThongBao(player, "Bạn đã nhận được đệ Bưu Gầy");
                        } else {
                            Service.gI().sendThongBao(player, "Đã có lỗi xảy ra !!");
                        }
                        break;
                    case 5:
                        if (player.pet == null) {
                            Service.gI().sendThongBao(player, "Ngươi cần phải có đệ mới sử dụng được chức năng này?");
                            return;
                        }
                        for (Item item : player.pet.inventory.itemsBody) {
                            if (item.isNotNullItem()) {
                                Service.gI().sendThongBao(player, "Cần bỏ đồ đệ tử đang mặc để sử dụng chức năng?");
                                return;
                            }
                        }
                        if (player.getSession() != null && player.getSession().cash < ConstNpcConfig.de36) {
                            Service.gI().sendThongBao(player, "Bạn không đủ " + ConstNpcConfig.de36 + " VND");
                            return;
                        }

                        if (PlayerDAO.subcash(player, ConstNpcConfig.de36)) {
//                            PetService.gI().createPetBerrusNhi(player, player.pet != null, player.gender);
                            Service.gI().sendThongBao(player, "Bạn đã nhận được đệ Berrus");
                        } else {
                            Service.gI().sendThongBao(player, "Đã có lỗi xảy ra !!");
                        }
                        break;

                }
            } else if (player.iDMark.getIndexMenu() == 888) {
                switch (select) {
                    case 0: //thay chiêu 2-3 đệ tử
                        if (player.getSession() != null && player.getSession().cash < ConstNpcConfig.skill23) {
                            Service.gI().sendThongBao(player, "Bạn không đủ " + ConstNpcConfig.skill23 + " VND");
                            return;
                        }

                        if (PlayerDAO.subcash(player, ConstNpcConfig.skill23)) {
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
                    case 1: //thay chiêu 2-4 đệ tử
                        if (player.getSession() != null && player.getSession().cash < ConstNpcConfig.skill24) {
                            Service.gI().sendThongBao(player, "Bạn không đủ " + ConstNpcConfig.skill24 + " VND");
                            return;
                        }

                        if (PlayerDAO.subcash(player, ConstNpcConfig.skill24)) {
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

                }
            } else if (player.iDMark.getIndexMenu() == 777) {
                switch (select) {
                    case 0:
                        Input.gI().createFormDoiThoiVang(player);
                        break;
                    case 1:
                        Input.gI().createFormDoiNgocXanh(player);
                        break;
                    case 2:
                        Input.gI().createFormDoiNgocHong(player);
                        break;
                }
            } else if (player.iDMark.getIndexMenu() == 782) {
                switch (select) {
                    case 0:
                        if (player.getSession() != null && player.getSession().actived) {
                            Service.gI().sendThongBao(player, "Bạn đã mở thành viên rồi");
                            return;
                        }
                        if (player.getSession() != null && player.getSession().cash < 20000) {
                            NpcService.gI().createBigMessage(player, avartar, "Bạn chưa nạp 20K, bạn có muốn nạp để mở thành viên không ?!!!", (byte) 1, "NẠP", "https://dragonball.online/nap-atm.php");
                            return;
                        }
                        if (PlayerDAO.updateActive(player, 1)) {
                            Service.gI().sendThongBao(player, "Bạn đã mở thành viên thành công");
                        } else {
                            Service.gI().sendThongBao(player, "Đã có lỗi xẩy ra khi kích hoạt tài khoản, vui long liên hệ admin nếu bị trừ tiền mà không kích hoạt được, chụp lại thông báo này");
                        }

                        break;
                    case 1:

                        break;

                }
            } else if (player.iDMark.getIndexMenu() == 0) {
                switch (mapId) {
                }
            }

        }
    }
}
