package boss.boss_manifest.HalloweenEvent;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/ifjict764
 *  sdt zalo: 0358176187
 * Chuyên chỉnh sữa mua bán source nro,...
 */

import boss.*;
import static boss.BossType.HALLOWEEN_EVENT;
import consts.ConstPlayer;
import item.Item;
import map.ItemMap;
import nro.player.Player;
import nro.services.EffectSkillService;
import nro.services.InventoryService;
import nro.services.ItemTimeService;
import nro.services.Service;
import nro.services.SkillService;
import utils.SkillUtil;
import utils.Util;

public class BiMa extends Boss {

    public BiMa() throws Exception {
        super(HALLOWEEN_EVENT, BossID.BIMA, true, true, BossesData.BI_MA);
    }

    @Override
    public void reward(Player plKill) {
        try {
            // Xác định loại vật phẩm rơi
            boolean isKeo = Util.isTrue(80, 100); // 80% là Kẹo, 20% là Bí ngô

            // Nếu là kẹo
            int itemId = isKeo ? 901 : 585;
            int soLuong = isKeo ? Util.nextInt(1, 5) : Util.nextInt(10, 20);

            // 🔥 Hiệu ứng rơi tự nhiên, tản ngẫu nhiên xung quanh vị trí boss
            for (int i = 0; i < soLuong; i++) {
                int x = this.location.x + Util.nextInt(-80, 80);
                int y = this.zone.map.yPhysicInTop(x, this.location.y - Util.nextInt(60, 120));

                // Tạo vật phẩm rơi
                ItemMap item = new ItemMap(
                        this.zone,
                        itemId, // ID vật phẩm (Kẹo hoặc Bí ngô)
                        1, // Mỗi vật phẩm rơi 1 cái
                        x,
                        y,
                        plKill.id
                );

                // Thả vật phẩm xuống bản đồ
                Service.gI().dropItemMap(this.zone, item);

                // Hiệu ứng sáng nhỏ mỗi khi rơi
                Service.gI().sendEffAllPlayer(plKill, (short) 13, 1, -1, 1);

                // Delay nhỏ giữa mỗi vật phẩm để tạo hiệu ứng "mưa rơi"
                Thread.sleep(100);
            }

            // 🔔 Gửi thông báo cho người chơi
            if (isKeo) {
                Service.gI().sendThongBao(plKill, "Bạn nhận được " + soLuong + " Kẹo bàn tay!");
                Service.gI().chat(plKill, "Mưa Kẹo bàn tay rơi xung quanh bạn!");
            } else {
                Service.gI().sendThongBao(plKill, "Bạn nhận được " + soLuong + " Bí ngô!");
                Service.gI().chat(plKill, "Mưa Bí ngô rơi khắp nơi!");
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
                        // ❌ Nếu đang đeo khẩu trang thì KHÔNG bị nhiễm
                        if (pl.itemTime != null && pl.itemTime.isUseKhauTrang) {
                            Service.gI().chat(pl, "Khẩu trang đã giúp ta tránh được lời nguyền!");
                            return;
                        }

                        // ✅ Chỉ biến thành BiMa nếu chưa bị nhiễm và không thuộc 3 loại khác
                        if (!pl.itemTime.isBiMa
                                && !pl.itemTime.isMaTroi
                                && !pl.itemTime.isBoXuong
                                && !pl.itemTime.isDoiNhi) {

                            pl.itemTime.isBiMa = true;
                            pl.itemTime.lastTimeBiMa = System.currentTimeMillis();

                            // Cập nhật ngoại hình, hiệu ứng và chỉ số
                            Service.gI().chat(pl, "Huhuhu... Ta đã bị ma ám!");
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
            }
        }
    }

    @Override
    public void joinMap() {
        this.name = "Bí ma " + Util.nextInt(10, 100);
        super.joinMap(); //To change body of generated methods, choose Tools | Templates.
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
