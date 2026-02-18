/**
 * Author: ENZEEFXNROxBARCOLL
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.models;

public enum Reactions {
    // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */

    NONE(""),
    HEART("/-heart"),
    LIKE("/-strong"),
    HAHA(":>"),
    WOW(":o"),
    CRY(":-(("),
    ANGRY(":-h"),
    KISS(":-*"),
    TEARS_OF_JOY(":')"),
    SHIT("/-shit"),
    ROSE("/-rose"),
    BROKEN_HEART("/-break"),
    DISLIKE("/-weak"),
    LOVE(";xx"),
    CONFUSED(";-/"),
    WINK(";-)"),
    FADE("/-fade"),
    SUN("/-li"),
    BIRTHDAY("/-bd"),
    BOMB("/-bome"),
    OK("/-ok"),
    PEACE("/-v"),
    THANKS("/-thanks"),
    PUNCH("/-punch"),
    SHARE("/-share"),
    PRAY("_()_"),
    NO("/-no"),
    BAD("/-bad"),
    LOVE_YOU("/-loveu"),
    SAD("--b"),
    VERY_SAD(":(("),
    COOL("x-)"),
    NERD("8-)"),
    BIG_SMILE(";-d"),
    SUNGLASSES("b-)"),
    NEUTRAL(":--|"),
    SAD_FACE("p-("),
    BYE(":-bye"),
    SLEEPY("|-)"),
    WIPE(":wipe"),
    DIG(":-dig"),
    ANGUISH("&-("),
    HANDCLAP(":handclap"),
    ANGRY_FACE(">-|"),
    F_CHAIR(":-f"),
    L_CHAIR(":-l"),
    R_CHAIR(":-r"),
    SILENT(";-x"),
    SURPRISE(":-o"),
    EMBARRASSED(";-s"),
    AFRAID(";-a"),
    SAD2(":-<"),
    BIG_LAUGH(":))"),
    RICH("$-)"),
    BEER("/-beer");
    
    private final String value;
    
    Reactions(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static Reactions fromValue(String value) {
        for (Reactions reaction : values()) {
            if (reaction.value.equals(value)) {
                return reaction;
            }
        }
        return NONE;
    }
}

