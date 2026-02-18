//package boss.boss_manifest.Minuong;
//
//import boss.BossID;
//import boss.BossesData;
//import consts.ConstPlayer;
//import consts.ConstRatio;
//import java.util.List;
//import nro.player.Player;
//import nro.services.ItemTimeService;
//import nro.services.SkillService;
//import org.apache.log4j.Logger;
//import services.func.ChangeMapService;
//import utils.SkillUtil;
//import utils.Util;
//
///**
// * @Build by BinhViper
// */
//public class MiNuong extends PhoBanMiNuong {
//
//    private static final Logger logger = Logger.getLogger(MiNuong.class);
//    private boolean activeAttack;
//    
//    public MiNuong() throws Exception {
//        super(BossID.MI_NUONG_PHO_BAN, BossesData.MI_NUONG_PHO_BAN);
//    }
//
//    @Override
//    public void attack() {
//        if (!Util.canDoWithTime(this.lastTimeAttack, 100) || this.typePk != ConstPlayer.PK_ALL) {
//            return;
//        }
//
//        this.lastTimeAttack = System.currentTimeMillis();
//
//        try {
//            Player target = this.getPlayerAttack();
//            if (target == null || target.isDie()) {
//                return;
//            }
//
//            // Chọn ngẫu nhiên skill
//            this.playerSkill.skillSelect = Util.random(this.playerSkill.skills);
//
//            // Tấn công khi ở gần
//            if (Util.getDistance(this, target) <= 40) {
//                if (Util.isTrue(5, 20)) {
//                    int offsetX, offsetY;
//                    if (SkillUtil.isUseSkillChuong(this)) {
//                        offsetX = Util.getOne(-1, 1) * Util.nextInt(20, 200);
//                        offsetY = Util.nextBoolean() ? target.location.y : target.location.y - Util.nextInt(70);
//                    } else {
//                        offsetX = Util.getOne(-1, 1) * Util.nextInt(10, 40);
//                        offsetY = Util.nextBoolean() ? target.location.y : target.location.y - Util.nextInt(50);
//                    }
//                    this.moveTo(target.location.x + offsetX, offsetY);
//                }
//
//                SkillService.gI().useSkill(this, target, null, -1, null);
//                checkPlayerDie(target);
//
//            } else {
//                // Tìm người chơi trong phạm vi đặc biệt
//                for (Player p : this.zone.getNotBosses()) {
//                    if (p.location.x >= 820 && !p.effectSkin.isVoHinh) {
//                        this.activeAttack = true;
//                        break;
//                    }
//                }
//            }
//        } catch (Exception e) {
//            logger.error("Lỗi khi boss Mi Nuong tấn công", e);
//        }
//    }
//
//
//    @Override
//    public void joinMap() {
//        try {
//            this.zone = miNuong.getMapById(212);
//            ChangeMapService.gI().changeMap(this, this.zone, 1065, this.zone.map.yPhysicInTop(1065, 0));
//        } catch (Exception e) {
//            logger.error("Lỗi khi Mi Nương vào map", e);
//        }
//    }
//}
