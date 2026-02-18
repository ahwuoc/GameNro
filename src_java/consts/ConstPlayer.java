package consts;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/ifjict764
 *  sdt zalo: 0358176187
 * Chuyên chỉnh sữa mua bán source nro,...
 */
public class ConstPlayer {

    public static final int[] HEADMONKEY = {192, 195, 196, 199, 197, 200, 198};

    public static final byte[][] AURABIENHINH = {
        // LẦN LƯỢT TỪ LB 1-5
        {20, 20, 21, 27, 29}, //td
        {0, 22, 23, 24, 30}, //nm
        {20, 20, 21, 23, 25} //xd
    };
    // SỬA NGOẠI HÌNH TỪ LV 1-5 Ở ĐÂY
    public static final short[][] HEADBIENHINH = {
        {1773, 1779, 1777, 1776, 1778}, // head TD 
        {1767, 1780, 1781, 1782, 1784},// haed NM
        {1770, 1785, 1786, 1787, 1788}, //head XD
    };
    // THÂN NGOẠI HÌNH LV 1-5
    public static final short[] BODYBIENHINH = {1774, 1768, 1771}; // TD /NM/ XD
    public static final short[] LEGBIENHINH = {1775, 1769, 1772}; // TD /NM/ XD

    public static final byte TRAI_DAT = 0;
    public static final byte NAMEC = 1;
    public static final byte XAYDA = 2;

    //type pk
    public static final byte NON_PK = 0;
    public static final byte PK_PVP = 3;
    public static final byte PK_PVP_2 = 4;
    public static final byte PK_ALL = 5;

    //type fushion
    public static final byte NON_FUSION = 0;
    public static final byte LUONG_LONG_NHAT_THE = 4;
    public static final byte HOP_THE_PORATA = 6;
    public static byte HOP_THE_PORATA2 = 8;
    public static byte HOP_THE_PORATA3 = 10;
    public static byte LUONG_LONG_NHAT_THE_GOGETA = 12;
}
