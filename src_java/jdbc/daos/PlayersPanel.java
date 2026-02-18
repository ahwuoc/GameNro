package jdbc.daos;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;

public class PlayersPanel extends JPanel {

    // --- Cấu hình Database ---
    private static final String DB_URL = "jdbc:mysql://localhost:3306/backup?useUnicode=true&characterEncoding=utf-8";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";
    private static final String ICON_FOLDER = "data/icon/"; 

    private JTable table;
    private DefaultTableModel model;
    private JTextField txtSearch;
    private TableRowSorter<DefaultTableModel> sorter;
    
    // Cache dữ liệu
    private final Map<Integer, String> itemTemplateMap = new HashMap<>();
    private final Map<Integer, Integer> itemIconMap = new HashMap<>();
    private final Map<Integer, String> clanNameMap = new HashMap<>();
    private final Map<Integer, String> optionTemplateMap = new HashMap<>();
    
    // [NEW] List chứa data đầy đủ để lọc item
    private final List<ItemData> listAllItems = new ArrayList<>();

    // [NEW] Class lưu trữ thông tin item
    private static class ItemData {
        int id;
        String name;
        int type;
        int gender;
        public ItemData(int id, String name, int type, int gender) {
            this.id = id; this.name = name; this.type = type; this.gender = gender;
        }
    }
    
    // Cache Part Head
    private final Map<Integer, Integer> partHeadIconMap = new HashMap<>(); // ID Part -> Icon ID (Type 0)
    
    // Cache Icon ảnh (RAM)
    private final Map<Integer, ImageIcon> iconCache = new HashMap<>();
    private final Map<Integer, Boolean> noIconCache = new HashMap<>();
    private final Map<Integer, ImageIcon> headCache = new HashMap<>(); // Cache ảnh Head đã resize

    // --- MÀU SẮC GIAO DIỆN ---
    private final Color COLOR_PRIMARY = new Color(0, 120, 215);
    private final Color COLOR_SUCCESS = new Color(40, 167, 69);
    private final Color COLOR_BG_HEADER = new Color(230, 240, 255);
    private final Color COLOR_ALT_ROW = new Color(245, 245, 245);

    public PlayersPanel() {
        setLayout(new BorderLayout(15, 15));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        initStaticData();
        loadCacheData(); // Load Item Template & Clan
        loadPartsHead(); // Load Head Data -> Sau đó tự động gọi loadPlayersFromDB

        initTopControls();
        initTable();
    }

    // --- DB CONNECT ---
    private Connection getConnection() throws SQLException {
        try { Class.forName("com.mysql.cj.jdbc.Driver"); } 
        catch (ClassNotFoundException e) { try { Class.forName("com.mysql.jdbc.Driver"); } catch(Exception ex){} }
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    private void loadCacheData() {
        new Thread(() -> {
            listAllItems.clear();
            try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                // [MOD] Lấy thêm type, gender
                try (ResultSet rs = stmt.executeQuery("SELECT id, name, icon_id, type, gender FROM item_template")) {
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        String name = rs.getString("name");
                        int iconId = rs.getInt("icon_id");
                        int type = rs.getInt("type");
                        int gender = rs.getInt("gender");

                        itemTemplateMap.put(id, name);
                        itemIconMap.put(id, iconId);
                        
                        // Thêm vào list để lọc
                        listAllItems.add(new ItemData(id, name, type, gender));
                    }
                }
                try (ResultSet rs = stmt.executeQuery("SELECT id, name FROM clan")) {
                    while (rs.next()) clanNameMap.put(rs.getInt("id"), rs.getString("name"));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }
    
    // [NEW] Load Part Head (Type 0) để lấy Icon ID
    private void loadPartsHead() {
        new Thread(() -> {
            try (Connection conn = getConnection(); Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, data FROM part WHERE type = 0")) {
                while (rs.next()) {
                    try {
                        String dataStr = rs.getString("data");
                        JsonArray arr = new JsonParser().parse(dataStr).getAsJsonArray();
                        if (arr.size() > 0) {
                            // Lấy phần tử đầu tiên [[17,0,0],...] -> lấy số 17 làm icon_id
                            JsonArray firstElement = arr.get(0).getAsJsonArray();
                            int iconId = firstElement.get(0).getAsInt();
                            partHeadIconMap.put(rs.getInt("id"), iconId);
                        }
                    } catch (Exception ignored) {}
                }
            } catch (SQLException ignored) {}
            
            // [FIX] Sau khi load xong dữ liệu Head thì mới load danh sách Player
            // Điều này đảm bảo khi vẽ bảng, map icon đã có dữ liệu -> Hiện ảnh ngay lập tức
            SwingUtilities.invokeLater(() -> loadPlayersFromDB(""));
            
        }).start();
    }
    
    // [NEW] Vẽ Head từ Part ID
    private ImageIcon drawHeadIcon(int headPartId) {
        if (headPartId <= 0) return null;
        if (headCache.containsKey(headPartId)) return headCache.get(headPartId);

        Integer iconId = partHeadIconMap.get(headPartId);
        if (iconId != null) {
            // Tìm và load ảnh
            try {
                String[] zoomLevels = {"x4", "x3", "x2", "x1"};
                for (String zoom : zoomLevels) {
                    File f = new File(ICON_FOLDER + zoom + "/" + iconId + ".png");
                    if (f.exists()) {
                        BufferedImage img = ImageIO.read(f);
                        // Resize về 28x28 cho vừa ô bảng
                        Image dimg = img.getScaledInstance(28, 28, Image.SCALE_SMOOTH);
                        ImageIcon icon = new ImageIcon(dimg);
                        headCache.put(headPartId, icon);
                        return icon;
                    }
                }
            } catch (Exception e) {}
        }
        return null;
    }

    private ImageIcon getItemIcon(int itemId) {
        if (iconCache.containsKey(itemId)) return iconCache.get(itemId);
        if (noIconCache.containsKey(itemId)) return null;

        try {
            int iconId = itemIconMap.getOrDefault(itemId, -1);
            if (iconId == -1) {
                noIconCache.put(itemId, true);
                return null;
            }

            String[] zoomLevels = {"x4", "x3", "x2", "x1"};
            File f = null;
            for (String zoom : zoomLevels) {
                f = new File(ICON_FOLDER + zoom + "/" + iconId + ".png");
                if (f.exists()) break;
            }

            if (f != null && f.exists()) {
                BufferedImage img = ImageIO.read(f);
                Image dimg = img.getScaledInstance(20, 20, Image.SCALE_SMOOTH);
                ImageIcon icon = new ImageIcon(dimg);
                iconCache.put(itemId, icon);
                return icon;
            }
        } catch (Exception e) { }

        noIconCache.put(itemId, true);
        return null;
    }

    private void initStaticData() {
        String raw = "0,Tấn công +#;50,Sức đánh +#%;77,HP +#%;103,KI +#%;14,Chí mạng +#%;30,Khóa giao dịch;93,Hạn sử dụng # ngày;73,Không thể bán;9,Hiệu lực # phút";
        for (String s : raw.split(";")) {
            String[] p = s.split(",");
            if(p.length==2) optionTemplateMap.put(Integer.parseInt(p[0]), p[1]);
        }
    }

    private String getItemName(int id) { return itemTemplateMap.getOrDefault(id, "Unknown [" + id + "]"); }
    private String getClanName(int id) { return id == -1 ? "Không có" : clanNameMap.getOrDefault(id, "Clan [" + id + "]"); }
    private String getOptionName(int id) { return optionTemplateMap.getOrDefault(id, "Option " + id); }
    
    private String formatOption(int id, int param) {
        String tpl = getOptionName(id);
        return tpl.replace("#", String.valueOf(param));
    }

    // --- UI INIT ---
    private void initTopControls() {
        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(0, 0, 10, 0));

        JPanel searchP = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchP.setOpaque(false);
        
        txtSearch = new JTextField(25);
        txtSearch.putClientProperty("JTextField.placeholderText", "Nhập tên nhân vật để tìm...");
        txtSearch.setPreferredSize(new Dimension(250, 35));
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) loadPlayersFromDB(txtSearch.getText().trim());
            }
        });
        
        JButton btnSearch = createStyledButton("Tìm kiếm", COLOR_PRIMARY, Color.WHITE);
        btnSearch.addActionListener(e -> loadPlayersFromDB(txtSearch.getText().trim()));

        JButton btnReload = createStyledButton("Tải lại DB", new Color(100, 100, 100), Color.WHITE);
        btnReload.addActionListener(e -> {
            loadPartsHead(); // Tải lại cache -> tự động tải lại player
        });

        searchP.add(txtSearch);
        searchP.add(btnSearch);
        searchP.add(btnReload);
        
        JLabel lblHint = new JLabel("<html><b style='color:#0078D7'>Hướng dẫn:</b> Chuột phải để Buff Item | Double Click để sửa chi tiết</html>");
        lblHint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        top.add(searchP, BorderLayout.WEST);
        top.add(lblHint, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);
    }

    private void initTable() {
        // [MOD] Thêm cột Head
        String[] cols = {
            "ID", "Head", "Tên nhân vật", "Sức Mạnh", "Clan", 
            "Vàng", "Ngọc", "Thỏi Vàng", 
            "VNĐ", "Tổng Nạp", "Trạng thái"
        };
        
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1) return ImageIcon.class; // Cột Head là ảnh
                if (columnIndex == 0 || columnIndex == 7 || columnIndex == 8 || columnIndex == 9) return Long.class;
                return super.getColumnClass(columnIndex);
            }
        };
        
        table = new JTable(model);
        table.setRowHeight(40); // Tăng chiều cao dòng để hiện Head
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(232, 242, 252));
        table.setSelectionForeground(Color.BLACK);
        
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(COLOR_BG_HEADER);
        header.setForeground(Color.DARK_GRAY);
        header.setPreferredSize(new Dimension(0, 40));
        
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(50); // Head
        table.getColumnModel().getColumn(2).setPreferredWidth(150);
        
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                
                if (!isSelected) {
                    if (column == 8 || column == 9) setBackground(new Color(225, 255, 225));
                    else setBackground(row % 2 == 0 ? Color.WHITE : COLOR_ALT_ROW);
                }
                
                if (column == 10) {
                    setFont(new Font("Segoe UI", Font.BOLD, 12));
                    if ("Đã kích hoạt".equals(value)) setForeground(new Color(0, 128, 0));
                    else setForeground(Color.RED);
                } else {
                    setForeground(Color.BLACK);
                }
                return this;
            }
        });

        // [QUAN TRỌNG] Thêm Menu Chuột Phải
        createContextMenu();

        // Double Click Editor
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int r = table.getSelectedRow();
                    if (r != -1) {
                        int modelRow = table.convertRowIndexToModel(r);
                        int playerId = Integer.parseInt(model.getValueAt(modelRow, 0).toString());
                        openPlayerEditorDB(playerId);
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(new Color(220, 220, 220)));
        scroll.getViewport().setBackground(Color.WHITE);
        add(scroll, BorderLayout.CENTER);
    }

    // --- CONTEXT MENU (CHUỘT PHẢI) ---
    private void createContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        
        JMenuItem mBuffItem = new JMenuItem("Buff Item (Thêm vào hành trang)");
        mBuffItem.setFont(new Font("Segoe UI", Font.BOLD, 12));
        mBuffItem.setIcon(UIManager.getIcon("FileView.floppyDriveIcon")); // Icon mặc định swing
        mBuffItem.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r != -1) {
                int modelRow = table.convertRowIndexToModel(r);
                int pid = Integer.parseInt(model.getValueAt(modelRow, 0).toString());
                String name = model.getValueAt(modelRow, 2).toString(); // Cột tên dời sang index 2
                openBuffItemDialog(pid, name);
            }
        });

        menu.add(mBuffItem);

        table.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showMenu(e);
            }
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showMenu(e);
            }
            
            private void showMenu(MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint());
                if (r >= 0 && r < table.getRowCount()) {
                    table.setRowSelectionInterval(r, r);
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    // --- BUFF ITEM DIALOG (MULTI OPTION) ---
    private void openBuffItemDialog(int playerId, String playerName) {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Buff Item cho: " + playerName, true);
        d.setSize(450, 350);
        d.setLocationRelativeTo(null);
        d.setLayout(new GridBagLayout());
        
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 10, 10, 10);
        g.fill = GridBagConstraints.HORIZONTAL;

        // ID Item
        g.gridx=0; g.gridy=0; d.add(new JLabel("ID Item Template:"), g);
        JTextField txtId = new JTextField();
        g.gridx=1; d.add(txtId, g);
        
        // Tên Item & Icon
        JLabel lblItemName = new JLabel("---");
        lblItemName.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblItemName.setForeground(Color.BLUE);
        // Label chứa icon
        JLabel lblIcon = new JLabel();
        lblIcon.setPreferredSize(new Dimension(24, 24));
        
        JPanel pName = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pName.add(lblIcon);
        pName.add(lblItemName);
        
        g.gridx=1; g.gridy=1; d.add(pName, g);
        
        txtId.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }
            void update() {
                try {
                    int id = Integer.parseInt(txtId.getText().trim());
                    lblItemName.setText(getItemName(id));
                    lblIcon.setIcon(getItemIcon(id)); // Hiển thị icon
                } catch(Exception ex) { 
                    lblItemName.setText("---"); 
                    lblIcon.setIcon(null);
                }
            }
        });

        // Số lượng
        g.gridx=0; g.gridy=2; d.add(new JLabel("Số lượng:"), g);
        JTextField txtQty = new JTextField("1");
        g.gridx=1; d.add(txtQty, g);
        
        // Options
        g.gridx=0; g.gridy=3; d.add(new JLabel("Options (JSON):"), g);
        JTextField txtOpt = new JTextField("[]"); // Default empty array or specific options
        txtOpt.setToolTipText("Ví dụ: [[50,10],[77,10]] (SĐ +10%, HP +10%)");
        g.gridx=1; d.add(txtOpt, g);
        
        // Button
        JButton btnOk = createStyledButton("Thực hiện", COLOR_SUCCESS, Color.WHITE);
        g.gridx=0; g.gridy=4; g.gridwidth=2; 
        d.add(btnOk, g);

        btnOk.addActionListener(e -> {
            new Thread(() -> {
                try {
                    int idTemplate = Integer.parseInt(txtId.getText().trim());
                    int quantity = Integer.parseInt(txtQty.getText().trim());
                    String optJson = txtOpt.getText().trim();
                    
                    if(!optJson.startsWith("[") || !optJson.endsWith("]")) optJson = "[]";

                    // 1. Get current items_bag
                    String currentBag = "[]";
                    try (Connection conn = getConnection(); 
                         Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery("SELECT items_bag FROM player WHERE id=" + playerId)) {
                        if(rs.next()) currentBag = rs.getString("items_bag");
                    }

                    // 2. Parse and Append
                    JsonArray bagArr;
                    try {
                        bagArr = new JsonParser().parse(currentBag).getAsJsonArray();
                    } catch(Exception ex) { bagArr = new JsonArray(); }
                    
                    JsonArray newItem = new JsonArray();
                    newItem.add(idTemplate);
                    newItem.add(quantity);
                    newItem.add(optJson);
                    newItem.add(System.currentTimeMillis());
                    
                    bagArr.add(new JsonPrimitive(newItem.toString()));

                    // 3. Update DB
                    String updateSql = "UPDATE player SET items_bag = ? WHERE id = ?";
                    try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(updateSql)) {
                        ps.setString(1, bagArr.toString());
                        ps.setInt(2, playerId);
                        ps.executeUpdate();
                    }
                    
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(d, "Đã thêm vật phẩm thành công!");
                        d.dispose();
                        loadPlayersFromDB(txtSearch.getText().trim());
                    });
                    
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(d, "Lỗi: " + ex.getMessage()));
                }
            }).start();
        });
        
        d.setVisible(true);
    }

    private long countItemTotal(String... jsonLists) {
        long total = 0;
        for (String json : jsonLists) {
            try {
                if (json == null || json.isEmpty()) continue;
                JsonElement parsed = new JsonParser().parse(json);
                if (!parsed.isJsonArray()) continue;
                JsonArray arr = parsed.getAsJsonArray();
                for (JsonElement e : arr) {
                    JsonArray item;
                    if (e.isJsonPrimitive()) item = new JsonParser().parse(e.getAsString()).getAsJsonArray();
                    else item = e.getAsJsonArray();

                    if (item.size() >= 2) {
                        int id = item.get(0).getAsInt();
                        if (id == 457) total += item.get(1).getAsLong(); 
                    }
                }
            } catch (Exception e) {}
        }
        return total;
    }

    private void loadPlayersFromDB(String keyword) {
        new Thread(() -> {
            SwingUtilities.invokeLater(() -> model.setRowCount(0));
            String sql = "SELECT p.id, p.head, p.name, p.power, p.clan_id, p.data_inventory, p.items_bag, p.items_box, a.cash, a.danap, a.active " +
                         "FROM player p " +
                         "LEFT JOIN account a ON p.account_id = a.id ";
            
            if (!keyword.isEmpty()) sql += "WHERE p.name LIKE '%" + keyword + "%' ";
            
            // [SẮP XẾP ID TĂNG DẦN]
            sql += "ORDER BY p.id ASC LIMIT 50";

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    int headPart = rs.getInt("head");

                    row.add((long)rs.getInt("id"));
                    row.add(drawHeadIcon(headPart)); // [NEW] Hiển thị Head Icon
                    row.add(rs.getString("name"));
                    row.add(String.format("%,d", rs.getLong("power")));
                    row.add(getClanName(rs.getInt("clan_id")));

                    try {
                        JsonArray inv = new JsonParser().parse(rs.getString("data_inventory")).getAsJsonArray();
                        row.add(String.format("%,d", inv.get(0).getAsLong())); 
                        row.add(String.format("%,d", inv.get(1).getAsLong())); 
                    } catch (Exception e) {
                        row.add("0"); row.add("0");
                    }
                    
                    long goldBar = countItemTotal(rs.getString("items_bag"), rs.getString("items_box"));
                    row.add(goldBar); 

                    row.add(rs.getLong("cash")); 
                    row.add(rs.getLong("danap")); 
                    
                    int active = rs.getInt("active");
                    row.add(active == 1 ? "Đã kích hoạt" : "Chưa kích hoạt");

                    SwingUtilities.invokeLater(() -> model.addRow(row));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    // ========================================================================
    // [PLAYER EDITOR]
    // ========================================================================
    private void openPlayerEditorDB(int playerId) {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Chỉnh sửa Chi Tiết - ID: " + playerId, true);
        d.setSize(950, 750);
        d.setLocationRelativeTo(null);
        d.setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        
        Map<String, Component> inputs = new HashMap<>();
        Map<String, String> originalData = new HashMap<>();
        
        new Thread(() -> {
            String query = "SELECT p.*, a.cash, a.danap, a.active FROM player p " +
                           "LEFT JOIN account a ON p.account_id = a.id " +
                           "WHERE p.id = " + playerId;

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                
                if (rs.next()) {
                    int accountId = rs.getInt("account_id");
                    originalData.put("data_inventory", rs.getString("data_inventory"));
                    originalData.put("data_point", rs.getString("data_point"));
                    originalData.put("items_body", rs.getString("items_body"));
                    originalData.put("items_bag", rs.getString("items_bag"));
                    originalData.put("items_box", rs.getString("items_box"));
                    originalData.put("pet", rs.getString("pet"));

                    // 1. INFO TAB
                    JPanel pMainInfo = new JPanel(new GridBagLayout());
                    pMainInfo.setBorder(new EmptyBorder(15, 15, 15, 15));
                    GridBagConstraints g = new GridBagConstraints();
                    g.fill = GridBagConstraints.HORIZONTAL;
                    g.insets = new Insets(5, 5, 5, 5);
                    
                    JPanel pAcc = createSectionPanel("Thông tin Tài khoản");
                    addLabelInput(pAcc, "VND:", rs.getString("cash"), "cash", inputs);
                    addLabelInput(pAcc, "Tổng Nạp:", rs.getString("danap"), "danap", inputs);
                    JComboBox<String> cbActive = new JComboBox<>(new String[]{"0 - Chưa kích hoạt", "1 - Đã kích hoạt"});
                    cbActive.setSelectedIndex(rs.getInt("active") == 1 ? 1 : 0);
                    inputs.put("active_box", cbActive);
                    pAcc.add(cbActive);

                    JPanel pChar = createSectionPanel("Thông tin Nhân vật");
                    addLabelInput(pChar, "Tên:", rs.getString("name"), "name", inputs);
                    addLabelInput(pChar, "Sức mạnh:", rs.getString("power"), "power", inputs);
                    addLabelInput(pChar, "Head Part ID:", String.valueOf(rs.getInt("head")), "head", inputs); // Cho phép sửa đầu

                    JPanel pAsset = createSectionPanel("Tài sản");
                    JsonArray inv = new JsonParser().parse(originalData.get("data_inventory")).getAsJsonArray();
                    addLabelInput(pAsset, "Vàng:", inv.get(0).getAsString(), "gold", inputs);
                    addLabelInput(pAsset, "Ngọc:", inv.get(1).getAsString(), "gem", inputs);
                    addLabelInput(pAsset, "Hồng ngọc:", inv.size()>2?inv.get(2).getAsString():"0", "ruby", inputs);
                    addLabelInput(pAsset, "Coupon:", inv.size()>3?inv.get(3).getAsString():"0", "coupon", inputs);
                    addLabelInput(pAsset, "Điểm sự kiện:", inv.size()>4?inv.get(4).getAsString():"0", "event_point", inputs);

                    g.gridx=0; g.gridy=0; g.weightx=1.0; pMainInfo.add(pAcc, g);
                    g.gridy=1; pMainInfo.add(pChar, g);
                    g.gridy=2; g.weighty=1.0; g.anchor=GridBagConstraints.NORTH; pMainInfo.add(pAsset, g);

                    // 2. POINT TAB
                    JPanel pPoint = new JPanel(new GridLayout(0, 2, 10, 10));
                    pPoint.setBorder(new EmptyBorder(20, 40, 20, 40));
                    JsonArray point = new JsonParser().parse(originalData.get("data_point")).getAsJsonArray();
                    addLabelInputGrid(pPoint, "Tiềm năng:", getJsonVal(point, 2), "tiemnang", inputs);
                    addLabelInputGrid(pPoint, "HP Gốc:", getJsonVal(point, 5), "hpg", inputs);
                    addLabelInputGrid(pPoint, "KI Gốc:", getJsonVal(point, 6), "mpg", inputs);
                    addLabelInputGrid(pPoint, "Sức đánh:", getJsonVal(point, 7), "dameg", inputs);
                    addLabelInputGrid(pPoint, "Giáp:", getJsonVal(point, 8), "defg", inputs);
                    addLabelInputGrid(pPoint, "Chí mạng:", getJsonVal(point, 9), "critg", inputs);

                    // 3. ITEMS TAB (CÓ ICON)
                    JTabbedPane tabItems = new JTabbedPane();
                    DefaultTableModel mBody = createItemModel();
                    DefaultTableModel mBag = createItemModel();
                    DefaultTableModel mBox = createItemModel();
                    
                    loadItemsToModel(originalData.get("items_body"), mBody);
                    loadItemsToModel(originalData.get("items_bag"), mBag);
                    loadItemsToModel(originalData.get("items_box"), mBox);
                    
                    tabItems.addTab("Đồ đang mặc", createItemPanel(mBody, d));
                    tabItems.addTab("Hành trang", createItemPanel(mBag, d));
                    tabItems.addTab("Rương đồ", createItemPanel(mBox, d));

                    // 4. PET TAB [MOD]
                    JPanel pPet = new JPanel(new GridBagLayout());
                    pPet.setBorder(new EmptyBorder(20, 20, 20, 20));
                    
                    // Parse Pet Data
                    // Sample: ["[-1,0,\"$Đệ tử\",0,0,0]", "[...]"]
                    String petStr = rs.getString("pet");
                    if(petStr != null && !petStr.equals("[]") && !petStr.isEmpty()) {
                        try {
                            JsonArray petArr = new JsonParser().parse(petStr).getAsJsonArray();
                            if(petArr.size() > 0) {
                                String infoStr = petArr.get(0).getAsString(); // Lấy chuỗi thông tin cơ bản ở index 0
                                JsonArray infoArr = new JsonParser().parse(infoStr).getAsJsonArray();
                                
                                // Index: 0=Type, 1=Gender, 2=Name, 5=Status
                                JPanel pPetInfo = createSectionPanel("Thông tin Đệ Tử");
                                
                                // Loại đệ
                                JComboBox<String> cbPetType = new JComboBox<>(new String[]{"0 - Mabu", "1 - Fide", "2 - Cadic", "3 - Pic", "4 - Quy lão"});
                                cbPetType.setEditable(true);
                                cbPetType.setSelectedItem(infoArr.get(0).getAsString());
                                JPanel pType = new JPanel(new BorderLayout()); pType.add(new JLabel("Loại Đệ: "), BorderLayout.WEST); pType.add(cbPetType, BorderLayout.CENTER);
                                pPetInfo.add(pType);
                                inputs.put("pet_type", cbPetType);

                                // Giới tính
                                JComboBox<String> cbPetGender = new JComboBox<>(new String[]{"0 - Trái đất", "1 - Namếc", "2 - Xayda"});
                                cbPetGender.setSelectedIndex(infoArr.get(1).getAsInt());
                                JPanel pGen = new JPanel(new BorderLayout()); pGen.add(new JLabel("Giới tính: "), BorderLayout.WEST); pGen.add(cbPetGender, BorderLayout.CENTER);
                                pPetInfo.add(pGen);
                                inputs.put("pet_gender", cbPetGender);

                                addLabelInput(pPetInfo, "Tên Đệ tử:", infoArr.get(2).getAsString(), "pet_name", inputs);
                                
                                // Trạng thái
                                JComboBox<String> cbPetStatus = new JComboBox<>(new String[]{"0 - Đi theo", "1 - Bảo vệ", "2 - Tấn công", "3 - Về nhà", "4 - Hợp thể"});
                                try { cbPetStatus.setSelectedIndex(infoArr.get(5).getAsInt()); } catch(Exception ex) {}
                                JPanel pSta = new JPanel(new BorderLayout()); pSta.add(new JLabel("Trạng thái: "), BorderLayout.WEST); pSta.add(cbPetStatus, BorderLayout.CENTER);
                                pPetInfo.add(pSta);
                                inputs.put("pet_status", cbPetStatus);

                                GridBagConstraints gp = new GridBagConstraints();
                                gp.fill = GridBagConstraints.HORIZONTAL; gp.weightx=1.0; gp.anchor = GridBagConstraints.NORTH;
                                pPet.add(pPetInfo, gp);
                            }
                        } catch(Exception ex) { pPet.add(new JLabel("Lỗi đọc dữ liệu đệ tử: " + ex.getMessage())); }
                    } else {
                        pPet.add(new JLabel("Nhân vật này không có đệ tử."));
                    }

                    tabs.addTab("Thông tin chung", pMainInfo);
                    tabs.addTab("Chỉ số", pPoint);
                    tabs.addTab("Vật phẩm", tabItems);
                    tabs.addTab("Đệ tử", pPet);

                    d.add(tabs, BorderLayout.CENTER);

                    JPanel pBtn = new JPanel();
                    pBtn.setBorder(new EmptyBorder(10, 0, 10, 0));
                    JButton btnSave = createStyledButton("LƯU DỮ LIỆU", COLOR_SUCCESS, Color.WHITE);
                    btnSave.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    btnSave.setPreferredSize(new Dimension(200, 45));
                    
                    btnSave.addActionListener(ev -> savePlayerDB(playerId, accountId, inputs, mBody, mBag, mBox, originalData, d));
                    pBtn.add(btnSave);
                    d.add(pBtn, BorderLayout.SOUTH);
                    
                    SwingUtilities.invokeLater(() -> d.setVisible(true));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    // --- Helper UI Methods ---
    private String getJsonVal(JsonArray arr, int index) {
        if (index < arr.size()) return arr.get(index).getAsString();
        return "0";
    }

    private JPanel createSectionPanel(String title) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createTitledBorder(
            new LineBorder(new Color(200, 200, 200)), title,
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 12), Color.DARK_GRAY
        ));
        return p;
    }

    private void addLabelInput(JPanel p, String label, String value, String key, Map<String, Component> map) {
        JPanel row = new JPanel(new BorderLayout(10, 5));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(5, 5, 5, 5));
        JLabel lbl = new JLabel(label);
        lbl.setPreferredSize(new Dimension(100, 25));
        JTextField txt = new JTextField(value);
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        row.add(lbl, BorderLayout.WEST);
        row.add(txt, BorderLayout.CENTER);
        p.add(row);
        map.put(key, txt);
    }
    
    private void addLabelInputGrid(JPanel p, String label, String value, String key, Map<String, Component> map) {
        JPanel row = new JPanel(new BorderLayout(5, 0));
        JLabel lbl = new JLabel(label);
        JTextField txt = new JTextField(value);
        row.add(lbl, BorderLayout.NORTH);
        row.add(txt, BorderLayout.CENTER);
        p.add(row);
        map.put(key, txt);
    }

    private JPanel createItemPanel(DefaultTableModel model, JDialog parent) {
        JPanel p = new JPanel(new BorderLayout());
        JTable t = new JTable(model);
        t.setRowHeight(30); // Tăng chiều cao để hiện icon
        
        // Cột 0: ID, Cột 1: Icon (nhỏ), Cột 2: Tên, ...
        t.getColumnModel().getColumn(0).setPreferredWidth(50);
        t.getColumnModel().getColumn(1).setPreferredWidth(40);
        t.getColumnModel().getColumn(2).setPreferredWidth(150);
        t.getColumnModel().getColumn(4).setPreferredWidth(300);
        
        JPanel tool = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tool.setOpaque(false);
        JButton btnAdd = createStyledButton("Thêm Item", COLOR_PRIMARY, Color.WHITE);
        JButton btnDel = createStyledButton("Xóa Item", Color.RED, Color.WHITE);
        btnAdd.addActionListener(e -> openItemAddDialog(model, parent));
        btnDel.addActionListener(e -> {
            if(t.getSelectedRow()!=-1) model.removeRow(t.getSelectedRow());
        });
        tool.add(btnAdd); tool.add(btnDel);
        p.add(tool, BorderLayout.NORTH);
        p.add(new JScrollPane(t), BorderLayout.CENTER);
        return p;
    }

    private DefaultTableModel createItemModel() {
        return new DefaultTableModel(new String[]{"ID", "Icon", "Tên Item", "SL", "Options (Readable)", "Raw Options"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 2 || c == 3; }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1) return ImageIcon.class; // Render ảnh
                return Object.class;
            }
        };
    }

    private void loadItemsToModel(String jsonArrayStr, DefaultTableModel model) {
        try {
            JsonArray arr = new JsonParser().parse(jsonArrayStr).getAsJsonArray();
            for (JsonElement e : arr) {
                String innerStr = e.getAsString();
                JsonArray itemData = new JsonParser().parse(innerStr).getAsJsonArray();
                int id = itemData.get(0).getAsInt();
                if (id == -1) continue;
                int qty = itemData.get(1).getAsInt();
                String rawOpt = (itemData.size() > 2) ? itemData.get(2).getAsString() : "[]";
                String readableOpt = parseOptionReadable(rawOpt);
                
                // Load Icon
                ImageIcon icon = getItemIcon(id);
                
                model.addRow(new Object[]{id, icon, getItemName(id), qty, readableOpt, rawOpt});
            }
        } catch (Exception e) {}
    }
    
    private String parseOptionReadable(String jsonOpt) {
        try {
            StringBuilder sb = new StringBuilder();
            JsonArray arr = new JsonParser().parse(jsonOpt).getAsJsonArray();
            for (JsonElement e : arr) {
                JsonArray opt = e.getAsJsonArray();
                int id = opt.get(0).getAsInt();
                int param = opt.get(1).getAsInt();
                sb.append(formatOption(id, param)).append(", ");
            }
            if (sb.length() > 2) return sb.substring(0, sb.length() - 2);
        } catch (Exception e) { return jsonOpt; }
        return "";
    }

    // [MOD] Hàm thêm Item mới có bộ lọc Type và Gender
    private void openItemAddDialog(DefaultTableModel model, JDialog parent) {
        JDialog d = new JDialog(parent, "Thêm Vật Phẩm", true);
        d.setSize(900, 600);
        d.setLayout(new BorderLayout());
        d.setLocationRelativeTo(parent);

        // --- PANEL BỘ LỌC ---
        JPanel pFilter = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pFilter.setBorder(BorderFactory.createTitledBorder("Bộ Lọc"));

        JTextField txtSearch = new JTextField(15);
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm tên...");
        
        // ComboBox Type
        String[] types = {"- Tất cả Loại -", "0 - Áo", "1 - Quần", "2 - Găng", "3 - Giày", "4 - Rada", 
                          "5 - Cải trang/Tóc", "6 - Đậu thần", "12 - Ngọc rồng", "27 - Vật phẩm", "29 - Capsule/Bánh", "32 - Giáp tập"};
        JComboBox<String> cbType = new JComboBox<>(types);

        // ComboBox Gender
        String[] genders = {"- Tất cả Hệ -", "0 - Trái Đất", "1 - Namếc", "2 - Xayda", "3 - Chung/Tất cả"};
        JComboBox<String> cbGender = new JComboBox<>(genders);

        pFilter.add(new JLabel("Tên:")); pFilter.add(txtSearch);
        pFilter.add(new JLabel(" | Loại:")); pFilter.add(cbType);
        pFilter.add(new JLabel(" | Hệ:")); pFilter.add(cbGender);

        // --- BẢNG ITEM ---
        // Model search có icon, type, gender (để ẩn)
        DefaultTableModel searchModel = new DefaultTableModel(new String[]{"ID", "Icon", "Tên Item", "Type", "Gender"}, 0) {
             @Override public Class<?> getColumnClass(int c) { return c==1 ? ImageIcon.class : Object.class; }
             @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        
        // Load data từ List đã chuẩn bị sẵn
        for (ItemData item : listAllItems) {
            searchModel.addRow(new Object[]{item.id, getItemIcon(item.id), item.name, item.type, item.gender});
        }
        
        JTable t = new JTable(searchModel);
        t.setRowHeight(30);
        t.getColumnModel().getColumn(0).setPreferredWidth(50);
        t.getColumnModel().getColumn(1).setPreferredWidth(40);
        t.getColumnModel().getColumn(2).setPreferredWidth(350);
        
        // Ẩn cột Type và Gender đi (nhưng vẫn lọc được)
        t.getColumnModel().getColumn(3).setMinWidth(0);
        t.getColumnModel().getColumn(3).setMaxWidth(0);
        t.getColumnModel().getColumn(4).setMinWidth(0);
        t.getColumnModel().getColumn(4).setMaxWidth(0);
        
        TableRowSorter<DefaultTableModel> s = new TableRowSorter<>(searchModel);
        t.setRowSorter(s);

        // LOGIC LỌC
        Runnable doFilter = () -> {
            String text = txtSearch.getText().trim();
            int typeIdx = cbType.getSelectedIndex();
            int genderIdx = cbGender.getSelectedIndex();

            List<RowFilter<Object, Object>> filters = new ArrayList<>();

            if (!text.isEmpty()) filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), 2));
            
            if (typeIdx > 0) {
                try {
                    int val = Integer.parseInt(cbType.getSelectedItem().toString().split(" - ")[0]);
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, val, 3));
                } catch (Exception e) {}
            }
            
            if (genderIdx > 0) {
                try {
                    int val = Integer.parseInt(cbGender.getSelectedItem().toString().split(" - ")[0]);
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, val, 4));
                } catch (Exception e) {}
            }

            if (filters.isEmpty()) s.setRowFilter(null);
            else s.setRowFilter(RowFilter.andFilter(filters));
        };

        // Gắn sự kiện
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { doFilter.run(); }
            public void removeUpdate(DocumentEvent e) { doFilter.run(); }
            public void changedUpdate(DocumentEvent e) { doFilter.run(); }
        });
        cbType.addActionListener(e -> doFilter.run());
        cbGender.addActionListener(e -> doFilter.run());

        // Sự kiện chọn Item
        t.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int r = t.getSelectedRow();
                    if(r!=-1) {
                         int modelRow = t.convertRowIndexToModel(r);
                         int id = (int) searchModel.getValueAt(modelRow, 0);
                         ImageIcon icon = (ImageIcon) searchModel.getValueAt(modelRow, 1);
                         String name = (String) searchModel.getValueAt(modelRow, 2);
                         // Add vào bảng chính
                         model.addRow(new Object[]{id, icon, name, 1, "", "[]"});
                         d.dispose();
                    }
                }
            }
        });

        d.add(pFilter, BorderLayout.NORTH);
        d.add(new JScrollPane(t), BorderLayout.CENTER);
        d.setVisible(true);
    }

    private void savePlayerDB(int pid, int accountId, Map<String, Component> inputs, DefaultTableModel mBody, DefaultTableModel mBag, DefaultTableModel mBox, Map<String, String> originalData, JDialog d) {
        new Thread(() -> {
            try {
                // 1. UPDATE PLAYER
                JsonArray inv = new JsonArray();
                inv.add(getLongVal(inputs, "gold"));
                inv.add(getLongVal(inputs, "gem"));
                inv.add(getLongVal(inputs, "ruby"));
                inv.add(getLongVal(inputs, "coupon"));
                inv.add(getLongVal(inputs, "event_point"));

                JsonArray point = new JsonParser().parse(originalData.get("data_point")).getAsJsonArray();
                setVal(point, 1, getText(inputs, "power"));
                setVal(point, 2, getText(inputs, "tiemnang"));
                setVal(point, 5, getText(inputs, "hpg"));
                setVal(point, 6, getText(inputs, "mpg"));
                setVal(point, 7, getText(inputs, "dameg"));
                setVal(point, 8, getText(inputs, "defg"));
                setVal(point, 9, getText(inputs, "critg"));

                String jsonBag = buildItemJson(mBag, originalData.get("items_bag"));
                String jsonBox = buildItemJson(mBox, originalData.get("items_box"));
                String jsonBody = buildItemJson(mBody, originalData.get("items_body"));

                // [MOD] Save Pet
                String petJson = originalData.get("pet");
                if (petJson != null && !petJson.equals("[]") && inputs.containsKey("pet_name")) {
                    JsonArray petArr = new JsonParser().parse(petJson).getAsJsonArray();
                    if(petArr.size() > 0) {
                        // Lấy mảng info từ phần tử đầu tiên (String)
                        String infoStr = petArr.get(0).getAsString(); 
                        JsonArray infoArr = new JsonParser().parse(infoStr).getAsJsonArray();
                        
                        // Cập nhật giá trị vào mảng info
                        // Type
                        String typeStr = ((JComboBox)inputs.get("pet_type")).getSelectedItem().toString();
                        if(typeStr.contains(" - ")) typeStr = typeStr.split(" - ")[0];
                        setVal(infoArr, 0, typeStr);
                        
                        // Gender
                        setVal(infoArr, 1, String.valueOf(((JComboBox)inputs.get("pet_gender")).getSelectedIndex()));
                        
                        // Name
                        setVal(infoArr, 2, getText(inputs, "pet_name"));
                        
                        // Status
                        setVal(infoArr, 5, String.valueOf(((JComboBox)inputs.get("pet_status")).getSelectedIndex()));
                        
                        // Đóng gói mảng info thành String và gán lại vào mảng pet chính
                        petArr.set(0, new JsonPrimitive(infoArr.toString()));
                        petJson = petArr.toString();
                    }
                }

                String sqlPlayer = "UPDATE player SET name=?, power=?, head=?, data_inventory=?, data_point=?, items_body=?, items_bag=?, items_box=?, pet=? WHERE id=?";
                try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sqlPlayer)) {
                    ps.setString(1, getText(inputs, "name"));
                    ps.setLong(2, Long.parseLong(getText(inputs, "power")));
                    ps.setInt(3, Integer.parseInt(getText(inputs, "head"))); // Update Head
                    ps.setString(4, inv.toString());
                    ps.setString(5, point.toString());
                    ps.setString(6, jsonBody);
                    ps.setString(7, jsonBag);
                    ps.setString(8, jsonBox);
                    ps.setString(9, petJson);
                    ps.setInt(10, pid);
                    ps.executeUpdate();
                }

                // 2. UPDATE ACCOUNT
                String sqlAccount = "UPDATE account SET cash=?, danap=?, active=? WHERE id=?";
                try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sqlAccount)) {
                    long cash = getLongVal(inputs, "cash");
                    long danap = getLongVal(inputs, "danap");
                    JComboBox cbActive = (JComboBox) inputs.get("active_box");
                    int active = cbActive.getSelectedIndex(); 

                    ps.setLong(1, cash);
                    ps.setLong(2, danap);
                    ps.setInt(3, active);
                    ps.setInt(4, accountId);
                    ps.executeUpdate();
                }

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(d, "Lưu thành công!");
                    d.dispose();
                    loadPlayersFromDB("");
                });

            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(d, "Lỗi lưu: " + e.getMessage()));
            }
        }).start();
    }
    
    private String buildItemJson(DefaultTableModel model, String originalJson) {
        JsonArray newArr = new JsonArray();
        try { new JsonParser().parse(originalJson).getAsJsonArray(); } catch(Exception e) {}
        
        for (int i = 0; i < model.getRowCount(); i++) {
            try {
                int id = Integer.parseInt(model.getValueAt(i, 0).toString());
                int qty = Integer.parseInt(model.getValueAt(i, 2).toString());
                String rawOpt = model.getValueAt(i, 4).toString(); 
                
                JsonArray innerArr = new JsonArray();
                innerArr.add(id);
                innerArr.add(qty);
                innerArr.add(rawOpt); 
                innerArr.add(System.currentTimeMillis()); 
                newArr.add(new JsonPrimitive(innerArr.toString()));
            } catch (Exception e) { 
                newArr.add(new JsonPrimitive("[-1,0,\"[]\",0]"));
            }
        }
        return newArr.toString();
    }

    private String getText(Map<String, Component> inputs, String key) {
        Component c = inputs.get(key);
        if (c instanceof JTextField) return ((JTextField)c).getText();
        return "0";
    }
    
    private long getLongVal(Map<String, Component> inputs, String key) {
        try {
            String txt = ((JTextField)inputs.get(key)).getText();
            return Long.parseLong(txt.replaceAll("[^0-9-]", ""));
        } catch(Exception e) { return 0; }
    }

    private void setVal(JsonArray arr, int index, String val) {
        while (arr.size() <= index) {
            arr.add(new JsonPrimitive(0));
        }
        try {
            String cleanVal = val.replaceAll("[^0-9-]", "");
            long v = Long.parseLong(cleanVal);
            arr.set(index, new JsonPrimitive(v));
        } catch (Exception e) {
            arr.set(index, new JsonPrimitive(val));
        }
    }
    
    private static JButton createStyledButton(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }
}