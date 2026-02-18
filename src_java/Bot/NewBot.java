package Bot;

import java.util.Random;
import models.Template.ItemTemplate;
import nro.player.Pet;
import nro.server.Manager;
import skill.Skill;
import utils.SkillUtil;

public class NewBot {

    public static NewBot i;

    public boolean LOAD_PART = true;
    public int MAXPART = 0;
    public static int[][] PARTBOT = new int[Manager.ITEM_TEMPLATES.size()][4];

    private final String[] BOT_NAMES = {
        "bu1lit", "ngochan", "chubi", "octieu1st", "thietdz1", "bighero", "bom80m",
        "onenee", "chjnsu", "nmhayvl", "namek", "viethung", "hieuthuhai", "dmdmd",
        "oneneee", "kakalot", "nekodz", "s2haba2s", "octieu", "songokuz", "xdking",
        "xayda", "bigboss", "g23dubai", "ngochaan", "vipnhat", "kangg", "nmhayvv",
        "daxanh", "dumaa", "thanhlam", "minvuz", "b52b52", "traidatz3k", "chubedan",
        "danh132", "quydeep", "km1toi", "cuilamnha", "nappa", "bunboy", "ducne",
        "vuzgod", "top1xayda", "trumlau", "namecga", "thietdz", "bingo", "no1badboy",
        "hieuthuba", "zenyetein", "harro", "quyetratpr", "jxjjxx", "dznhatsv",
        "babynolove", "top100xd", "joker", "sieunamek", "aphelios", "tgkhoi",
        "trumtd1", "hxtblack", "datcubu", "amireux", "vtv24", "wheijx", "sjusy",
        "ophisdxd", "1dapvelang", "kimngox", "killer", "cuman999", "uhdnd",
        "zemtop1", "mozaa", "bests", "emkiendezet", "hxtblackdz", "testlam",
        "egthieu", "ndhxb", "goten", "drgonboy", "chiengtnd", "trumlo",
        "hieudubai", "zemnamek", "trumsv", "nhfbd", "test222", "phoenix",
        "fnkfd", "blackk", "sondeptrai", "test333", "gunny", "comcac", "hehehe",
        "liuzal", "nhdjd", "valla", "allladin", "kingen", "kakalot1", "thiet",
        "kizaru", "thiet1", "fgvsgs", "i3emthue", "1hit1em", "easylove",
        "jinguyen", "djmie", "kakatot", "songohan", "sanghakai", "helllllo",
        "lowni", "ahlatien", "bobi", "cncncncnc", "player", "datdz", "acmin",
        "onehit", "xayda1hp", "shynn", "lylux", "anhtuan", "zemsiuu", "s1tghp",
        "zicbum", "lucifer", "jimsan", "zicola", "ziicola", "siunhan",
        "blackgoku", "nmoctiu", "mabumap", "sieuxayda", "krillin", "bodoi",
        "lugia", "test001", "dlinhcute", "anhthuw", "duckar", "akadame1st",
        "picolo", "thekid", "phimsex", "dlinhcutee", "siuxayda", "legend1st",
        "dragon", "lostgame", "congduc", "hiepga", "hkjjkl", "xaydavip",
        "acdtest", "gokusama", "shisha", "20", "caychay", "huhuhihi", "broly",
        "daxanhne", "chuberong", "myxdieu", "caychayne", "picollo", "picoloz",
        "namec", "sieukame", "chubi1", "oneenee", "bosss", "jukaza", "bucacno",
        "gohanbeat", "alllla", "trumpem", "hikonpem", "hikonbomb", "thuong",
        "vailonoo", "shinoo", "hellonamec", "xaydachill", "noname", "googler",
        "quyenanh", "huyenthoai", "killua"
    };

    public static NewBot gI() {
        if (i == null) {
            i = new NewBot();
        }
        return i;
    }
    // Thêm biến data_charm
    public static long[] data_charm = {
        1731314659866L, 1731314658103L, 1731314661624L, 1731314664075L,
        1728729880025L, 1731314665945L, 1731314667513L, 1731314670067L,
        1731314672612L, 1731314674881L, 0
    };

    public void LoadPart() {
        if (LOAD_PART) {
            int i = 0;
            for (ItemTemplate it : Manager.ITEM_TEMPLATES) {
                if (it.type == 5) {
                    if (it.head != -1 && it.leg != -1 && it.body != -1 && it.leg != 194) {
                        PARTBOT[i][0] = it.head;
                        PARTBOT[i][1] = it.leg;
                        PARTBOT[i][2] = it.body;
                        PARTBOT[i][3] = it.gender;
                        i++;
                        MAXPART++;
                    }
                }
            }
            LOAD_PART = false;
        }
    }

    public String Getname() {
        Random random = new Random();
        return BOT_NAMES[random.nextInt(BOT_NAMES.length)];
    }

    public int getIndex(int gender) {
        int Random = new Random().nextInt(MAXPART);
        int gend = PARTBOT[Random][3];
        if (gend == gender) {
            return Random;
        } else {
            return getIndex(gender);
        }
    }

    public void runBot(int type, ShopBot shop, int slot) {
        LoadPart();
        for (int i = 0; i < slot; i++) {
            int gender = new Random().nextInt(3);
            int randomIndex = getIndex(gender);
            int head = PARTBOT[randomIndex][0];
            int leg = PARTBOT[randomIndex][1];
            int body = PARTBOT[randomIndex][2];

            // copy shop nếu có
            ShopBot shopCopy = (shop != null ? new ShopBot(shop) : null);

            // random cờ
            int flag = Manager.gI().FLAGS_BAGS
                    .get(new Random().nextInt(Manager.gI().FLAGS_BAGS.size())).id;

            // tạo bot
            Bot b = new Bot((short) head, (short) body, (short) leg, type, Getname(), shopCopy, (short) flag);

            // gán hành vi theo type
            switch (type) {
                case 0: // BOTQUAI thường
                    b.mo1 = new Mobb(b);
                    break;
                case 1: // BOTITEM (có shop)
                    if (shopCopy != null) {
                        shopCopy.bot = b;
                    }
                    break;
                case 2: // BOTQUAI_NAPPA
                    b.botnappa = new BotNappa(b);
                    break;
                case 3: // BOTQUAI_TUONGLAI
                    b.bottuonglai = new BotTuonglai(b);
                    break;
                case 4: // BOTQUAI_COLD
                    b.botcold = new BotCold1(b);
                    break;
                case 5: // BOTBOSS
                    b.boss = new Sanb(b);
                    break;
                case 6: // BOTCHAT
                    b.chatBot = new BotChatTG(b);
                    break;

                case 99: // BOTUPDE (debug/update + đệ)
                    Pet detu = new Pet(b);  // đệ tử của bot
                    detu.master = b;
                    detu.isPet = true;
                    detu.name = b.name + " Đệ";

                    // chỉ số cơ bản
                    detu.gender = b.gender;
                    detu.nPoint.hpMax = 1000;
                    detu.nPoint.hp = detu.nPoint.hpMax;
                    detu.nPoint.mpMax = 500;
                    detu.nPoint.mp = detu.nPoint.mpMax;
                    detu.nPoint.dameg = 50;

                    // skill cơ bản
                    detu.playerSkill.skills.add(SkillUtil.createSkill(Skill.KAMEJOKO, 1));
                    detu.playerSkill.skillSelect = detu.playerSkill.skills.get(0);

                    // AI cho đệ
                    detu.ai = new DeTuAI(detu);

                    // gán vào bot
                    b.pet = detu;

                    // add vào map sau khi bot join
                    if (b.zone != null) {
                        detu.zone = b.zone;
                        b.zone.addPlayer(detu);
                        detu.location.x = b.location.x + 30;
                        detu.location.y = b.location.y;
                    }
                    break;
            }

            // chỉ số cơ bản
            int congThem = new Random().nextInt(50_000_000);
            b.nPoint.limitPower = 7;
            b.nPoint.power = 10000 + congThem;
            b.nPoint.tiemNang = 20_000_000_000L + congThem;
            b.nPoint.dameg = 50000;
            b.nPoint.mpg = 2_000_000_000;
            b.nPoint.mpMax = 2_000_000_000;
            b.nPoint.mp = 2_000_000_000;
            b.nPoint.hpg = 500_000;
            b.nPoint.hpMax = 5_000_000;
            b.nPoint.hp = 5_000_000;
            b.nPoint.maxStamina = 20000;
            b.nPoint.stamina = 20000;
            b.nPoint.critg = 10;
            b.nPoint.defg = 10;
            b.gender = (byte) gender;

            // load skill, join map, thêm vào BotManager
            b.leakSkill();
            b.joinMap();
            BotManager.gI().bot.add(b);
        }
    }
}
