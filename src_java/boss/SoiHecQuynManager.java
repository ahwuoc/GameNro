/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package boss;

/**
 *
 * @author hoquo
 */
public class SoiHecQuynManager extends BossManager {

    private static SoiHecQuynManager instance;

    public static SoiHecQuynManager gI() {
        if (instance == null) {
            instance = new SoiHecQuynManager();
        }
        return instance;
    }

}
