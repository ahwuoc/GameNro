/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package mob.bigboss_manifest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import mob.BigBoss;
import mob.Mob;
import nro.player.Player;
import nro.server.Manager;
import nro.services.Service;

/**
 *
 * @author barcoll
 */
public class MayDoSucManh extends BigBoss {

    private final Map<Long, Long> damageMap = new HashMap<>();
    private final Map<Long, Integer> milestoneMap = new HashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public MayDoSucManh(Mob mob) {
        super(mob);
        startHealthRegeneration();
    }

    @Override
    public void setDie() {
        super.setDie();
        damageMap.clear();
        milestoneMap.clear();
    }

    public void moveTo(int x, int y) {
        this.location.x = x;
        this.location.y = y;
    }

    @Override
    public void injured(Player plAtt, long damage, boolean dieWhenHpFull) {
        if (isDie() || plAtt == null || damage <= 0) {
            return;
        }

        long hpBefore = this.point.hp;
        super.injured(plAtt, damage, dieWhenHpFull);
        long hpAfter = Math.max(this.point.hp, 0);
        long realDamage = hpBefore - hpAfter;

        if (realDamage > 0) {
            updateMilestone(plAtt, realDamage);
        }
    }

    public void updateMilestone(Player pl, long realDamage) {
        if (pl == null) {
            return;
        }

        long playerId = pl.id;

        long totalDamage = damageMap.getOrDefault(playerId, 0L) + realDamage;
        damageMap.put(playerId, totalDamage);

        int prevMilestone = milestoneMap.getOrDefault(playerId, 0);
        int currentMilestone = (int) (totalDamage / 10_000_000L);

        if (currentMilestone > prevMilestone) {
            int milestonesPassed = currentMilestone - prevMilestone;
            int points = milestonesPassed * 10;

            pl.point_maydam += points;
            milestoneMap.put(playerId, currentMilestone);

            if (!Manager.isTopMaydamChanged) {
                Manager.isTopMaydamChanged = true;
            }

            Service.gI().sendThongBao(pl, "Bạn đã nhận được " + points + " điểm Máy Đấm!");
            Service.gI().updatePlayerPointMayDam(pl);
        }

        pl.total_damage_maydam += realDamage;
        Service.gI().updatePlayerTotalDamage(pl);
    }

    private void startHealthRegeneration() {
        scheduler.scheduleAtFixedRate(() -> {
            if (!isDie()) {
                this.point.hp = this.point.maxHp;
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
}
