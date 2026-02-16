package boss.boss_manifest.Nappa;

import boss.Boss;
import boss.BossID;
import boss.BossStatus;
import boss.BossesData;
import java.util.Calendar;
import player.Pet;
import player.Player;
import services.Service;
import services.SkillService;
import services.TaskService;
import task.Pet.PetTaskService;
import utils.Util;

public class MapDauDinh extends Boss {

    private long st;

    public MapDauDinh() throws Exception {
        super(BossID.MAP_DAU_DINH, true, true, BossesData.MAP_DAU_DINH);
    }

    @Override
    public void joinMap() {
        super.joinMap();
        st = System.currentTimeMillis();
    }

    @Override
    public void reward(Player plKill) {
        TaskService.gI().checkDoneTaskKillBoss(plKill, this);
        if (plKill.isDeTu) {
            plKill = ((Pet) plKill).master;
            PetTaskService.checkDoneTaskKillBoss(plKill, this);
        }

    }

    @Override
    public long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            if (!plAtt.isDeTu && hour >= 18 && hour <= 19) {
                Service.gI().sendThongBaoOK(plAtt, "Đang khung h boss cho đệ Chỉ đệ tử mới có thể gây dame???");
                return 0;
            }

            this.nPoint.subHP(damage);
            if (isDie()) {
                this.setDie(plAtt);
                die(plAtt);
            }
            this.playerSkill.skillSelect = this.playerSkill.skills
                    .get(Util.nextInt(0, this.playerSkill.skills.size() - 1));
            SkillService.gI().useSkill(this, plAtt, null, -1, null);
            return damage;
        } else {
            return 0;
        }
    }

    @Override
    public void autoLeaveMap() {
        if (Util.canDoWithTime(st, 900000)) {
            this.changeStatus(BossStatus.LEAVE_MAP);
        }
        // if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
        // st = System.currentTimeMillis();
        // }
    }
}
