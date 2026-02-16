package boss.boss_manifest.Broly;

/*
 *
 *
 * 
 */
import boss.Boss;
import boss.BrolyManager;
import boss.BossData;
import boss.BossID;
import boss.BossStatus;
import static boss.BossType.BROLY;
import boss.BossesData;
import consts.ConstPlayer;
import item.Item;
import java.io.IOException;
import java.util.logging.Level;
import map.ItemMap;
import map.Zone;
import models.Combine.CombineService;
import network.Message;
import player.Inventory;
import player.Pet;
import player.Player;
import services.EffectSkillService;
import services.InventoryService;
import services.ItemService;
import services.PetService;
import services.PlayerService;
import services.Service;
import services.SkillService;
import services.func.ChangeMapService;
import services.func.UseItem;
import skill.Skill;
import utils.SkillUtil;
import utils.Util;

public class BrolySuperVip extends Boss {

    public BrolySuperVip() throws Exception {
        super(BROLY, BossID.BROLYSUPERVIP, false, true, BossesData.BROLYSUPERVIP);
    }

    public BrolySuperVip(Zone zone, int x, int y) throws Exception {

        super(BROLY, BossID.BROLYSUPERVIP, false, true, new BossData(
                "Broly Super Vip", // name
                ConstPlayer.XAYDA, // gender
                new short[] { 1661, 1662, 1663, 70, 98, 172 }, // outfit {head, body, leg, bag, aura, eff}
                10000, // dame
                new long[] { 1_666_666_667 }, // hp
                new int[] { 13, 20, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38 }, // map join
                new int[][] {

                        { Skill.TAI_TAO_NANG_LUONG, 1, 100000 }, { Skill.BIEN_KHI, 3, 100000 },
                        { Skill.ANTOMIC, 3, 50000 }, { Skill.ANTOMIC, 4, 50000 }, { Skill.ANTOMIC, 5, 50000 },
                        { Skill.ANTOMIC, 6, 50000 }, { Skill.ANTOMIC, 7, 50000 }, }, // skill
                new String[] {}, // text chat 1
                new String[] { "|-1|Haha! ta sẽ giết hết các ngươi",
                        "|-1|Sức mạnh của ta là tuyệt đối",
                        "|-1|Vào hết đây!!!", }, // text chat 2
                new String[] { "|-1|Các ngươi giỏi lắm. Ta sẽ quay lại." }, // text chat 3
                600// type appear
        ));
        this.zone = zone;
        this.location.x = x;
        this.location.y = y;
    }

    @Override
    public void reward(Player plKill) {
        plKill.pointboss += 2;
        byte type = (byte) (Util.nextInt(2, 4));
        if (plKill.pet == null) {
            PetService.gI().createNormalPetSuperGender(plKill, Util.nextInt(0, 2), type);
            Service.gI().sendThongBao(plKill, "Bạn đã nhận được đệ tử\n Vui lòng thoát game vào lại");
        }

    }

    @Override
    public void active() {
        super.active();
    }

    @Override
    public void joinMap() {
        this.name = "Broly Super Vip " + Util.nextInt(10, 100);

        if (Util.isTrue(3, 5) && this.zone != null) {
            ChangeMapService.gI().changeMap(this, this.zone, this.location.x, this.location.y);
            this.changeStatus(BossStatus.CHAT_S);
            this.notifyJoinMap();
        } else {
            super.joinMap();
        }
        if (this.pet == null) {
            PetService.gI().createNormalPetSuper(this, Util.nextInt(0, 2), (byte) Util.nextInt(2, 4));

        }

        // PlayerService.gI().changeAndSendTypePK(this.pet, ConstPlayer.PK_ALL);
        st = System.currentTimeMillis();
    }

    private long st;
    private boolean check;

    @Override
    public void autoLeaveMap() {
        if (Util.canDoWithTime(st, 900000)) {
            this.leaveMap();
        }
        if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
            st = System.currentTimeMillis();
        }
    }

    private void HopThe() {
        if (this.pet.isDie() || this.pet == null) {
            this.chat("Đệ tử mau hồi sinh hợp thể");
            return;
        }
        check = true;
        Service.gI().sendTitle(this, 362);
        this.chat("Hợp thể Porata");
        this.pet.status = 3;
        if (this.pet != null) {
            ChangeMapService.gI().exitMap(this.pet);
        }

        this.nPoint.setHp(Util.toIntOrLong(1_500_000_000 + this.pet.nPoint.hpMax));
        this.nPoint.dame += this.pet.nPoint.dame;
        Message msg;
        msg = new Message(-90);

        try {
            msg.writer().writeByte(1);// check type
            msg.writer().writeInt((int) this.id); // id boss
            msg.writer().writeShort(383);// set head
            msg.writer().writeShort(384);// setbody
            msg.writer().writeShort(385);// set leg
            msg.writer().writeByte(0);// set khỉ
            Service.gI().sendMessAllPlayerInMap(this, msg);

            msg.cleanup();
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(UseItem.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (!this.isDeTu) {
            PlayerService.gI().sendInfoHpMp(this);
        }
        Service.gI().point(this);
        Service.gI().Send_Info_NV(this);
        Service.gI().sendInfoPlayerEatPea(this);
        Message hu;
        try {
            hu = new Message(125);
            hu.writer().writeByte(4);
            hu.writer().writeInt((int) this.id);
            Service.gI().sendMessAllPlayerInMap(this, hu);
            hu.cleanup();
        } catch (Exception e) {

        }

    }

    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        this.pet.status = pet.PROTECT;
        if (!this.isDie()) {

            if (Util.isTrue(20, 100) && !check) {

                HopThe();
            }
            if (Util.isTrue(200, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }
            damage = this.nPoint.subDameInjureWithDeff(damage);
            //
            // if (damage > 10_000_000) {
            // damage = Util.nextInt(9_000_000, 10_000_000);
            // }
            this.nPoint.subHP(damage);
            if (isDie()) {
                this.setDie(plAtt);
                die(plAtt);
            }
            return damage;
        } else {
            return 0;
        }
    }

    private long lastTimeAttack;

    @Override
    public void attack() {

        if (Util.canDoWithTime(this.lastTimeAttack, 100) && this.typePk == ConstPlayer.PK_ALL) {

            this.lastTimeAttack = System.currentTimeMillis();
            try {
                Player pl = getPlayerAttack();
                if (pl == null || pl.isDie()) {
                    return;
                }
                this.playerSkill.skillSelect = this.playerSkill.skills
                        .get(Util.nextInt(7, this.playerSkill.skills.size() - 1));
                if (Util.getDistance(this, pl) <= this.getRangeCanAttackWithSkillSelect()) {
                    if (Util.isTrue(5, 20)) {
                        if (SkillUtil.isUseSkillChuong(this)) {
                            this.moveTo(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(20, 200)),
                                    Util.nextInt(10) % 2 == 0 ? pl.location.y : pl.location.y - Util.nextInt(0, 70));
                        } else {
                            this.moveTo(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(10, 40)),
                                    Util.nextInt(10) % 2 == 0 ? pl.location.y : pl.location.y - Util.nextInt(0, 50));
                        }
                    }

                    SkillService.gI().useSkill(this, pl, null, -1, null);
                    checkPlayerDie(pl);
                } else {
                    if (Util.isTrue(1, 2)) {
                        this.moveToPlayer(pl);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void leaveMap() {
        ChangeMapService.gI().exitMap(this);
        if (this.pet != null) {
            ChangeMapService.gI().exitMap(this.pet);
        }
        this.lastZone = null;
        this.lastTimeRest = System.currentTimeMillis();
        this.changeStatus(BossStatus.REST);
        BrolyManager.gI().removeBoss(this);
        this.dispose();
    }
}
