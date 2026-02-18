package nro.server.ui;

import nro.server.ServerManager;
import firewall.ProxyManager;
import nro.server.AutoSaveManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.time.Instant;

public class ServerManagerUI extends JFrame {

    // --- Class nội bộ quản lý Sidebar Item ---
    private static class NavItem {

        String name;
        Icon icon;
        String key;

        public NavItem(String name, String iconPath, String key) {
            this.name = name;
            this.key = key;
            this.icon = ServerGuiUtils.loadIcon(iconPath);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final Instant serverStartTime;
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private JList<NavItem> sidebar;

    public static volatile boolean REQUEST_AUTO_RESTART = false;

    public ServerManagerUI() {
        super("Server Control Panel - Manager");

        // Setup giao diện FlatLaf cho hiện đại (nếu có thư viện)
        ServerGuiUtils.setupTheme();

        initUI();
        startServerProcesses();

        this.serverStartTime = Instant.now();

        // Hook tắt server an toàn
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (REQUEST_AUTO_RESTART) {
                triggerRestartProcess();
            }
        }));
    }

    // --- Logic Restart Server ---
    public void triggerRestartProcess() {
        int seconds = 5;
        System.out.println(">>> Restarting Server in " + seconds + "s...");

        try {
            String currentDir = System.getProperty("user.dir");
            String osName = System.getProperty("os.name").toLowerCase();

            ProcessBuilder pb;
            if (osName.contains("win")) {
                pb = new ProcessBuilder("cmd", "/c", "start", "cmd", "/c", "timeout /t " + seconds + " /nobreak && run.bat");
            } else {
                pb = new ProcessBuilder("bash", "-c", "sleep " + seconds + "; ./run.sh &");
            }

            pb.directory(new File(currentDir));
            pb.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- Khởi tạo Giao diện ---
    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245)); // Màu nền tổng thể sáng nhẹ

        // Danh sách Menu
        NavItem[] menuItems = {
            new NavItem("Dashboard", "/icon/dashboard.png", "Dashboard"),
            new NavItem("Account", "/icon/Account.png", "Account"),
            new NavItem("Players", "/icon/user2.png", "Players"),
            new NavItem("Shop Items", "/icon/shop.png", "ShopEditor"),
            new NavItem("Giftcode", "/icon/gift.png", "Giftcode"),
            new NavItem("Topup Reward", "/icon/topup.png", "TopupReward"),
            new NavItem("Events", "/icon/calendar.png", "Events"),
            new NavItem("Boss Config", "/icon/monster.png", "Boss Config"),
            new NavItem("Security", "/icon/shield.png", "Security")
        };

        // Cấu hình Sidebar (JList)
        sidebar = new JList<>(menuItems);
        sidebar.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sidebar.setSelectedIndex(0);
        sidebar.setFixedCellHeight(55); // Tăng chiều cao mỗi dòng
        sidebar.setBackground(new Color(255, 255, 255));
        sidebar.setBorder(new EmptyBorder(10, 0, 10, 0));

        // Custom Renderer cho Sidebar đẹp hơn
        sidebar.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (value instanceof NavItem) {
                    NavItem item = (NavItem) value;
                    lbl.setText(item.name);
                    if (item.icon != null) {
                        lbl.setIcon(item.icon);
                    }
                }

                lbl.setBorder(new EmptyBorder(0, 20, 0, 0)); // Padding trái
                lbl.setIconTextGap(15);
                lbl.setFont(new Font("Segoe UI", isSelected ? Font.BOLD : Font.PLAIN, 14));

                if (isSelected) {
                    lbl.setBackground(new Color(230, 242, 255)); // Màu nền khi chọn (Xanh nhạt)
                    lbl.setForeground(new Color(0, 102, 204));   // Màu chữ khi chọn (Xanh đậm)
                    // Thêm vạch màu bên trái để đánh dấu
                    lbl.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 4, 0, 0, new Color(0, 120, 215)),
                            new EmptyBorder(0, 16, 0, 0)
                    ));
                } else {
                    lbl.setBackground(Color.WHITE);
                    lbl.setForeground(new Color(60, 60, 60));
                }
                return lbl;
            }
        });

        // Sidebar Container
        JScrollPane scrollSidebar = new JScrollPane(sidebar);
        scrollSidebar.setPreferredSize(new Dimension(260, getHeight())); // Rộng hơn chút
        scrollSidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(220, 220, 220))); // Viền phải nhẹ
        add(scrollSidebar, BorderLayout.WEST);

        // Content Panel (Chứa các màn hình chức năng)
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(new EmptyBorder(0, 0, 0, 0)); // Không viền thừa

        // Đăng ký các Panel con
        contentPanel.add(new DashboardPanel(), "Dashboard");
        contentPanel.add(new AccountPanel(), "Account");
        contentPanel.add(new PlayersPanel(), "Players");
        contentPanel.add(new ShopEditorPanel(), "ShopEditor");
        contentPanel.add(new GiftcodePanel(), "Giftcode");
        contentPanel.add(new TopupRewardPanel(), "TopupReward");
        contentPanel.add(new EventPanel(), "Events");
        contentPanel.add(new BossEditorPanel(), "Boss Config");
        contentPanel.add(new SecurityPanel(), "Security");

        add(contentPanel, BorderLayout.CENTER);

        // Xử lý chuyển tab khi click sidebar
        sidebar.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                NavItem selected = sidebar.getSelectedValue();
                if (selected != null) {
                    cardLayout.show(contentPanel, selected.key);
                }
            }
        });

        // Cấu hình cửa sổ chính
        setSize(1300, 850);
        setMinimumSize(new Dimension(1150, 750));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        // Sự kiện đóng cửa sổ an toàn
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int confirm = JOptionPane.showConfirmDialog(
                        ServerManagerUI.this,
                        "Bạn có chắc muốn dừng Server và thoát chương trình?",
                        "Xác nhận tắt Server",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                if (confirm == JOptionPane.YES_OPTION) {
                    shutdownServer();
                }
            }
        });
    }

    private void startServerProcesses() {
        System.out.println(">> [ServerManagerUI] Starting Server Engine...");
        new Thread(() -> {
            ServerManager.gI().run();
            // Hiển thị UI sau khi server khởi động (hoặc ngay lập tức tùy logic)
            EventQueue.invokeLater(() -> setVisible(true));
        }).start();
    }

    private void shutdownServer() {
        try {
            System.out.println(">> Đang lưu dữ liệu và đóng kết nối...");
            if (ProxyManager.getInstance() != null) {
                ProxyManager.getInstance().stopAll();
            }
            if (AutoSaveManager.getInstance() != null) {
                AutoSaveManager.getInstance().stopAutoSave();
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi đóng tài nguyên: " + e.getMessage());
        }

        System.out.println(">> Server shutting down... Bye!");
        System.exit(0);
    }

    public static void main(String[] args) {
        // Chạy trên luồng giao diện chuẩn Swing
        EventQueue.invokeLater(ServerManagerUI::new);
    }
}