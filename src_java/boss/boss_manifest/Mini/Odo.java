package boss.boss_manifest.Mini;

import boss.Boss;
import boss.BossStatus;
import static boss.BossType.O_DO_NEW;
import boss.BossesData;
import consts.ConstTaskBadges;
import event.EventManager;
import item.Item;
import item.Item.ItemOption;
import map.ItemMap;
import nro.player.Player;
import nro.services.PlayerService;
import nro.services.Service;
import task.Badges.BadgesTaskService;
import utils.Util;

public class Odo extends Boss {

    public Odo() throws Exception {
        super(O_DO_NEW,-Util.nextInt(1000, 1000000), true, true, BossesData.O_DO_NEW);
    }

    private static final String[] textOdo = new String[]{
        "Hôi quá, tránh xa ta ra", "Biến đi", "Trời ơi đồ ở dơ",
        "Thúi quá", "Mùi gì hôi quá"
    };
    private long lastTimeOdo;

    public void subHpWithOdo() {
        try {
            if (this.nPoint != null) {
                if (Util.canDoWithTime(lastTimeOdo, 5000)) {
                    for (int i = this.zone.getNotBosses().size() - 1; i >= 0; i--) {
                        Player pl = this.zone.getNotBosses().get(i);
                        if (pl != null && pl.nPoint != null && !pl.isDie()) {
                            int subHp = (int) ((long) pl.nPoint.hpMax * 5 / 100);
                            if (subHp >= pl.nPoint.hp) {
                                subHp = (int) (pl.nPoint.hp - 1);
                            }
                            Service.gI().chat(pl, textOdo[Util.nextInt(0, textOdo.length - 1)]);
                            PlayerService.gI().sendInfoHpMpMoney(pl);
                            Service.gI().Send_Info_NV(pl);
                            pl.injured(null, subHp, true, false);
                        }
                    }
                    this.lastTimeOdo = System.currentTimeMillis();
                    this.changeStatus(BossStatus.CHAT_S);
                }
            }
        } catch (Exception e) {
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
        subHpWithOdo();
    }

    @Override
    public void attack() {
        if (Util.canDoWithTime(this.lastTimeAttack, 100)) {  // Kiểm tra thời gian hồi chiêu của boss
            this.lastTimeAttack = System.currentTimeMillis();

            try {
                Player pl = getPlayerAttack();  // Lấy người chơi đang tấn công boss
                if (pl == null || pl.location == null) {
                    return;  // Nếu không có người chơi hoặc người chơi không có vị trí, bỏ qua
                }

                // Chọn một kỹ năng ngẫu nhiên từ danh sách kỹ năng của boss
                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, this.playerSkill.skills.size() - 1));

                // Kiểm tra xem người chơi có trong phạm vi tấn công của boss không
                if (Util.getDistance(this, pl) <= this.getRangeCanAttackWithSkillSelect()) {
                    // Nếu người chơi trong phạm vi, boss sẽ tấn công người chơi
                    attackPlayer(pl);
                } else {
                    // Nếu người chơi quá xa, boss sẽ di chuyển lại gần người chơi
                    if (Util.isTrue(1, 2)) {
                        this.moveToPlayer(pl);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();  // Bắt lỗi và ghi lại log
            }
        }
    }

    public void attackPlayer(Player pl) {
        if (pl == null) {
            return;  // Nếu không có người chơi, bỏ qua
        }
        // Giá trị sát thương ví dụ, bạn có thể điều chỉnh lại sao cho phù hợp
        long damage = Util.nextInt(50, 100);  // Sát thương ngẫu nhiên từ 50 đến 100

        // Áp dụng sát thương lên người chơi
        pl.injured(this, damage, true, true);  // Gọi phương thức injured của người chơi để nhận sát thương
        Service.gI().chat(pl, "Ở dơ tấn công bạn bằng một đòn tấn công hôi thối!");  // Gửi tin nhắn đến người chơi
    }

    public void die(Player killer) {
        this.changeStatus(BossStatus.DIE);
    }

    @Override
    public void reward(Player plKill) {
        // Rơi một số item ngẫu nhiên làm phần thưởng cho người chơi khi giết boss
        int[] itemne = {441, 442, 443, 444, 445, 446, 447, 459};
        Service.gI().dropItemMap(this.zone, Util.saoPhaLe(zone, Util.isTrue(95, 100) ? itemne[Util.nextInt(itemne.length - 1)] : itemne[itemne.length - 1], 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x, this.location.y), -1));

        if (Util.isTrue(1, 5)) {
            ItemMap item = new ItemMap(zone, 927, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), -1);
            item.options.add(new ItemOption(87, 0));
        }
        if (Util.isTrue(1, 10)) {
            ItemMap item2 = new ItemMap(zone, 926, 1, this.location.x + Util.nextInt(50), this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), -1);
            item2.options.add(new ItemOption(87, 0));
        }
        if (EventManager.LUNAR_NEW_YEAR) {
            ItemMap itemTet = new ItemMap(this.zone, 1194, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id);
            if (itemTet.itemTemplate != null) {
                Service.gI().dropItemMap(this.zone, itemTet);
                itemTet.options.add(new Item.ItemOption(30, 0));
                itemTet.options.add(new Item.ItemOption(93, 30));
                itemTet.options.add(new Item.ItemOption(174, 2026));
            }
        }
        BadgesTaskService.updateCountBagesTask(plKill, ConstTaskBadges.O_DO, 1);  // Cập nhật nhiệm vụ sau khi giết boss
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {  // Kiểm tra xem boss đã chết chưa
            // Áp dụng sát thương lên boss (có thể thay đổi cách tính sát thương tại đây nếu cần)
            this.nPoint.subHP(damage);

            if (this.isDie()) {
                // Nếu HP của boss về 0, đánh dấu boss là đã chết và thực hiện logic chết
                this.setDie(plAtt);
                die(plAtt);  // Gọi phương thức die để xử lý cái chết của boss
            }

            return (int) damage;  // Trả về sát thương đã áp dụng lên boss
        } else {
            return 0;  // Nếu boss đã chết, không áp dụng sát thương
        }
    }
}
