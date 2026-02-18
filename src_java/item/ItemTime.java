package item;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/ifjict764
 *  sdt zalo: 0358176187
 * Chuyên chỉnh sữa mua bán source nro,...
 */
import nro.player.NPoint;
import nro.player.Player;
import nro.services.Service;
import utils.Util;
import nro.services.ItemTimeService;

public class ItemTime {

    //id item text
    public static final byte DOANH_TRAI = 0;
    public static final byte BAN_DO_KHO_BAU = 1;
    public static final byte CON_DUONG_RAN_DOC = 2;
    public static final byte KHI_GAS_HUY_DIET = 3;
    public static final byte TIME_KEO_BUA_BAO = 4;
    public static final byte TEXT_NHAN_BUA_MIEN_PHI = 5;

    public static final int TIME_ITEM = 600000;
    public static final int TIME_OPEN_POWER = 8640000;
    public static final int TIME_MAY_DO = 1800000;
    public static final int TIME_MAY_DO2 = 1800000;
    public static final int TIME_EAT_MEAL = 600000;
    public static final int TIME_EAT_MEAL3 = 1200000;
    public static final int TIME_CMS = 3600000;
    public static final int TIME_DK = 1800000;
    public static final int TIME_RK = 3600000;
    public static final int TIME_NCD = 1800000;
    public static final int TIME_GIANGHOA = 30000;
    public static final int TIME_TD = 1800000;
    public static final int TIME_ITEM_X2_DT = 1800000;
    public static final int TIME_CAROT = 1800000;
    public static final int TIME_30P = 1800000;
    public static final int TIME_KILIS = 3600000;
    public static final int TIME_CO_BON_LA = 1800000;
    public static final int TIME_CHUOT_MAP = 1800000;
    public static final int TIME_VE_VANG = 7_200_000;

    private Player player;

    public boolean isUseBoHuyet;
    public boolean isUseBoKhi;
    public boolean isUseGiapXen;
    public boolean isUseCuongNo;
    public boolean isUseAnDanh;
    public boolean isUseBoHuyet2;
    public boolean isUseBoKhi2;
    public boolean isUseGiapXen2;
    public boolean isUseCuongNo2;
    public boolean isUseAnDanh2;

    public boolean isUseBuax2DeTu;

    public long lastTimeBoHuyet;
    public long lastTimeBoKhi;
    public long lastTimeGiapXen;
    public long lastTimeCuongNo;
    public long lastTimeAnDanh;

    public long lastTimeBuax2DeTu;

    public long lastTimeBoHuyet2;
    public long lastTimeBoKhi2;
    public long lastTimeGiapXen2;
    public long lastTimeCuongNo2;
    public long lastTimeAnDanh2;

    public boolean isUseMayDo;
    public long lastTimeUseMayDo;
    public boolean isUseMayDo2;
    public long lastTimeUseMayDo2;

    public boolean isOpenPower;
    public long lastTimeOpenPower;

    public boolean isUseTDLT;
    public long lastTimeUseTDLT;
    public int timeTDLT;

    public boolean isUseRX;
    public long lastTimeUseRX;
    public int timeRX;
    
    public boolean isUseRB;
    public long lastTimeUseRB;
    public int timeRB;

    public boolean isUseCMS;
    public long lastTimeUseCMS;
    public long timeLengthKilis;

    public boolean isUseNCD;
    public long lastTimeUseNCD;

    public boolean isUseGTPT;
    public long lastTimeUseGTPT;

    public boolean isUseDK;
    public long lastTimeUseDK;

    public boolean isEatMeal;
    public long lastTimeEatMeal;
    public int iconMeal;

    public boolean isEatMeal2;
    public long lastTimeEatMeal2;

    public boolean isCoBonLa;
    public long lastTimeCoBonLa;
    
    public boolean ischuotmap;
    public long lastTimechuotmap;

    public boolean isEatMeal3;
    public long lastTimeEatMeal3;

    public int iconMeal2;
    public int iconMeal3;
    public long lastTimeKhauTrang;
    public boolean isUseKhauTrang;
    public long lastTimevevang;
    public boolean isUsevevang;
    public long totalTimeVeVang;
    public long lastTimeLoX2;
    public boolean isUseLoX2;
    public long lastTimeLoX5;
    public boolean isUseLoX5;
    public long lastTimeLoX7;
    public boolean isUseLoX7;
    public long lastTimeLoX10;
    public boolean isUseLoX10;
    public long lastTimeLoX15;
    public boolean isUseLoX15;
    public boolean checkLoXTNSM;

    public boolean ispho1;
    public boolean ispho2;
    public boolean ispho3;
    public boolean isBanhTrungThu1Trung;
    public boolean isBanhTrungThu2Trung;
    public boolean isBanhTrungThuDb;
    public boolean isBanhTrungHop;

    public boolean isUseKilis;
    public long lastTimeUseKilis;

    public boolean isBoXuong;
    public long lastTimeBoXuong;
    public boolean isDoiNhi;
    public long lastTimeDoiNhi;
    public boolean isMaTroi;
    public long lastTimeMaTroi;
    public boolean isBiMa;
    public long lastTimeBiMa;
    
    public boolean iscarot;
    public long lastTimecarot;
    public boolean iconcarot;

    public long lastTimeBanhTrungThu1Trung;
    public long lastTimepho1;
    public long lastTimepho2;
    public long lastTimepho3;
    public long lastTimeBanhTrungThu2Trung;
    public long lastTimeBanhTrungThuDb;
    public long lastTimeBanhTrungThuHop;

    public ItemTime(Player player) {
        this.player = player;
    }

    public void update() {
        if (isUseKilis) {
            if (Util.canDoWithTime(lastTimeUseKilis, TIME_KILIS)) {
                isUseKilis = false;
            }
        }
        if (isUseLoX2) {
            if (Util.canDoWithTime(lastTimeLoX2, TIME_30P)) {
                isUseLoX2 = false;
                checkLoXTNSM = true;
                Service.gI().point(player);
            }
        }
        if (isUseLoX5) {
            if (Util.canDoWithTime(lastTimeLoX5, TIME_30P)) {
                isUseLoX5 = false;
                checkLoXTNSM = true;
                Service.gI().point(player);
            }
        }
        if (isUseLoX7) {
            if (Util.canDoWithTime(lastTimeLoX7, TIME_30P)) {
                isUseLoX7 = false;
                checkLoXTNSM = true;
                Service.gI().point(player);
            }
        }
        if (isUseLoX10) {
            if (Util.canDoWithTime(lastTimeLoX10, TIME_30P)) {
                isUseLoX10 = false;
                checkLoXTNSM = true;
                Service.gI().point(player);
            }
        }
        if (isUseLoX15) {
            if (Util.canDoWithTime(lastTimeLoX15, TIME_30P)) {
                isUseLoX15 = false;
                checkLoXTNSM = true;
                Service.gI().point(player);
            }
        }
        if (isCoBonLa) {
            if (Util.canDoWithTime(lastTimeCoBonLa, TIME_CO_BON_LA)) {
                isCoBonLa = false;
            }
        }
        if (ischuotmap) {
            if (Util.canDoWithTime(lastTimechuotmap, TIME_CHUOT_MAP)) {
                ischuotmap = false;
            }
        }
        if (isUseKhauTrang) {
            if (Util.canDoWithTime(lastTimeKhauTrang, TIME_30P)) {
                isUseKhauTrang = false;
                Service.gI().point(player);
            }
        }
        if (isUsevevang) {
            if (Util.canDoWithTime(lastTimevevang, TIME_VE_VANG)) {
                isUsevevang = false;
                Service.gI().point(player);
            }
        }
        if (isUseBuax2DeTu) {
            if (Util.canDoWithTime(lastTimeBuax2DeTu, TIME_30P)) {
                isUseBuax2DeTu = false;
                Service.gI().point(player);
            }
        }
        if (isEatMeal) {
            if (Util.canDoWithTime(lastTimeEatMeal, TIME_EAT_MEAL)) {
                isEatMeal = false;
                Service.gI().point(player);
            }
        }
        if (isEatMeal2) {
            if (Util.canDoWithTime(lastTimeEatMeal2, TIME_EAT_MEAL)) {
                isEatMeal2 = false;
                Service.gI().point(player);
            }
        }
        if (isEatMeal3) {
            if (Util.canDoWithTime(lastTimeEatMeal3, TIME_EAT_MEAL3)) {
                isEatMeal3 = false;
                Service.gI().point(player);
            }
        }
        if (isUseBoHuyet) {
            if (Util.canDoWithTime(lastTimeBoHuyet, TIME_ITEM)) {
                isUseBoHuyet = false;
                Service.gI().point(player);
            }
        }

        if (isUseBoKhi) {
            if (Util.canDoWithTime(lastTimeBoKhi, TIME_ITEM)) {
                isUseBoKhi = false;
                Service.gI().point(player);
            }
        }

        if (isUseGiapXen) {
            if (Util.canDoWithTime(lastTimeGiapXen, TIME_ITEM)) {
                isUseGiapXen = false;
            }
        }
        if (isUseCuongNo) {
            if (Util.canDoWithTime(lastTimeCuongNo, TIME_ITEM)) {
                isUseCuongNo = false;
                Service.gI().point(player);
            }
        }
        if (isUseAnDanh) {
            if (Util.canDoWithTime(lastTimeAnDanh, TIME_ITEM)) {
                isUseAnDanh = false;
            }
        }

        if (isUseBoHuyet2) {
            if (Util.canDoWithTime(lastTimeBoHuyet2, TIME_ITEM)) {
                isUseBoHuyet2 = false;
                Service.gI().point(player);
            }
        }

        if (isUseBoKhi2) {
            if (Util.canDoWithTime(lastTimeBoKhi2, TIME_ITEM)) {
                isUseBoKhi2 = false;
                Service.gI().point(player);
            }
        }
        if (isUseGiapXen2) {
            if (Util.canDoWithTime(lastTimeGiapXen2, TIME_ITEM)) {
                isUseGiapXen2 = false;
            }
        }
        if (isUseCuongNo2) {
            if (Util.canDoWithTime(lastTimeCuongNo2, TIME_ITEM)) {
                isUseCuongNo2 = false;
                Service.gI().point(player);
            }
        }
        if (isUseAnDanh2) {
            if (Util.canDoWithTime(lastTimeAnDanh2, TIME_ITEM)) {
                isUseAnDanh2 = false;
            }
        }
        if (isUseCMS) {
            if (Util.canDoWithTime(lastTimeUseCMS, TIME_CMS)) {
                isUseCMS = false;
            }
        }
        if (isUseGTPT) {
            if (Util.canDoWithTime(lastTimeUseGTPT, TIME_ITEM)) {
                isUseGTPT = false;
            }
        }
        if (isUseDK) {
            if (Util.canDoWithTime(lastTimeUseDK, TIME_DK)) {
                isUseDK = false;
            }
        }
        if (isOpenPower) {
            if (Util.canDoWithTime(lastTimeOpenPower, TIME_OPEN_POWER)) {
                player.nPoint.limitPower++;
                if (player.nPoint.limitPower > NPoint.MAX_LIMIT) {
                    player.nPoint.limitPower = NPoint.MAX_LIMIT;
                }
                player.nPoint.initPowerLimit();
                Service.gI().sendThongBao(player, "Giới hạn sức mạnh của bạn đã được tăng lên 1 bậc");
                isOpenPower = false;
            }
        }
        if (isUseMayDo) {
            if (Util.canDoWithTime(lastTimeUseMayDo, TIME_MAY_DO)) {
                isUseMayDo = false;
            }
        }
        if (isUseMayDo2) {
            if (Util.canDoWithTime(lastTimeUseMayDo2, TIME_MAY_DO2)) {
                isUseMayDo2 = false;
            }
        }
        if (isUseTDLT) {
            if (Util.canDoWithTime(lastTimeUseTDLT, timeTDLT)) {
                this.isUseTDLT = false;
                ItemTimeService.gI().sendCanAutoPlay(this.player);
            }
        }
        if (isUseRX) {
            if (Util.canDoWithTime(lastTimeUseRX, timeRX)) {
                isUseRX = false;
            }
        }
        if (isUseRB) {
            if (Util.canDoWithTime(lastTimeUseRB, timeRB)) {
                isUseRB = false;
            }
        }
        if (isBanhTrungThu1Trung) {
            if (Util.canDoWithTime(lastTimeBanhTrungThu1Trung, TIME_30P)) {
                isBanhTrungThu1Trung = false;
            }
        }
        if (isBanhTrungThu2Trung) {
            if (Util.canDoWithTime(lastTimeBanhTrungThu2Trung, TIME_30P)) {
                isBanhTrungThu2Trung = false;
            }
        }
        if (isBanhTrungThuDb) {
            if (Util.canDoWithTime(lastTimeBanhTrungThuDb, TIME_30P)) {
                isBanhTrungThuDb = false;
            }
        }
        if (isBanhTrungHop) {
            if (Util.canDoWithTime(lastTimeBanhTrungThuHop, TIME_30P)) {
                isBanhTrungHop = false;
            }
        }
        if (isMaTroi) {
            if (Util.canDoWithTime(lastTimeMaTroi, TIME_ITEM_X2_DT)) {
                isMaTroi = false;
            }
        }
        if (isDoiNhi) {
            if (Util.canDoWithTime(lastTimeDoiNhi, TIME_ITEM_X2_DT)) {
                isDoiNhi = false;
            }
        }
        if (isBoXuong) {
            if (Util.canDoWithTime(lastTimeBoXuong, TIME_ITEM_X2_DT)) {
                isBoXuong = false;
            }
        }
        if (isBiMa) {
            if (Util.canDoWithTime(lastTimeBiMa, TIME_ITEM_X2_DT)) {
                isBiMa = false;
            }
        }
        if (iscarot) {
            if (Util.canDoWithTime(lastTimecarot, TIME_CAROT)) {
                iscarot = false;
            }
        }
        if (ispho1) {
            if (Util.canDoWithTime(lastTimepho1, TIME_EAT_MEAL3)) {
                ispho1 = false;
            }
        }
        if (ispho2) {
            if (Util.canDoWithTime(lastTimepho2, TIME_EAT_MEAL3)) {
                ispho2 = false;
            }
        }
        if (ispho3) {
            if (Util.canDoWithTime(lastTimepho3, TIME_EAT_MEAL3)) {
                ispho3 = false;
            }
        }
    }

    public void dispose() {
        this.player = null;
    }
}
