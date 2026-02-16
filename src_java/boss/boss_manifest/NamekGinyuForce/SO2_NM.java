package boss.boss_manifest.NamekGinyuForce;
import boss.Boss;
import boss.BossID;
import boss.BossStatus;
import boss.BossesData;
import map.ItemMap;
import player.Player;
import services.Service;
import utils.Util;

public class SO2_NM extends Boss {

    private long st;

    public SO2_NM() throws Exception {
        super(BossID.SO_2_NM, false, true, BossesData.SO_2_NM);
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
         plKill.pointboss+=3;
        Service.gI().dropItemMap(this.zone, new ItemMap(zone, 861, Util.nextInt(5, 10), this.location.x + Util.nextInt(-50, 50), this.location.y, plKill.id));
        if (this.currentLevel == 1) {
            return;
        }
        
         if (Util.isTrue(1, 2)) {
            ItemMap it_coin = new ItemMap(this.zone, 1788, 1, this.location.x + 5,
                    this.zone.map.yPhysicInTop(this.location.x, this.location.y), plKill.id);
            it_coin.source = "SO2_NM";
            Service.gI().dropItemMap(this.zone, it_coin);
        }
    }

    @Override
    protected void notifyJoinMap() {
        if (this.currentLevel == 1) {
            return;
        }
        super.notifyJoinMap();
    }

    @Override
    public void doneChatS() {
        this.changeStatus(BossStatus.AFK);
    }

    @Override
    public void autoLeaveMap() {
        if (Util.canDoWithTime(st, 600000)) {
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
            if (boss.id == BossID.SO_1_NM && !boss.isDie()) {
                return;
            }
        }
        this.parentBoss.changeStatus(BossStatus.ACTIVE);
    }

}
