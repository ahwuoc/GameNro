package Bot;

import mob.Mob;
import nro.player.Player;
import nro.services.PlayerService;
import nro.services.Service;
import nro.services.SkillService;
import utils.Util;

public class DeTuAI {
    private final Player pet;

    public DeTuAI(Player pet) {
        this.pet = pet;
    }

    public void update() {
        if (pet == null) return;

        // Nếu chết thì hồi sinh
        if (pet.isDie()) {
            Service.gI().hsChar(pet, pet.nPoint.hpMax, pet.nPoint.mpMax);
            return;
        }

        // Nếu zone chưa có thì bỏ qua
        if (pet.zone == null) {
            return;
        }

        // Follow theo chủ (chỉ khi cùng zone)
        if (pet.master != null && pet.master.zone == pet.zone) {
            int dx = Math.abs(pet.location.x - pet.master.location.x);
            int dy = Math.abs(pet.location.y - pet.master.location.y);

            PlayerService.gI().playerMove(pet,
                    pet.master.location.x + Util.nextInt(-30, 30),
                    pet.master.location.y);
        }

        // Tìm quái gần nhất
        Mob mob = null;
        if (pet.zone.mobs != null && !pet.zone.mobs.isEmpty()) {
            mob = pet.zone.mobs.stream()
                    .filter(m -> !m.isDie())
                    .min((a, b) -> Integer.compare(
                            Util.getDistance(pet, a),
                            Util.getDistance(pet, b)))
                    .orElse(null);
        }

        // Đánh mob
        if (mob != null && pet.UseLastTimeSkill()) {
            if (pet.playerSkill != null && !pet.playerSkill.skills.isEmpty()) {
                pet.playerSkill.skillSelect = pet.playerSkill.skills.get(0);
                SkillService.gI().useSkill(pet, null, null, -1, null);

                // Train chỉ số
                trainPet();
            }
        }
    }

    private void trainPet() {
        // Cộng tiềm năng khi đánh
        pet.nPoint.tiemNang += 1000;
        if (pet.nPoint.tiemNang >= 5000) {
            pet.nPoint.tiemNang = 0;
            pet.nPoint.dameg += 5;
            pet.nPoint.hpMax += 50;
            Service.gI().point(pet);
        }
    }
}
