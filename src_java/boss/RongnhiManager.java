/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package boss;

/**
 *
 * @author hoquo
 */
public class RongnhiManager extends BossManager {

    private static RongnhiManager instance;

    public static RongnhiManager gI() {
        if (instance == null) {
            instance = new RongnhiManager();
        }
        return instance;
    }

}
