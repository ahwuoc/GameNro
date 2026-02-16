package Top.weekly;

/**
 * Represents a claim record for weekly top rewards
 */
public class WeeklyTopClaim {
    public int playerId;
    public int weekNumber;
    public int year;
    public int rankAchieved;
    public long claimedAt;

    public WeeklyTopClaim() {
    }

    public WeeklyTopClaim(int playerId, int weekNumber, int year, int rankAchieved) {
        this.playerId = playerId;
        this.weekNumber = weekNumber;
        this.year = year;
        this.rankAchieved = rankAchieved;
        this.claimedAt = System.currentTimeMillis();
    }

    public WeeklyTopClaim(int playerId, int weekNumber, int year, int rankAchieved, long claimedAt) {
        this.playerId = playerId;
        this.weekNumber = weekNumber;
        this.year = year;
        this.rankAchieved = rankAchieved;
        this.claimedAt = claimedAt;
    }

    @Override
    public String toString() {
        return "WeeklyTopClaim{" +
                "playerId=" + playerId +
                ", weekNumber=" + weekNumber +
                ", year=" + year +
                ", rankAchieved=" + rankAchieved +
                ", claimedAt=" + claimedAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WeeklyTopClaim that = (WeeklyTopClaim) o;

        if (playerId != that.playerId) return false;
        if (weekNumber != that.weekNumber) return false;
        if (year != that.year) return false;
        if (rankAchieved != that.rankAchieved) return false;
        return claimedAt == that.claimedAt;
    }

    @Override
    public int hashCode() {
        int result = playerId;
        result = 31 * result + weekNumber;
        result = 31 * result + year;
        result = 31 * result + rankAchieved;
        result = 31 * result + (int) (claimedAt ^ (claimedAt >>> 32));
        return result;
    }
}
