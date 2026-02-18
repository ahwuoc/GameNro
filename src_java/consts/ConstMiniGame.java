/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package consts;

import minigame.TX.TaiXiu;
import nro.player.Player;
import utils.Util;

/**
 *
 * @author hoquo
 */
public class ConstMiniGame {

    public static final byte MENU_CHINH = 0;
    public static final byte MENU_KEO_BUA_BAO = 1;
    public static final byte MENU_CON_SO_MAY_MAN_VANG = 2;
    public static final byte MENU_CON_SO_MAY_MAN_NGOC = 3;
    public static final byte MENU_CHON_AI_DAY = 4;
    public static final byte MENU_PLAY_KEO_BUA_BAO = 5;
    public static final byte MENU_LUCKY_NUMBER = 6;
    public static final byte MENU_PLAY_LUCKY_NUMBER_GOLD = 7;
    public static final byte MENU_PLAY_LUCKY_NUMBER_GEM = 8;
    public static final byte MENU_PLAY_DECISION_MAKER_GOLD = 9;
    public static final byte MENU_PLAY_DECISION_MAKER_RUBY = 10;
    public static final byte MENU_PLAY_DECISION_MAKER_GEM = 11;
    public static final byte MENU_WAIT_NEW_GAME = 12;
    //----------------------index TX------------------------------------------
    public static final int MENU_TAI_XIU = 13;
    public static final int MINIGAME_TAIXIU_CHAN = 14;
    public static final int MINIGAME_TAIXIU_LE = 15;
    public static final int MINIGAME_BAOTRI = 16;
    public static final int MINIGAME_TAIXIU_TUYCHON = 17;
    public static final int MINIGAME_TAIXIU_TAI = 18;
    public static final int MINIGAME_TAIXIU_XIU = 19;
    public static final byte TAI_XIU = 20;
    public static final byte TAI_XIU_TAI = 21;
    public static final byte TAI_XIU_XIU = 22;
    public static final byte TAI_XIU_EMPTY = 23;
    public static final byte TAI_XIU_HD = 24;

    public static String TextNpc(Player pl, byte type) {
        StringBuilder text = new StringBuilder();
        switch (type) {
            case TAI_XIU -> {
                if (!TaiXiu.baotri) {
                    text.append("\n|8|Trò chơi Tài Xỉu đang được diễn ra\n\n")
                            .append("|6|Thử vận may của bạn với trò chơi Tài Xỉu! Đặt cược và dự đoán đúng\n")
                            .append("kết quả, bạn sẽ được nhận thưởng lớn. Hãy tham gia ngay và\n")
                            .append(" cùng trải nghiệm sự hồi hộp, thú vị trong trò chơi này!\n\n")
                            .append("|7|(Điều kiện tham gia : Nhiệm vụ : Tới tiểu đội sát thủ)\n\n")
                            .append("|2|Đặt tối đa giới hạn Vàng hiện tại của bạn\n\n")
                            .append("|7| Lưu ý : Thoát game khi chốt Kết quả sẽ MẤT Tiền cược và Tiền thưởng");
                } else {
                    text.append("\n|7|- NHÀ CÁI ĐANG BẢO TRÌ -\n")
                            .append("|3|Kết quả vẫn sẽ được trao nếu bạn đã đặt cược\n\n")
                            .append("|7|Thời gian còn lại: ")
                            .append(Util.msToTimeNdung(TaiXiu.gI().lastTimeEnd + TaiXiu.TIME_END))
                            .append("\n\n|7|Hệ thống sắp bảo trì");
                }
            }
            case TAI_XIU_TAI -> {
                text.append("\n|7|- Mini Game -")
                        .append(!TaiXiu.gI().listKetQua.isEmpty()
                                ? "\n|3|Kết quả kì trước:  " + TaiXiu.gI().getKetQua()
                                : "")
                        .append(pl.isAdmin()
                                ? "\n\n|7|Admin [" + pl.name + "] Đã can thiệp kết quả: " + TaiXiu.gI().getCau()
                                : "")
                        .append("\n\n|2|Cửa Tài (")
                        .append(TaiXiu.gI().playersTai.size())
                        .append(" người - bạn đặt ")
                        .append(Util.numberToMoney(pl.goldTai))
                        .append(" Vàng - tổng ")
                        .append(Util.numberToMoney(TaiXiu.gI().totalTai))
                        .append(" Vàng)\n")
                        .append("Cửa Xỉu  (")
                        .append(TaiXiu.gI().playersXiu.size())
                        .append(" người - bạn đặt ")
                        .append(Util.numberToMoney(pl.goldXiu))
                        .append(" Vàng - tổng ")
                        .append(Util.numberToMoney(TaiXiu.gI().totalXiu))
                        .append(" Vàng)\n\n|7|Thời gian còn lại: ")
                        .append(Util.msToTimeNdung(TaiXiu.gI().lastTimeEnd + TaiXiu.TIME_END))
                        .append("\n|3|Bạn đã cược Tài : ")
                        .append(Util.numberToMoney(pl.goldTai))
                        .append(" Vàng");
            }
            case TAI_XIU_XIU -> {
                text.append("\n|7|- Mini Game -")
                        .append(!TaiXiu.gI().listKetQua.isEmpty()
                                ? "\n|3|Kết quả kì trước:  " + TaiXiu.gI().getKetQua()
                                : "")
                        .append(pl.isAdmin()
                                ? "\n\n|7|Admin [" + pl.name + "] Đã can thiệp kết quả: " + TaiXiu.gI().getCau()
                                : "")
                        .append("\n\n|2|Cửa Tài (")
                        .append(TaiXiu.gI().playersTai.size())
                        .append(" người - bạn đặt ")
                        .append(Util.numberToMoney(pl.goldTai))
                        .append(" Vàng - tổng ")
                        .append(Util.numberToMoney(TaiXiu.gI().totalTai))
                        .append(" Vàng)\n")
                        .append("Cửa Xỉu (")
                        .append(TaiXiu.gI().playersXiu.size())
                        .append(" người - bạn đặt ")
                        .append(Util.numberToMoney(pl.goldXiu))
                        .append(" Vàng - tổng ")
                        .append(Util.numberToMoney(TaiXiu.gI().totalXiu))
                        .append(" Vàng)\n\n|7|Thời gian còn lại: ")
                        .append(Util.msToTimeNdung(TaiXiu.gI().lastTimeEnd + TaiXiu.TIME_END))
                        .append("\n|3|Bạn đã cược Xỉu : ")
                        .append(Util.numberToMoney(pl.goldXiu))
                        .append(" Vàng");
            }
            case TAI_XIU_EMPTY -> {
                text.append("\n|7|- Mini Game -")
                        .append(!TaiXiu.gI().listKetQua.isEmpty()
                                ? "\n|3|Kết quả kì trước:  " + TaiXiu.gI().getKetQua()
                                : "")
                        .append(pl.isAdmin()
                                ? "\n\n|7|Admin [" + pl.name + "] Đã can thiệp kết quả: " + TaiXiu.gI().getCau()
                                : "")
                        .append("\n\n|2|Cửa Tài (")
                        .append(TaiXiu.gI().playersTai.size())
                        .append(" người - bạn đặt ")
                        .append(Util.numberToMoney(pl.goldTai))
                        .append(" Vàng - tổng ")
                        .append(Util.numberToMoney(TaiXiu.gI().totalTai))
                        .append(" Vàng)\n")
                        .append("Cửa Xỉu (")
                        .append(TaiXiu.gI().playersXiu.size())
                        .append(" người - bạn đặt ")
                        .append(Util.numberToMoney(pl.goldXiu))
                        .append(" Vàng - tổng ")
                        .append(Util.numberToMoney(TaiXiu.gI().totalXiu))
                        .append(" Vàng)\n\n|7|Thời gian còn lại: ")
                        .append(Util.msToTimeNdung(TaiXiu.gI().lastTimeEnd + TaiXiu.TIME_END));
            }
            case TAI_XIU_HD -> {
                text.append("|5|Có 2 nhà cái Tài và Xĩu, bạn chỉ được chọn 1 nhà để tham gia\n\n")
                        .append("|6|Sau khi kết thúc thời gian đặt cược. Hệ thống sẽ tung xí ngầu để biết kết quả Tài Xỉu\n\n")
                        .append("Nếu Tổng số 3 con xí ngầu  bé hơn 11 là XỈU\n")
                        .append("Nếu Tổng số 3 con xí ngầu lớn hơn 11 TÀI\n")
                        .append("Nếu 3 Xí ngầu cùng 1 số là TAM HOA (Nhà cái lụm hết)\n\n")
                        .append("|7|Lưu ý: Số Vàng nhận được khi thắng là 180%. Trong quá trình diễn ra khi đặt cược nếu thoát game trong lúc phát thưởng phần quà sẽ bị HỦY");
            }
        }
        return text.toString();
    }
}
