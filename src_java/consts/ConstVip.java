package consts;

public class ConstVip {
    // VIP Levels
    public static final int VIP_NONE = 0;
    public static final int VIP_1 = 1;
    public static final int VIP_2 = 2;
    public static final int VIP_3 = 3;
    public static final int VIP_4 = 4;

    // VIP Purchase Prices (in VND)
    public static final int PRICE_VIP_1 = 20_000;
    public static final int PRICE_VIP_2 = 50_000;
    public static final int PRICE_VIP_3 = 50_000;
    public static final int PRICE_VIP_4 = 50_000;

    // Get price for upgrading to a specific VIP level
    public static int getPriceForVip(int vipLevel) {
        return switch (vipLevel) {
            case VIP_1 -> PRICE_VIP_1;
            case VIP_2 -> PRICE_VIP_2;
            case VIP_3 -> PRICE_VIP_3;
            case VIP_4 -> PRICE_VIP_4;
            default -> 0;
        };
    }

    // Get discounted price based on current VIP level
    public static int getDiscountedPrice(int targetVip, int currentVip) {
        return getPriceForVip(targetVip);
    }
}
