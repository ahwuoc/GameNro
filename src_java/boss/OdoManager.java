/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package boss;

/**
 *
 * @author hoquo
 */
public class OdoManager extends BossManager {

    private static OdoManager instance;

    public static OdoManager gI() {
        if (instance == null) {
            instance = new OdoManager();
        }
        return instance;
    }

}
