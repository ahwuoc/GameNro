/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package task;

import item.Item;
import java.util.ArrayList;
import nro.services.ItemService;

/**
 *
 * @author Administrator
 */
public class KolTaskTemplate {

    public int id;
    public String info;
    public int max_count;
    public ArrayList<Item> rewards;

    public KolTaskTemplate(int id, String name, int max_count) {
        this.id = id;
        this.info = name;
        this.max_count = max_count;
        this.rewards = getRewardItem();
    }

    private ArrayList<Item> getRewardItem() {
        rewards = new ArrayList<>();
        switch (id) {
            case 0 -> {
                rewards.add(ItemService.gI().createNewItemLock(1785, 3));
            }
            case 1 -> {
                rewards.add(ItemService.gI().createNewItemLock(1592, 5));
                rewards.add(ItemService.gI().createNewItemLock(1758, 5));
            }
            case 2 -> {
                rewards.add(ItemService.gI().createNewItemLock(1360, 1));
            }
            case 3 -> {
                Item cerBerus = ItemService.gI().createNewItemLock(1654, 1);
                cerBerus.itemOptions.add(new Item.ItemOption(93, 120));
                rewards.add(cerBerus);
            }
            case 4 -> {
                rewards.add(ItemService.gI().createNewItemLock(1788, 1));
            }
            case 5 -> {
                Item caiTrang = ItemService.gI().createNewItemLock(1499, 1);
                caiTrang.itemOptions.add(new Item.ItemOption(50, 25));
                caiTrang.itemOptions.add(new Item.ItemOption(103, 30));
                caiTrang.itemOptions.add(new Item.ItemOption(93, 180));
                rewards.add(caiTrang);
                rewards.add(ItemService.gI().createNewItemLock(1592, 5));
                rewards.add(ItemService.gI().createNewItemLock(1758, 5));
            }
            case 6 -> {
                rewards.add(ItemService.gI().createNewItemLock(1592, 10));
                rewards.add(ItemService.gI().createNewItemLock(1758, 10));
            }
        }
        return rewards;
    }
}
