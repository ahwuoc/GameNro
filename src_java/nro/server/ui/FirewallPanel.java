package nro.server.ui;

import firewall.ProxyManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class FirewallPanel extends JPanel {

    private JTextField txtTargetIP;
    private JTextField txtTargetPort;
    private JTextField txtListenPort;
    private JTable tableProxy;
    private DefaultTableModel modelProxy;

    public FirewallPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        initForm();
        initTable();
        initBottomControls();
    }

    private void initForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(ServerGuiUtils.createSectionBorder("Add Firewall / Proxy Rule"));
        
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.fill = GridBagConstraints.HORIZONTAL;

        // Input Fields
        txtTargetIP = new JTextField("127.0.0.1", 12);
        txtTargetPort = new JTextField("14445", 6);
        txtListenPort = new JTextField("24445", 6);
        
        JButton btnStart = ServerGuiUtils.createStyledButton("Start Proxy", new Color(40, 167, 69), Color.WHITE);

        // Row 1
        g.gridx = 0; g.gridy = 0;
        form.add(new JLabel("Game Server IP:"), g);
        g.gridx = 1;
        form.add(txtTargetIP, g);
        
        g.gridx = 2;
        form.add(new JLabel("Game Port:"), g);
        g.gridx = 3;
        form.add(txtTargetPort, g);
        
        g.gridx = 4;
        form.add(new JLabel("Listen Port (Firewall):"), g);
        g.gridx = 5;
        form.add(txtListenPort, g);
        
        g.gridx = 6;
        form.add(btnStart, g);

        // Logic Button Start
        btnStart.addActionListener(e -> startProxy());

        add(form, BorderLayout.NORTH);
    }

    private void initTable() {
        String[] cols = {"Target IP", "Target Port", "Listen Port", "Status"};
        modelProxy = new DefaultTableModel(cols, 0);
        tableProxy = new JTable(modelProxy);
        tableProxy.setRowHeight(25);
        
        JScrollPane scroll = new JScrollPane(tableProxy);
        scroll.setBorder(ServerGuiUtils.createSectionBorder("Active Proxies"));
        
        add(scroll, BorderLayout.CENTER);
    }

    private void initBottomControls() {
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);

        JButton btnStopAll = ServerGuiUtils.createStyledButton("Stop All Proxies", new Color(220, 53, 69), Color.WHITE);
        btnStopAll.addActionListener(e -> stopAllProxies());

        bottom.add(btnStopAll);
        add(bottom, BorderLayout.SOUTH);
    }

    // ================= LOGIC XỬ LÝ =================

    private void startProxy() {
        try {
            String ip = txtTargetIP.getText().trim();
            int tPort = Integer.parseInt(txtTargetPort.getText().trim());
            int lPort = Integer.parseInt(txtListenPort.getText().trim());

            // Gọi ProxyManager (Logic backend cũ)
            // Lưu ý: Hàm này yêu cầu truyền model để tự update giao diện
            boolean success = ProxyManager.getInstance().startProxy(ip, tPort, lPort, modelProxy);

            if (success) {
                JOptionPane.showMessageDialog(this, "Proxy đã khởi chạy thành công!");
            } else {
                JOptionPane.showMessageDialog(this, "Không thể chạy Proxy (Port đang bận?)", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập Port là số.", "Lỗi nhập liệu", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi hệ thống: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopAllProxies() {
        if (modelProxy.getRowCount() == 0) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, 
                "Bạn có chắc muốn dừng TẤT CẢ Proxy đang chạy?\nNgười chơi kết nối qua Proxy sẽ bị mất kết nối.", 
                "Xác nhận", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            // Lặp qua bảng để dừng từng cái
            while (modelProxy.getRowCount() > 0) {
                try {
                    // Lấy Listen Port từ cột thứ 3 (index 2)
                    int lPort = Integer.parseInt(modelProxy.getValueAt(0, 2).toString());
                    
                    // Gọi hàm stop của Manager
                    ProxyManager.getInstance().stopProxy(lPort, modelProxy, 0);
                } catch (Exception e) {
                    // Nếu lỗi dòng này thì xóa dòng đó đi để tránh lặp vô tận
                    modelProxy.removeRow(0);
                    e.printStackTrace();
                }
            }
            JOptionPane.showMessageDialog(this, "Đã dừng tất cả Proxy.");
        }
    }
}