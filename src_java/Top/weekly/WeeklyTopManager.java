package Top.weekly;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Singleton manager for Weekly Top Race System
 * Handles business logic for top type rotation, rankings, and reward claims
 */
public class WeeklyTopManager {
    private static WeeklyTopManager instance;
    private List<TopTypeConfig> topTypes;
    private WeeklyTopDAO dao;

    private WeeklyTopManager() {
        this.dao = new WeeklyTopDAO();
    }

    /**
     * Get singleton instance
     */
    public static WeeklyTopManager getInstance() {
        if (instance == null) {
            synchronized (WeeklyTopManager.class) {
                if (instance == null) {
                    instance = new WeeklyTopManager();
                }
            }
        }
        return instance;
    }

    /**
     * Initialize manager - load top types from database
     * Called once on server startup
     */
    public void initialize() {
        try {
            this.topTypes = dao.loadTopTypes();
            if (topTypes.isEmpty()) {
                System.err.println("WARNING: No top types loaded from database!");
            } else {
                System.out.println("Loaded " + topTypes.size() + " top types");
            }
        } catch (Exception e) {
            System.err.println("Error initializing WeeklyTopManager: " + e.getMessage());
            e.printStackTrace();
            this.topTypes = new java.util.ArrayList<>();
        }
    }


    public int getCurrentWeekNumber() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.WEEK_OF_YEAR);
    }

    /**
     * Get current year
     * @return Year from Calendar.YEAR
     */
    public int getCurrentYear() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.YEAR);
    }

    /**
     * Get current top type based on week rotation
     * Uses formula: weekNumber % totalTopTypes
     * @return Current TopTypeConfig
     */
    public TopTypeConfig getCurrentTopType() {
        // Lazy load if not initialized
        if (topTypes == null) {
            initialize();
        }
        
        if (topTypes == null || topTypes.isEmpty()) {
            System.err.println("ERROR: No top types available!");
            return null;
        }

        int weekNumber = getCurrentWeekNumber();
        int index = weekNumber % topTypes.size();
        
        for (TopTypeConfig config : topTypes) {
            if (config.orderIndex == index) {
                return config;
            }
        }

        // Fallback to first top type if not found
        return topTypes.get(0);
    }

    /**
     * Get top rankings for current top type
     * @param limit Number of top players to return
     * @return List of WeeklyTopEntry sorted by points descending
     */
    public List<WeeklyTopEntry> getTopRankings(int limit) {
        try {
            TopTypeConfig currentType = getCurrentTopType();
            if (currentType == null) {
                System.out.println("[WeeklyTop] getCurrentTopType returned null");
                return List.of();
            }
            System.out.println("[WeeklyTop] Loading rankings for column: " + currentType.columnName);
            List<WeeklyTopEntry> entries = dao.loadTopEntries(currentType.columnName, limit);
            System.out.println("[WeeklyTop] Loaded " + entries.size() + " entries from database");
            return entries;
        } catch (Exception e) {
            System.err.println("Error loading top rankings: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Get player's rank in current top type
     * @param playerId Player ID
     * @return Player's rank (1-based), or -1 if not in top
     */
    public int getPlayerRank(int playerId) {
        List<WeeklyTopEntry> rankings = getTopRankings(1000); // Get top 1000
        for (WeeklyTopEntry entry : rankings) {
            if (entry.playerId == playerId) {
                return entry.rank;
            }
        }
        return -1;
    }

    /**
     * Get player's score in current top type
     * @param playerId Player ID
     * @return Player's score, or 0 if not found
     */
    public long getPlayerScore(int playerId) {
        List<WeeklyTopEntry> rankings = getTopRankings(1000);
        for (WeeklyTopEntry entry : rankings) {
            if (entry.playerId == playerId) {
                return entry.points;
            }
        }
        return 0;
    }

    /**
     * Check if player can claim reward (must be in top 10, not already claimed, and it's Sunday)
     * @param playerId Player ID
     * @return true if player can claim, false otherwise
     */
    public boolean canClaimReward(int playerId) {
        try {
            // Check if it's Sunday (day of week = 1 in Calendar) using Vietnam timezone
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            if (dayOfWeek != Calendar.SUNDAY) {
                System.out.println("[WeeklyTop] Cannot claim on day " + dayOfWeek + ", only on Sunday");
                return false;
            }

            int rank = getPlayerRank(playerId);
            if (rank < 1 || rank > 10) {
                return false;
            }

            int weekNumber = getCurrentWeekNumber();
            int year = getCurrentYear();
            return !dao.hasClaimedReward(playerId, weekNumber, year);
        } catch (Exception e) {
            System.err.println("Error checking claim reward: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get reward tier for a specific rank
     * @param rank Player's rank
     * @return WeeklyTopReward if found, null otherwise
     */
    public WeeklyTopReward getRewardForRank(int rank) {
        try {
            TopTypeConfig currentType = getCurrentTopType();
            if (currentType == null) {
                return null;
            }

            List<WeeklyTopReward> rewards = dao.loadRewardsByTopType(currentType.id);
            for (WeeklyTopReward reward : rewards) {
                if (reward.isRankInTier(rank)) {
                    return reward;
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error loading reward for rank " + rank + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if player has already claimed reward for a specific week
     * @param playerId Player ID
     * @param weekNumber Week number
     * @param year Year
     * @return true if already claimed, false otherwise
     */
    public boolean hasClaimedReward(int playerId, int weekNumber, int year) {
        return dao.hasClaimedReward(playerId, weekNumber, year);
    }

    /**
     * Record a reward claim
     * @param playerId Player ID
     * @param rank Player's rank
     * @return true if claim was recorded successfully, false otherwise
     */
    public boolean recordClaim(int playerId, int rank) {
        try {
            int weekNumber = getCurrentWeekNumber();
            int year = getCurrentYear();
            return dao.recordClaim(playerId, weekNumber, year, rank);
        } catch (Exception e) {
            System.err.println("Error recording claim: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get remaining time in current week (until next Monday 00:00) using Vietnam timezone
     * @return Remaining time in milliseconds
     */
    public long getRemainingTimeInWeek() {
        TimeZone vn = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
        Calendar now = Calendar.getInstance(vn);
        Calendar nextMonday = Calendar.getInstance(vn);
        
        // Set to next Monday 00:00
        nextMonday.add(Calendar.WEEK_OF_YEAR, 1);
        nextMonday.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        nextMonday.set(Calendar.HOUR_OF_DAY, 0);
        nextMonday.set(Calendar.MINUTE, 0);
        nextMonday.set(Calendar.SECOND, 0);
        nextMonday.set(Calendar.MILLISECOND, 0);

        long remaining = nextMonday.getTimeInMillis() - now.getTimeInMillis();
        return Math.max(0, remaining);
    }

    /**
     * Get all top types
     * @return List of TopTypeConfig
     */
    public List<TopTypeConfig> getTopTypes() {
        return topTypes;
    }
}
