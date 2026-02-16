package boss_world;

import Top.weekly.ItemOption;
import boss.Boss;
import boss.BossID;
import boss.BossesData;
import item.Item;
import map.ItemMap;
import player.Player;
import services.EffectSkillService;
import services.Service;
import utils.Util;

public class boss_world_2 extends Boss {
    public boss_world_2() throws Exception {
        super(BossID.BOSS_THE_GIOI_2, BossesData.BOSS_WORLD_2);
    }

    @Override
    public void reward(Player plKill) {
        plKill.pointboss += 20;
        int random_coin_2k = Util.nextInt(1, 5);
        for (int i = 0; i < random_coin_2k; i++) {
            ItemMap item_map = new ItemMap(this.zone, 1788, 1, this.location.x + (i * 5),
                    this.zone.map.yPhysicInTop(this.location.x,
                            this.location.y - 24),

                     -1);
            item_map.options.add(new Item.ItemOption(30, Util.nextInt(1, 30)));
            item_map.source = "BossWorld2";
            Service.gI().dropItemMap(this.zone, item_map);
        }
        int random_coin_5k = Util.nextInt(1, 3);
        for (int i = 0; i < random_coin_5k; i++) {
            ItemMap item_map = new ItemMap(this.zone, 1789, 1, this.location.x + (i * 5),
                    this.zone.map.yPhysicInTop(this.location.x,
                            this.location.y - 24),
                   -1);
            item_map.options.add(new Item.ItemOption(30, Util.nextInt(1, 30)));
            Service.gI().dropItemMap(this.zone, item_map);
        }
    }

    @Override
    public void active() {
        super.active();
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }
            damage = this.nPoint.subDameInjureWithDeff(damage);
            if (!piercing && effectSkill != null && effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = 1;
            }
            if (damage > 30_000_000) {
                damage = 30_000_000;
            }
            this.nPoint.subHP(damage);
            if (isDie()) {
                this.setDie(plAtt);
                die(plAtt);
            }
            return damage;
        } else {
            return 0;
        }
    }
}