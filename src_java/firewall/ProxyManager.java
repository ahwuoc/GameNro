/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package firewall;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.table.DefaultTableModel;
import utils.Logger;
/**
 *
 * @author ADMIN PC
 */
public class ProxyManager {
    private static ProxyManager instance;
    private final Map<Integer, TCPProxy> activeProxies;

    private ProxyManager() {
        this.activeProxies = new ConcurrentHashMap<>();
    }

    public static synchronized ProxyManager getInstance() {
        if (instance == null) {
            instance = new ProxyManager();
        }
        return instance;
    }

    public boolean startProxy(String targetIp, int targetPort, int listenPort, DefaultTableModel tableModel) {
        if (activeProxies.containsKey(listenPort)) {
            Logger.error("Port " + listenPort + " đã được sử dụng.\n");
            return false;
        }

        TCPProxy proxy = new TCPProxy(targetIp, targetPort, listenPort);
        activeProxies.put(listenPort, proxy);
        proxy.start();
        tableModel.addRow(new Object[]{targetIp, targetPort, listenPort, "Running"});
        return true;
    }

    public boolean stopProxy(int listenPort, DefaultTableModel tableModel, int viewRow) {
        TCPProxy proxy = activeProxies.remove(listenPort);
        if (proxy != null) {
            proxy.stop();
            tableModel.removeRow(viewRow);
            return true;
        }
        return false;
    }
    
    public void stopAll() {
        activeProxies.values().forEach(TCPProxy::stop);
        activeProxies.clear();
    }
}