package npc.npc_manifest;


import consts.ConstNpc;
import item.Item;
import npc.Npc;
import player.Player;
import services.InventoryService;
import services.ItemService;
import services.Service;
import services.func.ChangeMapService;
import services.func.TopService;
import shop.ShopService;
import utils.Util;

public class BerusNhi extends Npc {

    public BerusNhi(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            switch (mapId) {
                case 42 -> {
                    createOtherMenu(player, ConstNpc.BASE_MENU, "Ta giúp ngươi qua map hủy diệt"
                            ,
                            "Núi Hủy Diệt\nTrái Đất","Đóng"
                    );
                }
                case 43 -> {
                    createOtherMenu(player, ConstNpc.BASE_MENU, "Ta giúp ngươi qua map hủy diệt",
                            "Núi Hủy Diệt\nNa mec", "Đóng");
                }
                case 44 -> {
                    createOtherMenu(player, ConstNpc.BASE_MENU, "Ta giúp ngươi qua map hủy diệt"
                            
                           ,
                            "Núi Hủy Diệt\nXayDa", "Đóng");
                }
                case 5 -> {
                    createOtherMenu(player, ConstNpc.BASE_MENU, "Ta giúp ngươi qua map Địa Ngục",
                            "Địa Ngục\nTrái Đất", "Dòng Thời Gian Tương Lai\nTrái Đất","Đóng"
                           );
                    }
                default ->
                    super.openBaseMenu(player);
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.iDMark.isBaseMenu()) {
                switch (mapId) {
                    case 42 -> {
                        if (select == 0) {
                            
                            ChangeMapService.gI().changeMapNonSpaceship(player, 200, 742, 264);
                        }
                       
                        
                    }
                    case 43 -> {
                        if (select == 0) {
                            ChangeMapService.gI().changeMapNonSpaceship(player, 201, Util.nextInt(1110, 1140), 264);
                        }
                    }
                    case 44 -> {
                        if (select == 0) {
                            ChangeMapService.gI().changeMapNonSpaceship(player, 202, Util.nextInt(700, 726), 264);
                            
                        }
                        }
                    case 5 -> {
                        if (select == 0) {
                            ChangeMapService.gI().changeMapNonSpaceship(player, 174, Util.nextInt(50, 150), 408);
                            } else if (select == 1) {
                             ChangeMapService.gI().changeMapNonSpaceship(player, 213, Util.nextInt(100, 150), 288);
                        }
                    }
                }
            }
           
                }
            }
}  
 


