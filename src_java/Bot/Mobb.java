/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Bot;

import java.util.Random;
import mob.Mob;
import nro.services.PlayerService;
import nro.services.SkillService;
import utils.Util;

/**
 * AI cho bot thường (đánh quái, đổi map)
 * 
 * @author Administrator
 */
public class Mobb {

    private Mob mAttack;          // mục tiêu hiện tại
    public long lastTimeChanM;    // thời gian lần cuối đổi map
    public Bot bot;               // bot chủ

    public Mobb(Bot b) {
        this.bot = b;
    }

    public void update() {
        attack();
        changeMap();
    }

    // Chọn mob để tấn công
    private void getMobAttack() {
        if (bot.zone != null && bot.zone.mobs.size() >= 1) {
            if (mAttack == null || mAttack.isDie()) {
                mAttack = bot.zone.mobs.get(new Random().nextInt(bot.zone.mobs.size()));
            }
        }
    }

    // Bot tấn công mob
    private void attack() {
        getMobAttack();
        if (mAttack != null) {
            // chọn skill
            if (Util.isTrue(50, 100)) {
                bot.playerSkill.skillSelect = bot.playerSkill.skills.get(0);
            } else {
                bot.playerSkill.skillSelect = bot.playerSkill.skills.get(1);
            }

            // di chuyển và đánh
            if (bot.UseLastTimeSkill()) {
                PlayerService.gI().playerMove(bot, mAttack.location.x, mAttack.location.y);
                SkillService.gI().useSkill(bot, null, mAttack, -1, null);
            }
        }
    }

    // Bot đổi map sau một khoảng thời gian ngẫu nhiên
    private void changeMap() {
        if (lastTimeChanM < (System.currentTimeMillis() - 150000) - new Random().nextInt(150000)) {
            bot.joinMap();
            lastTimeChanM = System.currentTimeMillis();
        }
    }
}
