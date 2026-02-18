/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package boss;

/**
 *
 * @author Administrator
 */
public class BabyManager extends BossManager {

    private static BabyManager instance;

    public static BabyManager gI() {
        if (instance == null) {
            instance = new BabyManager();
        }
        return instance;
    }

}
