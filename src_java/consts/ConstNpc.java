package consts;

import minigame.TX.TaiXiu;
import nro.player.Player;
import utils.Util;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/ifjict764
 *  sdt zalo: 0358176187
 * Chuyên chỉnh sữa mua bán source nro,...
 */
public class ConstNpc {

    public static final String HUONG_DAN_2010 = "Sự kiện 20-11\n"
            + "Để có những vật phẩm cần tìm như sau\n"
            + "Treo quái rơi: Lá trà tươi, Que tre, Nia tre\n"
            + "Săn boss: Mảnh giấy, Bao bì thiệp, keo dán\n"
            + "Hoa cúc và lời chúc được bán tại santa\n"
            + "Sử dụng Lời chúc có tỉ lệ ra pet vĩnh vĩnh chỉ số tối đa 15%\n"
            + "Lời chúc Đặc biệt: thỏi vàng, hộp 20/11, Đá ngũ sắc, đá lửa, đá cam, Nro\n"
            + "Đặc biệt hộp 20/11 có thể ra cải trang vĩnh viễn rất cao";

    public static final String HUONG_DAN_NAP = "Ta sẽ giới thiệu con về giá trị BONUS khi nạp\n"
            + "Mệnh Giá nạp quyết định tới giá trị nạp\b"
            + "Giá trị nạp càng cao tỉ lệ BONUS tăng càng cao\b"
            + "20k tăng thêm 2% giá trị nạp\b"
            + "50k tăng thêm 5% giá trị nạp\b"
            + "100k tăng thêm 7% giá trị nạp\b"
            + "200k tăng thêm 10% giá trị nạp\b"
            + "500k tăng thêm 15% giá trị nạp\b"
            + "1 triệu tăng thêm 20% giá trị nạp\b"
            + "2 triệu tăng thêm 25% giá trị nạp\b"
            + "5 triệu tăng thêm 30% giá trị nạp\b"
            + "10 triệu tăng thêm 35% giá trị nạp";

    public static final String CALICK_KE_CHUYEN = "20 năm trước bọn Android sát thủ đã đánh bại nhóm bảo vệ trái đất của Sôngoku và Cađíc, Pôcôlô ...\n"
            + "Riêng Sôngoku vì bệnh tim nên đã chết trước đó nên không thể tham gia trận đánh...\n"
            + "Từ đó đến nay bọn chúng tàn phá Trái Đất không hề thương tiếc\n"
            + "Cháu và mẹ may mắn sống sót nhờ lẩn trốn tại tần hầm của công ty Capsule...\n"
            + "Cháu tuy cũng là siêu xayda nhưng cũng không thể làm gì được bọn Android sát thủ...\n"
            + "Chỉ có Sôngoku mới có thể đánh bại bọn chúng\n"
            + "mẹ cháu đã chế tạo thành công cỗ máy thời gian\n"
            + "và cháu quay về quá khứ để cứu Sôngoku...\n"
            + "Bệnh của Gôku ở quá khứ là nan y, nhưng với trình độ y học tương lai chỉ cần uống thuốc là khỏi...\n"
            + "Hãy đi theo cháu đến tương lai giúp nhóm của Gôku đánh bạn bọn Android sát thủ\n"
            + "Khi nào chú cần sự giúp đỡ của cháu hãy đến đây nhé";

    public static final String HUONG_DAN_DOANH_TRAI = "1) Trại độc nhãn là nơi các ngươi không nên vào vì những tướng tá rất mạnh. Hahaha\b"
            + "2) Trong trại độc nhãn, mỗi vị tướng đều giữ ngọc rồng từ 4 sao đến 6 sao, tùy lúc\n"
            + "3) Nếu ngươi thích chết thì cứ việc vào. Nhưng ta chỉ cho vào mỗi ngày một lần thôi, để ngươi khỏi phải chết nhiều, hahaha.\b"
            + "4) Các vị tướng trong trại rất mạnh nhé, các ngươi không đơn giản có thể đánh bại họ bằng cách bình thường như đánh quái được đâu\n"
            + "5) Muốn vào, ngươi phải đi cùng một người đồng đội cùng bang (phải đứng gần ngươi). Nhưng ta khuyên là nên đi 3-4 người cùng.\b"
            + "6) Mỗi lần vào, ngươi chỉ có 30 phút để đánh. Sau 30 phút mà ngươi vẫn không thắng, ta sẽ cho máy bay chở ngươi về nhà.";

    public static final String HUONG_DAN_BLACK_BALL_WAR = "Mỗi ngày từ 20h đến 21h các hành tinh có Ngọc Rồng Sao Đen sẽ xảy ra 1 cuộc đại chiến\n"
            + "Người nào tìm thấy và giữ được Ngọc Rồng Sao Đen sẽ mang phần thưởng về cho bang của mình trong vòng 1 ngày\n"
            + "Lưu ý mỗi bang có thể chiếm hữu nhiều viên khác nhau nhưng nếu cùng loại cũng chỉ nhận được 1 lần phần thưởng đó. Có 2 cách để thắng:\n"
            + "1) Giữ ngọc sao đen trên người hơn 5 phút liên tục\n"
            + "2) Sau 30 phút tham gia tàu sẽ đón về và đang giữ ngọc sao đen trên người\n"
            + "Các phần thưởng như sau\b1 sao đen: +21% sức đánh\b2 sao đen: +35% HP\b"
            + "3 sao đen: Biến 35% tấn công thành HP\b4 sao đen: Phản 35% sát thương\b"
            + "5 sao đen: 35% Sức đánh chí mạng\b6 sao đen: KI+40%\b7 sao đen: 14% Né đòn";
    public static final String HUONG_DAN_MAP_MA_BU = "Tại khu vực này, ta đã dùng ma pháp phong ấn\b"
            + "dù các ngươi có mạnh đến đâu\bcũng sẽ trở thành yếu đuối như nhau\n"
            + "Chỉ có ta với con nhóc Ôsin mới giải được ma pháp này\b"
            + "Khi đó sức mạnh của ngươi sẽ được\bgiải phóng theo điểm tích lũy (TL)\n"
            + "Khi đủ điểm tích lũy bằng cách\bhạ lẫn nhau hoặc hạ boss\b"
            + "ta sẽ đưa ngươi xuống tầng tiếp theo";
    public static final String HUONG_DAN_DOI_SKH_VIP = "Nguyên liệu cần để làm SKH VIP là :\n"
            + "1 Lọ Sơn Tăng Dame, 1 món Thiên Sứ và 2 món SKH thường\n"
            + "Lưu ý SKH VIP sẽ tạo ra đưa vào món Thiên Sứ\n"
            + "Ví dụ nguyên liệu gồm : Quần Xayda Thiên Tứ + 2 món SKH thường ngẫu nhiên\n"
            + "Bạn sẽ nhận lại được Quần Xayda với chỉ số SKH VIP";

    public static final String NPC_DHVT23 = "Đại hội quy tụ nhiều cao thủ như là Jacky Chun, Thiên Xin Hăng, Tàu Bảy Bảy... Phần thường là 1 rương gỗ chưa nhiều vật phẩm giá trị.\bKhi hạ được 1 đối thủ, phần thưởng sẽ nâng lên 1 cấp.\bRương càng cao cấp, vật phẩm trong đó càng giá trị hơn.\n"
            + "Mỗi ngày bạn chỉ được nhận 1 phần thưởng.\bBạn hãy cố gắng hết sức mình\bđể nhận phần thưởng xứng đáng nhất nhé";

    public static final String HUONG_DAN_KHI_GAS_HUY_DIET = "Chúng ta gặp rắc rối rồi\bThượng Đế nói với tôi rằng có 1 loại khí\bgọi là Destron Gas, thứ này không thuộc về nơi đây\n"
            + "Nó tích tụ trên Trái Đất\bvà nó sẽ hủy diệt mọi mô tế bào sống\bCó tất cả 4 địa điểm mà Thượng Đế bảo tôi nói với cậu\bCậu có thể đến kiểm tra...\n"
            + "Đầu tiên là Thành phố Santa tọa lạc ở phía tây nam của thủ đô ở Viễn Đông.\n"
            + "Thứ hai là gần Kim Tự Tháp ở vùng Sa Mạc viễn tây của thủ đô Phía Bắc.\n"
            + "Thứ ba Vùng Đất Băng Giá ở Phương Bắc xa xôi\n"
            + "Thứ tư là Hành tinh Bóng Tối đang che phủ một phần địa cầu\bCậu đã hiểu rõ chưa?";

    public static final String HUONG_DAN_BILL = "Đại hội võ thuật liên vũ trụ\blà nơi quy tụ các cao thủ từ khắp các Vũ Trụ\bĐược tổ chức hàng ngày tại các thời điểm như sau:\n"
            + "Thứ 2: 6h, Thứ 3: 13h, Thứ 4: 15h\bThứ 5: 17h, Thứ 6: 18h, Thứ 7: 12h, Chủ Nhật 10h\bVới các phần thưởng vô cùng hấp dẫn như sau\n"
            + "Top 10: 1 phiếu giảm giá, 1 capsule Vàng\bTop 1: tặng thêm 1 Rađa cấp 13\bPhần thưởng được trao tại vũ trụ của chiến binh\bĐến gặp Whis tại vũ trụ của ngươi để nhận thưởng";

    public static final String HUONG_DAN_NRNM = "Để gọi được Rồng Thần Namếc, anh cần phải làm các việc sau\b"
            + "1) Đang có bang hội\b2) Tập họp đủ 7 viên ngọc rồng Namếc tại đây\b3) Em sẽ gọi rồng và anh nào giữ ngọc 1 sao sẽ được chọn điều ước\b"
            + "4) Thời gian gọi Rồng Thần là 8h-22h\n"
            + "(Lưu ý) Điều ước sẽ có tác dụng với tất cả thành viên trong bang có mặt tại đây\b"
            + "Sau khi điều ước được thực hiện, tất cả ngọc sẽ biến thành đá trong 1 ngày\b"
            + "Những ai vừa nhận điều ước, hoặc bang hội nhận điều ước\b"
            + "phải chờ 7 ngày sau mới có thể nhận điều ước khác";
    public static final String TAP_TU_DONG = "Tập luyện vẫn tiếp tục và sức mạnh vẫn tăng khi đã Offline\n"
            + "Hiệu quả tập luyện như sau:\bThần Mèo: 20 sức mạnh mỗi phút\bYajirô: 40 sức mạnh mỗi phút\bMr.PôPô: 80 sức mạnh mỗi phút\bThượng đế: 160 sức mạnh mỗi phút\n"
            + "Khỉ Bubbles: 320 sức mạnh mỗi phút\bThần Vũ Trụ: 640 sức mạnh mỗi phút\bTổ sư Kaio: 1280 sức mạnh mỗi phút\n"
            + "Có thể tặng ngọc để thắng mà không cần thách đấu\n"
            + "Nếu đăng ký tập thường xuyên mỗi khi Offline không cần phải đến đây vẫn tập luyện được";
    public static final String THONG_TIN_DAI_HOI_VO_THUAT = "Lịch thi đấu trong ngày\bGiải Nhi đồng: 8,14,18h\bGiải Siêu cấp 1: 9,13,19h\bGiải Siêu Cấp 2: 10,15,20h\bGiải Siêu cấp 3: 11,16,21h\bGiải Ngoại hạng: 12,17,22,23h\n"
            + "Giải thưởng khi thắng mỗi vòng\bGiải Nhi đồng: 200 ngọc\bGiải Siêu cấp 1: 400 ngọc\bGiải Siêu cấp 2: 600 ngọc\bGiải Siêu cấp 3: 800 ngọc\bGiải Ngoại hạng: 5 thỏi vàng\bVô địch: 5 viên đá nâng cấp\n"
            + "Lệ phí đăng ký các giải đấu\bGiải Nhi đồng: 200 ngọc\bGiải Siêu cấp 1: 400 ngọc\bGiải Siêu cấp 2: 600 ngọc\bGiải Siêu cấp 3: 800 ngọc\bGiải Ngoại hạng: 5 thỏi vàng\n"
            + "Vui lòng đến đúng giờ để đăng ký thi đấu";

    public static final String THONG_TIN_SIEU_HANG = "Giải đấu thể hiện đẳng cấp thực sự\bCác trận đấu diễn ra liên tục bất kể ngày đêm\bBạn hãy tham gia thi đấu để nâng hạng\bvà nhận giải thưởng khủng nhé\n"
            + "Cơ cấu giải thưởng như sau\b(chốt và trao giải ngẫu nhiên từ 20h-23h mỗi ngày)\bTop 1 thưởng 100 ngọc\bTop 2-10 thưởng 20 ngọc\bTop 11-100 thưởng 5 ngọc\bTop 101-1000 thưởng 1 ngọc\n"
            + "Mỗi ngày các bạn được tặng 1 vé tham dự miễn phí\b(tích lũy tối đa 3 vé) khi thua sẽ mất đi 1 vé\bKhi hết vé bạn phải trả 1 ngọc để đấu tiếp\b(trừ ngọc khi trận đấu kết thúc)\n"
            + "Bạn không thể thi đấu với đấu thủ\bcó hạng nhỏ hơn mình\bChúc bạn may mắn, chào đoàn kết và quyết thắng";
    //npcid
    public static final byte ONG_GOHAN = 0;
    public static final byte ONG_PARAGUS = 1;
    public static final byte ONG_MOORI = 2;
    public static final byte RUONG_DO = 3;
    public static final byte DAU_THAN = 4;
    public static final byte CON_MEO = 5;
    public static final byte KHU_VUC = 6;
    public static final byte BUNMA = 7;
    public static final byte DENDE = 8;
    public static final byte APPULE = 9;
    public static final byte DR_DRIEF = 10;
    public static final byte CARGO = 11;
    public static final byte CUI = 12;
    public static final byte QUY_LAO_KAME = 13;
    public static final byte TRUONG_LAO_GURU = 14;
    public static final byte VUA_VEGETA = 15;
    public static final byte URON = 16;
    public static final byte BO_MONG = 17;
    public static final byte THAN_MEO_KARIN = 18;
    public static final byte THUONG_DE = 19;
    public static final byte THAN_VU_TRU = 20;
    public static final byte BA_HAT_MIT = 21;
    public static final byte TRONG_TAI = 22;
    public static final byte GHI_DANH = 23;
    public static final byte RONG_THIENG = 24;
    public static final byte LINH_CANH = 25;
    public static final byte DOC_NHAN = 26;
    public static final byte RONG_THIENG_NAMEC = 27;
    public static final byte CUA_HANG_KY_GUI = 28;
    public static final byte RONG_OMEGA = 29;
    public static final byte RONG_2S = 30;
    public static final byte RONG_3S = 31;
    public static final byte RONG_4S = 32;
    public static final byte RONG_5S = 33;
    public static final byte RONG_6S = 34;
    public static final byte RONG_7S = 35;
    public static final byte RONG_1S = 36;
    public static final byte BUNMA_TL = 37;
    public static final byte CALICK = 38;
    public static final byte SANTA = 39;
    public static final byte MABU_MAP = 40;
    public static final byte TRUNG_THU = 41;
    public static final byte QUOC_VUONG = 42;
    public static final byte TO_SU_KAIO = 43;
    public static final byte OSIN = 44;
    public static final byte KIBIT = 45;
    public static final byte BABIDAY = 46;
    public static final byte GIUMA_DAU_BO = 47;
    public static final byte NGO_KHONG = 48;
    public static final byte DUONG_TANG = 49;
    public static final byte QUA_TRUNG = 50;
    public static final byte DUA_HAU = 51;
    public static final byte HUNG_VUONG = 52;
    public static final byte TAPION = 53;
    public static final byte LY_TIEU_NUONG = 54;
    public static final byte BILL = 55;
    public static final byte WHIS = 56;
    public static final byte CHAMPA = 57;
    public static final byte VADOS = 58;
    public static final byte TRONG_TAI_2 = 59;
    public static final byte GOKU_SSJ = 60;
    public static final byte GOKU_SSJ_2 = 61;
    public static final byte POTAGE = 62;
    public static final byte JACO = 63;
    public static final byte YARIROBE = 65;
    public static final byte NOI_BANH = 66;
    public static final byte MR_POPO = 67;
    public static final byte PANCHY = 68;
    public static final byte THO_DAI_CA = 69;
    public static final byte BARDOCK = 70;
    public static final byte TORI_BOT = 72;
    public static final byte FIDE = 74;
    public static final byte CADIC = 75;
    public static final byte DAI_THIEN_SU = 77;
    public static final byte DAI_THIEN_SU2 = -77;
    public static final byte QUOC_VUONG_TRANH_NGOC = 78;
    public static final byte BROLY_SAMA = 79;
    public static final byte RUONG_SUU_TAP = 80;
    public static final byte MYUU = 81;
    public static final byte CHI_CHI = 82;
    public static final byte QUAY_NUOC_MIA = 83;
    public static final byte NPC_TRUNG_THU = 84;
    public static final byte CAY_NEU = 85;
    public static final byte CAY_THONG = 86;
    public static final byte PHO_ANH_HAI = 87;
    public static final byte HOA_HONG = 104;
    public static final byte HAI_HOA_HONG = 105;
    public static final byte BULMA_THO = 109;
    //----------------------index menu------------------------------------------
    //menu o len tang map mabu
    public static final int GO_UPSTAIRS_MENU = 10000;
    public static final int REWARD_TOP = 1475;

    //index menu base
    public static final int IGNORE_MENU = 72002;
    public static final int BASE_MENU = 31072002;
    public static final int BASE_MENU_TRANH_NGOC = 31072003;
    public static final int BASE_MENU_DUA_NGOC = 31072004;

    //index quy lão kamê
    public static final int MENU_OPEN_DBKB = 500;
    public static final int MENU_OPENED_DBKB = 501;
    public static final int MENU_ACCEPT_GO_TO_BDKB = 502;
    public static final int MO_RONG_COLLECTION_BOX = 251013;

    //index than vu tru
    public static final int MENU_DI_CHUYEN = 500;

    //index menu ba hat mit
    public static final int MENU_OPTION_SHOP_BUA = 500;
    public static final int MENU_START_COMBINE = 501;
    public static final int MENU_NANG_DOI_SKH_VIP = 502;
    public static final int MENU_CHE_TAO_CHAN_MENH = 503;
    public static final int MENU_NANG_CAP_CHAN_MENH = 504;
    public static final int MENU_SACH_TUYET_KY = 505;

    //index menu whis
    public static final int MENU_DAP_DO = 508;
//tranh ngọc
    public static final int REGISTER_TRANH_NGOC = 800;
    public static final int LOG_OUT_TRANH_NGOC = 801;
    //index menu linh canh
    public static final int MENU_JOIN_DOANH_TRAI = 502;

    //index menu con meo
    public static final int MAKE_MATCH_PVP = 502;
    public static final int MAKE_FRIEND = 503;
    public static final int REVENGE = 504;
    public static final int TUTORIAL_SUMMON_DRAGON = 505;
    public static final int SUMMON_SHENRON = 506;
    public static final int INTRINSIC = 507;
    public static final int CONFIRM_OPEN_INTRINSIC = 508;
    public static final int CONFIRM_OPEN_INTRINSIC_VIP = 509;
    public static final int CONFIRM_LEAVE_CLAN = 510;
    public static final int CONFIRM_NHUONG_PC = 511;
    public static final int MENU_ADMIN = 512;
    public static final int MENU_BOT = 534;
    public static final int BAN_PLAYER = 513;
    public static final int BUFF_PET = 514;
    public static final int CONFIRM_REMOVE_ALL_ITEM_LUCKY_ROUND = 515;
    public static final int MENU_FIND_PLAYER = 516;
    public static final int CONFIRM_DISSOLUTION_CLAN = 517;
    public static final int CONFIRM_TELE_NAMEC = 522;
    public static final int UP_TOP_ITEM = 527;
    public static final int MA_BAO_VE = 528;
    public static final int OTT = 529;
    public static final int OTT_ACCEPT = 530;
    public static final int RUONG_GO = 531;
    public static final int DAT_CUOC_HAT_MIT = 532;
    public static final int BUY_BACK = 533;
    public static final int MENU_OPTION_USE_ITEM726 = 726;
    public static final int MENU_SIEU_THAN_THUY = 2006;
    public static final int MENU_XUONG_TANG_DUOI = 2007;

    //index menu rong thieng
    public static final int SHENRON_CONFIRM = 501;
    public static final int SHENRON_1_1 = 502;
    public static final int SHENRON_1_2 = 503;
    public static final int SHENRON_2 = 504;
    public static final int SHENRON_3 = 505;
    public static final int SHENRON_1_3 = 506;
    public static final int SHOW_SHENRON_NAMEK_CONFIRM = 31720020;
    public static final int SHENRON_NAMEK_CONFIRM = 31720021;
    public static final int SUMMON_SHENRON_EVENT = 31720022;
    public static final int SHOW_SHENRON_EVENT_CONFIRM = 31720023;
    public static final int SHENRON_EVENT_CONFIRM = 31720024;
    public static final int SUMMON_SHENRON_EVENT_NOEL = 31720025;
    public static final int SHOW_SHENRON_EVENT_CONFIRM_NOEL = 31720026;
    public static final int SHENRON_EVENT_CONFIRM_NOEL = 31720027;

    public static final int SUB_MENU = 31720025;
    public static final int TAP_TU_DONG_CONFIRM = 31720026;
    public static final int MENU_CLAN_UP = 4;
    public static final int MENU_CLAN_TASK = 5;
    public static final int MENU_CLAN_TASK_REMOVE = 6;

    public static final int DANGKYDHVT_CONFIRM = 0;

    //index menu magic tree
    public static final int MAGIC_TREE_NON_UPGRADE_LEFT_PEA = 501;
    public static final int MAGIC_TREE_NON_UPGRADE_FULL_PEA = 502;
    public static final int MAGIC_TREE_CONFIRM_UPGRADE = 503;
    public static final int MAGIC_TREE_UPGRADE = 504;
    public static final int MAGIC_TREE_CONFIRM_UNUPGRADE = 505;

    //index menu mabu egg
    public static final int CAN_NOT_OPEN_EGG = 500;
    public static final int CAN_OPEN_EGG = 501;
    public static final int CONFIRM_OPEN_EGG = 502;
    public static final int CONFIRM_DESTROY_EGG = 503;

    //index menu bill egg
    public static final int CAN_NOT_OPEN_BILL = 500;
    public static final int CAN_OPEN_BILL = 501;
    public static final int CONFIRM_OPEN_BILL = 502;
    public static final int CONFIRM_DESTROY_BILL = 503;

    //index menu npc nhà
    public static final int QUA_TAN_THU = 500;

    public static final int MENU_PHAN_THUONG = 501;

    public static final int NAP_THE = 502;

    public static final int NAP_TIEN = 503;

    public static final int MOC_QUA_TANG = 507;

    public static final int NAP_VANG = 504;

    public static final int NAP_NGOC = 505;

    public static final int CHUYEN_KHOAN = 506;

    public static final int CONTENT_CHUYEN_KHOAN = 525;
    //index menu quốc vương
    public static final int OPEN_POWER_MYSEFT = 500;
    public static final int OPEN_POWER_PET = 501;

    //index menu thượng đế
    public static final int MENU_CHOOSE_LUCKY_ROUND = 500;

    //index menu cui
    public static final int MENU_FIND_KUKU = 501;
    public static final int MENU_FIND_MAP_DAU_DINH = 502;
    public static final int MENU_FIND_RAMBO = 503;

    //index menu rồng omega
    public static final int MENU_NOT_OPEN_BDW = 500;
    public static final int MENU_OPEN_BDW = 501;
    public static final int MENU_REWARD_BDW = 502;
    //index menu osin
    public static final int MENU_NOT_OPEN_MMB = 500;
    public static final int MENU_OPEN_MMB = 501;
    public static final int MENU_REWARD_MMB = 502;

    //index menu rồng sao đen
    public static final int MENU_PHU_HP = 500;
    public static final int MENU_OPTION_PHU_HP = 501;
    public static final int MENU_OPTION_GO_HOME = 502;

    //index menu bò mộng
    public static final int MENU_OPTION_LEVEL_SIDE_TASK = 500;
    public static final int MENU_OPTION_PAY_SIDE_TASK = 501;
    public static final int CHUC_NANG_BANG_HOI = 530;
    public static final int CHUC_NANG_BANG_HOI2 = 531;
    public static final int MENU_BO_MONG = 532;

    //------------------------------shop id-------------------------------------
    //npcname_shoporder
    public static final int SHOP_BUNMA_QK_0 = 500;
    public static final int SHOP_DENDE_0 = 501;
    public static final int SHOP_APPULE_0 = 502;
    public static final int SHOP_SANTA_0 = 503;
    public static final int SHOP_SANTA_1 = 504;
    public static final int SHOP_URON_0 = 505;
    public static final int SHOP_BA_HAT_MIT_0 = 506;
    public static final int SHOP_BA_HAT_MIT_1 = 507;
    public static final int SHOP_BA_HAT_MIT_2 = 508;
    public static final int SIDE_BOX_LUCKY_ROUND = 509;
    public static final int SHOP_BUNMA_TL_0 = 510;
    public static final int SIDE_BOX_ITEM_REWARD = 511;
    public static final int MAIL_BOX = 5558;
    public static final int CONFIRM_REMOVE_ALL_ITEM_MAIL_BOX = 5559;
    public static final int SHOP_LEARN_SKILL = 5560;
    public static final int MENU_KOL = 5561;

    public static final int CHUC_NANG_PHA_LE = 0;
    public static final int PHA_LE_HOA_TRANG_BI = 2;
    public static final int CHUYEN_HOA_TRANG_BI = 3;

    public static final int SACH_TUYET_KY = 0;
    public static final int DONG_THANH_SACH_CU = 1;
    public static final int DOI_SACH_TUYET_KY = 2;

    public static final int HOC_TUYET_KY = 0;

    public static final int LEARN_SKILL = 10;
    public static final int FAST_LEARN_SKILL = 11;

    public static final int CHE_TAO_TRANG_BI_THIEN_SU = 1;

    public static final int HOP_QUA_THAN_LINH = 251003;

    public static final int SET_TLTD = 1200;
    public static final int SET_TLNM = 1201;
    public static final int SET_TLXD = 1202;
    public static final int SET_HDTD = 1300;
    public static final int SET_HDNM = 1301;
    public static final int SET_HDXD = 1302;
    public static final int SET_VAI_THO_TD = 1303;
    public static final int SET_VAI_THO_NM = 1304;
    public static final int SET_VAI_THO_XD = 1305;
    public static final int MENU_OPTION_USE_ITEM1703 = 1703;
    public static final int MENU_OPTION_USE_ITEM1704 = 1704;
    public static final int MENU_OPTION_USE_ITEM1536 = 1536;
    public static final int RECEIVE_KOL_TASK = 251014;
    public static final int MENU_QUAN_LY_SERVER = 251015;

    public static final int MENU_OPTION_USE_ITEM1806 = 1806;
    public static final int MENU_OPTION_USE_ITEM1807 = 1807;
    public static final int MENU_OPTION_USE_ITEM1808 = 1808;
    public static final int MENU_OPEN_SUKIEN_BINH = 1809;

    //poke
    public static final int MENU_DOI_THUONG = 1815;
    public static final int MENU_TOP_BONG_MASTER = 1816;
    public static final int MENU_TOP_THANG_9_VIP = 1817;
    public static final int MENU_TOP_THANG_9 = 1818;

    //poke
    public static final int MENU_HOP_DIEM = 1828;
    public static final int MENU_VONG_QUAY_VANG = 1827;
    public static final int MENU_VONG_QUAY_DAC_BIET = 1826;

    // 🎃 Halloween Event Menus
    public static final int MENU_TOP_HALLOWEEN_MASTER = 1901;     // Top Halloween tổng hợp (Master)
    public static final int MENU_TOP_HOP_KEO_HALLOWEEN = 1902;    // Top Hộp Kẹo Ma Quỷ
    public static final int MENU_TOP_THIEP_HALLOWEEN = 1903;      // Top Thiệp Ma Halloween
    public static final int MENU_TOP_PHAO_BONG = 1906;
    public static final int MENU_TOP_LIXI = 1905;

    //event trung thu
    public static final int MENU_HOP_QUA_TRUNG_THU_VIP = 1819;
    public static final int MENU_LONG_DEN_TREO = 1820;
    //event 20-11 -halowen
    public static final int MENU_TRA_HOA_CUC = 1821;
    public static final int MENU_MA_QUY = 1822;
    public static final int MENU_CAPSULE_VIP = 1823;
    public static final int MENU_THIEP_CHUC_VIP = 1824;
    public static final int MENU_HOP_QUA_2010 = 1825;
    //menu nấu bánh
    public static final int MENU_DUA_TOP = 1810;
    public static final int BANH_TET = 1816;
    public static final int BANH_CHUNG = 1817;
    public static final int MENU_BANH_TET = 1811;
    public static final int MENU_BANH_TET_2 = 1812;
    public static final int MENU_BANH_CHUNG = 1813;
    public static final int MENU_BANH_CHUNG_2 = 1814;
    public static final int BASE_MENU_INDEX = 1815;
    //menu nấu bánh trung thu
    public static final int MENU_BANH_TRUNG_THU_GA_QUAY = 1817;
    public static final int MENU_BANH_TRUNG_THU_GA_QUAY_2 = 1816;
    public static final int MENU_BANH_TRUNG_THU_GA_QUAY_HAO_HAN = 1818;
    public static final int MENU_BANH_TRUNG_THU_GA_QUAY_HAO_HAN_2 = 1819;
    public static final int MENU_BANH_TRUNG_THU_HAT_SEN = 1820;
    public static final int MENU_BANH_TRUNG_THU_HAT_SEN_2 = 1821;
    public static final int MENU_DOI_THO_CUNG = 1822;
    //cây thông noel
    public static final int MENU_DOI_VPSK = 325236;
    public static final int MENU_DOI_VPSK_2 = 325237;
    public static final int MENU_DOI_VPSK_1 = 325238;
    public static final int MENI_XEM_DIEM = 235236;
    public static final int MAY_DAM = 235239;
    public static final int MENU_NR = 325240;

    public static final int MENU_OPEN_MMB_WITH_JAR = 1001;
    public static final int MENU_OPEN_MMB_NO_JAR = 1002;
    public static final int MENU_NOT_OPEN_MMB_WITH_JAR = 1003;
    public static final int MENU_NOT_OPEN_MMB_NO_JAR = 1004;
    public static final int BUA_HO_TRO = 1005;
    public static final int BINH_HUT_NANG_LUONG = 1006;
    public static final int MENU_NANG_GIOI_HAN = 1007;
    public static final int LAM_HOP_KEO_MA_QUY = 533;
    public static final int LAM_HOP_KEO_MA_QUY_2 = 532;
    public static final int LAM_HOP_KEO_MA_QUY_x10 = 534;
    public static final int LAM_HOP_KEO_MA_QUY_x10_2 = 535;
    //menu envent 20-11
    public static final int LAM_TUI_TRA_KHO = 517;
    public static final int LAM_TUI_TRA_KHO_2 = 518;
    public static final int HOP_TRA = 519;
    public static final int HOP_TRA_2 = 520;
    public static final int HOP_TRA_HOA_CUC = 521;
    public static final int HOP_TRA_HOA_CUC_2 = 522;
    public static final int THIEP_CHUC_THUONG_2 = 523;
    public static final int THIEP_CHUC_THUONG = 524;
    public static final int THIEP_CHUC_DAC_BIET = 525;
    public static final int THIEP_CHUC_DAC_BIET_2 = 526;
    public static final int TRANG_TRI_THUONG = 527;
    public static final int TRANG_TRI_THUONG_2 = 528;
    public static final int TRANG_TRI_VIP = 531;
    public static final int TRANG_TRI_VIP_2 = 530;
    // skh
    public static final int MENU_OPTION_USE_ITEM1655 = 1655;
    public static final int menutd = 1106;
    public static final int menunm = 1107;
    public static final int menuxd = 1108;
}
