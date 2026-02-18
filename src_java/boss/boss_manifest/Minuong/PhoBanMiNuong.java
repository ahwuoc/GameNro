//package boss.boss_manifest.Minuong;
//
//import boss.AppearType;
//import boss.Boss;
//import boss.BossData;
//import boss.BossManager;
//import boss.BossStatus;
//import boss.boss_manifest.replica.MapMiNuong;
//import item.Item.ItemOption;
//import static item.ItemTime.BAN_DO_KHO_BAU;
//import java.util.ArrayList;
//import java.util.List;
//import map.ItemMap;
//import mob.Mob;
//import nro.player.Player;
//import nro.services.ItemTimeService;
//import nro.services.Service;
//import services.func.ChangeMapService;
//import utils.Util;
//
///**
// *
// * NguyenTuanBinh
// *
// */
//public abstract class PhoBanMiNuong extends Boss {
//
//    private int percent;
//    protected MapMiNuong miNuong;
//
//    public PhoBanMiNuong(int id, BossData data, MapMiNuong miNuong) throws Exception {
//        super(id, data);
//        this.miNuong = miNuong;
//        this.rest();
//    }
//    
//    @Override
//    public void rest() {
//        int nextLevel = this.currentLevel + 1;
//        if (nextLevel >= this.data.length) {
//            nextLevel = 0;
//        }
//        if (this.data[nextLevel].getTypeAppear() == AppearType.DEFAULT_APPEAR
//                && Util.canDoWithTime(lastTimeRest, secondsRest * 1000)) {
//            this.changeStatus(BossStatus.RESPAWN);
//        }
//        long currentTimeMillis = System.currentTimeMillis();
//        long elapsedTime = currentTimeMillis - lastTimeRest;
//
//        this.percent = (int) (elapsedTime * 100 / ((secondsRest - 3) * 1000));
//        if (percent <= 100) {
//            Service.gI().SendMabu(this.zoneFinal, this.percent);
//        }
//    }
//
//    @Override
//    public void attack() {
//        super.attack();
//    }
//
//    public boolean isMobDie() {
//        try {
//            for (Mob mob : this.zone.mobs) {
//                if (mob != null && mob.isDie() == false) {
//                    return false;
//                }
//            }
//        } catch (Exception e) {
//        }
//        return true;
//    }
//
//    @Override
//    public void checkPlayerDie(Player pl) {
//        if (pl.isDie()) {
//            Service.gI().chat(this, "Chừa chưa ranh con, nên nhớ ta là " + this.name);
//        }
//    }
//
//    @Override
//    public void leaveMap() {
//        super.leaveMap();
//        BossManager.gI().removeBoss(this);
//    }
//
//    @Override
//    public void reward(Player pl) {
//        pl.clan.miNuong_haveGone = true;
//        pl.clan.miNuong.timePickReward = true;
//        pl.clan.miNuong.setLastTimeOpen(System.currentTimeMillis() + 30000);
//        for (Player plm : pl.clan.membersInGame) {
//            ItemTimeService.gI().removeTextmiNuong(pl);
//            ItemTimeService.gI().sendTextTime(plm, (byte) BAN_DO_KHO_BAU, "Mị nương sắp kết thúc : ", 30);
//        }
//
//        List<ItemMap> items = new ArrayList<>();
//
//        // Tạo và thêm các đối tượng ItemMap vào danh sách
//        for (int i = 0; i < 4; i++) {
//            int offsetX = i * 10;
//            ItemMap itemGold = new ItemMap(this.zone, 190, 30000, this.location.x + offsetX, this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), pl.id);
//            items.add(itemGold);
//        }
//
//        // Tạo itCaiTrang và thêm các option vào
//        ItemMap itCaiTrang = new ItemMap(this.zone, 860, 1, this.location.x + 40, this.zone.map.yPhysicInTop(this.location.x, this.location.y), pl.id);
//        itCaiTrang.options.add(new ItemOption(77, Util.nextInt(15, 20)));
//        itCaiTrang.options.add(new ItemOption(103, Util.nextInt(15, 20)));
//        itCaiTrang.options.add(new ItemOption(50, Util.nextInt(15, 20)));
//        itCaiTrang.options.add(new ItemOption(117, Util.nextInt(10, 20)));
//        if (Util.isTrue(95, 100)) {
//            itCaiTrang.options.add(new ItemOption(93, Util.nextInt(1, 15)));
//            itCaiTrang.options.add(new ItemOption(30, 0));
//        } else {
//            itCaiTrang.options.add(new ItemOption(30, 0));
//        }
//        items.add(itCaiTrang);
//
//        // Tạo itCaiTrang_1 và thêm các option vào
//        ItemMap itCaiTrang_1 = new ItemMap(this.zone, 860, 1, this.location.x + 50, this.zone.map.yPhysicInTop(this.location.x, this.location.y), pl.id);
//        itCaiTrang_1.options.add(new ItemOption(77, Util.nextInt(15, 20)));
//        itCaiTrang_1.options.add(new ItemOption(103, Util.nextInt(15, 20)));
//        itCaiTrang_1.options.add(new ItemOption(50, Util.nextInt(15, 20)));
//        itCaiTrang_1.options.add(new ItemOption(117, Util.nextInt(10, 20)));
//        if (Util.isTrue(95, 100)) {
//            itCaiTrang_1.options.add(new ItemOption(93, Util.nextInt(1, 15)));
//            itCaiTrang_1.options.add(new ItemOption(30, 0));
//        } else {
//            itCaiTrang_1.options.add(new ItemOption(30, 0));
//        }
//        items.add(itCaiTrang_1);
//
//        // Tạo itBanDo và thêm vào danh sách
//        ItemMap itBanDo = new ItemMap(this.zone, 611, 1, this.location.x + 60, this.zone.map.yPhysicInTop(this.location.x, this.location.y), pl.id);
//        items.add(itBanDo);
//
//        // Thực hiện drop các ItemMap từ danh sách
//        for (ItemMap item : items) {
//            Service.gI().dropItemMap(this.zone, item);
//        }
//    }
//
//    @Override
//    protected boolean useSpecialSkill() {
//        return false;
//    }
//
//    @Override
//    protected void notifyPlayeKill(Player player) {
//    }
//
//    @Override
//    public void joinMap() {
//        try {
//            this.zone = this.miNuong.getMapById(mapJoin[Util.nextInt(0, mapJoin.length - 1)]);
//            ChangeMapService.gI().changeMap(this, this.zone, 1065, this.zone.map.yPhysicInTop(1065, 0));
//        } catch (Exception e) {
//
//        }
//    }
//
//}
