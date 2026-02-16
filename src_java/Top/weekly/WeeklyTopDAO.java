package Top.weekly;

import jdbc.DBConnecter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Weekly Top Race System
 * Handles all database operations for top types, rankings, and claims
 */
public class WeeklyTopDAO {

    /**
     * Load all top type configurations from database
     * Called once during initialization
     * @return List of TopTypeConfig objects
     */
    public List<TopTypeConfig> loadTopTypes() {
        List<TopTypeConfig> topTypes = new ArrayList<>();
        String sql = "SELECT id, name, order_index, column_name FROM weekly_top_types ORDER BY order_index ASC";

        try (Connection conn = DBConnecter.getConnectionServer();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                TopTypeConfig config = new TopTypeConfig(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getInt("order_index"),
                    rs.getString("column_name")
                );
                topTypes.add(config);
            }
        } catch (SQLException e) {
            System.err.println("Error loading top types: " + e.getMessage());
            e.printStackTrace();
        }

        return topTypes;
    }

    /**
     * Load reward configurations for a specific top type
     * @param topTypeId ID of the top type
     * @return List of WeeklyTopReward objects
     */
    public List<WeeklyTopReward> loadRewardsByTopType(int topTypeId) {
        List<WeeklyTopReward> rewards = new ArrayList<>();
        String sql = "SELECT id, top_type_id, rank_from, rank_to, details, description " +
                     "FROM weekly_top_rewards WHERE top_type_id = ? ORDER BY rank_from ASC";

        try (Connection conn = DBConnecter.getConnectionServer();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, topTypeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    WeeklyTopReward reward = new WeeklyTopReward(
                        rs.getInt("id"),
                        rs.getInt("top_type_id"),
                        rs.getInt("rank_from"),
                        rs.getInt("rank_to"),
                        rs.getString("description")
                    );
                    // Parse JSON details
                    reward.parseDetails(rs.getString("details"));
                    rewards.add(reward);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading rewards for top type " + topTypeId + ": " + e.getMessage());
            e.printStackTrace();
        }

        return rewards;
    }

    /**
     * Load top rankings from player table
     * @param columnName Column name to sort by (e.g., "pointboss", "power")
     * @param limit Number of top players to return
     * @return List of WeeklyTopEntry objects sorted by points descending
     */
    public List<WeeklyTopEntry> loadTopEntries(String columnName, int limit) {
        List<WeeklyTopEntry> entries = new ArrayList<>();
        String sql = "SELECT id, name, " + columnName + " as points, head, gender " +
                     "FROM player WHERE " + columnName + " > 0 " +
                     "ORDER BY " + columnName + " DESC LIMIT ?";

        try (Connection conn = DBConnecter.getConnectionServer();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    WeeklyTopEntry entry = new WeeklyTopEntry(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rank,
                        rs.getLong("points"),
                        rs.getShort("head"),
                        (short)0,  // body - không có trong DB
                        (short)0,  // leg - không có trong DB
                        rs.getByte("gender")
                    );
                    entries.add(entry);
                    rank++;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading top entries for column " + columnName + ": " + e.getMessage());
            e.printStackTrace();
        }

        return entries;
    }

    /**
     * Check if a player has already claimed reward for a specific week
     * @param playerId Player ID
     * @param weekNumber Week number
     * @param year Year
     * @return true if claim exists, false otherwise
     */
    public boolean hasClaimedReward(int playerId, int weekNumber, int year) {
        String sql = "SELECT 1 FROM weekly_top_claims WHERE player_id = ? AND week_number = ? AND year = ?";

        try (Connection conn = DBConnecter.getConnectionServer();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, playerId);
            pstmt.setInt(2, weekNumber);
            pstmt.setInt(3, year);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error checking claim status: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Record a reward claim in the database
     * @param playerId Player ID
     * @param weekNumber Week number
     * @param year Year
     * @param rank Player's rank when claimed
     * @return true if claim was recorded successfully, false otherwise
     */
    public boolean recordClaim(int playerId, int weekNumber, int year, int rank) {
        String sql = "INSERT INTO weekly_top_claims (player_id, week_number, year, rank_achieved) " +
                     "VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnecter.getConnectionServer();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, playerId);
            pstmt.setInt(2, weekNumber);
            pstmt.setInt(3, year);
            pstmt.setInt(4, rank);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            // Check if it's a duplicate key error
            if (e.getMessage().contains("Duplicate entry")) {
                System.out.println("Player " + playerId + " already claimed reward for week " + weekNumber);
                return false;
            }
            System.err.println("Error recording claim: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Load a specific claim record
     * @param playerId Player ID
     * @param weekNumber Week number
     * @param year Year
     * @return WeeklyTopClaim object if found, null otherwise
     */
    public WeeklyTopClaim loadClaimRecord(int playerId, int weekNumber, int year) {
        String sql = "SELECT id, player_id, week_number, year, rank_achieved, claimed_at " +
                     "FROM weekly_top_claims WHERE player_id = ? AND week_number = ? AND year = ?";

        try (Connection conn = DBConnecter.getConnectionServer();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, playerId);
            pstmt.setInt(2, weekNumber);
            pstmt.setInt(3, year);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new WeeklyTopClaim(
                        rs.getInt("player_id"),
                        rs.getInt("week_number"),
                        rs.getInt("year"),
                        rs.getInt("rank_achieved"),
                        rs.getTimestamp("claimed_at").getTime()
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading claim record: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }
}
