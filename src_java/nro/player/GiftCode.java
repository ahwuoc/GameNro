package nro.player;

/**
 *
 *  Box ZALO:https://zalo.me/g/ifjict764
 *  sdt zalo: 0358176187
 * Chuyên chỉnh sữa mua bán source nro,...
 */

import java.util.ArrayList;
import java.util.List;

public class GiftCode {

    public List<String> rewards;

    public GiftCode() {
        this.rewards = new ArrayList<>();
    }

    public void add(String code) {
        this.rewards.add(code);
    }

    public boolean isUsedGiftCode(String code) {
        return rewards.contains(code);
    }

    public void dispose() {
        if (rewards != null) {
            rewards.clear();
            rewards = null;
        }
    }

}
