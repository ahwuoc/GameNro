package boss.boss_manifest.LuyenTap;

import map.Map;
import map.Zone;
import nro.player.Player;
import nro.server.Manager;
import nro.services.MapService;
import nro.services.PlayerService;
import nro.services.Service;
import skill.Skill;
import utils.Util;

/**
 * @author outcast c-cute hột me 😳
 */
public class NPC_MrPôPô extends Player {

    public void initNPC_MrPôPô() {
        init();
    }
    private static NPC_MrPôPô instance;

    public static NPC_MrPôPô getInstance() {
        if (instance == null) {
            instance = new NPC_MrPôPô();
        }
        return instance;
    }

    private Zone z;

    @Override
    public short getHead() {
        return 83;
    }

    @Override
    public short getBody() {
        return 84;
    }

    @Override
    public short getLeg() {
        return 85;
    }

    public void joinMap(Zone z, Player player) {
        this.z = z;
        MapService.gI().goToMap(player, z);
        z.load_Me_To_Another(player);
    }
    
    private long lastTimeMove;
    
    private void move() {
        if (Util.canDoWithTime(lastTimeMove, 1000)) {
            if (Util.isTrue(2, 3)) {
                int x = this.location.x;
                x += Util.nextInt(-50, 50);
                if (x > 470 || x < 250) {
                    x = Util.nextInt(250, 470);
                }
                int y = 240;
                PlayerService.gI().playerMove(this, x, y);
            }
            lastTimeMove = System.currentTimeMillis();
        }
    }

    private long lastTimeHoiPhuc;
    private final long TimeHoiPhuc = 5000; // 5 giây cho mỗi lần hồi phục

    @Override
    public void update() {
        if (!Util.canDoWithTime(lastTimeHoiPhuc, TimeHoiPhuc)) {
            return;
        }

        // Chỉ hồi nếu chưa chết
        if (!this.isDie()) {
            Thread.ofVirtual().start(() -> hoiPhuc());
        } else {
            // Nếu chết, thực hiện logic chết
            Service.gI().hsChar(this, nPoint.hpMax, nPoint.mpMax);
        }

        lastTimeHoiPhuc = System.currentTimeMillis();
    }

    // Phương thức hồi phục boss
    public void hoiPhuc() {
        // Chỉ hồi khi chưa chết
        if (!this.isDie() && this.nPoint.hp < 2000000000) {
            this.nPoint.hp = 2000000000;
            PlayerService.gI().sendInfoHpMpMoney(this);
            if (z != null) {
                z.loadAnotherToMe(this);
                z.load_Me_To_Another(this);
            }
        }
    }

    // Hàm khởi tạo NPC MrPôPô
    private void init() {
        int id = -251003;
        for (Map m : Manager.MAPS) {
            if (MapService.gI().isMapTapLuyen(m.mapId)) {
                for (Zone z : m.zones) {
                    NPC_MrPôPô pl = new NPC_MrPôPô();
                    pl.name = "Mr.PôPô";
                    pl.gender = 0;
                    pl.id = id + 1;
                    pl.nPoint.hpMax = 9_000_000_000_000_000_000L;
                    pl.nPoint.hpg = 9_000_000_000_000_000_000L;
                    pl.nPoint.hp = 9_000_000_000_000_000_000L;
                    pl.nPoint.setFullHpMp();
                    pl.location.x = 239;
                    pl.location.y = 336;
                    pl.isNpc = true;
                    pl.typePk = 5;
                    joinMap(z, pl);
                    z.setNpc(pl);
                }
            }
        }
    }

    @Override
    public long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (plAtt != null && plAtt.playerSkill != null && plAtt.playerSkill.skillSelect != null) {
            byte skillId = plAtt.playerSkill.skillSelect.template.id;

            // Danh sách skill giảm damage
            switch (skillId) {
                case Skill.ANTOMIC:
                case Skill.DEMON:
                case Skill.DRAGON:
                case Skill.GALICK:
                case Skill.KAIOKEN:
                case Skill.KAMEJOKO:
                case Skill.MASENKO:
                case Skill.DICH_CHUYEN_TUC_THOI:
                case Skill.LIEN_HOAN:
                case Skill.DE_TRUNG:
                case Skill.MAKANKOSAPPO:
                case Skill.QUA_CAU_KENH_KHI:
                case Skill.TU_SAT:
                case Skill.SUPER_KAME:
                case Skill.LIEN_HOAN_CHUONG:
                case Skill.MA_PHONG_BA:
                case Skill.BIEN_HINH:
                case Skill.PHAN_THAN:
//                case Skill.SUPER_TRAI_DAT:
//                case Skill.SUPER_NAMEC:
//                case Skill.SUPER_SAIYAN:
//                case Skill.GONG:
                    damage = damage * 2 / 3;
                    break;
            }

            // Nếu chết thì set die
            if (isDie()) {
                this.setDie(plAtt);
            }

            // Nếu skill giảm damage, đảo giá trị piercing
            boolean finalPiercing = switch (skillId) {
                case Skill.ANTOMIC, Skill.DEMON, Skill.DRAGON, Skill.GALICK, Skill.KAIOKEN, Skill.KAMEJOKO, Skill.MASENKO, Skill.DICH_CHUYEN_TUC_THOI, Skill.LIEN_HOAN, Skill.DE_TRUNG, Skill.MAKANKOSAPPO, Skill.QUA_CAU_KENH_KHI, Skill.TU_SAT, Skill.SUPER_KAME, Skill.LIEN_HOAN_CHUONG, Skill.MA_PHONG_BA, Skill.BIEN_HINH, Skill.PHAN_THAN ->
                    !piercing;
                default ->
                    piercing;
            };

            return super.injured(plAtt, damage, finalPiercing, isMobAttack);
        }

        // Trường hợp không có người tấn công
        return super.injured(plAtt, damage, piercing, isMobAttack);
    }
}
