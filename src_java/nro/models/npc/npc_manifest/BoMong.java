package nro.models.npc.npc_manifest;

/**
 *  Box ZALO:https://zalo.me/g/ifjict764
 *  sdt zalo: 0358176187
 * Chuyên chỉnh sữa mua bán source nro,...
 */

import consts.ConstNpc;
import consts.ConstTask;
import models.Achievement.AchievementService;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.Service;
import nro.services.TaskService;
import services.func.Input;

public class BoMong extends Npc {

    public BoMong(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                if (this.mapId == 47 || this.mapId == 84) {
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Ngươi muốn vip, có nhiều cách, nạp thẻ là nhanh nhất, còn không thì chịu khó cày hãy nghe lời thầy dạy cần cù bù siêng năng.",
                            "Nạp Ngọc",
                            "Nhiệm vụ\nhàng ngày", 
                            "Nhiệm vụ\nthành tích");
                }
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (this.mapId == 47 || this.mapId == 84) {
                if (player.iDMark.isBaseMenu()) {
                    switch (select) {
                        case 0 -> {
                            this.createOtherMenu(player, ConstNpc.MENU_BO_MONG,
                                        "Ngươi muốn thêm ngọc thì chịu khó làm vài nhiệm vụ sẽ được ngọc thưởng",
                                        "Hướng\ndẫn\nnạp thẻ", "Nhập\nGift Code");
                        }

                        case 1 -> {
                            if (player.playerTask.sideTask.template != null) {
                                String npcSay = "Nhiệm vụ hiện tại: " + player.playerTask.sideTask.getName() + " ("
                                        + player.playerTask.sideTask.getLevel() + ")"
                                        + "\nHiện tại đã hoàn thành: " + player.playerTask.sideTask.count + "/"
                                        + player.playerTask.sideTask.maxCount + " ("
                                        + player.playerTask.sideTask.getPercentProcess() + "%)\nSố nhiệm vụ còn lại trong ngày: "
                                        + player.playerTask.sideTask.leftTask + "/" + ConstTask.MAX_SIDE_TASK;
                                this.createOtherMenu(player, ConstNpc.MENU_OPTION_PAY_SIDE_TASK,
                                        npcSay, "Trả nhiệm\nvụ", "Hủy nhiệm\nvụ");
                            } else {
                                this.createOtherMenu(player, ConstNpc.MENU_OPTION_LEVEL_SIDE_TASK,
                                        "Tôi có vài nhiệm vụ theo cấp bậc, "
                                                + "sức cậu có thể làm được cái nào?",
                                        "Dễ", "Bình thường", "Khó", "Siêu khó", "Địa ngục", "Từ chối");
                            }
                        }
                        case 2 -> {
                            AchievementService.gI().openAchievementUI(player);
                        }

                    }
                } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_OPTION_LEVEL_SIDE_TASK) {
                    switch (select) {
                        case 0, 1, 2, 3, 4 -> TaskService.gI().changeSideTask(player, (byte) select);
                    }
                }  else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_BO_MONG) {
                    switch (select) {
                        case 0 -> {
                            Service.gI().sendThongBao(player,
                                    "Bạn có thể có ngọc từ ví điện tử, gift code (thẻ cào rốt)\n"
                                    + "10,000đ - 10 ngọc\n"
                                    + "20,000đ - 25 ngọc\n"
                                    + "30,000đ - 38 ngọc\n"
                                    + "50,000đ - 70 ngọc\n"
                                    + "100,000đ - 150 ngọc\n"
                                    + "200,000đ - 350 ngọc\n"
                                    + "300,000đ - 650 ngọc\n"
                                    + "500,000đ - 1100 ngọc\n"
                                    + "1,000,000đ - 2500 ngọc\n"
                                    + "Nếu có vấn đề về nạp thẻ, vui lòng liên hệ tại http://nro.com để được giải đáp thắc mắc."
                            );
                        }
                        case 1 ->
                            Input.gI().createFormGiftCode(player);
                    }
                } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_OPTION_PAY_SIDE_TASK) {
                    switch (select) {
                        case 0 ->
                            TaskService.gI().paySideTask(player);
                        case 1 -> TaskService.gI().removeSideTask(player);
                    }
                }
            }
        }
    }
}
