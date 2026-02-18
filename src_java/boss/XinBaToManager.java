/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package boss;

/**
 *
 * @author Administrator
 */
public class XinBaToManager extends BossManager {

    private static XinBaToManager instance;

    public static XinBaToManager gI() {
        if (instance == null) {
            instance = new XinBaToManager();
        }
        return instance;
    }

}
