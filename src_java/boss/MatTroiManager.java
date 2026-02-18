/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package boss;

/**
 *
 * @author Administrator
 */
public class MatTroiManager extends BossManager {

    private static MatTroiManager instance;

    public static MatTroiManager gI() {
        if (instance == null) {
            instance = new MatTroiManager();
        }
        return instance;
    }

}
