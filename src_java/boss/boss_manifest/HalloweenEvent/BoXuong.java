package boss.boss_manifest.HalloweenEvent;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/ifjict764
 *  sdt zalo: 0358176187
 *  Chuyên chỉnh sửa - mua bán source NRO,...
 */
import boss.*;
import static boss.BossType.HALLOWEEN_EVENT;
import consts.ConstPlayer;
import map.ItemMap;
import item.Item;
import nro.player.Player;
import nro.services.*;
import utils.SkillUtil;
import utils.Util;

public class BoXuong extends Boss {

    public BoXuong() throws Exception {
        super(HALLOWEEN_EVENT, BossID.BOXUONG, true, true, BossesData.BO_XUONG);
    }

    @Override
    public void reward(Player plKill) {
        try {
            // 🧺 Kiểm tra người chơi có giỏ đựng (ID 1348) không
            Item gioDung = InventoryService.gI().findItemBag(plKill, 1348);

            if (gioDung != null && gioDung.quantity > 0) {
                // ✅ Có giỏ đựng → cho phép nhận item 585
                ItemMap it = new ItemMap(this.zone, 585, 1, this.location.x,
                        this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id);
                Service.gI().dropItemMap(this.zone, it);

                // 🔻 Trừ 1 giỏ đựng
                InventoryService.gI().subQuantityItemsBag(plKill, gioDung, 1);
                InventoryService.gI().sendItemBag(plKill);

                Service.gI().sendThongBao(plKill, "Bạn đã nhặt được vật phẩm bí ngô!");
            } else {
                // ❌ Không có giỏ đựng → không thể nhặt
                Service.gI().sendThongBao(plKill, "Bạn cần có Giỏ đựng ngọc bí để nhận vật phẩm!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(10, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }
            damage = this.nPoint.subDameInjureWithDeff(damage / 7);
            if (!piercing && effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = damage / 1;
            }
            if (damage > this.nPoint.hpMax / 50) {
                damage = this.nPoint.hpMax / 50;
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

    @Override
    public void attack() {
        if (Util.canDoWithTime(this.lastTimeAttack, 500) && this.typePk == ConstPlayer.PK_ALL) {
            this.lastTimeAttack = System.currentTimeMillis();
            try {
                Player pl = getPlayerAttack();
                if (pl == null || pl.isDie()) {
                    return;
                }

                this.nPoint.dame = pl.nPoint.hpMax / Util.nextInt(30, 50);
                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, this.playerSkill.skills.size() - 1));

                if (Util.getDistance(this, pl) <= this.getRangeCanAttackWithSkillSelect()) {

                    if (Util.isTrue(5, 20)) {
                        if (SkillUtil.isUseSkillChuong(this)) {
                            this.moveTo(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(20, 200)),
                                    Util.nextInt(10) % 2 == 0 ? pl.location.y : pl.location.y - Util.nextInt(0, 70));
                        } else {
                            this.moveTo(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(10, 40)),
                                    Util.nextInt(10) % 2 == 0 ? pl.location.y : pl.location.y - Util.nextInt(0, 50));
                        }
                    }

                    SkillService.gI().useSkill(this, pl, null, -1, null);

                    if (pl.isPl() || pl.isPet) {
                        // 😷 Nếu đang đeo khẩu trang thì miễn nhiễm
                        if (pl.itemTime != null && pl.itemTime.isUseKhauTrang) {
                            Service.gI().chat(pl, "Khẩu trang đã giúp ta tránh khỏi lời nguyền Bộ Xương!");
                            return;
                        }

                        // 💀 Chỉ biến thành Bộ Xương nếu chưa ở dạng khác
                        if (!pl.itemTime.isBoXuong
                                && !pl.itemTime.isMaTroi
                                && !pl.itemTime.isDoiNhi
                                && !pl.itemTime.isBiMa) {

                            pl.itemTime.isBoXuong = true;
                            pl.itemTime.lastTimeBoXuong = System.currentTimeMillis();

                            Service.gI().chat(pl, "Ta đã hóa thành Bộ Xương!");
                            Service.gI().point(pl);
                            ItemTimeService.gI().sendAllItemTime(pl);
                            Service.gI().Send_Caitrang(pl);
                        }
                    }

                    checkPlayerDie(pl);
                } else {
                    if (Util.isTrue(1, 2)) {
                        this.moveToPlayer(pl);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void joinMap() {
        this.name = "Xương khô " + Util.nextInt(10, 100);
        super.joinMap();
        st = System.currentTimeMillis();
    }

    private long st;

    @Override
    public void autoLeaveMap() {
        if (Util.canDoWithTime(st, 900000)) {
            this.leaveMapNew();
        }
        if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
            st = System.currentTimeMillis();
        }
    }
}
