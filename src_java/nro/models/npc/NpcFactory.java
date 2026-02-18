package nro.models.npc;

import Bot.BotManager;
import boss.BossID;
import nro.services.ClanService;
import nro.services.Service;
import nro.services.ItemService;
import nro.services.NgocRongNamecService;
import nro.services.IntrinsicService;
import nro.services.InventoryService;
import nro.services.NpcService;
import nro.services.PetService;
import nro.services.PlayerService;
import nro.services.FriendAndEnemyService;
import consts.ConstNpc;
import boss.BossManager;
import boss.boss_manifest.TrungThuEvent.KhiDot;
import clan.Clan;
import java.util.HashMap;
import java.util.Random;
import services.func.ChangeMapService;
import services.func.SummonDragon;
import static services.func.SummonDragon.SHENRON_1_STAR_WISHES_1;
import static services.func.SummonDragon.SHENRON_1_STAR_WISHES_2;
import static services.func.SummonDragon.SHENRON_SAY;
import nro.player.Player;
import item.Item;
import java.lang.System.Logger.Level;
import jdbc.daos.NDVSqlFetcher;
import matches.PVPService;
import nro.server.Client;
import nro.server.Maintenance;
import nro.server.Manager;
import services.func.Input;
import utils.Logger;
import utils.Util;
import models.SuperDivineWater.SuperDivineWaterService;
import nro.services.SubMenuService;
import models.ShenronEvent.ShenronEventService;
import models.ShenronEvent_NOEL.ShenronEventServicenoel;
import models.kygui.ConsignItem;
import models.kygui.ConsignShopService;
import services.func.SummonDragonNamek;
import nro.models.npc.npc_manifest.*;

public class NpcFactory {

    public static final java.util.Map<Long, Object> PLAYERID_OBJECT = new HashMap<>();

    public static Npc createNPC(int mapId, int status, int cx, int cy, int tempId) {
        int avatar = Manager.NPC_TEMPLATES.get(tempId).avatar;
        try {
            return switch (tempId) {
                case ConstNpc.NGO_KHONG -> new NgoKhong(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.PHO_ANH_HAI -> new PhoAnhHai(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.CAY_NEU -> new CayNeu(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.THO_DAI_CA -> new ThoDaiCa(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.NPC_TRUNG_THU -> new TrungThu(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.CAY_THONG -> new Caythong(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.NOI_BANH -> new Noibanh(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.MYUU -> new Myuu(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.QUAY_NUOC_MIA -> new QuayNuocMia(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.RUONG_SUU_TAP -> new RuongSuuTap(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.GHI_DANH -> new GhiDanh(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.TRONG_TAI -> new TrongTai(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.POTAGE -> new Potage(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.MR_POPO -> new MrPoPo(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.QUY_LAO_KAME -> new QuyLaoKame(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.TRUONG_LAO_GURU -> new TruongLaoGuru(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.VUA_VEGETA -> new VuaVegeta(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.CUA_HANG_KY_GUI -> new KyGui(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.ONG_GOHAN -> new OngGohan(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.ONG_MOORI -> new OngMoori(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.ONG_PARAGUS -> new OngParagus(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.BUNMA -> new Bulma(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.HAI_HOA_HONG -> new HaiHoaHong(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.DENDE -> new Dende(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.APPULE -> new Appule(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.DR_DRIEF -> new DrDrief(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.CARGO -> new Cargo(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.CUI -> new Cui(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.SANTA -> new Santa(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.URON -> new Uron(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.BA_HAT_MIT -> new BaHatMit(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.RUONG_DO -> new RuongDo(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.DAU_THAN -> new DauThan(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.CALICK -> new Calick(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.JACO -> new Jaco(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.THUONG_DE -> new ThuongDe(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.VADOS -> new Vados(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.THAN_VU_TRU -> new ThanVuTru(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.KIBIT -> new Kibit(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.OSIN -> new Osin(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.BABIDAY -> new Babiday(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.LY_TIEU_NUONG -> new LyTieuNuong(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.LINH_CANH -> new LinhCanh(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.QUA_TRUNG -> new QuaTrung(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.QUOC_VUONG -> new QuocVuong(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.QUOC_VUONG_TRANH_NGOC -> new QuocVuongTranhNgoc(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.CHI_CHI -> new ChiChi(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.BUNMA_TL -> new BulmaTuongLai(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.RONG_OMEGA -> new RongOmega(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.RONG_1S -> new Rong1Sao(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.RONG_2S -> new Rong2Sao(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.RONG_3S -> new Rong3Sao(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.RONG_4S -> new Rong4Sao(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.RONG_5S -> new Rong5Sao(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.RONG_6S -> new Rong6Sao(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.RONG_7S -> new Rong7Sao(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.DAI_THIEN_SU -> new DaiThienSu(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.DAI_THIEN_SU2 -> new DaiThienSu2(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.WHIS -> new Whis(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.BILL -> new Bill(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.BO_MONG -> new BoMong(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.THAN_MEO_KARIN -> new Karin(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.GOKU_SSJ -> new GokuSSJ(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.GOKU_SSJ_2 -> new GokuSSJ2(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.TAPION -> new Tapion(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.DOC_NHAN -> new DocNhan(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.GIUMA_DAU_BO -> new GiuMaDauBo(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.PANCHY -> new PanChy(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.TO_SU_KAIO -> new ToSuKaio(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.BARDOCK -> new Bardock(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.DUONG_TANG -> new DuongTang(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.TORI_BOT -> new ToriBot(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.FIDE -> new Fide(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.CADIC -> new Cadic(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.BROLY_SAMA -> new Brolysama(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.HOA_HONG -> new HoaHong(mapId, status, cx, cy, tempId, avatar);
                case ConstNpc.BULMA_THO -> new BulmaTho(mapId, status, cx, cy, tempId, avatar);
                default -> new Npc(mapId, status, cx, cy, tempId, avatar) {
                    @Override
                    public void openBaseMenu(Player player) {
                        if (canOpenNpc(player)) {
                            super.openBaseMenu(player);
                        }
                    }

                    @Override
                    public void confirmMenu(Player player, int select) {
                        if (canOpenNpc(player)) {
                        }
                    }
                };
            };
        } catch (Exception e) {
            Logger.logException(NpcFactory.class, e, "Lỗi load npc");
            return null;
        }
    }

    public static void createNpcRongThieng() {
        new Npc(-1, -1, -1, -1, ConstNpc.RONG_THIENG, -1) {
            @Override
            public void confirmMenu(Player player, int select) {
                switch (player.iDMark.getIndexMenu()) {
                    case ConstNpc.IGNORE_MENU:
                        break;
                    case ConstNpc.SHOW_SHENRON_NAMEK_CONFIRM:
                        SummonDragonNamek.gI().showConfirmShenron(player, player.iDMark.getIndexMenu(), (byte) select);
                        break;
                    case ConstNpc.SHENRON_NAMEK_CONFIRM:
                        if (select == 0) {
                            SummonDragonNamek.gI().confirmWish();
                        } else if (select == 1) {
                            SummonDragonNamek.gI().sendWhishesNamec(player);
                        }
                        break;
                    case ConstNpc.SHOW_SHENRON_EVENT_CONFIRM:
                        if (player.shenronEvent != null) {
                            player.shenronEvent.showConfirmShenron((byte) select);
                        }
                        break;
                    case ConstNpc.SHENRON_EVENT_CONFIRM:
                        if (player.shenronEvent != null) {
                            if (select == 0) {
                                player.shenronEvent.confirmWish();
                            } else if (select == 1) {
                                player.shenronEvent.sendWhishesShenron();
                            }
                        }
                        break;
                    case ConstNpc.SHOW_SHENRON_EVENT_CONFIRM_NOEL:
                        if (player.shenronEventnoel != null) {
                            player.shenronEventnoel.showConfirmShenron((byte) select);
                        }
                        break;
                    case ConstNpc.SHENRON_EVENT_CONFIRM_NOEL:
                        if (player.shenronEventnoel != null) {
                            if (select == 0) {
                                player.shenronEventnoel.confirmWish();
                            } else if (select == 1) {
                                player.shenronEventnoel.sendWhishesShenron();
                            }
                        }
                        break;
                    case ConstNpc.SHENRON_CONFIRM:
                        if (select == 0) {
                            SummonDragon.gI().confirmWish();
                        } else if (select == 1) {
                            SummonDragon.gI().reOpenShenronWishes(player);
                        }
                        break;
                    case ConstNpc.SHENRON_1_1:
                        if (player.iDMark.getIndexMenu() == ConstNpc.SHENRON_1_1 && select == SHENRON_1_STAR_WISHES_1.length - 1) {
                            NpcService.gI().createMenuRongThieng(player, ConstNpc.SHENRON_1_2, SHENRON_SAY, SHENRON_1_STAR_WISHES_2);
                            break;
                        }
                    case ConstNpc.SHENRON_1_2:
                        if (player.iDMark.getIndexMenu() == ConstNpc.SHENRON_1_2 && select == SHENRON_1_STAR_WISHES_2.length - 1) {
                            NpcService.gI().createMenuRongThieng(player, ConstNpc.SHENRON_1_1, SHENRON_SAY, SHENRON_1_STAR_WISHES_1);
                            break;
                        }
                    default:
                        SummonDragon.gI().showConfirmShenron(player, player.iDMark.getIndexMenu(), (byte) select);
                        break;
                }
            }
        };
    }

    public static void createNpcConMeo() {
        new Npc(-1, -1, -1, -1, ConstNpc.CON_MEO, 351) {
            @Override
            public void confirmMenu(Player player, int select) {
                switch (player.iDMark.getIndexMenu()) {
                    case ConstNpc.MENU_OPTION_USE_ITEM1703 -> {
                        if (select == 0) {
                            IntrinsicService.gI().settltd(player);
                        } else if (select == 1) {
                            IntrinsicService.gI().settlnm(player);
                        } else if (select == 2) {
                            IntrinsicService.gI().settlxd(player);
                        }
                    }
                    case ConstNpc.MENU_OPTION_USE_ITEM1704 -> {
                        if (select == 0) {
                            IntrinsicService.gI().sethdtd(player);
                        } else if (select == 1) {
                            IntrinsicService.gI().sethdnm(player);
                        } else if (select == 2) {
                            IntrinsicService.gI().sethdxd(player);
                        }
                    }
                    case ConstNpc.SET_TLTD, ConstNpc.SET_VAI_THO_TD -> {
                        switch (select) {
                            case 0 -> ItemService.gI().settlkaio(player);
                            case 1 -> ItemService.gI().settlgenki(player);
                            case 2 -> ItemService.gI().settlson(player);
                        }
                    }
                    case ConstNpc.SET_TLNM, ConstNpc.SET_VAI_THO_NM -> {
                        switch (select) {
                            case 0 -> ItemService.gI().settlpico(player);
                            case 1 -> ItemService.gI().settloctieu(player);
                            case 2 -> ItemService.gI().settlpiko(player);
                        }
                    }
                    case ConstNpc.SET_TLXD, ConstNpc.SET_VAI_THO_XD -> {
                        switch (select) {
                            case 0 -> ItemService.gI().settlgalick(player);
                            case 1 -> ItemService.gI().settlcadick(player);
                            case 2 -> ItemService.gI().settlnappa(player);
                        }
                    }
                    case ConstNpc.SET_HDTD -> {
                        switch (select) {
                            case 0 -> ItemService.gI().sethdkaio(player);
                            case 1 -> ItemService.gI().sethdgenki(player);
                            case 2 -> ItemService.gI().sethdson(player);
                        }
                    }
                    case ConstNpc.SET_HDNM -> {
                        switch (select) {
                            case 0 -> ItemService.gI().sethdpico(player);
                            case 1 -> ItemService.gI().sethdoctieu(player);
                            case 2 -> ItemService.gI().sethdpiko(player);
                        }
                    }
                    case ConstNpc.SET_HDXD -> {
                        switch (select) {
                            case 0 -> ItemService.gI().sethdcadick(player);
                            case 1 -> ItemService.gI().sethdcadic(player);
                            case 2 -> ItemService.gI().sethdnappa(player);
                        }
                    }
                    case ConstNpc.IGNORE_MENU -> {
                    }
                    case ConstNpc.SUMMON_SHENRON_EVENT -> {
                        if (select == 0) {
                            ShenronEventService.gI().summonShenron(player);
                        }
                    }
                    case ConstNpc.SUMMON_SHENRON_EVENT_NOEL -> {
                        if (select == 0) {
                            ShenronEventServicenoel.gI().summonShenron(player);
                        }
                    }
                    case ConstNpc.MAKE_MATCH_PVP -> {
                        if (Maintenance.isRunning) {
                        }
                        PVPService.gI().sendInvitePVP(player, (byte) select);
                    }
                    case ConstNpc.MAKE_FRIEND -> {
                        if (select == 0) {
                            Object playerId = PLAYERID_OBJECT.get(player.id);
                            if (playerId != null) {
                                try {
                                    FriendAndEnemyService.gI().acceptMakeFriend(player, Integer.parseInt(String.valueOf(playerId)));
                                } catch (NumberFormatException e) {
                                }
                            }
                        }
                    }
                    case ConstNpc.REVENGE -> {
                        if (select == 0) {
                            PVPService.gI().acceptRevenge(player);
                        }
                    }
                    case ConstNpc.TUTORIAL_SUMMON_DRAGON -> {
                        if (select == 0) {
                            NpcService.gI().createTutorial(player, -1, SummonDragon.SUMMON_SHENRON_TUTORIAL);
                        }
                    }
                    case ConstNpc.SUMMON_SHENRON -> {
                        if (select == 0) {
                            NpcService.gI().createTutorial(player, -1, SummonDragon.SUMMON_SHENRON_TUTORIAL);
                        } else if (select == 1) {
                            SummonDragon.gI().summonShenron(player);
                        }
                    }
                    case ConstNpc.MENU_OPTION_USE_ITEM726 -> {
                        if (select == 0) {
                            SuperDivineWaterService.gI().joinMapThanhThuy(player);
                        }
                    }
                    case ConstNpc.MENU_SIEU_THAN_THUY -> {
                        if (select == 0) {
                            ChangeMapService.gI().changeMap(player, 46, -1, Util.nextInt(300, 400), 408);
                        }
                    }
                    case ConstNpc.TAP_TU_DONG_CONFIRM -> {
                        if (select == 0) {
                            ChangeMapService.gI().changeMapBySpaceShip(player, player.lastMapOffline, player.lastZoneOffline, player.lastXOffline);
                        }
                    }
                    case ConstNpc.INTRINSIC -> {
                        switch (select) {
                            case 0 -> IntrinsicService.gI().showAllIntrinsic(player);
                            case 1 -> IntrinsicService.gI().showConfirmOpen(player);
                            case 2 -> IntrinsicService.gI().showConfirmOpenVip(player);
                            default -> {
                            }
                        }
                    }
                    case ConstNpc.CONFIRM_OPEN_INTRINSIC -> {
                        if (select == 0) {
                            IntrinsicService.gI().open(player);
                        }
                    }
                    case ConstNpc.CONFIRM_OPEN_INTRINSIC_VIP -> {
                        if (select == 0) {
                            IntrinsicService.gI().openVip(player);
                        }
                    }
                    case ConstNpc.CONFIRM_LEAVE_CLAN -> {
                        if (select == 0) {
                            ClanService.gI().leaveClan(player);
                        }
                    }
                    case ConstNpc.CONFIRM_NHUONG_PC -> {
                        if (select == 0) {
                            ClanService.gI().phongPc(player, (int) PLAYERID_OBJECT.get(player.id));
                        }
                    }
                    case ConstNpc.BAN_PLAYER -> {
                        if (select == 0) {
                            PlayerService.gI().banPlayer((Player) PLAYERID_OBJECT.get(player.id));
                            Service.gI().sendThongBao(player, "Ban người chơi " + ((Player) PLAYERID_OBJECT.get(player.id)).name + " thành công");
                        }
                    }
                    case ConstNpc.BUFF_PET -> {
                        if (select == 0) {
                            Player pl = (Player) PLAYERID_OBJECT.get(player.id);
                            if (pl.pet == null) {
                                PetService.gI().createNormalPet(pl);
                                Service.gI().sendThongBao(player, "Phát đệ tử cho " + ((Player) PLAYERID_OBJECT.get(player.id)).name + " thành công");
                            }
                        }
                    }
                    case ConstNpc.OTT -> {
                        if (select < 3) {
                            Player pl = (Player) PLAYERID_OBJECT.get(player.id);
                            player.iDMark.setOtt(select);
                            String[] selects = new String[]{"Kéo", "Búa", "Bao", "Hủy"};
                            NpcService.gI().createMenuConMeo(pl, ConstNpc.OTT_ACCEPT, -1,
                                    player.name + " muốn chơi oẳn tù tì với bạn mức cược 5tr.", selects, player);
                        }
                    }
                    case ConstNpc.OTT_ACCEPT -> {
                        if (select < 3) {
                            Player pl = (Player) PLAYERID_OBJECT.get(player.id);
                            int slp1 = pl.iDMark.getOtt();
                            int slp2 = select;
                            if (slp1 == -1 || slp2 == -1) {
                                return;
                            }
                            pl.iDMark.setOtt(-1);
                            String[] selects = new String[]{"Kéo", "Búa", "Bao"};
                            Service.gI().chat(pl, selects[slp1]);
                            Service.gI().chat(player, selects[slp2]);
                            Service.gI().sendEffAllPlayer(pl, 1000 + slp1, 1, 2, 1);
                            Service.gI().sendEffAllPlayer(player, 1000 + slp2, 1, 2, 1);
                            if (slp1 == slp2) {
                                Service.gI().sendThongBao(pl, "Hòa!");
                                Service.gI().sendThongBao(player, "Hòa!");
                            } else if (slp1 == 0 && slp2 == 2 || slp1 == 1 && slp2 == 0 || slp1 == 2 && slp2 == 1) {
                                Service.gI().sendThongBao(pl, "Thắng!");
                                Service.gI().sendThongBao(player, "Thua!");
                                pl.inventory.gold += 4800000;
                                player.inventory.gold -= 5000000;
                                Service.gI().sendMoney(pl);
                                Service.gI().sendMoney(player);
                            } else {
                                Service.gI().sendThongBao(pl, "Thua!");
                                Service.gI().sendThongBao(player, "Thắng!");
                                pl.inventory.gold -= 5000000;
                                player.inventory.gold += 4800000;
                                Service.gI().sendMoney(pl);
                                Service.gI().sendMoney(player);
                            }
                        }
                    }
                    case ConstNpc.MENU_OPTION_USE_ITEM1806, ConstNpc.MENU_OPTION_USE_ITEM1807, ConstNpc.MENU_OPTION_USE_ITEM1808 -> {
                        try {
                            ItemService.gI().OpenSKH(player, player.iDMark.getIndexMenu(), select);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Logger.error("Lỗi mở hộp quà");
                        }
                    }
                    case ConstNpc.MENU_BOT -> {
                        switch (select) {
                            case 0 -> Input.gI().createFormBotQuai(player);
                            case 1 -> Input.gI().createFormBotQuaiNappa(player);
                            case 2 -> Input.gI().createFormBotQuaiTuonglai(player);
                            case 3 -> Input.gI().createFormBotQuaiCold(player);
                            case 4 -> Input.gI().createFormBotItem(player);
                            case 5 -> Input.gI().createFormBotBoss(player);
                            case 6 -> Input.gI().createFormBotUpDe(player);
                            case 7 -> Input.gI().createFormBotChat(player);
                        }
                    }
                    case ConstNpc.MENU_OPTION_USE_ITEM1655 -> {
                        switch (select) {
                            case 0 -> IntrinsicService.gI().sattd(player);
                            case 1 -> IntrinsicService.gI().satnm(player);
                            case 2 -> IntrinsicService.gI().setxd(player);
                        }
                    }
                    case ConstNpc.menutd -> {
                        try {
                            switch (select) {
                                case 0 -> ItemService.gI().setSongoku(player);
                                case 1 -> ItemService.gI().setKaioKen(player);
                                case 2 -> ItemService.gI().setThenXinHang(player);
                            }
                        } catch (Exception e) {
                        }
                    }
                    case ConstNpc.menunm -> {
                        try {
                            switch (select) {
                                case 0 -> ItemService.gI().setLienHoan(player);
                                case 1 -> ItemService.gI().setPicolo(player);
                                case 2 -> ItemService.gI().setPikkoroDaimao(player);
                            }
                        } catch (Exception e) {
                        }
                    }
                    case ConstNpc.menuxd -> {
                        try {
                            switch (select) {
                                case 0 -> ItemService.gI().setKakarot(player);
                                case 1 -> ItemService.gI().setCadic(player);
                                case 2 -> ItemService.gI().setNappa(player);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    case 3213 -> {
                        if (select == 0) {
                            Item it7 = InventoryService.gI().findItemBagByTemp(player, 1500);
                            InventoryService.gI().subQuantityItemsBag(player, it7, 1);
                            InventoryService.gI().sendItemBag(player);
                            try {
                                new KhiDot().zoneFinal = player.zone;
                                new KhiDot().joinMap();
                            } catch (Exception e) {
                            }
                        }
                    }
                    case ConstNpc.MENU_ADMIN -> {
                        switch (select) {
                            case 0 -> {
                                for (int i = 14; i <= 20; i++) {
                                    Item item = ItemService.gI().createNewItem((short) i);
                                    InventoryService.gI().addItemBag(player, item);
                                }
                                InventoryService.gI().sendItemBag(player);
                            }
                            case 1 -> PetService.gI().createNormalPet(player, player.gender);
                            case 2 -> {
                                if (player.isAdmin()) {
                                    System.out.println(player.name + " Đang bảo trì game!");
                                    Maintenance.gI().start(30);
                                }
                            }
                            case 3 -> Input.gI().createFormFindPlayer(player);
                            case 4 -> BossManager.gI().showListBoss(player);
                            case 5 -> BossManager.gI().createBoss(BossID.SUPER_BROLY);
                            case 6 -> Input.gI().createFormBuffVND(player);
                            case 7 -> Input.gI().createFromMailBox(player);
                        }
                    }
                    case 671 -> {
                        if (select == 0) {
                            long[] time = new long[]{900000, 1800000, 3600000, 86400000, 259200000, 604800000, 1296000000};
                            var bb = ItemService.gI().getTemplate(player.LearnSkill.ItemTemplateSkillId);
                            String[] subName = bb.name.split("");
                            byte level = Byte.parseByte(subName[subName.length - 1]);
                            player.LearnSkill.Time = time[level - 1] + System.currentTimeMillis();
                            player.nPoint.tiemNang -= player.LearnSkill.Potential;
                            Service.gI().point(player);
                            Service.gI().ClosePanel(player);
                            NpcService.gI().createTutorial(player, NpcService.gI().getAvatar(13 + player.gender), "Con đã học thành công, hãy cố gắng chờ đợi nha");
                        }
                    }
                    case ConstNpc.CONFIRM_DISSOLUTION_CLAN -> {
                        if (select == 0) {
                            Clan clan = player.clan;
                            clan.deleteDB(clan.id);
                            Manager.CLANS.remove(clan);
                            player.clan = null;
                            player.clanMember = null;
                            ClanService.gI().sendMyClan(player);
                            ClanService.gI().sendClanId(player);
                            Service.gI().sendThongBao(player, "Đã giải tán bang hội.");
                        }
                    }
                    case ConstNpc.CONFIRM_REMOVE_ALL_ITEM_LUCKY_ROUND -> {
                        if (select == 0) {
                            for (int i = 0; i < player.inventory.itemsBoxCrackBall.size(); i++) {
                                player.inventory.itemsBoxCrackBall.set(i, ItemService.gI().createItemNull());
                            }
                            player.inventory.itemsBoxCrackBall.clear();
                            Service.gI().sendThongBao(player, "Đã xóa hết vật phẩm trong rương");
                        }
                    }
                    case ConstNpc.CONFIRM_REMOVE_ALL_ITEM_MAIL_BOX -> {
                        if (select == 0) {
                            for (int i = 0; i < player.inventory.itemsMailBox.size(); i++) {
                                player.inventory.itemsMailBox.set(i, ItemService.gI().createItemNull());
                            }
                            player.inventory.itemsMailBox.clear();
                            if (NDVSqlFetcher.updateMailBox(player)) {
                                Service.gI().sendThongBao(player, "Xóa hết vật phẩm hòm thư thành công");
                            }
                        }
                    }
                    case ConstNpc.MENU_FIND_PLAYER -> {
                        Player p = (Player) PLAYERID_OBJECT.get(player.id);
                        if (p != null) {
                            switch (select) {
                                case 0 -> {
                                    if (p.zone != null) ChangeMapService.gI().changeMapYardrat(player, p.zone, p.location.x, p.location.y);
                                }
                                case 1 -> {
                                    if (p.zone != null) ChangeMapService.gI().changeMap(p, player.zone, player.location.x, player.location.y);
                                }
                                case 2 -> Input.gI().createFormChangeName(player, p);
                                case 3 -> {
                                    String[] selects = new String[]{"Đồng ý", "Hủy"};
                                    NpcService.gI().createMenuConMeo(player, ConstNpc.BAN_PLAYER, -1, "Bạn có chắc chắn muốn ban " + p.name, selects, p);
                                }
                                case 4 -> {
                                    Service.gI().sendThongBao(player, "Kick người chơi " + p.name + " thành công");
                                    Client.gI().getPlayers().remove(p);
                                    Client.gI().kickSession(p.getSession());
                                }
                            }
                        }
                    }
                    case ConstNpc.CONFIRM_TELE_NAMEC -> {
                        if (select == 0) {
                            NgocRongNamecService.gI().teleportToNrNamec(player);
                            player.inventory.subGemAndRuby(50);
                            Service.gI().sendMoney(player);
                        }
                    }
                    case ConstNpc.MA_BAO_VE -> {
                        if (select == 0) {
                            if (player.mbv == 0) {
                                if (player.inventory.gold >= 30000) {
                                    player.inventory.gold -= 30000;
                                    Service.gI().sendMoney(player);
                                    player.mbv = player.iDMark.getMbv();
                                    player.baovetaikhoan = true;
                                    Service.gI().sendThongBao(player, "Kích hoạt thành công, tài khoản đang được bảo vệ");
                                } else {
                                    Service.gI().sendThongBao(player, "Bạn không đủ tiền để kích hoạt bảo vệ tài khoản");
                                }
                            } else {
                                if (player.baovetaikhoan) {
                                    player.baovetaikhoan = false;
                                    Service.gI().sendThongBao(player, "Chức năng bảo vệ tài khoản đang tắt");
                                } else {
                                    player.baovetaikhoan = true;
                                    Service.gI().sendThongBao(player, "Tài khoản đang được bảo vệ");
                                }
                            }
                        }
                    }
                    case ConstNpc.UP_TOP_ITEM -> {
                        if (select == 0) {
                            if (player.inventory.gold >= 5000000 && player.iDMark.getIdItemUpTop() != -1) {
                                ConsignItem it = ConsignShopService.gI().getItemBuy(player.iDMark.getIdItemUpTop());
                                if (it == null || it.isBuy) {
                                    Service.gI().sendThongBao(player, "Vật phẩm không tồn tại hoặc đã được bán");
                                    return;
                                }
                                if (it.player_sell != player.id) {
                                    Service.gI().sendThongBao(player, "Vật phẩm không thuộc quyền sở hữu");
                                    ConsignShopService.gI().openShopKyGui(player);
                                    return;
                                }
                                player.inventory.gold -= 5000000;
                                Service.gI().sendMoney(player);
                                Service.gI().sendThongBao(player, "Thành công");
                                it.isUpTop += 1;
                                ConsignShopService.gI().openShopKyGui(player);
                            } else {
                                Service.gI().sendThongBao(player, "Bạn không đủ vàng");
                                player.iDMark.setIdItemUpTop(-1);
                            }
                        }
                    }
                    case ConstNpc.RUONG_GO -> {
                        int i = player.indexWoodChest;
                        if (i < 0 || player.itemsWoodChest.isEmpty() || i >= player.itemsWoodChest.size()) {
                            return;
                        }
                        Item itemWoodChest = player.itemsWoodChest.get(i);
                        player.indexWoodChest--;
                        String info = "|1|" + itemWoodChest.template.name;
                        String info2 = "\n|2|";
                        if (!itemWoodChest.itemOptions.isEmpty()) {
                            for (Item.ItemOption io : itemWoodChest.itemOptions) {
                                if (io.optionTemplate.id != 102 && io.optionTemplate.id != 73) {
                                    info2 += io.getOptionString() + "\n";
                                }
                            }
                        }
                        info = (info2.length() > "\n|2|".length() ? (info + info2).trim() : info.trim()) + "\n|0|" + itemWoodChest.template.description;
                        NpcService.gI().createMenuConMeo(player, ConstNpc.RUONG_GO, -1, "Bạn nhận được\n"
                                + info.trim(), "OK" + (i > 0 ? " [" + i + "]" : ""));
                    }
                    case ConstNpc.HOP_QUA_THAN_LINH -> {
                        if (InventoryService.gI().getCountEmptyBag(player) < 5) {
                            Service.gI().sendThongBao(player, "Cần 5 ô hành trang mới có thể mở!!!");
                            return;
                        }
                        Item HopQuaThanLinh = InventoryService.gI().findItemBag(player, 1228);
                        if (HopQuaThanLinh == null) return;

                        // 0: TD, 1: NM, 2: XD
                        int genderIdx = select;
                        if (genderIdx < 0 || genderIdx > 2) return;

                        short[][] itemIds = {
                            {555, 557, 559}, // Ao
                            {556, 558, 560}, // Quan
                            {562, 564, 566}, // Gang
                            {563, 565, 567}  // Giay
                        };
                        int[][] optValues = {
                            {800, 900, 950}, // Ao: Base param for opt 47
                            {200, 100, 200}, // Ao: Random range for opt 47
                            {47, 45, 42},    // Quan: Base param for opt 22
                            {5, 5, 8},       // Quan: Random range for opt 22
                            {3500, 3300, 3500}, // Gang: Base param for opt 0
                            {1200, 1100, 1400}, // Gang: Random range for opt 0
                            {42, 47, 45},    // Giay: Base param for opt 23
                            {5, 5, 4}        // Giay: Random range for opt 23
                        };

                        Item ao = ItemService.gI().createNewItem(itemIds[0][genderIdx]);
                        Item quan = ItemService.gI().createNewItem(itemIds[1][genderIdx]);
                        Item gang = ItemService.gI().createNewItem(itemIds[2][genderIdx]);
                        Item giay = ItemService.gI().createNewItem(itemIds[3][genderIdx]);
                        Item nhan = ItemService.gI().createNewItem((short) 561);

                        // Set Options
                        Random rand = new Random();
                        ao.itemOptions.add(new Item.ItemOption(47, optValues[0][genderIdx] + rand.nextInt(optValues[1][genderIdx])));
                        
                        int quanOpt22 = optValues[2][genderIdx] + rand.nextInt(optValues[3][genderIdx]);
                        quan.itemOptions.add(new Item.ItemOption(22, quanOpt22));
                        quan.itemOptions.add(new Item.ItemOption(27, quanOpt22 * 1000 * 15 / 100));

                        gang.itemOptions.add(new Item.ItemOption(0, optValues[4][genderIdx] + rand.nextInt(optValues[5][genderIdx])));

                        int giayOpt23 = optValues[6][genderIdx] + rand.nextInt(optValues[7][genderIdx]);
                        giay.itemOptions.add(new Item.ItemOption(23, giayOpt23));
                        giay.itemOptions.add(new Item.ItemOption(28, giayOpt23 * 1000 * 15 / 100));

                        nhan.itemOptions.add(new Item.ItemOption(14, 14 + rand.nextInt(4)));
                        
                        // Common options
                        Item[] items = {ao, quan, gang, giay, nhan};
                        for (Item it : items) {
                            it.itemOptions.add(new Item.ItemOption(21, 18));
                            it.itemOptions.add(new Item.ItemOption(30, 1));
                            InventoryService.gI().addItemBag(player, it);
                        }

                        InventoryService.gI().subQuantityItemsBag(player, HopQuaThanLinh, 1);
                        InventoryService.gI().sendItemBag(player);
                        String[] tribeName = {"Trái Đất", "Namek", "Xayda"};
                        Service.gI().sendThongBao(player, "Bạn nhận được 1 set thần linh " + tribeName[genderIdx]);
                    }
                    case ConstNpc.MENU_XUONG_TANG_DUOI -> {
                        if (player.fightMabu.pointMabu >= player.fightMabu.POINT_MAX && player.zone.map.mapId != 120) {
                            ChangeMapService.gI().changeMap(player, player.zone.map.mapIdNextMabu((short) player.zone.map.mapId), -1, -1, 100);
                        }
                    }
                }
            }
        };
    }
}