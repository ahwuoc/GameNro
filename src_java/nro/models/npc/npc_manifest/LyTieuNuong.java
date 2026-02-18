package nro.models.npc.npc_manifest;

/**
 * Box ZALO:https://zalo.me/g/ifjict764 sdt zalo: 0358176187 Chuyên chỉnh sữa
 * mua bán source nro,...
 */
import consts.ConstMiniGame;
import minigame.DecisionMaker.DecisionMaker;
import minigame.DecisionMaker.DecisionMakerGem;
import minigame.DecisionMaker.DecisionMakerGold;
import minigame.DecisionMaker.DecisionMakerRuby;
import minigame.LuckyNumber.LuckyNumber;
import minigame.LuckyNumber.LuckyNumberService;
import minigame.RockPaperScissors.RockPaperScissors;
import minigame.TX.TaiXiuService;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.TaskService;
import services.func.Input;

public class LyTieuNuong extends Npc {


    public LyTieuNuong(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
            createOtherMenu(player, ConstMiniGame.MENU_CHINH, "Bạn muốn tham gia mini game nào?",
                    "Kéo\nBúa\nBao", "Con số\nmay mắn\nvàng",
                    "Con số\nmay mắn\nngọc xanh", "Chọn ai đây", "Tài Xỉu", "Đóng");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            switch (player.iDMark.getIndexMenu()) {
                case ConstMiniGame.MENU_CHINH -> {
                    switch (select) {
                        case 0 ->
                            createOtherMenu(player, ConstMiniGame.MENU_KEO_BUA_BAO, "Hãy chọn mức cược.",
                                    "1 Tr vàng", "5 Tr vàng", "10 Tr vàng");
                        case 1 -> {
                            LuckyNumber.showMenu(this, player, false);
                            player.iDMark.setGemCSMM(false);
                        }
                        case 2 -> {
                            LuckyNumber.showMenu(this, player, true);
                            player.iDMark.setGemCSMM(true);
                        }
                        case 3 ->
                            DecisionMaker.gI().showMenu(this, player);
                        case 4 ->
                            createOtherMenu(player, ConstMiniGame.MENU_TAI_XIU,
                                    "Xin chào , Ta là Lý Tiểu Nương - Quản lí casino TX tại Đảo Kame",
                                    "Thể lệ", "Tham gia", "Đóng");
                    }
                }

                case ConstMiniGame.MENU_TAI_XIU, ConstMiniGame.MINIGAME_TAIXIU_TUYCHON, ConstMiniGame.MINIGAME_TAIXIU_TAI, ConstMiniGame.MINIGAME_TAIXIU_XIU -> {
                    TaiXiuService.gI().confirmMenu(this, player, select, player.iDMark.getIndexMenu());
                }

                case ConstMiniGame.MENU_KEO_BUA_BAO ->
                    RockPaperScissors.confirmMenu(this, player, select);

                case ConstMiniGame.MENU_PLAY_KEO_BUA_BAO -> {
                    if (player.iDMark.getTimePlayKeoBuaBao() - System.currentTimeMillis() > 0) {
                        RockPaperScissors.confirmPlay(this, player, select);
                    } else {
                        createOtherMenu(player, ConstMiniGame.MENU_KEO_BUA_BAO, "Hãy chọn mức cược.",
                                "1 Tr vàng", "5 Tr vàng", "10 Tr vàng");
                    }
                }

                case ConstMiniGame.MENU_CON_SO_MAY_MAN_VANG -> {
                    /* để trống */ }
                case ConstMiniGame.MENU_CON_SO_MAY_MAN_NGOC -> {
                    /* để trống */ }

                case ConstMiniGame.MENU_CHON_AI_DAY -> {
                    switch (select) {
                        case 0 ->
                            DecisionMaker.gI().showTutorial(this, player);
                        case 1 ->
                            DecisionMakerGold.showMenuSelect(this, player);
                        case 2 ->
                            DecisionMakerRuby.showMenuSelect(this, player);
                        case 3 ->
                            DecisionMakerGem.showMenuSelect(this, player);
                    }
                }

                case ConstMiniGame.MENU_LUCKY_NUMBER -> {
                    if (select == 0) {
                        LuckyNumber.showMenu(this, player, player.iDMark.isGemCSMM());
                    }
                }

                case ConstMiniGame.MENU_PLAY_LUCKY_NUMBER_GOLD, ConstMiniGame.MENU_PLAY_LUCKY_NUMBER_GEM -> {
                    switch (select) {
                        case 0 ->
                            LuckyNumber.showMenu(this, player, player.iDMark.isGemCSMM());
                        case 1 ->
                            Input.gI().createFormSelectOneNumberLuckyNumber(player, player.iDMark.isGemCSMM());
                        case 2 ->
                            LuckyNumberService.addOneNumber(player, true);
                        case 3 ->
                            LuckyNumberService.addOneNumber(player, false);
                        case 4 ->
                            LuckyNumber.showMenuTutorials(this, player);
                    }
                }

                case ConstMiniGame.MENU_PLAY_DECISION_MAKER_GOLD -> {
                    switch (select) {
                        case 0 ->
                            DecisionMakerGold.showMenuSelect(this, player);
                        case 1 ->
                            DecisionMakerGold.selectPlay(this, player, true);
                        case 2 ->
                            DecisionMakerGold.selectPlay(this, player, false);
                    }
                }

                case ConstMiniGame.MENU_PLAY_DECISION_MAKER_GEM -> {
                    switch (select) {
                        case 0 ->
                            DecisionMakerGem.showMenuSelect(this, player);
                        case 1 ->
                            DecisionMakerGem.selectPlay(this, player, true);
                        case 2 ->
                            DecisionMakerGem.selectPlay(this, player, false);
                    }
                }

                case ConstMiniGame.MENU_PLAY_DECISION_MAKER_RUBY -> {
                    switch (select) {
                        case 0 ->
                            DecisionMakerRuby.showMenuSelect(this, player);
                        case 1 ->
                            DecisionMakerRuby.selectPlay(this, player, true);
                        case 2 ->
                            DecisionMakerRuby.selectPlay(this, player, false);
                    }
                }

                case ConstMiniGame.MENU_WAIT_NEW_GAME -> {
                    if (select == 0) {
                        DecisionMaker.gI().showTutorial(this, player);
                    }
                }
            }
        }
    }
}
