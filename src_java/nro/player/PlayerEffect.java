package nro.player;

import consts.ConstTaskBadges;
import lombok.Getter;
import lombok.Setter;
import task.Badges.BadgesTaskService;

/**
 * @author Heat 😳
 */
@Setter
@Getter
public class PlayerEffect {

    public static int baseDaiGiaMoiNhu = 1000;// Bán 1 ngàn thỏi vàng 1 ngày // đại gia
    public static int baseTrumUocRong = 10; // Ước rồng 1 sao ngày 5 lần ->OK // Youtube
    public static int baseTrumSanBoss = 30; // Săn cumber/ cooler, chill 30 con ->OK -> Memory
    public static int baseThanhDapDo = 1000; // Đập 1000 lần
    public static int baseNongDanChamChi = 20;// Hoàn thành 20 nhiệm vụ địa ngục tại bò mộng -> DH trùm cuối
    public static int baseOngThanVeChai = 100; // Nhặt 100 món đồ trong 1 ngày -> Ok -> DH trùm ve chai
    public static int baseBiMocSachTui = 1; // Nhận khi mua vip 3 ->Ok -> Đại thần
    public static int basePhanCung = 100;// Giết 100 người trong ngày -> Ok -> Phan cứng

    private int pointDaiGiaMoiNhu;
    private int pointTrumUocRong;
    private int pointTrumSanBoss;
    private int pointThanhDapDo;
    private int pointNongDanChamChi;
    private int pointOngThanVeChai;
    private int pointBiMocSachTui;
    private int pointPhanCung;
    private Player player;

    public PlayerEffect(Player player) {
        this.player = player;
    }

    public void addPointDaiGiamMoiNhu(int value) {
        this.pointDaiGiaMoiNhu += value;
    }

    public void addPointTrumUocRong() {
        this.pointTrumUocRong++;
        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.TRUM_UOC_RONG, 1);
    }

    public void addPointTrumSanBoss() {
        this.pointTrumSanBoss++;
        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.TRUM_SAN_BOSS, 1);
    }

    public void addPointThanhDapDo() {
        this.pointThanhDapDo++;
        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.THANH_DAP_DO_7, 1);
    }

    public void addPointNongDanChamChi() {
        this.pointNongDanChamChi++;
        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.NONG_DAN_CHAM_CHI, 1);
    }

    public void addPointOngThanVeChai() {
        this.pointOngThanVeChai++;
        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.ONG_THAN_VE_CHAI, 1);
    }

    public void addPointBiMocSachTui() {
        this.pointBiMocSachTui++;
    }

    public void addPointPhanCung() {
        this.pointPhanCung++;
    }

    public void subPointEff(int point, int value) {
        point -= value;
    }

    public void subPointEffectDaiGia(int point) {
        pointDaiGiaMoiNhu -= point;
    }

    public void subPointEffectSanBoss(int point) {
        pointTrumSanBoss -= point;
    }

    public void subPointEffectUocRong(int point) {
        pointTrumUocRong -= point;
    }

    public void subPointEffectVeChai(int point) {
        pointOngThanVeChai -= point;
    }

    public void subPointEffectDapDo(int point) {
        pointThanhDapDo -= point;
    }

    public void subPointEffectChamChi(int point) {
        pointNongDanChamChi -= point;
    }

    public void subPointEffectSachTui(int point) {
        pointBiMocSachTui -= point;
    }

    public void subPointEffectPhanCung(int point) {
        pointPhanCung -= point;
    }

    public boolean isEff(int pointEff, int pointBase) {
        if (pointEff >= pointBase) {
            return true;
        }
        return false;
    }

}

