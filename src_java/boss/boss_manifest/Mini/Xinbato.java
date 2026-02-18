package boss.boss_manifest.Mini;


import boss.Boss;
import boss.BossID;
import boss.BossManager;
import boss.BossStatus;
import static boss.BossType.XIN_BA_TO;
import boss.BossesData;
import java.util.ArrayList;
import java.util.List;
import map.Zone;
import nro.player.Player;
import services.func.ChangeMapService;
import utils.Logger;
import utils.Util;

public class Xinbato extends Boss {

    private long lastTimeDrop;
    private long st;
    private int timeLeave;
    private boolean Check = false;
    private long z1 = 0;
    private long lastTimRestPawn;

    public Xinbato() throws Exception {
        super(XIN_BA_TO,BossID.XINBATO_1 , BossesData.XINBATO_1);
    }

    @Override
    public void joinMap() {
        if (zoneFinal != null) {
            joinMapByZone(zoneFinal);
            this.changeStatus(BossStatus.CHAT_S);
            this.wakeupAnotherBossWhenAppear();
            this.z1 = 0;
            this.Check = false;
            return;
        }

        if (this.zone == null) {
            if (this.parentBoss != null) {
                this.zone = parentBoss.zone;
            } else if (this.lastZone == null) {
                this.zone = getMapJoin();
            } else {
                this.zone = this.lastZone;
            }
        }

        if (this.zone != null) {
            try {
                List<Zone> availableZones = new ArrayList<>();

                // Lọc các zone thỏa mãn điều kiện: số lượng người chơi <= 10 và không có boss
                for (Zone zone : this.zone.map.zones) {
                    if (zone.getNumOfPlayers() <= 10 && !BossManager.gI().checkBosses(zone, BossID.XINBATO_1)) {
                        availableZones.add(zone);
                    }
                }

                if (!availableZones.isEmpty()) {
                    // Random chọn một zone hợp lệ
                    int randomIndex = Util.nextInt(availableZones.size());
                    this.zone = availableZones.get(randomIndex);
                    ChangeMapService.gI().changeMap(this, this.zone, Util.nextInt(100, 500), this.zone.map.yPhysicInTop(this.location.x,
                            this.location.y - 24));
                    this.changeStatus(BossStatus.CHAT_S);
                    st = System.currentTimeMillis();
                    timeLeave = Util.nextInt(100000, 300000);
                } else {
                    this.leaveMapNew();
                    return;
                }
            } catch (Exception e) {
                Logger.error(this.data[0].getName() + ": Lỗi đang tiến hành REST\n");
                this.changeStatus(BossStatus.REST);
            }
        } else {
            Logger.error(this.data[0].getName() + ": Lỗi map đang tiến hành RESPAWN\n");
            this.changeStatus(BossStatus.RESPAWN);
        }
    }

    @Override
    public void chatM() {
        if (this.data[this.currentLevel].getTextM().length == 0) {
            return;
        }
        if (!Util.canDoWithTime(this.lastTimeChatM, this.timeChatM)) {
            return;
        }
        String textChat = this.data[this.currentLevel].getTextM()[Util.nextInt(0, this.data[this.currentLevel].getTextM().length - 1)];
        int prefix = Integer.parseInt(textChat.substring(1, textChat.lastIndexOf("|")));
        textChat = textChat.substring(textChat.lastIndexOf("|") + 1);
        this.chat(prefix, textChat);
        this.lastTimeChatM = System.currentTimeMillis();
        this.timeChatM = Util.nextInt(3000, 20000);
    }

    @Override
    public void active() {
        this.attack();
    }

    @Override
    public void autoLeaveMap() {
        if (Util.canDoWithTime(st, timeLeave)) {
            this.leaveMapNew();
        }
    }

    @Override
    public void leaveMap() {
        ChangeMapService.gI().exitMap(this);
        this.lastZone = null;
        this.lastTimeRest = System.currentTimeMillis();
        this.changeStatus(BossStatus.REST);

    }

    public void NhatXuong1() {
        Check = true;
        z1 = System.currentTimeMillis();

    }

    public boolean Check() {
        return Check;
    }

    @Override
public void attack() {
    if (Util.canDoWithTime(this.lastTimeAttack, 100)) {
        this.lastTimeAttack = System.currentTimeMillis();
        try {
            Player pl = getPlayerAttack();
            if (pl == null || pl.location == null) {
                return;
            }
            
            // Tính khoảng cách giữa boss và người chơi
            double distance = Util.getDistance(this, pl);
            
            // Thêm cơ hội trượt đòn dựa trên khoảng cách
            boolean isMissed = false;
            if (distance < 50) { // Boss rất gần người chơi
                isMissed = Util.isTrue(1, 2); // 50% cơ hội trượt (1 trong 2)
            } else if (distance < 100) { // Boss ở khoảng cách trung bình
                isMissed = Util.isTrue(1, 3); // 33% cơ hội trượt (1 trong 3)
            } else { // Boss xa người chơi
                isMissed = Util.isTrue(1, 5); // 20% cơ hội trượt (1 trong 5)
            }
            
            if (isMissed) {
                // Boss trượt đòn, có thể log hoặc xử lý nếu cần
                
                return; // Bỏ qua tấn công nếu bị trượt
            }
            
            // Nếu không trượt, tiếp tục với logic tấn công bình thường
            this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, this.playerSkill.skills.size() - 1));
            if (distance <= this.getRangeCanAttackWithSkillSelect()) {
                if (Util.isTrue(5, 20) && distance > 50) {
                    if (Util.isTrue(5, 20)) {
                        this.moveTo(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(20, 200)),
                                Util.nextInt(10) % 2 == 0 ? pl.location.y : pl.location.y - Util.nextInt(0, 70));
                    } else {
                        this.moveTo(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(10, 40)), pl.location.y);
                    }
                } else if (distance <= 50) {
                    // Thêm logic cho tấn công cự ly gần nếu cần
                }
                checkPlayerDie(pl);
            } else {
                if (Util.isTrue(1, 2)) {
                    this.moveToPlayer(pl);
                }
            }

            if (1 > 0) {
                if (Util.canDoWithTime(1, 5000)) {
                    z1 = 0;
                    Check = false;
                }
            }
        } catch (Exception ex) {
            // Xử lý lỗi nếu có
        }
    }
}


    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        return 0;
    }

    @Override
    public void reward(Player plKill) {
    }
}
