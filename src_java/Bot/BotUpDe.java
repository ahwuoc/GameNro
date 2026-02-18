package Bot;

import map.Map;
import map.Zone;
import nro.player.Pet;
import nro.services.PetService;
import nro.services.MapService;
import services.func.ChangeMapService;
import utils.Util;

public class BotUpDe extends Bot {

   public BotUpDe(short head, short body, short leg, int type, String name, ShopBot shop, short flag) {
      super(head, body, leg, type, name, shop, flag);
   }

   @Override
   public void joinMap() {
      Zone zone = getRandomZone(MapToPow());
      if (zone != null){
         ChangeMapService.gI().goToMap(this, zone);
         this.zone.load_Me_To_Another(this);
         this.mo1.lastTimeChanM = System.currentTimeMillis();
         try {
            if (this.pet == null) {
               PetService.gI().createNormalPet(this, (int) this.gender);
            }
            Thread.startVirtualThread(() -> {
               try {
                  long start = System.currentTimeMillis();
                  while (this.pet == null && System.currentTimeMillis() - start < 3000) {
                     Thread.sleep(100);
                  }
                  if (this.pet != null) {
                     this.pet.changeStatus(Pet.ATTACK);
                     this.pet.joinMapMaster();
                  }
               } catch (Exception e) {
               }
            });
         } catch (Exception e) {
         }
      }
   }

   @Override
   public Zone getRandomZone(int mapId) {
      Map map = MapService.gI().getMapById(mapId);
      Zone zone = null;
      try {
         if (map != null) {
            for (Zone z : map.zones) {
               if (z.isFullPlayer()) continue;
               if (countUpDeInZone(z) < 2) { // giới hạn bot up đệ trong map
                  zone = z;
                  break;
               }
            }
            if (zone == null) {
               Zone randomZone = map.zones.get(Util.nextInt(0, map.zones.size() - 1));
               if (!randomZone.isFullPlayer() && countUpDeInZone(randomZone) < 2) {
                  zone = randomZone;
               }
            }
         }
      } catch (Exception e) {
      }
      if (zone != null) {
         return zone;
      } else {
         return super.getRandomZone(mapId);
      }
   }

   private int countUpDeInZone(Zone zone) {
      int count = 0;
      try {
         for (Bot b : BotManager.gI().bot) {
            if (b instanceof BotUpDe && b.zone == zone) {
               count++;
            }
         }
      } catch (Exception e) {
      }
      return count;
   }
}


