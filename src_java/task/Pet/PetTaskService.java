
package task.Pet;

import boss.Boss;
import boss.BossID;
import consts.ConstMob;
import consts.ConstTask;
import java.util.List;
import mob.Mob;
import player.Pet;
import player.Player;
import server.Manager;
import services.Service;

/**
 *
 * @author ducpro
 */
public class PetTaskService {
    public static void checkDoneTaskKillBoss(Player player, Boss boss) {
        if (player != null && player.PetTask != null) {
            if ((player.PetTask.id == 10 && boss.id == BossID.KUKU)
                    || (player.PetTask.id == 11 && boss.id == BossID.MAP_DAU_DINH)
                    || (player.PetTask.id == 12 && boss.id == BossID.RAMBO)) {
                player.PetTask.count++;
                if (PetTaskService.isDoneTask(player)) {
                    Service.gI().sendThongBao(player,
                            "Chúc Mừng Đệ ngươi đã hoàn Thành nhiệm vụ " + PetTaskService.nameTask(player.PetTask.id));
                }
            }

        }
    }

    public static void checkTaskKillMob(Player player, Mob mob) {
        if (player.PetTask != null) {
            if ((player.PetTask.id == 1 && (mob.tempId >= 1 && mob.tempId <= 9))
                    || (player.PetTask.id == 2 && (mob.tempId >= 15 && mob.tempId <= 17))
                    || (player.PetTask.id == 4 && (mob.tempId == ConstMob.HEO_RUNG_ME))
                    || (player.PetTask.id == 5 && (mob.tempId >= 25 && mob.tempId <= 27))
                    || (player.PetTask.id == 6 && (mob.tempId == ConstMob.NAPPA))
                    || (player.PetTask.id == 7 && (mob.tempId == ConstMob.APPULE))
                    || (player.PetTask.id == 8 && (mob.tempId == ConstMob.THAN_LAN_XANH))) {
                player.PetTask.count++;
                if (PetTaskService.isDoneTask(player)) {
                    Service.gI().sendThongBao(player,
                            "Chúc Mừng Đệ ngươi đã hoàn Thành nhiệm vụ " + PetTaskService.nameTask(player.PetTask.id));
                }

            }
        }

    }

    public static void checkDoneTaskPowerPet(Player player, long power) {
        if (player.isDeTu) {
            if ((power >= 1_500_000 && ((Pet) player).master.PetTask.id == 3)
                    || (power >= 1_000_000_000 && ((Pet) player).master.PetTask.id == 9)
                    || (power >= 5_000_000_000L && ((Pet) player).master.PetTask.id == 13)) {
                ((Pet) player).master.PetTask.count++;
                if (PetTaskService.isDoneTask(((Pet) player).master)) {
                    Service.gI().sendThongBao(player, "Chúc Mừng Đệ ngươi đã hoàn Thành nhiệm vụ "
                            + PetTaskService.nameTask(((Pet) player).master.PetTask.id));
                }
            }

        }
    }

    // xử lý update task
    public static void updateCountPetTask(Player player, int id, int amount) {
        if (player.PetTask.id == id) {
            player.PetTask.count += amount;
            if (player.PetTask.count > player.PetTask.countMax) {
                player.PetTask.count = player.PetTask.countMax;
            }
        }
    }

    // Lấy name nhiệm vụ
    public static String nameTask(int id) {
        List<petTaskTemplate> templates = Manager.PET_TASKS_TEMPLATE;
        petTaskTemplate taskTemplate = null;
        for (petTaskTemplate template : templates) {
            if (template.getId() == id) {
                taskTemplate = template;
                break;
            }
        }
        if (taskTemplate == null) {
            return "Hãy Đợi Cập Nhật..";

        }
        return taskTemplate.getName();
    }

    public static int Reward(int id) {
        List<petTaskTemplate> templates = Manager.PET_TASKS_TEMPLATE;
        petTaskTemplate taskTemplate = null;
        for (petTaskTemplate template : templates) {
            if (template.getId() == id) {
                taskTemplate = template;
                break;
            }
        }
        if (taskTemplate == null) {
            return 0;

        }
        return taskTemplate.getReward();
    }

    // check Done
    public static boolean isDoneTask(Player player) {
        if (player.PetTask.count >= player.PetTask.countMax && player.PetTask.countMax > 0) {
            return true;
        }
        return false;
    }

    // Cập nhật nhiệm vụ
    public static void UpdateTask(Player player, int id) {
        if (player == null || player.PetTask == null)
            return;
        List<petTaskTemplate> templates = Manager.PET_TASKS_TEMPLATE;
        petTaskTemplate taskTemplate = null;
        for (petTaskTemplate template : templates) {
            if (template.getId() == id) {
                taskTemplate = template;
                break;
            }
        }
        if (taskTemplate == null) {
            return;
        }
        player.PetTask.template = taskTemplate;
        player.PetTask.setCount(0); // Đặt lại count
        player.PetTask.setCountMax(taskTemplate.getMaxcount());

    }
}
