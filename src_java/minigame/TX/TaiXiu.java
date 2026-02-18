package minigame.TX;

import java.util.ArrayList;
import java.util.List;
import nro.player.Player;
import nro.services.Service;
import utils.Util;

/**
 *
 * @author by Ts
 */
public class TaiXiu implements Runnable {

    public static boolean chinhCau = false;
    public static boolean baotri = false;
    public long totalTai;
    public long totalXiu;
    protected int a, b, c, total;
    protected boolean ketquaTai, ketquaXiu;
    public long lastTimeEnd;
    public static final int TIME_END = 60000;
    public List<Player> playersTai = new ArrayList<>();
    public List<Player> playersXiu = new ArrayList<>();
    public List<String> listKetQua = new ArrayList<>();

    private static TaiXiu I;

    public static TaiXiu gI() {
        if (I == null) {
            I = new TaiXiu();
        }
        return I;
    }

    public StringBuilder getKetQua() {
        StringBuilder kq = new StringBuilder();
        if (!listKetQua.isEmpty()) {
            listKetQua.forEach(text -> kq.append("\n|5|").append(text));
        }
        return kq;
    }

    public String getCau() {
        total = a + b + c;
        return a + " : " + b + " : " + c + " Tổng: " + total + " (" + (total == 3 || total == 18 ? "Tam Hoa" : total < 11 ? "Xỉu" : "Tài") + ")";
    }

    private void setKetQua() {
        total = a + b + c;
        if (total >= 4 && total <= 10) {
            ketquaXiu = true;
        } else if (total >= 11 && total <= 17) {
            ketquaTai = true;
        }
        if (listKetQua.size() > 5) {
            listKetQua.clear();
        }
        listKetQua.add(a + " : " + b + " : " + c + " Tổng: " + total + " (" + (total == 3 || total == 18 ? "Tam Hoa" : total < 11 ? "Xỉu" : "Tài") + ")");
    }

    public void addXiu(Player pl, long gold) {
        if (gold != 0 && pl.inventory.gold >= gold) {
            pl.inventory.gold -= gold;
            Service.gI().sendMoney(pl);
            pl.goldXiu += gold;
            totalXiu += gold;
            if (!playersXiu.contains(pl)) {
                playersXiu.add(pl);
            }
        }
    }

    public void addTai(Player pl, long gold) {
        if (gold != 0 && pl.inventory.gold >= gold) {
            pl.inventory.gold -= gold;
            Service.gI().sendMoney(pl);
            pl.goldTai += gold;
            totalTai += gold;
            if (!playersTai.contains(pl)) {
                playersTai.add(pl);
            }
        }
    }

    public void chinhCau(byte type) {
        chinhCau = true;
        switch (type) {
            case 0 -> {
                a = Util.nextInt(3, 6);
                b = Util.nextInt(4, 6);
                c = Util.nextInt(4, 6);
            }
            case 1 -> {
                a = Util.nextInt(1, 3);
                b = Util.nextInt(1, 3);
                c = Util.nextInt(1, 4);
            }
            case 2 -> {
                a = 6;
                b = 6;
                c = 6;
            }
        }
    }

    @Override
    public void run() {
        while (true) {
            if (Util.canDoWithTime(lastTimeEnd, TIME_END - 1000)) {
                try {
                    if (chinhCau == false) {
                        if (totalXiu > totalTai) {
                            if (playersXiu.size() > playersTai.size()) {
                                if (Util.isTrue(90, 100)) {
                                    a = Util.nextInt(3, 6);
                                    b = Util.nextInt(4, 6);
                                    c = Util.nextInt(4, 6);
                                } else {
                                    a = Util.nextInt(1, 6);
                                    b = Util.nextInt(1, 6);
                                    c = Util.nextInt(1, 6);
                                }
                            } else if (Util.isTrue(10, 100)) {
                                a = Util.nextInt(1, 3);
                                b = Util.nextInt(1, 3);
                                c = Util.nextInt(1, 4);
                            } else {
                                a = Util.nextInt(3, 6);
                                b = Util.nextInt(4, 6);
                                c = Util.nextInt(4, 6);
                            }
                        } else if (totalTai > totalXiu) {
                            if (playersTai.size() > playersXiu.size()) {
                                if (Util.isTrue(90, 100)) {
                                    a = Util.nextInt(1, 3);
                                    b = Util.nextInt(1, 3);
                                    c = Util.nextInt(1, 4);
                                } else {
                                    a = Util.nextInt(1, 6);
                                    b = Util.nextInt(1, 6);
                                    c = Util.nextInt(1, 6);
                                }
                            } else if (Util.isTrue(10, 100)) {
                                a = Util.nextInt(3, 6);
                                b = Util.nextInt(4, 6);
                                c = Util.nextInt(4, 6);
                            } else {
                                a = Util.nextInt(1, 3);
                                b = Util.nextInt(1, 3);
                                c = Util.nextInt(1, 4);
                            }
                        } else {
                            if (playersXiu.size() > playersTai.size()) {
                                if (Util.isTrue(90, 100)) {
                                    a = Util.nextInt(3, 6);
                                    b = Util.nextInt(4, 6);
                                    c = Util.nextInt(4, 6);
                                } else {
                                    a = Util.nextInt(1, 6);
                                    b = Util.nextInt(1, 6);
                                    c = Util.nextInt(1, 6);
                                }
                            } else if (playersTai.size() > playersXiu.size()) {
                                if (Util.isTrue(90, 100)) {
                                    a = Util.nextInt(1, 3);
                                    b = Util.nextInt(1, 3);
                                    c = Util.nextInt(1, 4);
                                } else {
                                    a = Util.nextInt(1, 6);
                                    b = Util.nextInt(1, 6);
                                    c = Util.nextInt(1, 6);
                                }
                            } else if (Util.isTrue(10, 100)) {
                                a = 6;
                                b = 6;
                                c = 6;
                            } else {
                                a = Util.nextInt(1, 6);
                                b = Util.nextInt(1, 6);
                                c = Util.nextInt(1, 6);
                            }
                        }
                    }
                } finally {
                    lastTimeEnd = System.currentTimeMillis();
                    chinhCau = false;
                    setKetQua();
                    if (ketquaXiu) {
                        for (Player pl : playersXiu) {
                            long gold = pl.goldXiu * 2 * 90 / 100;
                            pl.goldXiu = 0;
                            Service.gI().sendThongBaoFromAdmin(pl, "|8|[ Nhà Cái Tài Xỉu ]\n|3|Kết quả xóc ra là:\n|2|" + a + " : " + b + " : " + c + "\n|5|Tổng là: " + total + " (XỈU)\n\n|7|Bạn Đã Chiến Thắng\n|1|Số Tiền Nhận Được Là\n|3|" + Util.numberToMoney(gold) + " Vàng");
                            try {
                                pl.inventory.gold += gold;
                            } catch (Exception e) {
                            }
                            Service.gI().sendMoney(pl);
                        }
                        for (Player pl : playersTai) {
                            Service.gI().sendThongBaoFromAdmin(pl, "|8|[ Nhà Cái Tài Xỉu ]\n|3|Kết quả xóc ra là:\n|2|" + a + " : " + b + " : " + c + "\n|5|Tổng là: " + total + " (XỈU)\n\n|7|Bạn Đã Thua\n|1|Số Tiền Nhà Cái Ăn Được Là\n|3|" + Util.numberToMoney(pl.goldTai) + " Vàng");
                            pl.goldTai = 0;
                        }
                    } else if (ketquaTai) {
                        for (Player pl : playersTai) {
                            long gold = pl.goldTai * 2 * 90 / 100;
                            pl.goldTai = 0;
                            Service.gI().sendThongBaoFromAdmin(pl, "|8|[ Nhà Cái Tài Xỉu ]\n|3|Kết quả xóc ra là:\n|2|" + a + " : " + b + " : " + c + "\n|5|Tổng là: " + total + " (TÀI)\n\n|7|Bạn Đã Chiến Thắng\n|1|Số Tiền Nhận Được Là\n|3|" + Util.numberToMoney(gold) + " Vàng");
                            try {
                                pl.inventory.gold += gold;
                            } catch (Exception e) {
                            }
                            Service.gI().sendMoney(pl);
                        }
                        for (Player pl : playersXiu) {
                            Service.gI().sendThongBaoFromAdmin(pl, "|8|[ Nhà Cái Tài Xỉu ]\n|3|Kết quả xóc ra là:\n|2|" + a + " : " + b + " : " + c + "\n|5|Tổng là: " + total + " (TÀI)\n\n|7|Bạn Đã Thua\n|1|Số Tiền Nhà Cái Ăn Được Là\n|3|" + Util.numberToMoney(pl.goldXiu) + " Vàng");
                            pl.goldXiu = 0;
                        }
                    } else {
                        for (Player pl : playersTai) {
                            Service.gI().sendThongBaoFromAdmin(pl, "|8|[ Nhà Cái Tài Xỉu ]\n|3|Kết quả xóc ra là:\n|2|" + a + " : " + b + " : " + c + "\n|5|Tổng là: " + total + " (Tam Hoa)\n\n|7|Bạn Đã Thua, Nhà Cái Ăn Tất\n|1|Số Tiền Nhà Cái Ăn Được Là\n|3|" + Util.numberToMoney(pl.goldTai) + " Vàng");
                            pl.goldTai = 0;
                        }
                        for (Player pl : playersXiu) {
                            Service.gI().sendThongBaoFromAdmin(pl, "|8|[ Nhà Cái Tài Xỉu ]\n|3|Kết quả xóc ra là:\n|2|" + a + " : " + b + " : " + c + "\n|5|Tổng là: " + total + " (Tam Hoa)\n\n|7|Nhà Cái Ăn Tất\n|1|Số Tiền Nhà Cái Ăn Được Là\n|3|" + Util.numberToMoney(pl.goldXiu) + " Vàng");
                            pl.goldXiu = 0;
                        }
                    }
                    ketquaXiu = false;
                    ketquaTai = false;
                    totalTai = 0;
                    totalXiu = 0;
                    playersTai.clear();
                    playersXiu.clear();
                }
            }
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
        }
    }
}
