/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package boss.boss_manifest.Rocket;
import boss.Boss;
import boss.BossID;
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
public class Meowth extends Boss {

    private long st;

    public Meowth() throws Exception {
        super(TRUNGTHU_EVENT, BossID.MEOWTH, BossesData.MEOWTH);
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        return super.injured(plAtt, 1, piercing, isMobAttack);
    }
    @Override
    public void reward(Player plKill) {
        plKill.effect.addPointTrumSanBoss();
        if (Util.isTrue(30, 100)) {
            ItemMap caiTrang = new ItemMap(this.zone, 1878, 1, this.location.x + Util.nextInt(-15, 15),
                    this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id);
            caiTrang.options.add(new ItemOption(174, 2025));
            caiTrang.options.add(new ItemOption(93, Util.nextInt(1, 5)));
            caiTrang.options.add(new ItemOption(30, 1));
            Service.gI().dropItemMap(this.zone, caiTrang);
        } else if (Util.isTrue(90, 100)) {
            ItemMap pikachu = new ItemMap(this.zone, 1482, 1, this.location.x + Util.nextInt(-15, 15),
                    this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id);
            pikachu.options.add(new ItemOption(50, 22));
            pikachu.options.add(new ItemOption(77, 22));
            pikachu.options.add(new ItemOption(103, 22));
            pikachu.options.add(new ItemOption(101, 36));
            pikachu.options.add(new ItemOption(160, 36));
            if (Util.isTrue(95, 100)) {
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
    public void joinMap() {
        super.joinMap();
        st = System.currentTimeMillis();
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
    public void doneChatS() {
        if (this.currentLevel == 1) {
            return;
        }
    }
}
