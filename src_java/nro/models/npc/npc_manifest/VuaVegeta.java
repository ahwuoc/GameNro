package nro.models.npc.npc_manifest;

/**
 *
 * Box ZALO:https://zalo.me/g/ifjict764 sdt zalo: 0358176187 Chuyên chỉnh sữa
 * mua bán source nro,...
 */
import clan.Clan;
import consts.ConstNpc;
import consts.ConstPlayer;
import java.util.ArrayList;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.ItemService;
import nro.services.NpcService;
import nro.services.RewardService;
import nro.services.Service;
import nro.services.TaskService;
import services.func.ChangeMapService;
import services.func.Input;
import shop.ShopService;
import utils.SkillUtil;
import utils.TimeUtil;
import utils.Util;

public class VuaVegeta extends Npc {

    public VuaVegeta(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                if (player.gender != ConstPlayer.XAYDA) {
                    NpcService.gI().createTutorial(player, tempId, avartar, "Con hãy về hành tinh của mình mà thể hiện");
                    return;
                }
                ArrayList<String> menu = new ArrayList<>();
                if (!player.canRewardBeNa && !player.canRewardLanCon) {
                    menu.add("Nhiệm vụ");
                    menu.add("Học\nKỹ năng");
                    Clan clan = player.clan;
                    if (clan != null) {
                        menu.add("Về khu\nvực bang");
                        if (clan.isLeader(player)) {
                            menu.add("Giải tán\nBang hội");
                        }
                    }
                } else {
                    if (player.canRewardBeNa) {
                        menu.add("Giao\nBé Na");
                    }
                    if (player.canRewardLanCon) {
                        menu.add("Giao\nLân Con");
                    }
                    if (player.canRewardTuanLoc) {
                        menu.add("Giao\nTuần Lộc");
                    }
                }
                String[] menus = menu.toArray(String[]::new);
                createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Chào con, ta rất vui khi gặp được con\nCon muốn làm gì nào ?", menus);
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            String chosen = player.iDMark.menuSelect[select];

            if (chosen.equals("Giao\nBé Na")) {
                RewardService.gI().rewardBeNa(player);
                return;
            }

            if (chosen.equals("Giao\nLân Con")) {
                RewardService.gI().rewardLanCon(player);
                return;
            }

            if (chosen.equals("Giao\nTuần Lộc")) {
                RewardService.gI().rewardTuanLoc(player);
                return;
            }
            if (player.iDMark.isBaseMenu()) {
                switch (select) {
                    case 0 ->
                        NpcService.gI().createTutorial(player, tempId, avartar, player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).name);
                    case 1 -> {
                        if (player.LearnSkill.Time != -1) {
                            var ngoc = 5;
                            var time = player.LearnSkill.Time - System.currentTimeMillis();
                            if (time / 600_000 >= 2) {
                                ngoc += time / 600_000;
                            }
                            String[] subName = ItemService.gI().getTemplate(player.LearnSkill.ItemTemplateSkillId).name.split("");
                            byte level = Byte.parseByte(subName[subName.length - 1]);
                            this.createOtherMenu(player, 12,
                                    "Con đang học kỹ năng\n" + SkillUtil.findSkillTemplate(SkillUtil.getTempSkillSkillByItemID(player.LearnSkill.ItemTemplateSkillId)).name + " cấp " + level + "\nThời gian còn lại " + TimeUtil.getTime(time),
                                    "Học\nCấp tốc\n" + ngoc + " ngọc", "Huỷ", "Bỏ qua");
                        } else {
                            ShopService.gI().opendShop(player, "QUY_LAO", false);
                        }
                    }
                    case 2 -> {
                        Clan clan = player.clan;
                        if (clan != null) {
                            ChangeMapService.gI().changeMapNonSpaceship(player, 184, Util.nextInt(100, 200), 432);
                        }
                    }
                    case 3 -> {
                        Clan clan = player.clan;
                        if (clan != null) {
                            if (clan.isLeader(player)) {
                                createOtherMenu(player, 3, "Con có chắc muốn giải tán bang hội không?", "Đồng ý", "Từ chối");
                            }
                        }
                    }
                }
            } else if (player.iDMark.getIndexMenu() == 3) {
                Clan clan = player.clan;
                if (clan != null) {
                    if (clan.isLeader(player)) {
                        if (select == 0) {
                            Input.gI().createFormGiaiTanBangHoi(player);
                        }
                    }
                }
            }
        }
    }
}
