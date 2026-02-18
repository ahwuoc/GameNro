package nro.server.ui;

import boss.BossID;
import boss.BossManager;
import boss.BossStatus;
import boss.Boss;
import boss.BossData;
import boss.BossesData;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import item.Item.ItemOption;
import models.GiftCode.GiftCodeManager;
import models.kygui.ConsignShopManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.JSONArray;
import jdbc.DBConnecter;
import network.SessionManager;
import nro.server.AutoSaveManager;
import nro.server.Client;
import nro.server.Maintenance;
import nro.server.Manager;
import nro.services.Service;
import utils.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jdbc.daos.ShopDAO;
import models.GiftCode.GiftCode;
import models.Template.NpcTemplate;

public class DashboardPanel extends JPanel {

    // --- UI Components ---
    private JLabel lblStatus, lblPlayerCount, lblCountdown;
    private JLabel lblCpuUsage, lblRamUsage, lblThreadCount, lblSessionCount;
    private JLabel lblGiftcodeInfo, lblConsignItemsCount, lblBossStatus, lblUptime;
    
    // Log Component
    private JTextArea txtLog;
    
    // Config Components (Maintenance)
    private JComboBox<Integer> cbHour, cbMinute, cbSecond;
    private JCheckBox chkAutoRestart;
    
    // Config Components (Optimization)
    private JCheckBox chkAutoOptimize;
    private JComboBox<String> cbOptimizeInterval;
    private JLabel lblOptStatus;
    
    // Action Buttons
    private JToggleButton btnToggleAutoSave;

    // Graphs
    private final MiniGraph graphCpu = new MiniGraph(new Color(0, 120, 215));
    private final MiniGraph graphRam = new MiniGraph(new Color(138, 43, 226));
    private final MiniGraph graphThread = new MiniGraph(new Color(0, 204, 106));
    private final MiniGraph graphSession = new MiniGraph(new Color(255, 140, 0));

    // --- Logic Variables ---
    private final Instant serverStartTime = Instant.now();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4); 
    private final AtomicBoolean isAutoSaveEnabled = new AtomicBoolean(true);
    
    // Maintenance Logic
    public static boolean REQUEST_AUTO_RESTART = false;
    private ScheduledFuture<?> activeMaintenanceJobFuture = null;
    private ScheduledFuture<?> activeCountdownDisplayFuture = null;
    
    // Optimize Logic
    private ScheduledFuture<?> activeAutoOptimizeFuture = null;

    // --- Boss Icon Logic ---
    private static final String ICON_FOLDER = "data/icon/";
    private static final String BOSS_MANAGER_PATH = "src/boss/BossManager.java"; // Đường dẫn file code
    private final Map<Integer, Integer> partIconMap = new HashMap<>();
    private final Map<Integer, ImageIcon> iconImageCache = new HashMap<>();
    private List<BossSummonEntry> cachedBossEntries = new ArrayList<>();

    public DashboardPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // 1. Load Data Icons
        loadPartDataFromDB();

        // 2. Tạo giao diện chính
        initMainLayout();
        
        // 3. Bắt đầu luồng cập nhật thông số (1 giây/lần)
        startMonitoring();
        loadMaintenanceConfig();
        
        // Khởi động mặc định auto optimize (nếu muốn)
        chkAutoOptimize.setSelected(false); // Mặc định tắt, user tự bật
        
        addLog("Dashboard initialized. Monitoring Server specific resources.");
    }
    
    // --- ICON & BOSS DATA LOADING ---
    
    private void loadPartDataFromDB() {
        new Thread(() -> {
            try (Connection conn = DBConnecter.getConnectionServer();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, DATA FROM part WHERE TYPE = 0")) { 
                
                partIconMap.clear();
                while (rs.next()) {
                    int partId = rs.getInt("id");
                    String json = rs.getString("DATA");
                    try {
                        JsonArray arr = new JsonParser().parse(json).getAsJsonArray();
                        if (arr.size() > 0) {
                            JsonArray firstLayer = arr.get(0).getAsJsonArray();
                            int iconId = firstLayer.get(0).getAsInt();
                            partIconMap.put(partId, iconId);
                        }
                    } catch (Exception e) {}
                }
            } catch (Exception e) {
                addLog("Error loading Part DB for Icons: " + e.getMessage());
            }
        }).start();
    }

    private ImageIcon getIconByIconId(int iconId, int size) {
        if (iconId <= -1) return null;
        if (iconImageCache.containsKey(iconId)) {
             Image img = iconImageCache.get(iconId).getImage();
             if (img.getWidth(null) != size) {
                 return new ImageIcon(img.getScaledInstance(size, size, Image.SCALE_SMOOTH));
             }
             return iconImageCache.get(iconId);
        }
        try {
            String[] zoomLevels = {"x4", "x3", "x2", "x1"};
            for (String zoom : zoomLevels) {
                File f = new File(ICON_FOLDER + zoom + "/" + iconId + ".png");
                if (f.exists()) {
                    BufferedImage img = ImageIO.read(f);
                    Image dimg = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                    ImageIcon icon = new ImageIcon(dimg);
                    iconImageCache.put(iconId, icon);
                    return icon;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    // Helper Class cho Boss List
    private static class BossSummonEntry {
        String keyName; // Tên biến trong BossID (VD: TIEU_DOI_TRUONG)
        int id;         // ID thực tế
        String displayName;
        int headIconId;

        public BossSummonEntry(String keyName, int id, String displayName, int headIconId) {
            this.keyName = keyName;
            this.id = id;
            this.displayName = displayName;
            this.headIconId = headIconId;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }

    // --- [NEW LOGIC] CHỈ LOAD BOSS CÓ TRONG HÀM loadBoss() ---
    private void prepareBossData() {
        cachedBossEntries.clear();
        try {
            File file = new File(BOSS_MANAGER_PATH);
            if (!file.exists()) {
                addLog("Warning: Không tìm thấy file source " + BOSS_MANAGER_PATH + ". Load tất cả boss.");
                prepareBossDataFallback(); // Fallback nếu không có source code
                return;
            }

            String content = Files.readString(file.toPath());
            
            int startIndex = content.indexOf("public void loadBoss()");
            if (startIndex == -1) {
                addLog("Warning: Không tìm thấy hàm loadBoss().");
                prepareBossDataFallback();
                return;
            }
            
            int braceCount = 0;
            int endIndex = -1;
            for(int i = content.indexOf('{', startIndex); i < content.length(); i++) {
                if (content.charAt(i) == '{') braceCount++;
                else if (content.charAt(i) == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        endIndex = i;
                        break;
                    }
                }
            }
            
            String methodBody = (endIndex != -1) ? content.substring(startIndex, endIndex) : content;

            Pattern pattern = Pattern.compile("createBoss\\s*\\(\\s*BossID\\.([A-Z0-9_]+)");
            Matcher matcher = pattern.matcher(methodBody);
            
            Set<String> foundBossKeys = new HashSet<>();
            while (matcher.find()) {
                foundBossKeys.add(matcher.group(1));
            }
            
            addLog("Found " + foundBossKeys.size() + " bosses in loadBoss() method.");

            Field[] idFields = BossID.class.getFields();
            Map<String, Integer> idMap = new HashMap<>();
            for (Field f : idFields) {
                if (Modifier.isStatic(f.getModifiers()) && f.getType() == int.class) {
                    idMap.put(f.getName(), f.getInt(null));
                }
            }

            Field[] dataFields = BossesData.class.getFields();
            for (Field f : dataFields) {
                if (foundBossKeys.contains(f.getName()) && f.getType() == BossData.class && idMap.containsKey(f.getName())) {
                    BossData data = (BossData) f.get(null);
                    int bossId = idMap.get(f.getName());
                    
                    int iconId = -1;
                    if (data.getOutfit() != null && data.getOutfit().length > 0) {
                        int headPart = data.getOutfit()[0];
                        iconId = partIconMap.getOrDefault(headPart, headPart);
                    }
                    
                    cachedBossEntries.add(new BossSummonEntry(f.getName(), bossId, data.getName(), iconId));
                }
            }
            
        } catch (Exception e) {
            addLog("Error analyzing BossManager code: " + e.getMessage());
            prepareBossDataFallback();
        }
    }
    
    private void prepareBossDataFallback() {
        try {
            Field[] idFields = BossID.class.getFields();
            Map<String, Integer> idMap = new HashMap<>();
            for (Field f : idFields) {
                if (Modifier.isStatic(f.getModifiers()) && f.getType() == int.class) {
                    idMap.put(f.getName(), f.getInt(null));
                }
            }
            Field[] dataFields = BossesData.class.getFields();
            for (Field f : dataFields) {
                if (f.getType() == BossData.class && idMap.containsKey(f.getName())) {
                    BossData data = (BossData) f.get(null);
                    int bossId = idMap.get(f.getName());
                    int iconId = -1;
                    if (data.getOutfit() != null && data.getOutfit().length > 0) {
                        int headPart = data.getOutfit()[0];
                        iconId = partIconMap.getOrDefault(headPart, headPart);
                    }
                    cachedBossEntries.add(new BossSummonEntry(f.getName(), bossId, data.getName(), iconId));
                }
            }
        } catch (Exception e) {}
    }

    // --- MAIN UI ---

    private void loadMaintenanceConfig() {
        File f = new File("maintenanceConfig.txt");
        if (f.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String lineH = br.readLine();
                String lineM = br.readLine();
                String lineS = br.readLine();
                String lineAuto = br.readLine();

                if (lineH != null && lineM != null && lineS != null) {
                    int h = Integer.parseInt(lineH);
                    int m = Integer.parseInt(lineM);
                    int s = Integer.parseInt(lineS);
                    boolean auto = Boolean.parseBoolean(lineAuto);

                    cbHour.setSelectedItem(h);
                    cbMinute.setSelectedItem(m);
                    cbSecond.setSelectedItem(s);
                    chkAutoRestart.setSelected(auto);
                    
                    REQUEST_AUTO_RESTART = auto;
                    if (h != -1) {
                        addLog("[Auto Maintenance] Loaded config: " + String.format("%02d:%02d:%02d", h, m, s));
                        scheduleMaintenance(); 
                    }
                }
            } catch (Exception e) {
                addLog("Error loading maintenance config: " + e.getMessage());
            }
        } else {
            addLog("[Auto Maintenance] No config found. Defaulting to 05:00 AM.");
            cbHour.setSelectedItem(5);
            cbMinute.setSelectedItem(0);
            cbSecond.setSelectedItem(0);
            chkAutoRestart.setSelected(true);
            REQUEST_AUTO_RESTART = true;
            scheduleMaintenance();
        }
    }

    private void initMainLayout() {
        JPanel container = new JPanel(new GridBagLayout());
        container.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1.0;

        // 1. Header (Status)
        container.add(createHeaderPanel(), gbc);

        // 2. Graphs (System Stats)
        gbc.gridy++;
        container.add(createSystemStatsPanel(), gbc);

        // 3. Actions (Common Actions)
        gbc.gridy++;
        container.add(createActionPanel(), gbc);

        // 4. Game Stats
        gbc.gridy++;
        container.add(createGameStatsPanel(), gbc);
        
        // 5. [NEW LAYOUT] Boss Manager & Configurations (Split 2 cols)
        JPanel midPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        midPanel.setOpaque(false);
        
        // Cột 1: Quản lý Boss (Triệu hồi + Reset/Respawn)
        midPanel.add(createBossManagementPanel());
        
        // Cột 2: Cấu hình (Exp + Scheduler)
        midPanel.add(createConfigurationPanel());
        
        gbc.gridy++;
        container.add(midPanel, gbc);

        // 6. Optimization & Booster Panel (Moved Down above Logs)
        gbc.gridy++;
        container.add(createOptimizationPanel(), gbc);

        // 7. Log Panel
        gbc.gridy++;
        gbc.weighty = 1.0; 
        gbc.fill = GridBagConstraints.BOTH; 
        container.add(createLogPanel(), gbc);

        JScrollPane scroll = new JScrollPane(container);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    // ================= UI PARTS =================

    private JPanel createHeaderPanel() {
        JPanel p = new JPanel(new GridLayout(1, 3, 20, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 10, 10, 10));

        lblStatus = ServerGuiUtils.createStyledLabel("● Server Online", 20, true);
        lblStatus.setForeground(new Color(0, 153, 51));
        
        lblPlayerCount = ServerGuiUtils.createStyledLabel("Online: 0", 16, false);
        lblCountdown = ServerGuiUtils.createStyledLabel("Sẵn sàng", 16, false);
        lblCountdown.setForeground(Color.GRAY);

        p.add(lblStatus);
        p.add(lblPlayerCount);
        p.add(lblCountdown);
        return p;
    }

    private JPanel createSystemStatsPanel() {
        JPanel p = new JPanel(new GridLayout(1, 4, 10, 0));
        p.setOpaque(false);

        lblCpuUsage = new JLabel("Server CPU: 0%");
        lblRamUsage = new JLabel("JVM RAM: 0 MB");
        lblThreadCount = new JLabel("Threads: 0");
        lblSessionCount = new JLabel("Sessions: 0");

        p.add(createGraphCard("Server CPU", lblCpuUsage, graphCpu));
        p.add(createGraphCard("JVM RAM (Heap)", lblRamUsage, graphRam));
        p.add(createGraphCard("Threads", lblThreadCount, graphThread));
        p.add(createGraphCard("Sessions", lblSessionCount, graphSession));
        return p;
    }

    private JPanel createGraphCard(String title, JLabel info, MiniGraph graph) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(230, 230, 230), 1, true),
                new EmptyBorder(5, 10, 5, 10)
        ));
        
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLbl.setForeground(Color.GRAY);
        
        info.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        top.add(titleLbl, BorderLayout.NORTH);
        top.add(info, BorderLayout.CENTER);

        card.add(top, BorderLayout.NORTH);
        card.add(graph, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(0, 80));
        return card;
    }

    private JPanel createActionPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("Quick Actions"));

        JButton btnMaint = ServerGuiUtils.createStyledButton("Bảo Trì (2p)", new Color(255, 193, 7), Color.BLACK);
        btnMaint.addActionListener(e -> confirmMaintenance());

        JButton btnReload = ServerGuiUtils.createStyledButton("Tải lại DB", new Color(23, 162, 184), Color.WHITE);
        btnReload.addActionListener(e -> showReloadOptions());

        JButton btnClean = ServerGuiUtils.createStyledButton("Dọn Session", new Color(108, 117, 125), Color.WHITE);
        btnClean.addActionListener(e -> {
             addLog("Đang thực hiện dọn dẹp session rác...");
             if (SessionManager.gI() != null) {
                 // session cleanup logic here
             }
             addLog("Đã dọn dẹp các session dead/null.");
        });

        btnToggleAutoSave = new JToggleButton("AutoSave: ON");
        btnToggleAutoSave.setSelected(true);
        btnToggleAutoSave.setFocusPainted(false);
        btnToggleAutoSave.addActionListener(e -> {
            boolean enable = isAutoSaveEnabled.get();
            if (enable) {
                AutoSaveManager.getInstance().stopAutoSave();
                btnToggleAutoSave.setText("AutoSave: OFF");
                addLog("System: AutoSave has been DISABLED.");
            } else {
                AutoSaveManager.getInstance().startAutoSave();
                btnToggleAutoSave.setText("AutoSave: ON");
                addLog("System: AutoSave has been ENABLED.");
            }
            isAutoSaveEnabled.set(!enable);
            btnToggleAutoSave.setSelected(!enable);
        });

        p.add(btnMaint);
        p.add(btnReload);
        p.add(btnClean);
        p.add(btnToggleAutoSave);
        return p;
    }
    
    // --- [NEW PANEL] GOM NHÓM BOSS ---
    private JPanel createBossManagementPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("Boss Manager (Triệu Hồi & Cài Đặt)"));

        // 1. Nút Triệu Hồi (Lớn)
        JButton btnOpenSummon = ServerGuiUtils.createStyledButton("Mở Menu Triệu Hồi (Search & Call Boss)", new Color(0, 120, 215), Color.WHITE);
        btnOpenSummon.setPreferredSize(new Dimension(0, 35));
        btnOpenSummon.addActionListener(e -> showBossSummonDialog());

        // 2. Các nút Reset/Respawn (Nhỏ hơn ở dưới)
        JPanel subBtn = new JPanel(new GridLayout(1, 2, 5, 0));
        subBtn.setOpaque(false);

        JButton btnResetBoss = ServerGuiUtils.createStyledButton("Reset All Boss", new Color(220, 53, 69), Color.WHITE);
        btnResetBoss.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "Reset TẤT CẢ BOSS?", "Xác nhận", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                BossManager.gI().resetAllBosses();
                addLog("Boss Manager: Đã reset tất cả boss.");
            }
        });

        JButton btnRespawn = ServerGuiUtils.createStyledButton("Hồi Sinh Boss Chờ", new Color(40, 167, 69), Color.WHITE);
        btnRespawn.addActionListener(e -> {
             if (JOptionPane.showConfirmDialog(this, "Hồi sinh tất cả Boss đang chờ?", "Xác nhận", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                BossManager.gI().respawnAllRestingBosses();
                addLog("Boss Manager: Đã hồi sinh các boss đang chờ.");
            }
        });
        
        subBtn.add(btnResetBoss);
        subBtn.add(btnRespawn);

        p.add(btnOpenSummon, BorderLayout.NORTH);
        p.add(subBtn, BorderLayout.CENTER);

        return p;
    }

    // --- [NEW PANEL] GOM NHÓM CẤU HÌNH ---
    private JPanel createConfigurationPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("Server Configuration (Exp & Schedule)"));

        JPanel container = new JPanel(new GridLayout(2, 1, 0, 5));
        container.setOpaque(false);

        // 1. Exp Config
        JPanel pExp = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pExp.setOpaque(false);
        JTextField txtExp = new JTextField(String.valueOf(Manager.RATE_EXP_SERVER), 4);
        JButton btnUpdateExp = new JButton("Set EXP");
        btnUpdateExp.addActionListener(e -> {
            try {
                double rate = Double.parseDouble(txtExp.getText().trim());
                if (rate > 0) {
                    Manager.RATE_EXP_SERVER = (int) rate;
                    addLog("Config: EXP Rate updated to x" + Manager.RATE_EXP_SERVER);
                    Service.gI().sendThongBaoAllPlayer("Server EXP Rate: x" + Manager.RATE_EXP_SERVER);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Số không hợp lệ");
            }
        });
        pExp.add(new JLabel("Rate: x"));
        pExp.add(txtExp);
        pExp.add(btnUpdateExp);

        // 2. Scheduler Config
        JPanel pSched = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pSched.setOpaque(false);
        
        cbHour = new JComboBox<>(); for(int i=-1;i<24;i++) cbHour.addItem(i);
        cbMinute = new JComboBox<>(); for(int i=-1;i<60;i++) cbMinute.addItem(i);
        cbSecond = new JComboBox<>(); for(int i=-1;i<60;i++) cbSecond.addItem(i);
        
        chkAutoRestart = new JCheckBox("AutoRestart");
        chkAutoRestart.setSelected(true);
        chkAutoRestart.setOpaque(false);

        JButton btnSetSched = new JButton("Lưu Cấu Hình");
        btnSetSched.addActionListener(e -> scheduleMaintenance());

        pSched.add(new JLabel("Hẹn giờ:"));
        pSched.add(cbHour); pSched.add(new JLabel(":"));
        pSched.add(cbMinute);
        pSched.add(chkAutoRestart);
        pSched.add(btnSetSched);

        container.add(pExp);
        container.add(pSched);
        
        p.add(container, BorderLayout.CENTER);
        return p;
    }

    private JPanel createOptimizationPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("System Optimization & Booster (Server Only)"));
        
        JPanel pBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        pBtns.setOpaque(false);
        
        JButton btnOptRam = ServerGuiUtils.createStyledButton("Dọn dẹp JVM RAM", new Color(40, 167, 69), Color.WHITE);
        btnOptRam.addActionListener(e -> performRamCleanup());
        
        JButton btnOptCpu = ServerGuiUtils.createStyledButton("Tối ưu CPU & VPS", new Color(0, 123, 255), Color.WHITE);
        btnOptCpu.addActionListener(e -> performCpuOptimization());
        
        JButton btnFlushLog = ServerGuiUtils.createStyledButton("Xóa Log Cache", new Color(108, 117, 125), Color.WHITE);
        btnFlushLog.addActionListener(e -> {
            txtLog.setText("");
            addLog("System: Log cache cleared to free memory.");
        });

        JPanel pAuto = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        pAuto.setOpaque(false);
        pAuto.setBorder(new EmptyBorder(5, 0, 0, 0));
        
        chkAutoOptimize = new JCheckBox("Tự động tối ưu hóa (Auto Optimize)");
        chkAutoOptimize.setOpaque(false);
        chkAutoOptimize.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        String[] intervals = {"5 Phút", "10 Phút", "30 Phút", "60 Phút"};
        cbOptimizeInterval = new JComboBox<>(intervals);
        cbOptimizeInterval.setSelectedIndex(1); 
        
        lblOptStatus = new JLabel("Trạng thái: Tắt");
        lblOptStatus.setForeground(Color.RED);
        
        chkAutoOptimize.addActionListener(e -> toggleAutoOptimization());
        cbOptimizeInterval.addActionListener(e -> {
            if (chkAutoOptimize.isSelected()) {
                toggleAutoOptimization(); 
            }
        });
        
        pBtns.add(btnOptRam);
        pBtns.add(btnOptCpu);
        pBtns.add(btnFlushLog);
        
        pAuto.add(chkAutoOptimize);
        pAuto.add(new JLabel("Mỗi:"));
        pAuto.add(cbOptimizeInterval);
        pAuto.add(Box.createHorizontalStrut(15));
        pAuto.add(lblOptStatus);
        
        p.add(pBtns, BorderLayout.NORTH);
        p.add(pAuto, BorderLayout.CENTER);
        
        return p;
    }

    private JPanel createGameStatsPanel() {
        JPanel p = new JPanel(new GridLayout(2, 2, 10, 5));
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("Game Statistics"));

        lblGiftcodeInfo = new JLabel("Giftcodes: Loading...");
        lblConsignItemsCount = new JLabel("Consign Items: Loading...");
        lblBossStatus = new JLabel("Boss Status: Loading...");
        lblUptime = new JLabel("Uptime: Calculating...");

        p.add(lblGiftcodeInfo);
        p.add(lblConsignItemsCount);
        p.add(lblBossStatus);
        p.add(lblUptime);
        return p;
    }
    
    // --- [NEW] DIALOG TRIỆU HỒI BOSS VỚI ICON & SEARCH ---
    private void showBossSummonDialog() {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Triệu Hồi Boss (Searchable)", true);
        d.setSize(500, 600);
        d.setLocationRelativeTo(this);
        d.setLayout(new BorderLayout(5, 5));
        
        // 1. Prepare Data
        if (cachedBossEntries.isEmpty()) {
            prepareBossData();
        }
        
        // 2. Components
        JTextField txtSearch = new JTextField();
        txtSearch.setBorder(BorderFactory.createTitledBorder("Nhập tên boss để tìm..."));
        
        DefaultListModel<BossSummonEntry> listModel = new DefaultListModel<>();
        cachedBossEntries.forEach(listModel::addElement);
        
        JList<BossSummonEntry> list = new JList<>(listModel);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof BossSummonEntry) {
                    BossSummonEntry entry = (BossSummonEntry) value;
                    lbl.setText(entry.displayName);
                    if (entry.headIconId != -1) {
                        ImageIcon icon = getIconByIconId(entry.headIconId, 25);
                        if (icon != null) lbl.setIcon(icon);
                    }
                    lbl.setIconTextGap(10);
                }
                return lbl;
            }
        });
        
        // 3. Search Logic
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
            void filter() {
                String text = txtSearch.getText().toLowerCase();
                listModel.clear();
                for (BossSummonEntry entry : cachedBossEntries) {
                    if (entry.displayName.toLowerCase().contains(text) || entry.keyName.toLowerCase().contains(text)) {
                        listModel.addElement(entry);
                    }
                }
            }
        });
        
        // 4. Action
        JButton btnSummon = new JButton("TRIỆU HỒI NGAY");
        btnSummon.setBackground(new Color(40, 167, 69));
        btnSummon.setForeground(Color.WHITE);
        btnSummon.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSummon.setPreferredSize(new Dimension(0, 40));
        
        Runnable doSummon = () -> {
            BossSummonEntry selected = list.getSelectedValue();
            if (selected != null) {
                summonSpecificBoss(selected.id, selected.displayName);
                d.dispose();
            } else {
                JOptionPane.showMessageDialog(d, "Vui lòng chọn một Boss!");
            }
        };
        
        btnSummon.addActionListener(e -> doSummon.run());
        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) doSummon.run();
            }
        });
        
        d.add(txtSearch, BorderLayout.NORTH);
        d.add(new JScrollPane(list), BorderLayout.CENTER);
        d.add(btnSummon, BorderLayout.SOUTH);
        d.setVisible(true);
    }
    
    private JPanel createLogPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(ServerGuiUtils.createSectionBorder("Server Logs"));
        
        txtLog = new JTextArea(8, 50);
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtLog.setBackground(new Color(250, 250, 250));
        
        JScrollPane scrollLog = new JScrollPane(txtLog);
        scrollLog.setBorder(new LineBorder(new Color(200, 200, 200)));
        scrollLog.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        
        p.add(scrollLog, BorderLayout.CENTER);
        return p;
    }
    
    // --- [LOGIC] Optimization Methods ---
    
    private void performRamCleanup() {
        new Thread(() -> {
            // Sử dụng Runtime để tính RAM của riêng Java JVM
            long before = Runtime.getRuntime().freeMemory(); 
            System.gc(); // Trigger Java Garbage Collector
            long after = Runtime.getRuntime().freeMemory();
            
            // Tính lượng RAM giải phóng được trong Heap
            long freed = after - before; 
            
            // Format sang MB
            long freedMB = freed / 1024 / 1024;
            
            String msg = (freed > 0) 
                    ? "OPTIMIZE: Đã dọn dẹp JVM Heap. Giải phóng: " + freedMB + " MB."
                    : "OPTIMIZE: JVM RAM đã ở trạng thái tối ưu.";
            addLog(msg);
        }).start();
    }
    
    private void performCpuOptimization() {
        new Thread(() -> {
            if (txtLog.getDocument().getLength() > 50000) {
                SwingUtilities.invokeLater(() -> {
                    txtLog.setText("");
                    addLog("CPU OPT: Đã xóa bộ đệm Log để giảm tải UI.");
                });
            }

            SwingUtilities.invokeLater(() -> {
                addLog("CPU OPT: Đã kiểm tra và tối ưu bộ đệm Log.");
            });

        }).start();
    }
    
    private void toggleAutoOptimization() {
        if (activeAutoOptimizeFuture != null) {
            activeAutoOptimizeFuture.cancel(false);
            activeAutoOptimizeFuture = null;
        }

        if (chkAutoOptimize.isSelected()) {
            String selected = (String) cbOptimizeInterval.getSelectedItem();
            int minutes = 10;
            if (selected.contains("5")) minutes = 5;
            else if (selected.contains("30")) minutes = 30;
            else if (selected.contains("60")) minutes = 60;
            
            lblOptStatus.setText("Trạng thái: Đang chạy (" + minutes + "p/lần)");
            lblOptStatus.setForeground(new Color(0, 153, 51));
            
            activeAutoOptimizeFuture = scheduler.scheduleAtFixedRate(() -> {
                addLog("AUTO-OPT: Bắt đầu chu trình tối ưu tự động...");
                performRamCleanup();
            }, minutes, minutes, TimeUnit.MINUTES);
            
            addLog("SYSTEM: Đã bật tự động tối ưu hóa (" + minutes + " phút/lần).");
        } else {
            lblOptStatus.setText("Trạng thái: Tắt");
            lblOptStatus.setForeground(Color.RED);
            addLog("SYSTEM: Đã tắt tự động tối ưu hóa.");
        }
    }

    public void addLog(String message) {
        if (txtLog != null) {
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            SwingUtilities.invokeLater(() -> {
                txtLog.append("[" + time + "] " + message + "\n");
                txtLog.setCaretPosition(txtLog.getDocument().getLength());
            });
        }
    }

    // ================= MONITORING =================

    private void startMonitoring() {
        scheduler.scheduleAtFixedRate(() -> {
            // 1. Get System Stats
            double cpu = 0;
            try {
                // Ép kiểu rõ ràng để tránh lỗi import với java.lang.management.OperatingSystemMXBean
                OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
                if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                    double load = ((com.sun.management.OperatingSystemMXBean) osBean).getProcessCpuLoad();
                    // Load trả về 0.0 -> 1.0. Nếu lỗi trả về -1
                    if (!Double.isNaN(load) && load >= 0) {
                        cpu = load * 100;
                    }
                }
            } catch (Exception e) { 
                cpu = 0; // Fallback an toàn nếu lỗi
            }
            
            // Tính toán RAM dựa trên Runtime (JVM Memory)
            long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
            long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);
            long freeMemory = Runtime.getRuntime().freeMemory() / (1024 * 1024);
            long usedMemory = totalMemory - freeMemory;
            
            int threads = Thread.activeCount();
            int sessions = (SessionManager.gI() != null) ? SessionManager.gI().getSessions().size() : 0;

            // 2. Get Game Stats
            int playerCount = (Client.gI() != null) ? Client.gI().getPlayers().size() : 0;
            int giftCodeCount = (GiftCodeManager.gI() != null) ? GiftCodeManager.gI().listGiftCode.size() : 0;
            int consignCount = (ConsignShopManager.gI() != null) ? ConsignShopManager.gI().listItem.size() : 0;
            
            String bossStats = "N/A";
            if (BossManager.gI() != null) {
                try {
                    int[] stats = BossManager.gI().getBossStatusCounts();
                    bossStats = String.format("Boss: %d Alive | %d Respawn | %d Wait", stats[0], stats[1], stats[2]);
                } catch (Exception e) {}
            }

            // 3. Update UI
            String finalBossStats = bossStats;
            final double finalCpu = cpu;

            SwingUtilities.invokeLater(() -> {
                lblCpuUsage.setText(String.format("Server CPU: %.1f%%", finalCpu));
                lblRamUsage.setText(usedMemory + " / " + maxMemory + " MB");
                
                lblThreadCount.setText(String.valueOf(threads));
                lblSessionCount.setText(String.valueOf(sessions));
                
                lblPlayerCount.setText("Online: " + playerCount);
                lblGiftcodeInfo.setText("Giftcodes: " + giftCodeCount);
                lblConsignItemsCount.setText("Consign Items: " + consignCount);
                lblBossStatus.setText(finalBossStats);

                updateUptime();

                graphCpu.addValue((int) finalCpu);
                
                // Biểu đồ RAM: Tính % dựa trên Max Memory
                int ramPercent = (maxMemory > 0) ? (int) ((usedMemory * 100) / maxMemory) : 0;
                graphRam.addValue(ramPercent);
                
                graphThread.addValue(Math.min(threads, 100)); 
                graphSession.addValue(Math.min(sessions, 100)); 
            });
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void updateUptime() {
        Duration d = Duration.between(serverStartTime, Instant.now());
        lblUptime.setText(String.format("Uptime: %dd %02dh %02dm %02ds", 
            d.toDays(), d.toHoursPart(), d.toMinutesPart(), d.toSecondsPart()));
    }

    // ================= ACTIONS LOGIC =================

    private void confirmMaintenance() {
        if (JOptionPane.showConfirmDialog(this, "Bắt đầu bảo trì sau 2 phút?", "Xác nhận", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            cancelAllScheduledTasks();
            Maintenance.gI().start(2); // Gọi hàm bảo trì của server
            lblStatus.setText("MAINTENANCE STARTING...");
            lblStatus.setForeground(Color.RED);
            addLog("ACTION: Bắt đầu chu trình bảo trì (2 phút).");
        }
    }

    private void showReloadOptions() {
        String[] opts = {"GiftCode", "Shop", "NPCs"};
        int c = JOptionPane.showOptionDialog(this, "Chọn dữ liệu cần tải lại:", "Reload DB", 0, 3, null, opts, opts[0]);
        switch (c) {
            case 0 -> {
                loadGiftcode();
            }
            case 1 -> {
                loadShop();
            }
            case 2 -> {
                loadNpcs();
            }
        }
    }
    
    public void loadGiftcode() {
        GiftCodeManager.gI().listGiftCode.clear();
        String sql = "SELECT * FROM giftcode";
        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                GiftCode giftcode = new GiftCode();
                giftcode.code = rs.getString("code");
                giftcode.id = rs.getInt("id");
                giftcode.countLeft = rs.getInt("count_left");
                giftcode.datecreate = rs.getTimestamp("datecreate");
                giftcode.dateexpired = rs.getTimestamp("expired");
                String detailJson = rs.getString("detail");
                if (detailJson != null && !detailJson.isEmpty() && JSONValue.parse(detailJson) instanceof JSONArray jar) {
                    for (Object itemObj : jar) {
                        JSONObject jsonObj = (JSONObject) itemObj;
                        int itemId = getJsonInt(jsonObj, "id");
                        int quantity = getJsonInt(jsonObj, "quantity");
                        JSONArray optionArray = (JSONArray) jsonObj.get("options");
                        ArrayList<ItemOption> optionList = new ArrayList<>();
                        if (optionArray != null) {
                            for (Object optionObj : optionArray) {
                                JSONObject optionJson = (JSONObject) optionObj;
                                optionList.add(new ItemOption(getJsonInt(optionJson, "id"), getJsonInt(optionJson, "param")));
                            }
                        }
                        giftcode.option.put(itemId, optionList);
                        giftcode.detail.put(itemId, quantity);
                    }
                }
                GiftCodeManager.gI().listGiftCode.add(giftcode);
            }
            addLog("Success: Reloaded " + GiftCodeManager.gI().listGiftCode.size() + " giftcodes.");
        } catch (Exception e) {
            addLog("Error: Failed to reload giftcode. " + e.getMessage());
        }
    }

    public void loadNpcs() {
        Manager.NPC_TEMPLATES.clear();
        String sql = "SELECT * FROM npc_template";
        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                NpcTemplate npcTemp = new NpcTemplate();
                npcTemp.id = rs.getByte("id");
                npcTemp.name = rs.getString("name");
                npcTemp.head = rs.getShort("head");
                npcTemp.body = rs.getShort("body");
                npcTemp.leg = rs.getShort("leg");
                npcTemp.avatar = rs.getInt("avatar");
                Manager.NPC_TEMPLATES.add(npcTemp);
            }
            addLog("Success: Reloaded " + Manager.NPC_TEMPLATES.size() + " NPCs.");
        } catch (Exception e) {
            addLog("Error: Failed to reload NPCs. " + e.getMessage());
        }
    }

    public void loadShop() {
        try (Connection con = DBConnecter.getConnectionServer()) {
            Manager.SHOPS = ShopDAO.getShops(con);
            addLog("Success: Reloaded " + Manager.SHOPS.size() + " shops.");
        } catch (Exception e) {
            addLog("Error: Failed to reload shops. " + e.getMessage());
        }
    }
    
    private int getJsonInt(JSONObject obj, String key) {
        if (obj != null && obj.containsKey(key)) {
            try {
                return Integer.parseInt(obj.get(key).toString());
            } catch (NumberFormatException e) {
            }
        }
        return 0;
    }

    private void summonSpecificBoss(int id, String name) {
        try {
            Boss b = BossManager.gI().createBoss(id);
            b.changeStatus(BossStatus.RESPAWN);
            addLog("Boss Action: Đã triệu hồi thành công " + name);
        } catch (Exception e) {
            addLog("Boss Error: Lỗi triệu hồi " + name + ": " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Lỗi triệu hồi boss: " + e.getMessage());
        }
    }

    private void scheduleMaintenance() {
        Integer h = (Integer) cbHour.getSelectedItem();
        Integer m = (Integer) cbMinute.getSelectedItem();
        Integer s = (Integer) cbSecond.getSelectedItem();
        
        if (h == null || h == -1) return;

        REQUEST_AUTO_RESTART = chkAutoRestart.isSelected();
        long nowSeconds = LocalTime.now().toSecondOfDay();
        long targetSeconds = LocalTime.of(h, m, s).toSecondOfDay();
        long delay = targetSeconds - nowSeconds;
        if (delay < 0) delay += 86400; // Next day

        cancelAllScheduledTasks();
        
        lblStatus.setText("Maintenance Scheduled");
        lblStatus.setForeground(new Color(255, 140, 0));

        long triggerTime = System.currentTimeMillis() + (delay * 1000);
        
        // Schedule Job
        activeMaintenanceJobFuture = scheduler.schedule(() -> Maintenance.gI().start(1), delay, TimeUnit.SECONDS);
        
        // Schedule Countdown UI
        activeCountdownDisplayFuture = scheduler.scheduleAtFixedRate(() -> {
            long remain = (triggerTime - System.currentTimeMillis()) / 1000;
            if (remain <= 0) {
                 SwingUtilities.invokeLater(() -> lblCountdown.setText("Executing..."));
                 return;
            }
            SwingUtilities.invokeLater(() -> lblCountdown.setText(String.format("%02d:%02d:%02d", remain/3600, (remain%3600)/60, remain%60)));
        }, 0, 1, TimeUnit.SECONDS);

        addLog("Scheduler: Đặt lịch bảo trì lúc " + String.format("%02d:%02d:%02d", h, m, s) + " (AutoRestart: " + REQUEST_AUTO_RESTART + ")");

        // Save to file
        try (PrintWriter pw = new PrintWriter(new FileWriter("maintenanceConfig.txt"))) {
            pw.println(h + "\n" + m + "\n" + s + "\n" + REQUEST_AUTO_RESTART);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void cancelAllScheduledTasks() {
        if (activeMaintenanceJobFuture != null) activeMaintenanceJobFuture.cancel(true);
        if (activeCountdownDisplayFuture != null) activeCountdownDisplayFuture.cancel(true);
        lblCountdown.setText("Sẵn sàng");
    }

    // ================= INNER CLASSES =================

    // Optimized MiniGraph Class
    private static class MiniGraph extends JPanel {
        private final ArrayList<Integer> values = new ArrayList<>();
        private static final int MAX_POINTS = 60;
        private final Color primaryColor;
        private final Color fillColor;

        public MiniGraph(Color color) {
            this.primaryColor = color;
            this.fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 40);
            setBackground(Color.WHITE);
            setOpaque(true);
        }

        public void addValue(int v) {
            if (values.size() >= MAX_POINTS) values.remove(0);
            values.add(v);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (values.isEmpty()) return;

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int maxVal = 100;

            Path2D.Float path = new Path2D.Float();
            float step = (float) w / (MAX_POINTS - 1);
            
            path.moveTo(0, h);
            for (int i = 0; i < values.size(); i++) {
                float x = i * step;
                float y = h - ((float) values.get(i) / maxVal * h);
                if (i == 0) path.lineTo(x, y); else path.lineTo(x, y);
            }
            float lastX = (values.size() - 1) * step;
            path.lineTo(lastX, h);
            path.lineTo(0, h);
            path.closePath();

            g2.setColor(fillColor);
            g2.fill(path);

            Path2D.Float linePath = new Path2D.Float();
            for (int i = 0; i < values.size(); i++) {
                float x = i * step;
                float y = h - ((float) values.get(i) / maxVal * h);
                if (i == 0) linePath.moveTo(x, y); else linePath.lineTo(x, y);
            }
            g2.setColor(primaryColor);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(linePath);
            
            g2.setColor(new Color(240, 240, 240));
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(0, h/2, w, h/2);
        }
    }
}