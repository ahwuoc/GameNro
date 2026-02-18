/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package nro.models.npc.npc_manifest;

import consts.ConstDailyGift;
import consts.ConstNpc;
import item.Item;
import nro.player.Player;

/**
 *
 * @author Administrator
 */
public class PanChy extends Rong1Sao {

    public PanChy(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (this.mapId == 184) {
                this.createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Bạn đang ở bang " + player.clan.name + "\n\n"
                        + "|5|Cấp độ bang: " + player.clan.level + "\n\n"
                        + "|3|Bang chủ: " + player.clan.getLeader().name + "\n"
                        + "Mỗi ngày điểm danh sẽ nhận: 1 Capsule bang\n\n"
                        + "|7|Tăng 20%TNSM Level " + player.clan.level + "/2 Để mở khoá\n\n"
                        + "|7|Tăng 1%SĐ,HP,KI Level " + player.clan.level + "/3 Để mở khoá\n\n"
                        + "|7|Shop Bang Hội Level " + player.clan.level + "/5 Để mở khoá\n\n"
                        + "|7|Tăng 5%SĐ,HP,KI Level " + player.clan.level + "/8 Để mở khoá\n\n"
                        + "|7|Nâng chỉ số Level " + player.clan.level + "/10 Để mở khoá",
                        "Điểm danh", "Nâng cấp\nBang hội", "Quyên góp\nBang hội");
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.iDMark.isBaseMenu()) {
                switch (select) {
                    case 0,1,2 -> {
                        this.npcChat(player, "Đang Update!");
                    }
                }
            }
        }
    }
}
