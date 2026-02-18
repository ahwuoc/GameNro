package services.func;

import consts.ConstNpcConfig;
import nro.player.Player;
import nro.services.Service;
import java.io.DataOutputStream;
import java.io.IOException;
import network.Message;

/**
 *
 * @author 💖 Trần Lại 💖
 * @copyright 💖 GirlkuN 💖
 *
 */
public class RadaService {

    private static RadaService instance;

    private RadaService() {

    }

    public static RadaService getInstance() {
        if (instance == null) {
            instance = new RadaService();
        }
        return instance;
    }

    public void setIDAuraEff(Player player, byte aura) {
        try {
            Message mss = new Message(ConstNpcConfig.RADA_CARD);
            DataOutputStream ds = mss.writer();
            ds.writeByte(4);
            ds.writeInt((int) player.id);
            ds.writeShort(aura);
            ds.flush();
            Service.gI().sendMessAllPlayerInMap(player, mss);
            mss.cleanup();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}