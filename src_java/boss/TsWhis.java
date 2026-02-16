package boss;

import consts.ConstPlayer;
import boss.Boss;
import boss.BossesData;
import boss.BossID;
import item.Item;
import java.util.Random;
import map.ItemMap;
import player.Player;
import services.EffectSkillService;
import services.PlayerService;
import services.Service;
import services.TaskService;
import utils.Util;
import services.func.ChangeMapService;
import skill.Skill;

public class TsWhis extends Boss {

    public TsWhis() throws Exception {
        super(-99, new BossData(
                "Whis Thiên Sứ",
                ConstPlayer.TRAI_DAT,
                new short[]{505, 506, 507, 157, -1, -1},
                100000,
                new long[]{ 3_000_000_000L},
                new int[]{200,201,202},
                new int[][]{
                    {Skill.SUPER_KAME, 7, 100000},
                    {Skill.GALICK, 7, 1000}, {Skill.LIEN_HOAN, 7, 1000},
                    {Skill.THAI_DUONG_HA_SAN, 3, 50000},
                    {Skill.DICH_CHUYEN_TUC_THOI, 3, 50000}},
                new String[]{"|-1|ko ai nạp ta???"}, //text chat 1
                new String[]{"|-1|Hihihaha"}, //text chat 2
                new String[]{"|-1|Ban hết acc đê!!!"}, //text chat 3
                300));
    }

    @Override
    public void reward(Player plKill) {
           int [] pet = {1568,1596,1597,1629,1630,1631};//id danh sach vp
           if(Util.isTrue(20,100)){
               //tyr le roi vat pham 20%
                
                ItemMap it = new ItemMap(this.zone, pet[Util.nextInt(pet.length)], 1, this.location.x + 5, this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), plKill.id);//random 1 trong nhung id tren
            it.options.add(new Item.ItemOption(50, Util.nextInt(20, 35)));
            it.options.add(new Item.ItemOption(77, Util.nextInt(20, 40)));//oprion tu chinh vd 77 la hp dang de rando 20-40%
            it.options.add(new Item.ItemOption(103, Util.nextInt(20, 40)));
            it.options.add(new Item.ItemOption(14, Util.nextInt(5, 8)));
             
            if (Util.isTrue(996, 1000)) {//tyr le ra hsd 
                it.options.add(new Item.ItemOption(93, Util.nextInt(1,7)));///93 la hsd 1-7 ngay
            }
            Service.gI().dropItemMap(this.zone, it);
           }
        
           for (int i = 0; i < Util.nextInt(1,5); i++) {
                
                ItemMap it = new ItemMap(this.zone, 1641, (int) 1, this.location.x + i * 10, this.zone.map.yPhysicInTop(this.location.x,
                        this.location.y - 24), plKill.id);
                
                Service.gI().dropItemMap(this.zone, it);
            }
         ItemMap it = new ItemMap(this.zone, 1640, (int) 1, this.location.x ,this.zone.map.yPhysicInTop(this.location.x,
                        this.location.y - 24), plKill.id);
                
                Service.gI().dropItemMap(this.zone, it);
    
    }
    @Override
    public void active() {
        if (this.typePk == ConstPlayer.NON_PK) {
            this.changeToTypePK();
        }
        this.attack();
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }
            
            damage = this.nPoint.subDameInjureWithDeff(damage / 1);
            if (!piercing && effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = damage / 1;
            }
//            if (damage > 50_000_000) {
//                damage = 50_000_000;
//            }
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

}
