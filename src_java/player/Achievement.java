package player;

import consts.ConstAchievement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import models.Template.AchievementQuest;
import models.Template.AchievementTemplate;
import server.Manager;

public class Achievement {

    private Player player;

    @Getter
    private List<AchievementQuest> achievementList;

    @Getter
    private Set<Integer> receivedTieuTienMilestones = new HashSet<>();

    public Achievement(Player player) {
        this.player = player;
        this.achievementList = new ArrayList<>();
    }

    public void add(AchievementQuest achievement) {
        this.achievementList.add(achievement);
    }

    public AchievementQuest get(int index) {
        return index >= 0 && index < achievementList.size() ? achievementList.get(index) : null;
    }

    public long getCompleted(int index) {
        AchievementQuest aq = get(index);
        if (aq != null) {
            switch (index) {
                case 0, 1, 16 -> {
                    aq.completed = player.nPoint.power;
                }
                case 2 -> {
                    aq.completed = player.magicTree.level;
                }
                case ConstAchievement.HOAT_DONG_CHAM_CHI -> {
                    return aq.completed / (60 * 60 * 1000);
                }
            }
            return aq.completed;
        }
        return 0;
    }

    public boolean isFinish(int index, long maxCount) {
        AchievementQuest aq = get(index);
        return aq != null && (aq.isRecieve || getCompleted(index) >= maxCount);
    }

    public boolean isRecieve(int index) {
        AchievementQuest aq = get(index);
        return aq != null && aq.isRecieve;
    }

    public boolean canReward(int index) {
        AchievementQuest aq = get(index);
        AchievementTemplate at = Manager.ACHIEVEMENT_TEMPLATE.get(index);
        return aq != null && !aq.isRecieve && getCompleted(index) >= at.maxCount;
    }

    public void done(int index, long completed) {
        if (index >= 0 && index < achievementList.size()) {
            achievementList.set(index, new AchievementQuest(get(index).completed + completed, get(index).isRecieve));
        }
    }

    public void doneNotAdd(int index, long completed) {
        if (index >= 0 && index < achievementList.size()) {
            achievementList.set(index, new AchievementQuest(completed, get(index).isRecieve));
        }
    }

    public void reward(int index) {
        if (index >= 0 && index < achievementList.size()) {
            achievementList.set(index, new AchievementQuest(get(index).completed, true));
        }
    }

    public void dispose() {
        if (achievementList != null) {
            achievementList.clear();
            achievementList = null;
        }
        if (receivedTieuTienMilestones != null) {
            receivedTieuTienMilestones.clear();
            receivedTieuTienMilestones = null;
        }
        player = null;
    }

    public boolean isRecieveTieuTienMilestone(int amount) {
        return receivedTieuTienMilestones.contains(amount);
    }

    public void receiveTieuTienMilestone(int amount) {
        receivedTieuTienMilestones.add(amount);
        if (player != null) {
            player.achievementTieuTien = getTieuTienMilestonesData();
        }
    }

    public String getTieuTienMilestonesData() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Integer m : receivedTieuTienMilestones) {
            if (!first)
                sb.append(",");
            sb.append(m);
            first = false;
        }
        return sb.toString();
    }

    public void loadTieuTienMilestonesData(String data) {
        receivedTieuTienMilestones.clear();
        if (data == null || data.isEmpty()) {
            return;
        }
        String[] milestones = data.split(",");
        for (String m : milestones) {
            try {
                receivedTieuTienMilestones.add(Integer.parseInt(m.trim()));
            } catch (NumberFormatException e) {
            }
        }
    }

}
