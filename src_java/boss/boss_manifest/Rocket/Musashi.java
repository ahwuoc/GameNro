/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package boss.boss_manifest.Rocket;

import boss.Boss;
import boss.BossID;
import boss.BossStatus;
import static boss.BossType.TRUNGTHU_EVENT;
import boss.BossesData;
import item.Item.ItemOption;
import map.ItemMap;
import nro.player.Player;
import nro.services.Service;
import utils.Util;

/**
 *
 * @author Administrator
 */
public class Musashi extends Boss {

    private long st;

    public Musashi() throws Exception {
        super(BossID.MUSASHI, BossesData.MUSASHI);
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        return super.injured(plAtt, 1, piercing, isMobAttack);
    }

    @Override
    public void moveTo(int x, int y) {
        if (this.currentLevel == 1) {
            return;
        }
        super.moveTo(x, y);
    }

    @Override
    public void reward(Player plKill) {
        plKill.effect.addPointTrumSanBoss();
        if (Util.isTrue(30, 100)) {
            ItemMap caiTrang = new ItemMap(this.zone, 1877, 1, this.location.x + Util.nextInt(-15, 15),
                    this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id);
            caiTrang.options.add(new ItemOption(50, 38));
            caiTrang.options.add(new ItemOption(14, 13));
            caiTrang.options.add(new ItemOption(93, Util.nextInt(1, 5)));
            caiTrang.options.add(new ItemOption(30, 0));
            Service.gI().dropItemMap(this.zone, caiTrang);
        } else if (Util.isTrue(30, 100)) {
            ItemMap pikachu = new ItemMap(this.zone, 1873, 1, this.location.x + Util.nextInt(-15, 15),
                    this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id);
            pikachu.options.add(new ItemOption(174, 2025));
            if (Util.isTrue(99, 100)) {
                pikachu.options.add(new ItemOption(93, Util.nextInt(1, 5)));
            }
            pikachu.options.add(new ItemOption(30, 1));
            Service.gI().dropItemMap(this.zone, pikachu);
        } else {
            ItemMap n = new ItemMap(this.zone, Util.nextInt(1099,1103), 1, this.location.x + Util.nextInt(-15, 15),
                    this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id);
            n.options.add(new ItemOption(30, 0));
            Service.gI().dropItemMap(this.zone, n);
        }
        plKill.bongmaster++;
    }

    @Override
    protected void notifyJoinMap() {
        if (this.currentLevel == 1) {
            return;
        }
        super.notifyJoinMap();
    }

    @Override
    public void autoLeaveMap() {
        if (Util.canDoWithTime(st, 900000)) {
            this.leaveMapNew();
        }
        if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
            st = System.currentTimeMillis();
        }
    }

    @Override
    public void joinMap() {
        super.joinMap();
        st = System.currentTimeMillis();
    }

    @Override
    public void doneChatE() {
        if (this.parentBoss == null || this.parentBoss.bossAppearTogether == null
                || this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel] == null) {
            return;
        }
        for (Boss boss : this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel]) {
            if ((boss.id == BossID.KOCHIRO) && !boss.isDie()) {
                return;
            }
        }
        this.parentBoss.changeStatus(BossStatus.ACTIVE);
    }

}
