package zalo.services;

import zalo.server.Settings;
import zalo.utils.Apis;
import java.sql.*;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class DatabaseService {
    // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */

    
    private static DatabaseService instance;
    private Connection connectionA;
    private Connection connectionB;
    
    private DatabaseService() {
    }
    
    public static DatabaseService gI() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }
    
    public void initialize(String loginId) {
        try {
            String urlA = Settings.getDatabaseUrlA();
            String urlB = Settings.getDatabaseUrlB();
            String username = Settings.getDatabaseUsername();
            String password = Settings.getDatabasePassword();
            
            connectionA = DriverManager.getConnection(urlA, username, password);
            connectionB = DriverManager.getConnection(urlB, username, password);
            
            String dbNameA = Settings.getDatabaseNameA();
            String dbNameB = Settings.getDatabaseNameB();
            
            createTables(connectionA, dbNameA);
            createTables(connectionB, dbNameB);
        } catch (SQLException e) {
        }
    }
    
    private void createTables(Connection conn, String dbName) {
        try {
            String createUsersTable = 
                "CREATE TABLE IF NOT EXISTS zalo_users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "uid VARCHAR(50) UNIQUE NOT NULL, " +
                "name TEXT, " +
                "role INT DEFAULT 0, " +
                "vnd INT DEFAULT 0, " +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
            
            String createGroupsTable = 
                "CREATE TABLE IF NOT EXISTS zalo_groups (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "group_id VARCHAR(50) UNIQUE NOT NULL, " +
                "name TEXT, " +
                "member_total INT DEFAULT 0, " +
                "boss_notify TINYINT(1) DEFAULT 0, " +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
            
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createUsersTable);
                stmt.execute(createGroupsTable);
                
                try {
                    String checkColumn = "SELECT boss_notify FROM zalo_groups LIMIT 1";
                    stmt.executeQuery(checkColumn);
                } catch (SQLException e) {
                    String alterGroupsTable = 
                        "ALTER TABLE groups ADD COLUMN boss_notify TINYINT(1) DEFAULT 0";
                    try {
                        stmt.execute(alterGroupsTable);
                    } catch (SQLException alterEx) {
                    }
                }
                
            }
        } catch (SQLException e) {
        }
    }
    
    public CompletableFuture<Void> updateUserFromMessage(Map<String, Object> message, Apis api) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (connectionA == null || connectionA.isClosed()) {
                    return;
                }
                
                updateUserInDatabase(connectionA, message, api, "a");
            } catch (Exception e) {
            }
        });
    }
    
    private void updateUserInDatabase(Connection conn, Map<String, Object> message, Apis api, String dbName) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) message.get("data");
            if (data == null) {
                return;
            }
            
            Object uidFromObj = data.get("uidFrom");
            if (uidFromObj == null) {
                uidFromObj = data.get("uid");
            }
            if (uidFromObj == null) {
                return;
            }
            
            String uid = String.valueOf(uidFromObj);
            String name = null;
            Object nameObj = data.get("nameFrom");
            if (nameObj == null) {
                nameObj = data.get("name");
            }
            if (nameObj == null) {
                nameObj = data.get("displayName");
            }
            if (nameObj == null) {
                nameObj = data.get("dName");
            }
            if (nameObj != null) {
                name = String.valueOf(nameObj);
            }
            
            int role = 0;
            
            Object threadTypeObj = message.get("threadType");
            boolean isGroup = false;
            if (threadTypeObj instanceof Number) {
                isGroup = ((Number) threadTypeObj).intValue() == 1;
            } else if (threadTypeObj != null) {
                isGroup = String.valueOf(threadTypeObj).equals("1") || String.valueOf(threadTypeObj).equals("GROUP");
            }
            
            if (isGroup && api != null) {
                try {
                    String threadId = String.valueOf(message.get("threadId"));
                    if (threadId != null && !threadId.equals("null")) {
                        Map<String, Object> groupInfo = api.getGroupInfo.getGroupInfo(threadId).get();
                        if (groupInfo != null) {
                            Object gridInfoMapObj = groupInfo.get("gridInfoMap");
                            if (gridInfoMapObj instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> gridInfoMap = (Map<String, Object>) gridInfoMapObj;
                                @SuppressWarnings("unchecked")
                                Map<String, Object> groupData = (Map<String, Object>) gridInfoMap.get(threadId);
                                if (groupData != null) {
                                    String creatorId = null;
                                    Object creatorIdObj = groupData.get("creatorId");
                                    if (creatorIdObj != null) {
                                        creatorId = String.valueOf(creatorIdObj);
                                    }
                                    
                                    Object adminIdsObj = groupData.get("adminIds");
                                    List<String> adminIds = new ArrayList<>();
                                    if (adminIdsObj instanceof List) {
                                        @SuppressWarnings("unchecked")
                                        List<Object> adminList = (List<Object>) adminIdsObj;
                                        for (Object adminId : adminList) {
                                            adminIds.add(String.valueOf(adminId));
                                        }
                                    }
                                    
                                    if (creatorId != null && creatorId.equals(uid)) {
                                        role = 1;
                                    } else if (adminIds.contains(uid)) {
                                        role = 1;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                }
            }
            
            String checkUser = "SELECT uid, role FROM zalo_users WHERE uid = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(checkUser)) {
                pstmt.setString(1, uid);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        int currentRole = rs.getInt("role");
                        int finalRole = role;
                        
                        if (currentRole >= 2) {
                            finalRole = Math.max(currentRole, role);
                        }
                        
                        String updateUser = 
                            "UPDATE zalo_users SET " +
                            "name = COALESCE(?, name), " +
                            "role = ?, " +
                            "updated_at = CURRENT_TIMESTAMP " +
                            "WHERE uid = ?";
                        
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateUser)) {
                            updateStmt.setString(1, name);
                            updateStmt.setInt(2, finalRole);
                            updateStmt.setString(3, uid);
                            updateStmt.executeUpdate();
                        }
                    } else {
                        String insertUser = 
                            "INSERT INTO zalo_users (uid, name, role, vnd) " +
                            "VALUES (?, ?, ?, 0)";
                        
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertUser)) {
                            insertStmt.setString(1, uid);
                            insertStmt.setString(2, name);
                            insertStmt.setInt(3, role);
                            insertStmt.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException e) {
        } catch (Exception e) {
        }
    }
    
    public CompletableFuture<Void> updateGroupFromMessage(Map<String, Object> message, Apis api) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (connectionA == null || connectionA.isClosed()) {
                    return;
                }
                
                String threadId = String.valueOf(message.get("threadId"));
                if (threadId == null || threadId.equals("null") || threadId.isEmpty()) {
                    return;
                }
                
                try {
                    Map<String, Object> groupInfo = api.getGroupInfo.getGroupInfo(threadId).get();
                    
                    if (groupInfo == null || groupInfo.isEmpty()) {
                        return;
                    }
                    
                    Object gridInfoMapObj = groupInfo.get("gridInfoMap");
                    if (!(gridInfoMapObj instanceof Map)) {
                        return;
                    }
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> gridInfoMap = (Map<String, Object>) gridInfoMapObj;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> groupData = (Map<String, Object>) gridInfoMap.get(threadId);
                    
                    if (groupData == null || groupData.isEmpty()) {
                        return;
                    }
                    
                    String groupName = null;
                    Object nameObj = groupData.get("name");
                    if (nameObj == null) {
                        nameObj = groupData.get("groupName");
                    }
                    if (nameObj != null) {
                        groupName = String.valueOf(nameObj);
                    }
                    if (groupName == null || groupName.isEmpty()) {
                        groupName = "Group " + threadId;
                    }
                    
                    int memberTotal = 0;
                    Object memberTotalObj = groupData.get("totalMember");
                    if (memberTotalObj == null) {
                        memberTotalObj = groupData.get("memberCount");
                    }
                    if (memberTotalObj == null) {
                        memberTotalObj = groupData.get("memberTotal");
                    }
                    if (memberTotalObj instanceof Number) {
                        memberTotal = ((Number) memberTotalObj).intValue();
                    }
                    
                    updateGroupInDatabase(connectionA, threadId, groupName, memberTotal, "a");
                } catch (Exception e) {
                }
            } catch (Exception e) {
            }
        });
    }
    
    private void updateGroupInDatabase(Connection conn, String threadId, String groupName, int memberTotal, String dbName) {
        try {
            String checkGroup = "SELECT group_id, name, member_total FROM zalo_groups WHERE group_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(checkGroup)) {
                pstmt.setString(1, threadId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String currentName = rs.getString("name");
                        int currentMemberTotal = rs.getInt("member_total");
                        
                        boolean hasChanges = false;
                        if (groupName != null && !groupName.equals(currentName)) {
                            hasChanges = true;
                        }
                        if (memberTotal != currentMemberTotal) {
                            hasChanges = true;
                        }
                        
                        if (hasChanges) {
                            String updateGroup = 
                                "UPDATE zalo_groups SET " +
                                "name = ?, " +
                                "member_total = ?, " +
                                "updated_at = CURRENT_TIMESTAMP " +
                                "WHERE group_id = ?";
                            
                            try (PreparedStatement updateStmt = conn.prepareStatement(updateGroup)) {
                                updateStmt.setString(1, groupName);
                                updateStmt.setInt(2, memberTotal);
                                updateStmt.setString(3, threadId);
                                updateStmt.executeUpdate();
                            }
                        }
                    } else {
                        String insertGroup = 
                            "INSERT INTO zalo_groups (group_id, name, member_total) " +
                            "VALUES (?, ?, ?)";
                        
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertGroup)) {
                            insertStmt.setString(1, threadId);
                            insertStmt.setString(2, groupName);
                            insertStmt.setInt(3, memberTotal);
                            insertStmt.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException e) {
        }
    }
    
    public void updateBossNotify(String threadId, boolean enabled) {
        try {
            if (connectionA == null || connectionA.isClosed()) {
                return;
            }
            
            String updateSql = 
                "UPDATE zalo_groups SET boss_notify = ?, updated_at = CURRENT_TIMESTAMP WHERE group_id = ?";
            
            try (PreparedStatement pstmt = connectionA.prepareStatement(updateSql)) {
                pstmt.setInt(1, enabled ? 1 : 0);
                pstmt.setString(2, threadId);
                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected == 0) {
                    String insertSql = 
                        "INSERT INTO zalo_groups (group_id, name, member_total, boss_notify) " +
                        "VALUES (?, 'Unknown Group', 0, ?) " +
                        "ON DUPLICATE KEY UPDATE boss_notify = ?";
                    
                    try (PreparedStatement insertStmt = connectionA.prepareStatement(insertSql)) {
                        insertStmt.setString(1, threadId);
                        insertStmt.setInt(2, enabled ? 1 : 0);
                        insertStmt.setInt(3, enabled ? 1 : 0);
                        insertStmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
        }
    }
    
    public List<String> loadBossNotifyGroups() {
        List<String> groups = new ArrayList<>();
        try {
            if (connectionA == null || connectionA.isClosed()) {
                return groups;
            }
            
            String selectSql = "SELECT group_id FROM zalo_groups WHERE boss_notify = 1";
            
            try (PreparedStatement pstmt = connectionA.prepareStatement(selectSql);
                 ResultSet rs = pstmt.executeQuery()) {
                
                while (rs.next()) {
                    String groupId = rs.getString("group_id");
                    if (groupId != null && !groupId.isEmpty()) {
                        groups.add(groupId);
                    }
                }
            }
        } catch (SQLException e) {
        }
        return groups;
    }
    
    public int getUserRole(String uid) {
        try {
            if (connectionA == null || connectionA.isClosed()) {
                return 0;
            }
            
            String selectSql = "SELECT role FROM zalo_users WHERE uid = ?";
            try (PreparedStatement pstmt = connectionA.prepareStatement(selectSql)) {
                pstmt.setString(1, uid);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("role");
                    }
                }
            }
        } catch (SQLException e) {
        }
        return 0;
    }
    
    
    public void close() {
        try {
            if (connectionA != null && !connectionA.isClosed()) {
                connectionA.close();
            }
            if (connectionB != null && !connectionB.isClosed()) {
                connectionB.close();
            }
        } catch (SQLException e) {
        }
    }
}

