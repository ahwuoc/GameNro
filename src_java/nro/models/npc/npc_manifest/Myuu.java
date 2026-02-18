package nro.models.npc.npc_manifest;

import consts.ConstNpc;
import java.util.Calendar;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.Service;
import services.func.ChangeMapService;

public class Myuu extends Npc {

    public Myuu(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (this.mapId == 20) {
                createOtherMenu(player, ConstNpc.IGNORE_MENU, "Năm 740, ta tìm thấy kí sinh trùng của King Tuffle,\n"
                        + "sau đó ta đã nghiên cứu và chế tạo ra kí sinh trùng Baby.\n"
                        + "Baby có khả năng bám vào cơ thể của người khác,\n"
                        + "kiểm soát sức mạnh của họ và làm việc theo ý của ta.\n"
                        + "tuy nhiên ta đã mất kiểm soát nó hoàn toàn...\n"
                        + "ngươi có thể giúp ta chế ngự nó không ?", "Đi Thôi", "Đóng");
            } else if (this.mapId == 185) {
                createOtherMenu(player, ConstNpc.IGNORE_MENU, "Ngươi muốn về hả ?", "Quay Về", "Đóng");
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (this.mapId == 20 || this.mapId == 19) {
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);
                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK); // Chủ nhật = 1, Thứ hai = 2, ..., Thứ bảy = 7

                switch (select) {
                    case 0:
                        boolean isInTimeRange = (hour > 13 || (hour == 13 && minute >= 30)) && hour < 23;
                        boolean isValidDay = (dayOfWeek == Calendar.WEDNESDAY || dayOfWeek == Calendar.SUNDAY);

                        if (isInTimeRange && isValidDay) {
                            ChangeMapService.gI().changeMapNonSpaceship(player, 185, 145, 240);
                        } else {
                            Service.gI().sendThongBao(player, "Chỉ có thể vào lúc 13h30 đến 23h vào Thứ 4 và Chủ nhật.");
                        }
                        break;
                }
            } else if (this.mapId == 185) {
                switch (select) {
                    case 0:
                        ChangeMapService.gI().changeMapNonSpaceship(player, 20, 1100, 360);
                        break;
                }
            }
        }
    }
}
