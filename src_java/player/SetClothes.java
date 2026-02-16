package player;

import item.Item;

public class SetClothes {

    private Player player;

    public SetClothes(Player player) {
        this.player = player;
    }

    public byte songoku;
    public byte thienXinHang;
    public byte kirin;
    public byte kaioken;
    public byte thanVuTruKaio;

    public byte ocTieu;
    public byte pikkoroDaimao;
    public byte picolo;
    public byte lienHoan;
    public byte nail;
    public byte ToiThuong;
    public byte kakarot;
    public byte cadic;
    public byte nappa;
    public byte DaiThanh;
    public byte giamSatThuong;
    public byte cadicM;

    public byte worldcup;
    public byte setDHD;

    public boolean godClothes;
    public int ctHaiTac = -1;
    public int ctDietQuy = -1;
    public int ctBunmaTocMau = -1;
    public int ctTiecbaiBien = -1;
    public int petKhungLong = -1;

    public void setup() {
        setDefault();
        if (this.player.inventory.itemsBody.size() < 6) {
            return;
        }
        setupSKT();
        setupSKHNew();
        this.godClothes = true;
        for (int i = 0; i < 5; i++) {
            Item item = this.player.inventory.itemsBody.get(i);
            if (item.isNotNullItem()) {
                if (item.template.id > 567 || item.template.id < 555) {
                    this.godClothes = false;
                    break;
                }
            } else {
                this.godClothes = false;
                break;
            }
        }
        Item ct = this.player.inventory.itemsBody.get(5);
        if (ct.isNotNullItem()) {
            switch (ct.template.id) {
                case 618:
                case 619:
                case 620:
                case 621:
                case 622:
                case 623:
                case 624:
                case 626:
                case 627:
                    this.ctHaiTac = ct.template.id;
                    break;
                case 1087:
                case 1088:
                case 1089:
                case 1090:
                case 1091:
                    this.ctDietQuy = ct.template.id;
                    break;
                case 1208:
                case 1209:
                case 1210:
                    this.ctBunmaTocMau = ct.template.id;
                    break;

            }
        }

    }

    private void setupSKT() {
        for (int i = 0; i < 5; i++) {
            Item item = this.player.inventory.itemsBody.get(i);
            if (item.isNotNullItem()) {
                boolean isActSet = false;
                for (Item.ItemOption io : item.itemOptions) {
                    switch (io.optionTemplate.id) {
                        case 129:
                        case 141:
                            isActSet = true;
                            songoku++;
                            break;
                        case 127:
                        case 139:
                            isActSet = true;
                            thienXinHang++;
                            break;
                        case 128:
                        case 140:
                            isActSet = true;
                            kirin++;
                            break;
                        case 131:
                        case 143:
                            isActSet = true;
                            ocTieu++;
                            break;
                        case 132:
                        case 144:
                            isActSet = true;
                            pikkoroDaimao++;
                            break;
                        case 130:
                        case 142:
                            isActSet = true;
                            picolo++;
                            break;
                        case 135:
                        case 138:
                            isActSet = true;
                            nappa++;
                            break;
                        case 133:
                        case 136:
                            isActSet = true;
                            kakarot++;
                            break;
                        case 197:
                        case 198:
                            isActSet = true;
                            DaiThanh++;
                            break;
                        case 134:
                        case 137:
                            isActSet = true;
                            cadic++;
                            break;
                        case 250:
                        case 253:
                            isActSet = true;
                            kaioken++;
                            break;
                        case 251:
                        case 254:
                            isActSet = true;
                            lienHoan++;
                            break;
                        case 252:
                        case 255:
                            isActSet = true;
                            giamSatThuong++;
                            break;
                        case 169:
                        case 170:
                            isActSet = true;
                            ToiThuong++;
                        case 21:
                            if (io.param == 80) {
                                setDHD++;
                            }
                            break;
                    }

                    if (isActSet) {
                        break;
                    }
                }
            } else {
                break;
            }
        }
    }

    private void setupSKHNew() {
        for (int i = 0; i < 5; i++) {
            Item item = this.player.inventory.itemsBody.get(i);
            if (item.isNotNullItem()) {
                boolean isActSet = false;
                for (Item.ItemOption io : item.itemOptions) {
                    switch (io.optionTemplate.id) {
                        case 245:
                        case 246:
                        case 247:
                        case 248:
                            isActSet = true;
                            thanVuTruKaio++;
                            break;
                        case 237:
                        case 238:
                        case 239:
                        case 240:
                            isActSet = true;
                            nail++;
                            break;
                        case 241:
                        case 242:
                        case 243:
                        case 244:
                            isActSet = true;
                            cadicM++;
                            break;
                    }

                    if (isActSet) {
                        break;
                    }
                }
            } else {
                break;
            }
        }
    }

    private void setDefault() {
        this.songoku = 0;
        this.thienXinHang = 0;
        this.kirin = 0;
        this.kaioken = 0;
        this.ocTieu = 0;
        this.pikkoroDaimao = 0;
        this.picolo = 0;
        this.lienHoan = 0;
        this.kakarot = 0;
        this.cadic = 0;
        this.nappa = 0;
        this.giamSatThuong = 0;
        this.DaiThanh = 0;
        this.ToiThuong = 0;
        this.thanVuTruKaio = 0;

        this.nail = 0;

        this.cadicM = 0;

        this.setDHD = 0;
        this.worldcup = 0;
        this.godClothes = false;
        this.ctHaiTac = -1;
        this.ctDietQuy = -1;
        this.ctBunmaTocMau = -1;
        this.ctTiecbaiBien = -1;
    }

    public boolean checkSetGod() {
        if (this.player.inventory.itemsBody.size() < 5) {
            return false;
        }
        for (int i = 0; i < 5; i++) {
            Item item = this.player.inventory.itemsBody.get(i);
            if (item.isNotNullItem()) {
                if (item.template.id < 555 || item.template.id > 567) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    public boolean checkSetDes() {
        if (this.player.inventory.itemsBody.size() < 5) {
            return false;
        }
        for (int i = 0; i < 5; i++) {
            Item item = this.player.inventory.itemsBody.get(i);
            if (item.isNotNullItem()) {
                if (item.template.id < 650 || item.template.id > 662) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    public void dispose() {
        this.player = null;
    }
}
