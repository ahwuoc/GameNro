package models.kygui;

import consts.ConstNpc;
import item.Item;
import item.Item.ItemOption;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import network.Message;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.NpcService;
import nro.services.Service;

public class ConsignShopService {

    private static ConsignShopService instance;

    public static ConsignShopService gI() {
        if (instance == null) {
            instance = new ConsignShopService();
        }
        return instance;
    }

    // ============================================================
    // ✅ SORT ưu tiên isUpTop và id mới nhất -> đỡ nhảy item
    // ============================================================
    private List<ConsignItem> sortItems(List<ConsignItem> list) {
        list.sort(
                Comparator.comparing((ConsignItem i) -> i.isUpTop).reversed()
                        .thenComparing(i -> -i.id)
        );
        return list;
    }

    private List<ConsignItem> getItemKyGui2(Player pl, byte tab, byte to, byte max) {
        try {
            List<ConsignItem> src = new ArrayList<>();
            for (ConsignItem c : ConsignShopManager.gI().listItem) {
                if (c != null && c.tab == tab && !c.isBuy) {
                    src.add(c);
                }
            }

            sortItems(src);
            List<ConsignItem> result = new ArrayList<>();

            for (int i = to; i < max && i < src.size(); i++) {
                result.add(src.get(i));
            }

            return result;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<ConsignItem> getItemKyGui(Player pl, byte tab, byte... max) {
        List<ConsignItem> src = new ArrayList<>();

        for (ConsignItem c : ConsignShopManager.gI().listItem) {
            if (c != null && c.tab == tab && !c.isBuy && c.player_sell != pl.id) {
                src.add(c);
            }
        }

        sortItems(src);

        if (max.length == 2) {
            int from = max[0];
            int to = max[1];
            return src.subList(Math.min(from, src.size()), Math.min(to, src.size()));
        }

        if (max.length == 1 && src.size() > max[0]) {
            return src.subList(0, max[0]);
        }

        return src;
    }

    private List<ConsignItem> getItemKyGui() {
        List<ConsignItem> result = new ArrayList<>();
        for (ConsignItem it : ConsignShopManager.gI().listItem) {
            if (it != null && !it.isBuy) {
                result.add(it);
            }
        }
        return sortItems(result);
    }

    private boolean SubThoiVang(Player pl, int quatity) {
        for (Item item : pl.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.id == 457 && item.quantity >= quatity) {
                InventoryService.gI().subQuantityItemsBag(pl, item, quatity);
                return true;
            }
        }
        return false;
    }

    // ============================================================
    // ✅ MUA ITEM (synchronized chống đua)
    // ============================================================
    public void buyItem(Player pl, int id) {
        synchronized (ConsignShopManager.gI().listItem) {

            if (pl.nPoint.power < 17000000000L) {
                Service.gI().sendThongBao(pl, "Yêu cầu sức mạnh lớn hơn 17 tỷ");
                openShopKyGui(pl);
                return;
            }

            ConsignItem it = getItemBuy(id);
            if (it == null || it.isBuy) {
                Service.gI().sendThongBao(pl, "Vật phẩm không tồn tại hoặc đã được bán");
                return;
            }

            if (it.player_sell == pl.id) {
                Service.gI().sendThongBao(pl, "Không thể mua vật phẩm bản thân đăng bán");
                return;
            }

            boolean isBuy = false;

            // ✅ Mua bằng thỏi vàng
            if (it.goldSell > 0) {
                if (!SubThoiVang(pl, it.goldSell)) {
                    Service.gI().sendThongBao(pl, "Bạn không đủ thỏi vàng để mua vật phẩm");
                    return;
                }
                isBuy = true;
            }
            // ✅ Mua bằng Ruby (ngọc xanh)
            else if (it.gemSell > 0) {
                if (pl.inventory.ruby >= it.gemSell) {
                    pl.inventory.ruby -= it.gemSell;
                    isBuy = true;
                } else {
                    Service.gI().sendThongBao(pl, "Bạn không đủ Ngọc Xanh để mua vật phẩm này!");
                    return;
                }
            }

            Service.gI().sendMoney(pl);

            if (isBuy) {
                Item item = ItemService.gI().createNewItem(it.itemId);
                item.quantity = it.quantity;

                // ✅ deep copy item options
                for (ItemOption op : it.options) {
                    item.itemOptions.add(new ItemOption(op.optionTemplate, op.param));
                }

                it.isBuy = true;
                InventoryService.gI().addItemBag(pl, item);
                InventoryService.gI().sendItemBag(pl);

                ConsignShopManager.gI().save();
                Service.gI().sendThongBao(pl, "Bạn đã nhận được " + item.template.name);
                openShopKyGui(pl);
            }
        }
    }

    public ConsignItem getItemBuy(int id) {
        for (ConsignItem it : getItemKyGui()) {
            if (it != null && it.id == id) {
                return it;
            }
        }
        return null;
    }

    public ConsignItem getItemBuy(Player pl, int id) {
        for (ConsignItem it : ConsignShopManager.gI().listItem) {
            if (it != null && it.id == id && it.player_sell == pl.id) {
                return it;
            }
        }
        return null;
    }

    // ============================================================
    // ✅ AN TOÀN PAGE, HIỂN THỊ SHOP
    // ============================================================
    public void openShopKyGui(Player pl, byte index, int page) {

        if (page < 0) page = 0;

        int maxPage = Math.max(1, (getItemKyGui(pl, index).size() + 19) / 20);
        if (page >= maxPage) page = maxPage - 1;

        Message msg = null;
        try {
            msg = new Message(-100);
            msg.writer().writeByte(index);

            List<ConsignItem> items = getItemKyGui(pl, index);
            List<ConsignItem> itemsSend = getItemKyGui2(pl, index, (byte) (page * 20), (byte) (page * 20 + 20));

            msg.writer().writeByte(maxPage); 
            msg.writer().writeByte(page);
            msg.writer().writeByte(itemsSend.size());

            for (ConsignItem itk : itemsSend) {
                Item it = ItemService.gI().createNewItem(itk.itemId);
                it.itemOptions.clear();

                for (ItemOption op : itk.options) {
                    it.itemOptions.add(new ItemOption(op.optionTemplate, op.param));
                }

                msg.writer().writeShort(it.template.id);
                msg.writer().writeShort(itk.id);
                msg.writer().writeInt(itk.goldSell);
                msg.writer().writeInt(itk.gemSell);

                msg.writer().writeByte(0);

                if (pl.getSession().version >= 222)
                    msg.writer().writeInt(itk.quantity);
                else
                    msg.writer().writeByte(itk.quantity);

                msg.writer().writeByte(itk.player_sell == pl.id ? 1 : 0);
                msg.writer().writeByte(it.itemOptions.size());

                for (ItemOption op : it.itemOptions) {
                    msg.writer().writeByte(op.optionTemplate.id);
                    msg.writer().writeShort(op.param);
                }

                msg.writer().writeByte(0);
            }
            pl.sendMessage(msg);

        } catch (IOException ignored) {
        } finally {
            if (msg != null) msg.cleanup();
        }
    }

    // ============================================================
    // ✅ LÊN TOP ITEM
    // ============================================================
    public void upItemToTop(Player pl, int id) {
        ConsignItem it = getItemBuy(id);

        if (it == null || it.isBuy) {
            Service.gI().sendThongBao(pl, "Vật phẩm không tồn tại hoặc đã được bán");
            return;
        }

        if (it.player_sell != pl.id) {
            Service.gI().sendThongBao(pl, "Vật phẩm không thuộc quyền sở hữu");
            return;
        }

        pl.iDMark.setIdItemUpTop(id);
        NpcService.gI().createMenuConMeo(pl, ConstNpc.UP_TOP_ITEM, -1,
                "Bạn có muốn đưa vật phẩm ['" + ItemService.gI().createNewItem(it.itemId).template.name +
                        "'] của bản thân lên trang đầu?\nYêu cầu 2 Thỏi Vàng.",
                "Đồng ý", "Từ Chối");
    }

    public void StartupItemToTop(Player pl) {
        synchronized (ConsignShopManager.gI().listItem) {
            if (!SubThoiVang(pl, 2)) {
                Service.gI().sendThongBao(pl, "Bạn cần có ít nhất 2 thỏi vàng đưa vật phẩm lên trang đầu");
                return;
            }

            for (ConsignItem its : ConsignShopManager.gI().listItem) {
                if (its.id == pl.iDMark.getIdItemUpTop()) {
                    its.isUpTop = 1;
                    break;
                }
            }

            ConsignShopManager.gI().save();
            openShopKyGui(pl);
        }
    }

    // ============================================================
    // ✅ HUỶ / NHẬN TIỀN BÁN
    // ============================================================
    public void claimOrDel(Player pl, byte action, int id) {
        synchronized (ConsignShopManager.gI().listItem) {

            ConsignItem it = getItemBuy(pl, id);
            if (it == null) {
                Service.gI().sendThongBao(pl, "Vật phẩm không tồn tại");
                return;
            }

            switch (action) {
                case 1: // HỦY ITEM
                    if (it.isBuy) {
                        Service.gI().sendThongBao(pl, "Vật phẩm đã được bán");
                        return;
                    }

                    Item item = ItemService.gI().createNewItem(it.itemId);
                    item.quantity = it.quantity;
                    for (ItemOption op : it.options) {
                        item.itemOptions.add(new ItemOption(op.optionTemplate, op.param));
                    }

                    if (ConsignShopManager.gI().listItem.remove(it)) {
                        InventoryService.gI().addItemBag(pl, item);
                        InventoryService.gI().sendItemBag(pl);
                        ConsignShopManager.gI().save();
                        Service.gI().sendThongBao(pl, "Hủy bán vật phẩm thành công");
                    }
                    break;

                case 2: // NHẬN TIỀN
                    if (!it.isBuy) {
                        Service.gI().sendThongBao(pl, "Vật phẩm chưa được bán");
                        return;
                    }

                    if (it.goldSell > 0) {
                        Item tvAdd = ItemService.gI().createNewItem((short) 457);
                        tvAdd.quantity = it.goldSell - (it.goldSell * 10 / 100); // trừ phí 10%
                        InventoryService.gI().addItemBag(pl, tvAdd);
                    } else if (it.gemSell > 0) {
                        pl.inventory.ruby += it.gemSell - (it.gemSell * 10 / 100);
                    }

                    ConsignShopManager.gI().listItem.remove(it);
                    ConsignShopManager.gI().save();
                    Service.gI().sendMoney(pl);
                    Service.gI().sendThongBao(pl, "Bạn đã nhận tiền thành công");
                    break;
            }

            openShopKyGui(pl);
        }
    }

    // ============================================================
    // ✅ LẤY ITEM CÓ THỂ KÝ GỬI
    // ============================================================
    public List<ConsignItem> getItemCanKiGui(Player pl) {
        List<ConsignItem> result = new ArrayList<>();

        for (ConsignItem it : ConsignShopManager.gI().listItem) {
            if (it != null && it.player_sell == pl.id) {
                result.add(it);
            }
        }

        for (Item it : pl.inventory.itemsBag) {
            if (itemCanConsign(it)) {

                List<ItemOption> ops = new ArrayList<>();
                for (ItemOption op : it.itemOptions) {
                    ops.add(new ItemOption(op.optionTemplate, op.param));
                }

                result.add(new ConsignItem(
                        InventoryService.gI().getIndexBag(pl, it),
                        it.template.id,
                        (int) pl.id,
                        (byte) 4,
                        -1,
                        -1,
                        it.quantity,
                        (byte) -1,
                        ops,
                        false
                ));
            }
        }

        return result;
    }

    public boolean itemCanConsign(Item it) {
        return it != null && it.template != null && (
                it.itemOptions.stream().anyMatch(op -> op.optionTemplate.id == 86) ||
                        it.itemOptions.stream().anyMatch(op -> op.optionTemplate.id == 87) ||
                        it.template.type == 14 ||
                        it.template.type == 15 ||
                        it.template.type == 6 ||
                        it.template.id >= 14 && it.template.id <= 20 ||
                        it.template.id >= 0 && it.template.id <= 5
        );
    }

    public int getMaxId() {
        try {
            List<Integer> id = new ArrayList<>();
            for (ConsignItem it : ConsignShopManager.gI().listItem) {
                if (it != null) id.add(it.id);
            }
            return id.isEmpty() ? 0 : Collections.max(id);
        } catch (Exception e) {
            return 0;
        }
    }

    public byte getTabKiGui(Item it) {
        if (it.template.type >= 0 && it.template.type <= 2)
            return 0;
        else if (it.template.type >= 3 && it.template.type <= 4)
            return 1;
        else if (it.template.type == 29)
            return 2;
        else
            return 3;
    }

    // ============================================================
    // ✅ KÝ GỬI ITEM
    // ============================================================
    public void KiGui(Player pl, int id, int money, byte moneyType, int quantity) {
        try {

            if (!SubThoiVang(pl, 1)) {
                Service.gI().sendThongBao(pl, "Bạn cần có ít nhất 1 thỏi vàng để làm phí đăng bán");
                return;
            }

            Item it = ItemService.gI().copyItem(pl.inventory.itemsBag.get(id));

            for (Item.ItemOption op : it.itemOptions) {
                if (op.optionTemplate.id == 30) {
                    Service.gI().sendThongBao(pl, "Vật phẩm không thể ký gửi");
                    return;
                }
            }

            if (money <= 0 || quantity > it.quantity) return;
            if (quantity > 99 || quantity < 1) {
                Service.gI().sendThongBao(pl, "Ký gửi tối đa x99 và tối thiểu x1");
                return;
            }

            int newId = getMaxId() + 1;
            boolean success = false;

            List<ItemOption> ops = new ArrayList<>();
            for (ItemOption op : it.itemOptions) {
                ops.add(new ItemOption(op.optionTemplate, op.param));
            }

            switch (moneyType) {
                case 0:
                    if (money > 100000 || money < 0) {
                        Service.gI().sendThongBao(pl, "Không thể ký gửi quá 100000 thỏi vàng");
                    } else {
                        InventoryService.gI().subQuantityItemsBag(pl, pl.inventory.itemsBag.get(id), quantity);
                        ConsignShopManager.gI().listItem.add(new ConsignItem(newId, it.template.id, (int) pl.id, getTabKiGui(it), money, -1, quantity, (byte) 0, ops, false));
                        success = true;
                    }
                    break;
                case 1:
                    if (money > 1000000 || money < 0) {
                        Service.gI().sendThongBao(pl, "Không thể ký gửi quá 1000000 Ngọc");
                    } else {
                        InventoryService.gI().subQuantityItemsBag(pl, pl.inventory.itemsBag.get(id), quantity);
                        ConsignShopManager.gI().listItem.add(new ConsignItem(newId, it.template.id, (int) pl.id, getTabKiGui(it), -1, money, quantity, (byte) 0, ops, false));
                        success = true;
                    }
                    break;
                default:
                    Service.gI().sendThongBao(pl, "Có lỗi xảy ra");
                    break;
            }

            if (success) {
                ConsignShopManager.gI().save();
                openShopKyGui(pl);
                Service.gI().sendMoney(pl);
                Service.gI().sendThongBao(pl, "Đăng bán thành công");
            }
        } catch (Exception ignored) {
        }
    }

    // ============================================================
    // ✅ MỞ SHOP VỊ TRÍ TAB 4
    // ============================================================
    public void openShopKyGui(Player pl) {
        try {
            Message msg = new Message(-44);
            msg.writer().writeByte(2);
            msg.writer().writeByte(5);

            for (byte i = 0; i < 5; i++) {
                msg.writer().writeUTF(ConsignShopManager.gI().tabName[i]);

                if (i == 4) {
                    msg.writer().writeByte(0);
                    List<ConsignItem> items = getItemCanKiGui(pl);
                    msg.writer().writeByte(items.size());

                    for (ConsignItem itk : items) {
                        Item it = ItemService.gI().createNewItem(itk.itemId);
                        it.itemOptions.clear();

                        for (ItemOption op : itk.options) {
                            it.itemOptions.add(new ItemOption(op.optionTemplate, op.param));
                        }

                        msg.writer().writeShort(it.template.id);
                        msg.writer().writeShort(itk.id);
                        msg.writer().writeInt(itk.goldSell);
                        msg.writer().writeInt(itk.gemSell);

                        if (getItemBuy(pl, itk.id) == null)
                            msg.writer().writeByte(0);
                        else if (itk.isBuy)
                            msg.writer().writeByte(2);
                        else
                            msg.writer().writeByte(1);

                        msg.writer().writeInt(itk.quantity);
                        msg.writer().writeByte(1);
                        msg.writer().writeByte(it.itemOptions.size());

                        for (ItemOption op : it.itemOptions) {
                            msg.writer().writeByte(op.optionTemplate.id);
                            msg.writer().writeShort(op.param);
                        }

                        msg.writer().writeByte(0);
                        msg.writer().writeByte(0);
                    }
                } else {
                    List<ConsignItem> items = getItemKyGui(pl, i);
                    List<ConsignItem> itemsSend = getItemKyGui2(pl, i, (byte) 0, (byte) 20);

                    byte tab = (byte) (items.size() / 20 > 0 ? (items.size() / 20) + 1 : 1);
                    msg.writer().writeByte(tab);
                    msg.writer().writeByte(itemsSend.size());

                    for (ConsignItem itk : itemsSend) {
                        Item it = ItemService.gI().createNewItem(itk.itemId);
                        it.itemOptions.clear();

                        for (ItemOption op : itk.options) {
                            it.itemOptions.add(new ItemOption(op.optionTemplate, op.param));
                        }

                        msg.writer().writeShort(it.template.id);
                        msg.writer().writeShort(itk.id);
                        msg.writer().writeInt(itk.goldSell);
                        msg.writer().writeInt(itk.gemSell);

                        msg.writer().writeByte(0);
                        msg.writer().writeInt(itk.quantity);
                        msg.writer().writeByte(itk.player_sell == pl.id ? 1 : 0);
                        msg.writer().writeByte(it.itemOptions.size());

                        for (ItemOption op : it.itemOptions) {
                            msg.writer().writeByte(op.optionTemplate.id);
                            msg.writer().writeShort(op.param);
                        }

                        msg.writer().writeByte(0);
                        msg.writer().writeByte(0);
                    }
                }
            }

            pl.sendMessage(msg);
            msg.cleanup();

        } catch (IOException ignored) {
        }
    }
}
