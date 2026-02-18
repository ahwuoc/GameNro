package zalo.services;

import zalo.server.Settings;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class NroAccountService {
    // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */

    
    private static NroAccountService instance;
    
    private NroAccountService() {
    }
    
    public static NroAccountService gI() {
        if (instance == null) {
            instance = new NroAccountService();
        }
        return instance;
    }
    
    private Connection getConnection() throws SQLException {
        String dbUrl = Settings.getDatabaseUrlA();
        String dbUser = Settings.getDatabaseUsername();
        String dbPassword = Settings.getDatabasePassword();
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }
    
    public boolean updateAccountVnd(String username, int amount) {
        try (Connection conn = getConnection()) {
            String updateSql;
            if ("all".equalsIgnoreCase(username)) {
                updateSql = "UPDATE account SET vnd = vnd + ?, danap = danap + ?";
            } else {
                updateSql = "UPDATE account SET vnd = vnd + ?, danap = danap + ? WHERE username = ?";
            }
            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setInt(1, amount);
                pstmt.setInt(2, amount);
                if (!"all".equalsIgnoreCase(username)) {
                    pstmt.setString(3, username);
                }
                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
        }
        return false;
    }
    
    public boolean setAccountBanned(String username, boolean banned) {
        try (Connection conn = getConnection()) {
            String updateSql = "UPDATE account SET ban = ? WHERE username = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setInt(1, banned ? 1 : 0);
                pstmt.setString(2, username);
                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
        }
        return false;
    }
    
    public boolean setAccountAdmin(String username, boolean admin) {
        try (Connection conn = getConnection()) {
            String updateSql = "UPDATE account SET is_admin = ? WHERE username = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setInt(1, admin ? 1 : 0);
                pstmt.setString(2, username);
                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
        }
        return false;
    }
    
    public boolean addMemberToVip(String username) {
        try (Connection conn = getConnection()) {
            String updateSql;
            if ("all".equalsIgnoreCase(username)) {
                updateSql = "UPDATE account SET vip = vip + 1";
            } else {
                updateSql = "UPDATE account SET vip = vip + 1 WHERE username = ?";
            }
            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                if (!"all".equalsIgnoreCase(username)) {
                    pstmt.setString(1, username);
                }
                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
        }
        return false;
    }
}

