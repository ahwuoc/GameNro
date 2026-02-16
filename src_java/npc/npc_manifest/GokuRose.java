package npc.npc_manifest;


import consts.ConstNpc;
import item.Item;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jdbc.DBConnecter;
import npc.Npc;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import player.ArchivementSucManh;
import player.Player;
import server.Manager;
import services.InventoryService;
import services.ItemService;
import services.Service;
import services.func.ChangeMapService;
import services.func.TopService;
import shop.ShopService;
import utils.Util;

public class GokuRose extends Npc {

    public GokuRose(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            switch (mapId) {
                case 5 -> {

                    createOtherMenu(player, ConstNpc.BASE_MENU, "Lực Chiến cho biết sức chiến đấu của 1 chiến binh\n"
                            + "Khi Lực chiến đủ mạnh sẽ mở khóa Nhiều map mới\n"
                           +"|3| Chiến lực hiện tại: "+ String.format( Manager.formatNumber(player.lucchien)),
                            "TOP Lực Chiến","Nhận quà mốc","Xem quà","Đấu trường SkyPear"
                    );
                  
                   
                }
                case 207 -> {
                    createOtherMenu(player, ConstNpc.BASE_MENU, "Quay về đảo thôi!!",
                            "Về\nĐảo Kame", "Đóng");
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
                    case 5 -> {
                        if (select == 0) {
                             TopService.showListTop(player, 11);
                        }
                        if(select ==1){
                            if (player.getSession().actived) {
                                ArchivementSucManh.gI().getAchievement(player);
                            } else {
                                Service.gI().sendThongBao(player, "Mở thành viên đi rồi qua đây nhận nhe baby!");
                            }
                           
                  
                        
                        }
                        if(select ==2){
                            JSONArray dataArray;
                            JSONObject dataObject;
                            PreparedStatement ps = null;
                            ResultSet rs = null;
                            StringBuilder sb = new StringBuilder();
                            sb.append("|0|꧁__Lực chiến càng cao, nhận quà càng đã\n");
                            try ( Connection con2 = DBConnecter.getConnectionServer()) {
                                ps = con2.prepareStatement("SELECT * FROM moc_suc_manh");
                                rs = ps.executeQuery();

                                while (rs.next()) {
                                    dataArray = (JSONArray) JSONValue.parse(rs.getString("detail"));
                                    sb.append("◥_____________________◤\n|7|");
                                    sb.append("✎▶Mốc Lực Chiến ").append(Util.numberToMoney(ArchivementSucManh.POWERGIFT[rs.getInt("id") - 1]))
                                            .append("◀\n|0|");
                                    sb.append("◢¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯◣\n|0|");

                                    for (int i = 0; i < dataArray.size(); i++) {
                                        dataObject = (JSONObject) JSONValue.parse(String.valueOf(dataArray.get(i)));
                                        int tempid = Integer.parseInt(String.valueOf(dataObject.get("temp_id")));
                                        int quantity = Integer.parseInt(String.valueOf(dataObject.get("quantity")));
                                        JSONArray optionsArray = (JSONArray) dataObject.get("options");

                                        sb.append("▷ x").append(quantity).append(" ")
                                                .append(ItemService.gI().getTemplate(tempid).name).append("\n|4|");

                                        if (optionsArray != null) {
                                            for (int j = 0; j < optionsArray.size(); j++) {
                                                JSONObject optionObject = (JSONObject) optionsArray.get(j);
                                                int optionId = Integer.parseInt(String.valueOf(optionObject.get("id")));
                                                int param = Integer.parseInt(String.valueOf(optionObject.get("param")));

                                                String optionTemplateName = ItemService.gI().getItemOptionTemplate(optionId).name;
                                                String formattedOption = optionTemplateName.replace("#", String.valueOf(param));

                                                sb.append(formattedOption).append("\n");
                                            }
                                        }
                                        sb.append("\n|0|");
                                    }
                                }
                            } catch (SQLException ex) {
                                Logger.getLogger(DaiThienSu.class.getName()).log(Level.SEVERE, null, ex);
                            }

                            Service.gI().sendThongBaoFromAdmin(player, sb.toString());
                        }
                        if(select==3){
                            if(player.lucchien>15_000_000){
                                 ChangeMapService.gI().changeMapNonSpaceship(player, 207, Util.nextInt(700, 800), 408);
                            }else{
                                 Service.gI().sendThongBao(player, "Cần đạt tối thiểu 15tr lực chiến");
                            }
                        }
                        
                        
                    }
                    case 207 -> {
                        if (select == 0) {
                            ChangeMapService.gI().changeMapNonSpaceship(player, 5, Util.nextInt(400, 500), 312);
                        }
                    }
                    
                }
            }
           
                }
            }
}  
            
            
        


