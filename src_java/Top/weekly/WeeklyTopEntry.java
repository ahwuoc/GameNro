package Top.weekly;

/**
 * Represents a player entry in the weekly top rankings
 */
public class WeeklyTopEntry {
    public int playerId;
    public String playerName;
    public int rank;
    public long points;
    public short head;
    public short body;
    public short leg;
    public byte gender;

    public WeeklyTopEntry() {
    }

    public WeeklyTopEntry(int playerId, String playerName, int rank, long points) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.rank = rank;
        this.points = points;
    }

    public WeeklyTopEntry(int playerId, String playerName, int rank, long points, 
                         short head, short body, short leg, byte gender) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.rank = rank;
        this.points = points;
        this.head = head;
        this.body = body;
        this.leg = leg;
        this.gender = gender;
    }

    @Override
    public String toString() {
        return "WeeklyTopEntry{" +
                "playerId=" + playerId +
                ", playerName='" + playerName + '\'' +
                ", rank=" + rank +
                ", points=" + points +
                ", head=" + head +
                ", body=" + body +
                ", leg=" + leg +
                ", gender=" + gender +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WeeklyTopEntry that = (WeeklyTopEntry) o;

        if (playerId != that.playerId) return false;
        if (rank != that.rank) return false;
        if (points != that.points) return false;
        if (head != that.head) return false;
        if (body != that.body) return false;
        if (leg != that.leg) return false;
        if (gender != that.gender) return false;
        return playerName != null ? playerName.equals(that.playerName) : that.playerName == null;
    }

    @Override
    public int hashCode() {
        int result = playerId;
        result = 31 * result + (playerName != null ? playerName.hashCode() : 0);
        result = 31 * result + rank;
        result = 31 * result + (int) (points ^ (points >>> 32));
        result = 31 * result + (int) head;
        result = 31 * result + (int) body;
        result = 31 * result + (int) leg;
        result = 31 * result + (int) gender;
        return result;
    }
}
