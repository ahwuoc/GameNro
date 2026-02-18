package nro.models.npc;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/ifjict764
 *  sdt zalo: 0358176187
 * Chuyên chỉnh sữa mua bán source nro,...
 */
import map.Map;
import map.Zone;
import models.WorldMartialArtsTournament.WorldMartialArtsTournamentManager;
import nro.server.Manager;
import nro.services.MapService;
import nro.services.PlayerService;
import skill.Skill;
import boss.BossID;
import consts.ConstPlayer;
import nro.player.Player;
import nro.services.Service;
import utils.Util;

public class NonInteractiveNPC extends Player {

    private long lastTimeChat;
    private long lastTimeChat2;
    private long lastTimeMove;
    private short head = -1;
    private short body = -1;
    private short leg = -1;

    public void initNonInteractiveNPC() {
        init();
    }

    @Override
    public short getHead() {
        return head;
    }

    @Override
    public short getBody() {
        return body;
    }

    @Override
    public short getLeg() {
        return leg;
    }

    public void joinMap(Zone z, Player player) {
        MapService.gI().goToMap(player, z);
        z.load_Me_To_Another(player);
    }

    private Zone z;

    public void hoiPhuc() {
        if (this.nPoint.hp < 2000000000) {
            this.nPoint.hp = 2000000000;
            PlayerService.gI().sendInfoHpMpMoney(this);
            if (z != null) {
                z.loadAnotherToMe(this);
                z.load_Me_To_Another(this);
            }
        }
    }

    private long lastTimeHoiPhuc;
    private final long TimeHoiPhuc = 5000;

    @Override
    public void update() {
        if (!Util.canDoWithTime(lastTimeHoiPhuc, TimeHoiPhuc)) {
            return;
        }
        Thread.ofVirtual().start(() -> hoiPhuc());
        lastTimeHoiPhuc = System.currentTimeMillis();
        if (this.isDie()) {
            Service.gI().hsChar(this, nPoint.hpMax, nPoint.mpMax);
        }
        if (this.id == BossID.CAU_VANG) {
            movecauvang();
        }
        if (this.id == BossID.KHI_BUBBLES) {
            move();
        }
        if (Util.canDoWithTime(lastTimeChat, 5000)) {
            if (this.id == BossID.KHI_BUBBLES) {
                if (Util.isTrue(2, 3)) {
                    String[] text = {"ù ù khẹc khẹc", "khẹc khẹc", "éc éc"};
                    Service.gI().chat(this, text[Util.nextInt(text.length)]);
                }
            }
            lastTimeChat = System.currentTimeMillis();
        }
        if (Util.canDoWithTime(lastTimeChat2, 10000)) {
            if (this.zone.map.mapId == 52) {
                if (this.id == -114) {
                    for (int i = 0; i < WorldMartialArtsTournamentManager.gI().chatText.size(); i++) {
                        if (WorldMartialArtsTournamentManager.gI().chatText != null
                                && !WorldMartialArtsTournamentManager.gI().chatText.isEmpty()) {
                            try {
                                Service.gI().chat(this, WorldMartialArtsTournamentManager.gI().chatText.get(i));
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            }
            lastTimeChat2 = System.currentTimeMillis();
        }
    }
    private int dir = 1; // 1 = phải, -1 = trái
    public byte cdir;

    private void movecauvang() {
        if (Util.canDoWithTime(lastTimeMove, 200)) {

            int speed = 50;
            int x = this.location.x;

            int minX = 965;
            int maxX = 1188;

            // đổi hướng + quay mặt NPC
            if (x >= maxX) {
                dir = -1;
                this.cdir = -1; // quay mặt qua trái
            } else if (x <= minX) {
                dir = 1;
                this.cdir = 1; // quay mặt qua phải
            }

            x += speed * dir;

            int y = this.location.y;
            PlayerService.gI().playerMove(this, x, y);

            lastTimeMove = System.currentTimeMillis();
        }
    }

    private void move() {
        if (Util.canDoWithTime(lastTimeMove, 1000)) {
            if (Util.isTrue(2, 3)) {
                int x = this.location.x;
                x += Util.nextInt(-50, 50);
                if (x > 470 || x < 250) {
                    x = Util.nextInt(250, 470);
                }
                int y = 240;
                PlayerService.gI().playerMove(this, x, y);
            }
            lastTimeMove = System.currentTimeMillis();
        }
    }
    
    private void init() {
        for (Map m : Manager.MAPS) {
            switch (m.mapId) {
                case 45 -> {
                    for (Zone z : m.zones) {
                        NonInteractiveNPC pl = new NonInteractiveNPC();
                        pl.name = "Mr.PôPô";
                        pl.gender = 0;
                        pl.id = BossID.MRPOPO;
                        pl.head = 83;
                        pl.body = 84;
                        pl.leg = 85;
                        pl.nPoint.hpMax = 5100;
                        pl.nPoint.hpg = 5100;
                        pl.nPoint.hp = 5100;
                        pl.nPoint.setFullHpMp();
                        pl.location.x = 295;
                        pl.location.y = 408;
                        joinMap(z, pl);
                        z.setNpc(pl);
                    }
                }
                case 46 -> {
                    for (Zone z : m.zones) {
                        NonInteractiveNPC pl = new NonInteractiveNPC();
                        pl.name = "Yajirô";
                        pl.gender = 0;
                        pl.id = BossID.YAJIRO;
                        pl.head = 77;
                        pl.body = 78;
                        pl.leg = 79;
                        pl.nPoint.hpMax = 1100;
                        pl.nPoint.hpg = 1100;
                        pl.nPoint.hp = 1100;
                        pl.nPoint.setFullHpMp();
                        pl.location.x = 320;
                        pl.location.y = 408;
                        joinMap(z, pl);
                        z.setNpc(pl);
                    }
                }
                case 48 -> {
                    for (Zone z : m.zones) {
                        NonInteractiveNPC pl = new NonInteractiveNPC();
                        pl.name = "Khỉ Bubbles";
                        pl.gender = 0;
                        pl.id = BossID.KHI_BUBBLES;
                        pl.head = 95;
                        pl.body = 96;
                        pl.leg = 97;
                        pl.nPoint.hpMax = 30000;
                        pl.nPoint.hpg = 30000;
                        pl.nPoint.hp = 30000;
                        pl.nPoint.setFullHpMp();
                        pl.location.x = 360;
                        pl.location.y = 240;
                        joinMap(z, pl);
                        z.setNpc(pl);
                    }
                }
                case 0,7 -> {
                    for (Zone z : m.zones) {
                        NonInteractiveNPC pl = new NonInteractiveNPC();
                        pl.name = "Cậu vàng";
                        pl.gender = 0;
                        pl.id = BossID.CAU_VANG;
                        pl.head = 1997;
                        pl.body = 1998;
                        pl.leg = 1999;
                        pl.nPoint.hpMax = 10000;
                        pl.nPoint.hpg = 10000;
                        pl.nPoint.hp = 10000;
                        pl.nPoint.setFullHpMp();
                        pl.location.x = 1110;
                        pl.location.y = 432;
                        joinMap(z, pl);
                        z.setNpc(pl);
                    }
                }
                case 14 -> {
                    for (Zone z : m.zones) {
                        NonInteractiveNPC pl = new NonInteractiveNPC();
                        pl.name = "Cậu vàng";
                        pl.gender = 0;
                        pl.id = BossID.CAU_VANG;
                        pl.head = 1997;
                        pl.body = 1998;
                        pl.leg = 1999;
                        pl.nPoint.hpMax = 10000;
                        pl.nPoint.hpg = 10000;
                        pl.nPoint.hp = 10000;
                        pl.nPoint.setFullHpMp();
                        pl.location.x = 1110;
                        pl.location.y = 408;
                        joinMap(z, pl);
                        z.setNpc(pl);
                    }
                }
                case 51 -> {
                    for (Zone z : m.zones) {
                        NonInteractiveNPC pl = new NonInteractiveNPC();
                        pl.name = "Trọng Tài";
                        pl.gender = 0;
                        pl.id = -114;
                        pl.head = 114;
                        pl.body = 115;
                        pl.leg = 116;
                        pl.nPoint.hpMax = 500;
                        pl.nPoint.hpg = 500;
                        pl.nPoint.hp = 500;
                        pl.nPoint.setFullHpMp();
                        pl.location.x = 383;
                        pl.location.y = 112;
                        joinMap(z, pl);
                        z.setNpc(pl);
                    }
                }
                case 52 -> {
                    for (Zone z : m.zones) {
                        NonInteractiveNPC pl = new NonInteractiveNPC();
                        pl.name = "Trọng Tài";
                        pl.gender = 0;
                        pl.id = -114;
                        pl.head = 114;
                        pl.body = 115;
                        pl.leg = 116;
                        pl.nPoint.hpMax = 500;
                        pl.nPoint.hpg = 500;
                        pl.nPoint.hp = 500;
                        pl.nPoint.setFullHpMp();
                        pl.location.x = z.zoneId > 0 ? 301 : 373;
                        pl.location.y = 336;
                        joinMap(z, pl);
                        z.setNpc(pl);
                    }
                }
                case 129 -> {
                    for (Zone z : m.zones) {
                        NonInteractiveNPC pl = new NonInteractiveNPC();
                        pl.name = "Trọng Tài";
                        pl.gender = 0;
                        pl.id = -114;
                        pl.head = 114;
                        pl.body = 115;
                        pl.leg = 116;
                        pl.nPoint.hpMax = 500;
                        pl.nPoint.hpg = 500;
                        pl.nPoint.hp = 500;
                        pl.nPoint.setFullHpMp();
                        pl.location.x = 385;
                        pl.location.y = 264;
                        joinMap(z, pl);
                        z.setNpc(pl);
                    }
                }
                case 103 -> {
                    for (Zone z : m.zones) {
                        NonInteractiveNPC pl = new NonInteractiveNPC();
                        pl.name = "Trọng Tài";
                        pl.gender = 0;
                        pl.id = -114;
                        pl.head = 114;
                        pl.body = 115;
                        pl.leg = 116;
                        pl.nPoint.hpMax = 500;
                        pl.nPoint.hpg = 500;
                        pl.nPoint.hp = 500;
                        pl.nPoint.setFullHpMp();
                        pl.location.x = 401;
                        pl.location.y = 288;
                        joinMap(z, pl);
                        z.setNpc(pl);
                    }
                }
                case 146 -> {
                    for (Zone z : m.zones) {
                        NonInteractiveNPC pl = new NonInteractiveNPC();
                        pl.name = "Yajirô";
                        pl.gender = 0;
                        pl.id = -77;
                        pl.head = 77;
                        pl.body = 78;
                        pl.leg = 79;
                        pl.nPoint.hpMax = 1100;
                        pl.nPoint.hpg = 1100;
                        pl.nPoint.hp = 1100;
                        pl.nPoint.setFullHpMp();
                        pl.location.x = 100;
                        pl.location.y = 336;
                        joinMap(z, pl);
                        z.setNpc(pl);
                    }
                }
            }
        }
    }
}
