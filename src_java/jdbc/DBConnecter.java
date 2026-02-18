package jdbc;

import clan.Clan;
import com.mysql.jdbc.log.Log;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import utils.Logger;

public class DBConnecter {

    private static String DRIVER = "com.mysql.jdbc.Driver";
    private static final String URL = "jdbc:mysql://%s:%s/%s?useUnicode=yes&characterEncoding=UTF-8";
    private static String DB_HOST = "127.0.0.1";
    private static String DB_PORT = "3306";
    public static String DB_DATA = "barcoll";
    public static String DB_USER = "root";
    private static String DB_PASSWORD = "";
    private static int MIN_CONN = 5;
    private static int MAX_CONN = 20;
    private static long MAX_LIFE_TIME = 600000L;
    private static final HikariConfig config = new HikariConfig();
    private static final HikariDataSource ds;
    private static DBConnecter i;
    private static final Connection[] connections = new Connection[10];

    public static DBConnecter gI() {
        if (i == null) {
            i = new DBConnecter();
        }
        return i;
    }

    public static Connection getConnectionServer() throws SQLException {
        return ds.getConnection();
    }

    public static void close() {
        ds.close();
    }
    
    public Connection getConnectionForHistGoldBar() throws SQLException {
        if (connections[10] != null) {
            if (!connections[10].isValid(10)) {
                connections[10].close();
            }
        }
        if (connections[10] == null || connections[10].isClosed()) {
            try {
                connections[10] = getConnection();
                return getConnectionForHistGoldBar();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return connections[10];
    }

    public synchronized Connection getConnectionForLogin() throws SQLException {
        if (connections[0] != null) {
            if (!connections[0].isValid(10)) {
                connections[0].close();
            }
        }
        if (connections[0] == null || connections[0].isClosed()) {
            try {
                connections[0] = getConnection();
                return getConnectionForLogin();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return connections[0];
    }
    
    public Connection getConnectionForDebug() throws SQLException {
        if (connections[15] != null) {
            if (!connections[15].isValid(10)) {
                connections[15].close();
            }
        }
        if (connections[15] == null || connections[15].isClosed()) {
            try {
                connections[15] = getConnection();
                return getConnectionForDebug();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return connections[15];
    }

    public int getNextClanId() {
        String query = "SELECT MAX(id) AS max_id FROM clan";
        Connection con = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            con = DBConnecter.gI().getConnectionServer();
            if (con == null) {
                Logger.log("Không thể kết nối đến cơ sở dữ liệu Clan (con=null)");
                return 1; // fallback
            }

            stmt = con.createStatement();
            rs = stmt.executeQuery(query);

            if (rs.next()) {
                int maxId = rs.getInt("max_id");
                if (rs.wasNull()) {
                    return 1; // bảng trống
                }
                return maxId + 1;
            }

        } catch (SQLException e) {
            Logger.logException(Clan.class, e, "Lỗi khi lấy ID tiếp theo của clan");
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (Exception ignored) {
            }
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (Exception ignored) {
            }
            try {
                if (con != null) {
                    con.close();
                }
            } catch (Exception ignored) {
            }
        }
        return 1;
    }

    public synchronized Connection getConnectionForLogout() throws SQLException {
        if (connections[1] != null) {
            if (!connections[1].isValid(10)) {
                connections[1].close();
            }
        }
        if (connections[1] == null || connections[1].isClosed()) {
            try {
                connections[1] = getConnection();
                return getConnectionForLogout();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return connections[1];
    }

    public synchronized Connection getConnectionForSaveData() throws SQLException {
        if (connections[2] != null) {
            if (!connections[2].isValid(10)) {
                connections[2].close();
            }
        }
        if (connections[2] == null || connections[2].isClosed()) {
            try {
                connections[2] = getConnection();
                return getConnectionForSaveData();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return connections[2];
    }

    public synchronized Connection getConnectionForGame() throws SQLException {
        if (connections[3] != null) {
            if (!connections[3].isValid(10)) {
                connections[3].close();
            }
        }
        if (connections[3] == null || connections[3].isClosed()) {
            try {
                connections[3] = getConnection();
                return getConnectionForGame();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return connections[3];
    }

    public synchronized Connection getConnectionForClan() throws SQLException {
        try {
            if (connections[4] != null && !connections[4].isValid(10)) {
                connections[4].close();
                connections[4] = null;
            }

            if (connections[4] == null || connections[4].isClosed()) {
                connections[4] = getConnection();
            }

            return connections[4];
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new SQLException("Failed to get connection for Clan", ex);
        }
    }

    public Connection getConnectionForAutoSave() throws SQLException {
        if (connections[5] != null) {
            if (!connections[5].isValid(10)) {
                connections[5].close();
            }
        }
        if (connections[5] == null || connections[5].isClosed()) {
            try {
                connections[5] = getConnection();
                return getConnectionForAutoSave();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return connections[5];
    }

    public Connection getConnectionForSaveHistory() throws SQLException {
        if (connections[6] != null) {
            if (!connections[6].isValid(10)) {
                connections[6].close();
            }
        }
        if (connections[6] == null || connections[6].isClosed()) {
            try {
                connections[6] = getConnection();
                return getConnectionForSaveHistory();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return connections[6];
    }

    public Connection getConnectionForGetPlayer() throws SQLException {
        int maxRetry = 3; // hoặc nhiều hơn nếu bạn muốn
        int attempt = 0;

        while (attempt < maxRetry) {
            try {
                if (connections[7] != null && !connections[7].isValid(10)) {
                    connections[7].close();
                    connections[7] = null;
                }

                if (connections[7] == null || connections[7].isClosed()) {
                    connections[7] = getConnection();
                }

                if (connections[7] != null && connections[7].isValid(10)) {
                    return connections[7];
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            attempt++;
        }

        throw new SQLException("Failed to get valid connection after " + maxRetry + " attempts.");
    }

    public Connection getConnectionCreatPlayer() throws SQLException {
        if (connections[8] != null) {
            if (!connections[8].isValid(10)) {
                connections[8].close();
            }
        }
        if (connections[8] == null || connections[8].isClosed()) {
            try {
                connections[8] = getConnection();
                return getConnectionCreatPlayer();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return connections[8];
    }

    private static void loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream("data/config/config.properties")) {
            properties.load(input);

            DRIVER = properties.getProperty("database.driver", DRIVER);
            DB_HOST = properties.getProperty("database.host", DB_HOST);
            DB_PORT = properties.getProperty("database.port", DB_PORT);
//            DB_SERVER = properties.getProperty("database.server", DB_SERVER);
            DB_DATA = properties.getProperty("database.name", DB_DATA);
            DB_USER = properties.getProperty("database.user", DB_USER);
            DB_PASSWORD = properties.getProperty("database.pass", DB_PASSWORD);
            MIN_CONN = Integer.parseInt(properties.getProperty("database.min", String.valueOf(MIN_CONN)));
            MAX_CONN = Integer.parseInt(properties.getProperty("database.max", String.valueOf(MAX_CONN)));
            MAX_LIFE_TIME = Long.parseLong(properties.getProperty("database.lifetime", String.valueOf(MAX_LIFE_TIME)));

            System.out.println("Downloaded Loading file properties");

        } catch (IOException | NumberFormatException e) {
            Logger.log("[4;31m", "Không thể load file properties!\n");
        }
    }

    public void release(Connection con) {
//        this.connPool.free(con);
    }

    public Connection getConnection() throws Exception {
//        return this.connPool.getConnection();
//        return DBConnecter.getConnection();
        return null;
//        return this.connPool.getConnection();
//        return DBConnecter.getConnection();
    }

    public static NDVResultSet executeQuery(String query) throws Exception {
        try (Connection connection = DBConnecter.getConnectionServer(); PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            return new ResultSetImpl(preparedStatement.executeQuery());
        } catch (Exception e) {
            Logger.log("[4;31m", "Có lỗi xảy ra khi thực thi câu lệnh: " + query + "\n");
            throw e;
        }
    }

    public static NDVResultSet executeQuery(String query, Object... params) throws Exception {
        try (Connection connection = DBConnecter.getConnectionServer(); PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) {
                preparedStatement.setObject(i + 1, params[i]);
            }
            return new ResultSetImpl(preparedStatement.executeQuery());
        } catch (Exception e) {
            Logger.log("[4;31m", "Có lỗi xảy ra khi thực thi câu lệnh: " + query + "\n");
            throw e;
        }
    }

    public static int executeUpdate(String query) throws Exception {
        try (Connection connection = DBConnecter.getConnectionServer(); PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            return preparedStatement.executeUpdate();
        } catch (Exception e) {
            Logger.log("[4;31m", "Có lỗi xảy ra khi thực thi câu lệnh: " + query + "\n");
            throw e;
        }
    }

    public static int executeUpdate(String query, Object... params) throws Exception {
        if (query.toLowerCase().startsWith("insert") && query.endsWith("()")) {
            StringBuilder placeholder = new StringBuilder();
            placeholder.append("(");
            for (int i = 0; i < params.length; i++) {
                placeholder.append("?");
                if (i < params.length - 1) {
                    placeholder.append(",");
                }
            }
            placeholder.append(")");
            query = query.replace("()", placeholder.toString());
        }

        try (Connection connection = DBConnecter.getConnectionServer(); PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) {
                preparedStatement.setObject(i + 1, params[i]);
            }
            return preparedStatement.executeUpdate();
        } catch (Exception e) {
            Logger.log("[4;31m", "Có lỗi xảy ra khi thực thi câu lệnh: " + query + "\n");
            throw e;
        }
    }

    static {
        loadProperties();
        config.setDriverClassName(DRIVER);
        config.setJdbcUrl(String.format(URL, DB_HOST, DB_PORT, DB_DATA));
        config.setUsername(DB_USER);
        config.setPassword(DB_PASSWORD);
        config.setMinimumIdle(MIN_CONN);
        config.setMaximumPoolSize(MAX_CONN);

        config.setMaxLifetime(MAX_LIFE_TIME);  // ví dụ: 30 phút
        config.setIdleTimeout(60000);          // idleTimeout < maxLifetime

        // Các cấu hình thêm
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "true");

        ds = new HikariDataSource(config);
    }
}
