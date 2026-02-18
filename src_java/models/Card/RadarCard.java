package models.Card;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/ifjict764
 *  sdt zalo: 0358176187
 * Chuyên chỉnh sữa mua bán source nro,...
 */

import java.util.ArrayList;
import java.util.List;

public class RadarCard {

    public short Id;
    public short IconId;
    public byte Rank;
    public byte Max;
    public byte Type;
    public short Template;
    public short Head;
    public short Body;
    public short Leg;
    public short Bag;
    public String Name;
    public String Info;
    public List<OptionCard> Options;
    public short Require;
    public short RequireLevel;
    public short AuraId;

    public RadarCard() {
        Id = -1;
        IconId = -1;
        Rank = 0;
        Max = 0;
        Type = 0;
        Template = 1;
        Name = "";
        Info = "";
        Options = new ArrayList<>();
        Require = -1;
        RequireLevel = 0;
        AuraId = -1;
    }
}
