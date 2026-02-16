package services;

import consts.ConstPlayer;
import player.NewPet;
import player.Pet;
import player.Player;
import services.func.ChangeMapService;
import utils.SkillUtil;
import utils.Util;

public class PetService {

    private static PetService instance;

    public static PetService gI() {
        if (instance == null) {
            instance = new PetService();
        }
        return instance;
    }

    /**
     * Method chung để tạo pet mới
     * 
     * @param player  Player cần tạo pet
     * @param typePet Loại pet: 0=Normal, 1=Mabu, 2=Beerus, 3=Pic, 4=Black
     */
    public void createPet(Player player, byte typePet) {
        createPet(player, typePet, (byte) -1, (byte) 1);
    }

    /**
     * Method chung để tạo pet mới với gender
     * 
     * @param player  Player cần tạo pet
     * @param typePet Loại pet: 0=Normal, 1=Mabu, 2=Beerus, 3=Pic, 4=Black
     * @param gender  Giới tính pet (-1 = random)
     */
    public void createPet(Player player, byte typePet, byte gender) {
        createPet(player, typePet, gender, (byte) 1);
    }

    /**
     * Method chung để tạo pet mới với gender và limitPower
     * 
     * @param player     Player cần tạo pet
     * @param typePet    Loại pet: 0=Normal, 1=Mabu, 2=Beerus, 3=Pic, 4=Black
     * @param gender     Giới tính pet (-1 = random)
     * @param limitPower Giới hạn sức mạnh
     */
    public void createPet(Player player, byte typePet, byte gender, byte limitPower) {
        new Thread(() -> {
            try {
                boolean isMabu = typePet == 1;
                boolean isBeerus = typePet == 2;
                boolean isPic = typePet == 3;
                boolean isBlack = typePet == 4;

                if (gender >= 0) {
                    createNewPet(player, isMabu, isBeerus, isPic, isBlack, gender);
                } else {
                    createNewPet(player, isMabu, isBeerus, isPic, isBlack);
                }

                if (limitPower > 0) {
                    player.pet.nPoint.limitPower = limitPower;
                    player.pet.nPoint.initPowerLimit();
                }

                Thread.sleep(1000);
                String msg = getPetMessage(typePet);
                Service.gI().chatJustForMe(player, player.pet, msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String getPetMessage(byte typePet) {
        return switch (typePet) {
            case 1 -> "Oa oa oa...";
            case 2 -> "Black goku đây quỳ mẹ mày xuống!!!...";
            case 3 -> "Sư Phụ SooMe hiện thân tụi m quỳ xuống...";
            case 4 -> "Ta sẽ cho người biết sức mạnh của một vị thần là như thế nào !";
            default -> "Xin hãy thu nhận làm đệ tử";
        };
    }

    // Các method cũ giữ lại để tương thích ngược
    public void createNormalPet(Player player, int gender, byte... limitPower) {
        createPet(player, (byte) 0, (byte) gender, limitPower != null && limitPower.length > 0 ? limitPower[0] : 1);
    }

    public void createNormalPet(Player player, byte... limitPower) {
        createPet(player, (byte) 0, (byte) -1, limitPower != null && limitPower.length > 0 ? limitPower[0] : 1);
    }

    public void createMabuPet(Player player, byte... limitPower) {
        createPet(player, (byte) 1, (byte) -1, limitPower != null && limitPower.length > 0 ? limitPower[0] : 1);
    }

    public void createMabuPet(Player player, int gender, byte... limitPower) {
        createPet(player, (byte) 1, (byte) gender, limitPower != null && limitPower.length > 0 ? limitPower[0] : 1);
    }

    public void createBeerusPet(Player player, byte... limitPower) {
        createPet(player, (byte) 2, (byte) -1, limitPower != null && limitPower.length > 0 ? limitPower[0] : 1);
    }

    public void createBeerusPet(Player player, int gender, byte... limitPower) {
        createPet(player, (byte) 2, (byte) gender, limitPower != null && limitPower.length > 0 ? limitPower[0] : 1);
    }

    public void createPicPet(Player player, byte... limitPower) {
        createPet(player, (byte) 3, (byte) -1, limitPower != null && limitPower.length > 0 ? limitPower[0] : 1);
    }

    public void createPicPet(Player player, int gender, byte... limitPower) {
        createPet(player, (byte) 3, (byte) gender, limitPower != null && limitPower.length > 0 ? limitPower[0] : 1);
    }

    public void createBlackPet(Player player, byte... limitPower) {
        createPet(player, (byte) 4, (byte) -1, limitPower != null && limitPower.length > 0 ? limitPower[0] : 1);
    }

    public void createBlackPet(Player player, int gender, byte... limitPower) {
        createPet(player, (byte) 4, (byte) gender, limitPower != null && limitPower.length > 0 ? limitPower[0] : 1);
    }

    public void changeNormalPet(Player player, int gender) {
        changePet(player, (byte) 0, (byte) gender);
    }

    public void changeNormalPet(Player player) {
        changePet(player, (byte) 0);
    }

    public void changeMabuPet(Player player) {
        changePet(player, (byte) 1);
    }

    public void changeMabuPet(Player player, int gender) {
        changePet(player, (byte) 1, (byte) gender);
    }

    public void changeBeerusPet(Player player) {
        changePet(player, (byte) 2);
    }

    public void changeBeerusPet(Player player, int gender) {
        changePet(player, (byte) 2, (byte) gender);
    }

    public void changePicPet(Player player) {
        changePet(player, (byte) 3);
    }

    public void changePicPet(Player player, int gender) {
        changePet(player, (byte) 3, (byte) gender);
    }

    public void changeBlackPet(Player player) {
        changePet(player, (byte) 4);
    }

    public void changeBlackPet(Player player, int gender) {
        changePet(player, (byte) 4, (byte) gender);
    }

    /**
     * Method chung để đổi pet
     * 
     * @param player  Player cần đổi pet
     * @param typePet Loại pet: 0=Normal, 1=Mabu, 2=Beerus, 3=Pic, 4=Black
     */
    public void changePet(Player player, byte typePet) {
        changePet(player, typePet, (byte) -1);
    }

    /**
     * Method chung để đổi pet với gender
     * 
     * @param player  Player cần đổi pet
     * @param typePet Loại pet: 0=Normal, 1=Mabu, 2=Beerus, 3=Pic, 4=Black
     * @param gender  Giới tính pet (-1 = random)
     */
    public void changePet(Player player, byte typePet, byte gender) {
        byte limitPower = player.pet != null ? player.pet.nPoint.limitPower : 1;
        if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {
            player.pet.unFusion();
        }
        if (player.pet != null) {
            ChangeMapService.gI().exitMap(player.pet);
            player.pet.dispose();
            player.pet = null;
        }

        boolean isMabu = typePet == 1;
        boolean isBeerus = typePet == 2;
        boolean isPic = typePet == 3;
        boolean isBlack = typePet == 4;

        if (gender >= 0) {
            createNewPetWithType(player, isMabu, isBeerus, isPic, isBlack, gender, limitPower);
        } else {
            createNewPetWithType(player, isMabu, isBeerus, isPic, isBlack, (byte) -1, limitPower);
        }
    }

    private void createNewPetWithType(Player player, boolean isMabu, boolean isBeerus, boolean isPic, boolean isBlack,
            byte gender, byte limitPower) {
        new Thread(() -> {
            try {
                if (gender >= 0) {
                    createNewPet(player, isMabu, isBeerus, isPic, isBlack, gender);
                } else {
                    createNewPet(player, isMabu, isBeerus, isPic, isBlack);
                }
                player.pet.nPoint.limitPower = limitPower;
                player.pet.nPoint.initPowerLimit();
                Thread.sleep(1000);
                String msg = isMabu ? "Oa oa oa..."
                        : isBeerus ? "Black goku đây quỳ mẹ mày xuống!!!..."
                                : isPic ? "Sư Phụ SooMe hiện thân tụi m quỳ xuống..."
                                        : isBlack ? "Ta sẽ cho người biết sức mạnh của một vị thần là như thế nào !"
                                                : "Xin hãy thu nhận làm đệ tử";
                Service.gI().chatJustForMe(player, player.pet, msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void changeNamePet(Player player, String name) {
        try {
            if (!InventoryService.gI().isExistItemBag(player, 400)) {
                Service.gI().sendThongBao(player, "Bạn cần thẻ đặt tên đệ tử, mua tại Santa");
                return;
            } else if (Util.haveSpecialCharacter(name)) {
                Service.gI().sendThongBao(player, "Tên không được chứa ký tự đặc biệt");
                return;
            } else if (name.length() > 10) {
                Service.gI().sendThongBao(player, "Tên quá dài");
                return;
            }
            ChangeMapService.gI().exitMap(player.pet);
            player.pet.name = "$" + name.toLowerCase().trim();
            InventoryService.gI().subQuantityItemsBag(player, InventoryService.gI().findItemBag(player, 400), 1);
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    Service.gI().chatJustForMe(player, player.pet, "Cảm ơn sư phụ đã đặt cho con tên " + name);
                } catch (Exception e) {
                }
            }).start();
        } catch (Exception ex) {

        }
    }

    private int[] getDataPetNormal() {
        int[] petData = new int[5];
        petData[0] = Util.nextInt(40, 105) * 20; // hp
        petData[1] = Util.nextInt(40, 105) * 20; // mp
        petData[2] = Util.nextInt(20, 45); // dame
        petData[3] = Util.nextInt(9, 50); // def
        petData[4] = Util.nextInt(0, 2); // crit
        return petData;
    }

    private int[] getDataPetMabu() {
        int[] petData = new int[5];
        petData[0] = Util.nextInt(40, 105) * 20; // hp
        petData[1] = Util.nextInt(40, 105) * 20; // mp
        petData[2] = Util.nextInt(50, 120); // dame
        petData[3] = Util.nextInt(9, 50); // def
        petData[4] = Util.nextInt(0, 2); // crit
        return petData;
    }

    private int[] getDataPetPic() {
        int[] petData = new int[5];
        petData[0] = Util.nextInt(40, 115) * 20; // hp
        petData[1] = Util.nextInt(40, 115) * 20; // mp
        petData[2] = Util.nextInt(70, 140); // dame
        petData[3] = Util.nextInt(9, 50); // def
        petData[4] = Util.nextInt(0, 2); // crit
        return petData;
    }

    public void createPetFideNhi(Player player, boolean isChange, byte gender) {// zl 0822992003 Đức dz
        byte limitPower;
        if (isChange) {
            limitPower = player.pet.nPoint.limitPower;
            if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {// zl 0822992003 Đức dz
                player.pet.unFusion();
            }
            ChangeMapService.gI().exitMap(player.pet);
            player.pet.dispose();
            player.pet = null;
        } else {// zl 0822992003 Đức dz
            limitPower = 1;
        }
        new Thread(() -> {// Zzl 0822992003 Đức dz
            try {// zl 0822992003 Đức dz
                Pet pet = new Pet(player);
                pet.name = "$Fide Nhí";
                pet.gender = gender;
                pet.id = -player.id;
                pet.nPoint.power = 1500000;
                pet.typePet = 5;
                pet.nPoint.stamina = (short) 1000;
                pet.nPoint.maxStamina = (short) 1000;
                pet.nPoint.hpg = Util.nextInt(2000, 5000);
                pet.nPoint.mpg = Util.nextInt(2000, 5000);
                pet.nPoint.hpMax = Util.nextInt(2000, 5000);
                pet.nPoint.mpMax = Util.nextInt(2000, 5000);
                pet.nPoint.dameg = Util.nextInt(200, 300);
                pet.nPoint.defg = Util.nextInt(10, 30);
                pet.nPoint.critg = 5;
                for (int i = 0; i < 9; i++) {
                    pet.inventory.itemsBody.add(ItemService.gI().createItemNull());
                }
                int skillId[] = { 9, 4, 17 };
                pet.playerSkill.skills.add(SkillUtil.createSkill(skillId[Util.nextInt(0, 2)], 1));
                for (int i = 0; i < 3; i++) {// zl 0822992003 Đức dz
                    pet.playerSkill.skills.add(SkillUtil.createEmptySkill());
                }
                pet.nPoint.setFullHpMp();
                player.pet = pet;
                player.pet.nPoint.limitPower = limitPower;
                player.pointfusion.setHpFusion(Util.nextInt(20, 30));
                player.pointfusion.setMpFusion(Util.nextInt(20, 30));
                player.pointfusion.setDameFusion(Util.nextInt(20, 30));
                Thread.sleep(1000);
                Service.gI().chatJustForMe(player, player.pet, "\b|1|Con đây sư phụ ơi!!!");
            } catch (Exception e) {// zl 0822992003 Đức dz
                e.printStackTrace();
            }
        }).start();
    }

    public void createPetCellNhi(Player player, boolean isChange, byte gender) {// zl 0822992003 Đức dz
        byte limitPower;
        if (isChange) {// zl 0822992003 Đức dz
            limitPower = player.pet.nPoint.limitPower;
            if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {// zl 0822992003 Đức dz
                player.pet.unFusion();
            }
            ChangeMapService.gI().exitMap(player.pet);
            player.pet.dispose();
            player.pet = null;
        } else {// zl 0822992003 Đức dz
            limitPower = 1;
        }
        new Thread(() -> {// zl 0822992003 Đức dz
            try {// zl 0822992003 Đức dz
                Pet pet = new Pet(player);
                pet.name = "$Cell Nhí";
                pet.gender = gender;
                pet.id = -player.id;
                pet.nPoint.power = 1500000;
                pet.typePet = 6;
                pet.nPoint.stamina = (short) 1000;
                pet.nPoint.maxStamina = (short) 1000;
                pet.nPoint.hpg = Util.nextInt(2000, 5000);
                pet.nPoint.mpg = Util.nextInt(2000, 5000);
                pet.nPoint.hpMax = Util.nextInt(2000, 5000);
                pet.nPoint.mpMax = Util.nextInt(2000, 5000);
                pet.nPoint.dameg = Util.nextInt(200, 300);
                pet.nPoint.defg = Util.nextInt(25, 50);
                pet.nPoint.critg = 5;
                for (int i = 0; i < 9; i++) {
                    pet.inventory.itemsBody.add(ItemService.gI().createItemNull());
                }
                int skillId[] = { 9, 4, 17 };
                pet.playerSkill.skills.add(SkillUtil.createSkill(skillId[Util.nextInt(0, 2)], 1));
                for (int i = 0; i < 3; i++) {// zl 0822992003 Đức dz
                    pet.playerSkill.skills.add(SkillUtil.createEmptySkill());
                }
                pet.nPoint.setFullHpMp();
                player.pet = pet;
                player.pet.nPoint.limitPower = limitPower;
                player.pointfusion.setHpFusion(Util.nextInt(25, 45));
                player.pointfusion.setMpFusion(Util.nextInt(25, 45));
                player.pointfusion.setDameFusion(Util.nextInt(25, 45));
                Thread.sleep(1000);
                Service.gI().chatJustForMe(player, player.pet, "\b|1|Con đây sư phụ ơi!!!");
            } catch (Exception e) {// zl 0822992003 Đức dz
                e.printStackTrace();
            }
        }).start();
    }

    public void createPetBuuNhi(Player player, boolean isChange, byte gender) {// zl 0822992003 Đức dz
        byte limitPower;
        if (isChange) {// zl 0822992003 Đức dz
            limitPower = player.pet.nPoint.limitPower;
            if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {// zl 0822992003 Đức dz
                player.pet.unFusion();
            }
            ChangeMapService.gI().exitMap(player.pet);
            player.pet.dispose();
            player.pet = null;
        } else {// zl 0822992003 Đức dz
            limitPower = 1;
        }
        new Thread(() -> {// zl 0822992003 Đức dz
            try {// zl 0822992003 Đức dz
                Pet pet = new Pet(player);
                pet.name = "$Bưu Nhí";
                pet.gender = gender;
                pet.id = -player.id;
                pet.nPoint.power = 1500000;
                pet.typePet = 7;
                pet.nPoint.stamina = (short) 1000;
                pet.nPoint.maxStamina = (short) 1000;
                pet.nPoint.hpg = Util.nextInt(2000, 5000);
                pet.nPoint.mpg = Util.nextInt(2000, 5000);
                pet.nPoint.hpMax = Util.nextInt(2000, 5000);
                pet.nPoint.mpMax = Util.nextInt(2000, 5000);
                pet.nPoint.dameg = Util.nextInt(200, 300);
                pet.nPoint.defg = Util.nextInt(50, 100);
                pet.nPoint.critg = 15;
                for (int i = 0; i < 9; i++) {
                    pet.inventory.itemsBody.add(ItemService.gI().createItemNull());
                }
                int skillId[] = { 9, 4, 17 };
                pet.playerSkill.skills.add(SkillUtil.createSkill(skillId[Util.nextInt(0, 2)], 1));
                for (int i = 0; i < 3; i++) {// zl 0822992003 Đức dz
                    pet.playerSkill.skills.add(SkillUtil.createEmptySkill());
                }
                pet.nPoint.setFullHpMp();
                player.pet = pet;
                player.pet.nPoint.limitPower = limitPower;
                player.pointfusion.setHpFusion(Util.nextInt(40, 55));
                player.pointfusion.setMpFusion(Util.nextInt(40, 55));
                player.pointfusion.setDameFusion(Util.nextInt(40, 55));
                Thread.sleep(1000);
                Service.gI().chatJustForMe(player, player.pet, "\b|1|Con đây sư phụ ơi!!!");
            } catch (Exception e) {// zl 0822992003 Đức dz
                e.printStackTrace();
            }
        }).start();
    }

    public void createPetAdrBeach(Player player, boolean isChange, byte gender) {// zl 0822992003 Đức dz
        byte limitPower;
        if (isChange) {// zl 0822992003 Đức dz
            limitPower = player.pet.nPoint.limitPower;
            if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {// zl 0822992003 Đức dz
                player.pet.unFusion();
            }
            ChangeMapService.gI().exitMap(player.pet);
            player.pet.dispose();
            player.pet = null;
        } else {// zl 0822992003 Đức dz
            limitPower = 1;
        }
        new Thread(() -> {// zl 0822992003 Đức dz
            try {// Zzl 0822992003 Đức dz
                Pet pet = new Pet(player);
                pet.name = "$Adr Bãi biển";
                pet.gender = gender;
                pet.id = -player.id;
                pet.nPoint.power = 1500000;
                pet.typePet = 8;
                pet.nPoint.stamina = (short) 1000;
                pet.nPoint.maxStamina = (short) 1000;
                pet.nPoint.hpg = Util.nextInt(2000, 5000);
                pet.nPoint.mpg = Util.nextInt(2000, 5000);
                pet.nPoint.hpMax = Util.nextInt(2000, 5000);
                pet.nPoint.mpMax = Util.nextInt(2000, 5000);
                pet.nPoint.dameg = Util.nextInt(200, 300);
                pet.nPoint.defg = Util.nextInt(50, 100);
                pet.nPoint.critg = 15;
                for (int i = 0; i < 9; i++) {
                    pet.inventory.itemsBody.add(ItemService.gI().createItemNull());
                }
                int skillId[] = { 9, 4, 17 };
                pet.playerSkill.skills.add(SkillUtil.createSkill(skillId[Util.nextInt(0, 2)], 1));
                for (int i = 0; i < 3; i++) {// zl 0822992003 Đức dz
                    pet.playerSkill.skills.add(SkillUtil.createEmptySkill());
                }
                pet.nPoint.setFullHpMp();
                player.pet = pet;
                player.pet.nPoint.limitPower = limitPower;
                player.pointfusion.setHpFusion(Util.nextInt(40, 60));
                player.pointfusion.setMpFusion(Util.nextInt(40, 60));
                player.pointfusion.setDameFusion(Util.nextInt(40, 60));
                Thread.sleep(1000);
                Service.gI().chatJustForMe(player, player.pet, "\b|1|Con đây sư phụ ơi!!!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void createPetBerrusNhi(Player player, boolean isChange, byte gender) {// zl 0822992003 Đức dz
        byte limitPower;
        if (isChange) {// zl 0822992003 Đức dz
            limitPower = player.pet.nPoint.limitPower;
            if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {// zl 0822992003 Đức dz
                player.pet.unFusion();
            }
            ChangeMapService.gI().exitMap(player.pet);
            player.pet.dispose();
            player.pet = null;
        } else {// zl 0822992003 Đức dz
            limitPower = 1;
        }
        new Thread(() -> {// zl 0822992003 Đức dz
            try {// zl 0822992003 Đức dz
                Pet pet = new Pet(player);
                pet.name = "$Black Goku";
                pet.gender = gender;
                pet.id = -player.id;
                pet.nPoint.power = 1500000;
                pet.typePet = 9;
                pet.nPoint.stamina = (short) 1000;
                pet.nPoint.maxStamina = (short) 1000;
                pet.nPoint.hpg = Util.nextInt(2000, 5000);
                pet.nPoint.mpg = Util.nextInt(2000, 5000);
                pet.nPoint.hpMax = Util.nextInt(2000, 5000);
                pet.nPoint.mpMax = Util.nextInt(2000, 5000);
                pet.nPoint.dameg = Util.nextInt(200, 300);
                pet.nPoint.defg = Util.nextInt(50, 100);
                pet.nPoint.critg = 15;
                for (int i = 0; i < 9; i++) {
                    pet.inventory.itemsBody.add(ItemService.gI().createItemNull());
                }
                int skillId[] = { 9, 4, 17 };
                pet.playerSkill.skills.add(SkillUtil.createSkill(skillId[Util.nextInt(0, 2)], 1));
                for (int i = 0; i < 3; i++) {// zl 0822992003 Đức dz
                    pet.playerSkill.skills.add(SkillUtil.createEmptySkill());
                }
                pet.nPoint.setFullHpMp();
                player.pet = pet;
                player.pet.nPoint.limitPower = limitPower;
                player.pointfusion.setHpFusion(Util.nextInt(45, 80));
                player.pointfusion.setMpFusion(Util.nextInt(45, 80));
                player.pointfusion.setDameFusion(Util.nextInt(45, 80));
                Thread.sleep(1000);
                Service.gI().chatJustForMe(player, player.pet, "\b|1|Con đây sư phụ ơi!!!");
            } catch (Exception e) {// zl 0822992003 Đức dz
                e.printStackTrace();
            }
        }).start();
    }

    public void createPetMabuGay(Player player, boolean isChange, byte gender) {// zl 0822992003 Đức dz
        byte limitPower;
        if (isChange) {// zl 0822992003 Đức dz
            limitPower = player.pet.nPoint.limitPower;
            if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {// zl 0822992003 Đức dz
                player.pet.unFusion();
            }
            ChangeMapService.gI().exitMap(player.pet);
            player.pet.dispose();
            player.pet = null;
        } else {// zl 0822992003 Đức dz
            limitPower = 1;
        }
        new Thread(() -> {// zl 0822992003 Đức dz
            try {// zl 0822992003 Đức dz
                Pet pet = new Pet(player);
                pet.name = "$Mabu gầy";
                pet.gender = gender;
                pet.id = -player.id;
                pet.nPoint.power = 1500000;
                pet.typePet = 10;
                pet.nPoint.stamina = (short) 1000;
                pet.nPoint.maxStamina = (short) 1000;
                pet.nPoint.hpg = Util.nextInt(2000, 5000);
                pet.nPoint.mpg = Util.nextInt(2000, 5000);
                pet.nPoint.hpMax = Util.nextInt(2000, 5000);
                pet.nPoint.mpMax = Util.nextInt(2000, 5000);
                pet.nPoint.dameg = Util.nextInt(200, 300);
                pet.nPoint.defg = Util.nextInt(50, 100);
                pet.nPoint.critg = 15;
                for (int i = 0; i < 9; i++) {
                    pet.inventory.itemsBody.add(ItemService.gI().createItemNull());
                }
                int skillId[] = { 9, 4, 17 };
                pet.playerSkill.skills.add(SkillUtil.createSkill(skillId[Util.nextInt(0, 2)], 1));
                for (int i = 0; i < 3; i++) {// zl 0822992003 Đức dz
                    pet.playerSkill.skills.add(SkillUtil.createEmptySkill());
                }
                pet.nPoint.setFullHpMp();
                player.pet = pet;
                player.pet.nPoint.limitPower = limitPower;
                player.pointfusion.setHpFusion(Util.nextInt(45, 80));
                player.pointfusion.setMpFusion(Util.nextInt(45, 80));
                player.pointfusion.setDameFusion(Util.nextInt(45, 80));
                Thread.sleep(1000);
                Service.gI().chatJustForMe(player, player.pet, "\b|1|Con đây sư phụ ơi!!!");
            } catch (Exception e) {// zl 0822992003 Đức dz
                e.printStackTrace();
            }
        }).start();
    }

    private void createNewPet(Player player, boolean isMabu, boolean isBeerus, boolean isPic, boolean isBlack,
            byte... gender) {
        int[] data = isMabu ? isPic ? getDataPetMabu() : getDataPetPic() : getDataPetNormal();
        Pet pet = new Pet(player);
        pet.name = "$" + (isMabu ? "Mabư" : isBeerus ? "Black Goku" : isPic ? "Pic" : isBlack ? "Black" : "Đệ tử");
        pet.gender = (gender != null && gender.length != 0) ? gender[0] : (byte) Util.nextInt(0, 2);
        pet.id = player.isPl() ? -player.id : -Math.abs(player.id) - 100000;
        pet.nPoint.power = isMabu || isBeerus || isPic || isBlack ? 1500000 : 2000;
        pet.typePet = (byte) (isMabu ? 1 : isBeerus ? 2 : isPic ? 3 : isBlack ? 4 : 0);
        pet.nPoint.stamina = 1000;
        pet.nPoint.maxStamina = 1000;
        pet.nPoint.hpg = data[0];
        pet.nPoint.mpg = data[1];
        pet.nPoint.hpMax = data[0];
        pet.nPoint.mpMax = data[1];
        pet.nPoint.dameg = data[2];
        pet.nPoint.defg = data[3];
        pet.nPoint.critg = data[4];
        for (int i = 0; i < 9; i++) {
            pet.inventory.itemsBody.add(ItemService.gI().createItemNull());
        }
        int skillId[] = { 9, 4, 17 };
        pet.playerSkill.skills.add(SkillUtil.createSkill(skillId[Util.nextInt(0, 2)], 1));
        for (int i = 0; i < 3; i++) {
            pet.playerSkill.skills.add(SkillUtil.createEmptySkill());
        }
        pet.nPoint.setFullHpMp();
        player.pet = pet;
    }

    public void createNormalPetSuperGender(Player player, int gender, byte type) {// zl 0822992003 Đức dz
        new Thread(() -> {// zl 0822992003 Đức dz
            try {// zl 0822992003 Đức dz
                createNewPetSuperGender(player, (byte) gender, type);
                Thread.sleep(1000);
                Service.gI().chatJustForMe(player, player.pet, "Xin hãy thu nhận làm đệ tử");
            } catch (Exception e) {// zl 0822992003 Đức dz
                e.printStackTrace();
            }
        }).start();
    }

    public void createNormalPetSuper(Player player, int gender, byte type) {// zl 0822992003 Đức dz
        new Thread(() -> {// Zzl 0822992003 Đức dz
            try {// zl 0822992003 Đức dz
                createNewPetSuper(player, (byte) gender, type);
                Thread.sleep(1000);
                Service.gI().chatJustForMe(player, player.pet, "Xin hãy thu nhận làm đệ tử");
            } catch (Exception e) {// zl 0822992003 Đức dz
                e.printStackTrace();
            }
        }).start();
    }

    private void createNewPetSuper(Player player, byte gender, byte type) {// zl 0822992003 Đức dz
        int[] data = getDataPetNormal();
        Pet pet = new Pet(player);
        if (type == 2) {
            pet.name = "$" + "Songoku";
        } else if (type == 3) {
            pet.name = "$" + "Vegeta";
        } else if (type == 4) {
            pet.name = "$" + "Picolo";
        } else {
            pet.name = "$" + "Mabu";
        }

        pet.gender = (byte) Util.nextInt(0, 2);
        pet.id = -player.id;
        pet.nPoint.power = 1500000;
        pet.typePet = type;
        pet.nPoint.stamina = 1000;
        pet.nPoint.maxStamina = 1000;
        pet.nPoint.hpg = data[0];
        pet.nPoint.mpg = data[1];
        pet.nPoint.hpMax = data[0];
        pet.nPoint.mpMax = data[1];
        pet.nPoint.dameg = data[2];
        pet.nPoint.defg = data[3];
        pet.nPoint.critg = data[4];
        for (int i = 0; i < 9; i++) {
            pet.inventory.itemsBody.add(ItemService.gI().createItemNull());
        }
        int skillId[] = { 9, 4, 17 };
        pet.playerSkill.skills.add(SkillUtil.createSkill(skillId[Util.nextInt(0, 2)], 1));
        for (int i = 0; i < 3; i++) {// zl 0822992003 Đức dz
            pet.playerSkill.skills.add(SkillUtil.createEmptySkill());
        }
        pet.nPoint.setFullHpMp();
        player.pet = pet;
        player.pointfusion.setHpFusion(0);
        player.pointfusion.setMpFusion(0);
        player.pointfusion.setDameFusion(0);
    }

    private void createNewPetSuperGender(Player player, byte gender, byte type) {// zl 0822992003 Đức dz
        int[] data = getDataPetNormal();
        Pet pet = new Pet(player);
        pet.name = "$" + (type == 1 ? "[Broly]Mabư"
                : type == 2 ? "Songoku" : type == 3 ? "Vegeta" : type == 4 ? "Fide" : "[Broly]Đệ tử");
        pet.gender = (byte) Util.nextInt(0, 2);
        pet.id = -player.id;
        pet.gender = player.gender;
        pet.nPoint.power = 1500000;
        pet.typePet = type;
        pet.nPoint.stamina = 1000;
        pet.nPoint.maxStamina = 1000;
        pet.nPoint.hpg = data[0];
        pet.nPoint.mpg = data[1];
        pet.nPoint.hpMax = data[0];
        pet.nPoint.mpMax = data[1];

        pet.nPoint.dameg = data[2];
        pet.nPoint.defg = data[3];
        pet.nPoint.critg = data[4];
        for (int i = 0; i < 9; i++) {
            pet.inventory.itemsBody.add(ItemService.gI().createItemNull());
        }
        int skillId[] = { 9, 4, 17 };
        pet.playerSkill.skills.add(SkillUtil.createSkill(skillId[Util.nextInt(0, 2)], 1));
        for (int i = 0; i < 3; i++) {// zl 0822992003 Đức dz
            pet.playerSkill.skills.add(SkillUtil.createEmptySkill());
        }
        pet.nPoint.setFullHpMp();
        player.pet = pet;
        player.pointfusion.setHpFusion(0);
        player.pointfusion.setMpFusion(0);
        player.pointfusion.setDameFusion(0);
    }

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
        pl.newPet.nPoint.hpg = 500000;
        pl.newPet.nPoint.mpg = 500000;
        pl.newPet.nPoint.hp = 500000;
        pl.newPet.nPoint.mp = 500000;
        pl.newPet.nPoint.dameg = 1;
        pl.newPet.nPoint.defg = 1;
        pl.newPet.nPoint.critg = 1;
        pl.newPet.nPoint.stamina = 1;
        pl.newPet.nPoint.setBasePoint();
        pl.newPet.nPoint.setFullHpMp();
    }

    public void deletePet(Player player) {// zl 0822992003 Đức dz
        Pet pet = player.pet;
        if (pet != null) {// zl 0822992003 Đức dz
            if (player.fusion.typeFusion != ConstPlayer.NON_FUSION) {// zl 0822992003 Đức dz
                pet.unFusion();
            }
            ChangeMapService.gI().exitMap(pet);
            pet.dispose();
            player.pet = null;
        }
    }
}
