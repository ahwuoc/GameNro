package nro.services;

/*
 *
 *  Box ZALO: https://zalo.me/g/ifjict764
 *  SDT ZALO: 0358176187
 *  Chuyên chỉnh sửa, mua bán source NRO,...
 */
import consts.ConstPlayer;
import nro.player.NewPet;
import nro.player.Pet;
import nro.player.Player;
import services.func.ChangeMapService;
import utils.SkillUtil;
import utils.Util;

/**
 * Quản lý tạo, thay đổi, xóa, và chỉnh sửa Pet (Đệ tử)
 *
 * @author Mr
 */
public class PetService {

    private static PetService instance;

    public static PetService gI() {
        if (instance == null) {
            instance = new PetService();
        }
        return instance;
    }

    // ===========================================================
    // ======================= CREATE PET ========================
    // ===========================================================
    public void createNormalPet(Player player, int gender, byte... limitPower) {
        createPetThread(player, false, false, false, false, gender, "Xin hãy thu nhận tao làm đệ tử", limitPower);
    }

    public void createNormalPet(Player player, byte... limitPower) {
        createPetThread(player, false, false, false, false, player.gender, "Xin hãy thu nhận tao làm đệ tử", limitPower);
    }

    public void createMabuPet(Player player, int gender, byte... limitPower) {
        createPetThread(player, true, false, false, false, gender, "Oa oa oa...", limitPower);
    }

    public void createMabuPet(Player player, byte... limitPower) {
        createPetThread(player, true, false, false, false, player.gender, "Oa oa oa...", limitPower);
    }

    public void createUubPet(Player player, byte... limitPower) {
        createPetThread(player, false, true, false, false, player.gender, "Xin hãy thu nhận tao làm đệ tử", limitPower);
    }

    public void createKidBeerPet(Player player, byte... limitPower) {
        createPetThread(player, false, false, true, false, player.gender, "Hãy hợp tác với ta, Kakarot!", limitPower);
    }

    public void createJirenPet(Player player, byte... limitPower) {
        createPetThread(player, false, false, false, true, player.gender, "Xin hãy thu nhận tao làm đệ tử", limitPower);
    }

    private void createPetThread(Player player, boolean mabu, boolean uub, boolean kidBeer, boolean jiren,
            int gender, String chat, byte... limitPower) {
        Thread.startVirtualThread(() -> {
            try {
                createNewPet(player, mabu, uub, kidBeer, jiren, (byte) gender);
                if (limitPower != null && limitPower.length == 1) {
                    player.pet.nPoint.limitPower = limitPower[0];
                }
                Thread.sleep(1000);
                Service.gI().chatJustForMe(player, player.pet, chat);
            } catch (Exception ignored) {
            }
        });
    }

    // ===========================================================
    // ====================== CHANGE PET =========================
    // ===========================================================
    private void resetOldPet(Player player) {
        if (player.pet == null) {
            return;
        }
        byte limitPower = player.pet.nPoint.limitPower;
        if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
            player.pet.unFusion();
        }
        ChangeMapService.gI().exitMap(player.pet);
        player.pet.dispose();
        player.pet = null;
    }

    public void changeNormalPet(Player player, int gender) {
        byte limitPower = player.pet.nPoint.limitPower;
        resetOldPet(player);
        createNormalPet(player, gender, limitPower);
    }

    public void changeNormalPet(Player player) {
        byte limitPower = player.pet.nPoint.limitPower;
        resetOldPet(player);
        createNormalPet(player, limitPower);
    }

    public void changeMabuPet(Player player) {
        byte limitPower = player.pet.nPoint.limitPower;
        resetOldPet(player);
        createMabuPet(player, limitPower);
    }

    public void changeMabuPet(Player player, int gender) {
        byte limitPower = player.pet.nPoint.limitPower;
        resetOldPet(player);
        createMabuPet(player, gender, limitPower);
    }

    public void changeUubPet(Player player) {
        byte limitPower = player.pet.nPoint.limitPower;
        resetOldPet(player);
        createUubPet(player, player.pet.gender, limitPower);
    }

    public void changeKidBeerPet(Player player) {
        byte limitPower = player.pet.nPoint.limitPower;
        resetOldPet(player);
        createKidBeerPet(player, player.pet.gender, limitPower);
    }

    public void changeJirenPet(Player player) {
        byte limitPower = player.pet.nPoint.limitPower;
        resetOldPet(player);
        createJirenPet(player, player.pet.gender, limitPower);
    }

    // ===========================================================
    // ====================== DELETE PET =========================
    // ===========================================================
    public void deletePet(Player player) {
        if (player.pet != null) {
            if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
                player.pet.unFusion();
            }
            ChangeMapService.gI().exitMap(player.pet);
            player.pet.dispose();
            player.pet = null;
        }
    }

    // ===========================================================
    // ======================= RENAME PET ========================
    // ===========================================================
    public void changeNamePet(Player player, String name) {
        try {
            if (!InventoryService.gI().isExistItemBag(player, 400)) {
                Service.gI().sendThongBao(player, "Bạn cần thẻ đặt tên đệ tử, mua tại Santa");
                return;
            }
            if (Util.haveSpecialCharacter(name)) {
                Service.gI().sendThongBao(player, "Tên không được chứa ký tự đặc biệt");
                return;
            }
            if (name.length() > 10) {
                Service.gI().sendThongBao(player, "Tên quá dài");
                return;
            }

            ChangeMapService.gI().exitMap(player.pet);
            player.pet.name = "$" + name.toLowerCase().trim();
            InventoryService.gI().subQuantityItemsBag(player, InventoryService.gI().findItemBag(player, 400), 1);

            Thread.startVirtualThread(() -> {
                try {
                    Thread.sleep(1000);
                    Service.gI().chatJustForMe(player, player.pet, "Cảm ơn sư phụ đã đặt cho con tên " + name);
                } catch (Exception ignored) {
                }
            });

        } catch (Exception ignored) {
        }
    }

    // ===========================================================
    // ======================= PET DATA ==========================
    // ===========================================================
    private int[] getDataPetNormal() {
        return new int[]{Util.nextInt(40, 105) * 20, Util.nextInt(40, 105) * 20, Util.nextInt(20, 45),
            Util.nextInt(9, 50), Util.nextInt(0, 2)};
    }

    private int[] getDataPetMabu() {
        return new int[]{Util.nextInt(40, 105) * 20, Util.nextInt(40, 105) * 20, Util.nextInt(50, 120),
            Util.nextInt(9, 50), Util.nextInt(0, 2)};
    }

    private int[] getDataPetUub() {
        return new int[]{400_000, 400_000, 20_000, Util.nextInt(9, 50), Util.nextInt(0, 2)};
    }

    private int[] getDataPetKidBeer() {
        return new int[]{400_000, 400_000, 20_000, Util.nextInt(9, 50), Util.nextInt(0, 2)};
    }

    private int[] getDataPetJiren() {
        return new int[]{400_000, 400_000, 20_000, Util.nextInt(9, 50), Util.nextInt(0, 2)};
    }

    // ===========================================================
    // =================== CREATE NEW PET CORE ===================
    // ===========================================================
    private void createNewPet(Player player, boolean isMabu, boolean isUub, boolean isKidBeer, boolean isJiren, byte... gender) {
        int[] data = isMabu ? getDataPetMabu()
                : isUub ? getDataPetUub()
                        : isKidBeer ? getDataPetKidBeer()
                                : isJiren ? getDataPetJiren() : getDataPetNormal();

        Pet pet = new Pet(player);
        pet.name = "$" + (isMabu ? "Mabư" : isUub ? "Uub" : isKidBeer ? "Kid Beer" : isJiren ? "Kid Jiren" : "Đệ tử");
        pet.gender = (gender != null && gender.length != 0) ? gender[0] : (byte) Util.nextInt(0, 2);
        pet.id = player.isPl() ? -player.id : -Math.abs(player.id) - 100000;

        pet.nPoint.power = (isUub || isKidBeer || isJiren) ? 40_000_000_000L : isMabu ? 1_500_000L : 2000L;
        pet.typePet = (byte) (isMabu ? 1 : isUub ? 2 : isKidBeer ? 3 : isJiren ? 4 : 0);

        pet.nPoint.stamina = pet.nPoint.maxStamina = 1000;
        pet.nPoint.hpg = data[0];
        pet.nPoint.mpg = data[1];
        pet.nPoint.dameg = data[2];
        pet.nPoint.defg = data[3];
        pet.nPoint.critg = data[4];

        int itemBodySize = (pet.typePet >= 2) ? 9 : 7;
        for (int i = 0; i < itemBodySize; i++) {
            pet.inventory.itemsBody.add(ItemService.gI().createItemNull());
        }

        pet.playerSkill.skills.add(SkillUtil.createSkill(Util.nextInt(0, 2) * 2, 1));
        for (int i = 0; i < 6; i++) {
            pet.playerSkill.skills.add(SkillUtil.createEmptySkill());
        }

        pet.nPoint.setFullHpMp();
        player.pet = pet;
    }

    // ===========================================================
    // ==================== PET 2 (NEWPET) =======================
    // ===========================================================
    public static void Pet2(Player pl, int h, int b, int l) {
        if (pl.newPet != null) {
            pl.newPet.dispose();
        }

        pl.newPet = new NewPet(pl, (short) h, (short) b, (short) l);
        pl.newPet.name = "$";
        pl.newPet.gender = pl.gender;

        pl.newPet.nPoint.tiemNang = 1;
        pl.newPet.nPoint.power = 1;
        pl.newPet.nPoint.limitPower = 1;
        pl.newPet.nPoint.hpg = 500_000_000;
        pl.newPet.nPoint.mpg = 500_000_000;
        pl.newPet.nPoint.hp = 500_000_000;
        pl.newPet.nPoint.mp = 500_000_000;
        pl.newPet.nPoint.dameg = 1;
        pl.newPet.nPoint.defg = 1;
        pl.newPet.nPoint.critg = 1;
        pl.newPet.nPoint.stamina = 1;

        pl.newPet.nPoint.setBasePoint();
        pl.newPet.nPoint.setFullHpMp();
    }
}
