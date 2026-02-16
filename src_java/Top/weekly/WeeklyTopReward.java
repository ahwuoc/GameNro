package Top.weekly;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a reward tier for a specific rank range in weekly top
 */
public class WeeklyTopReward {
    public int id;
    public int topTypeId;                 // FK to weekly_top_types
    public int rankFrom;                  // Starting rank (e.g., 1)
    public int rankTo;                    // Ending rank (e.g., 1 for Top 1, or 3 for Top 2-3)
    public List<RewardItem> items;        // Parsed from JSON details
    public String description;            // "Top 1", "Top 2-3", etc.

    private static final Gson gson = new Gson();

    public WeeklyTopReward() {
        this.items = new ArrayList<>();
    }

    public WeeklyTopReward(int id, int topTypeId, int rankFrom, int rankTo, String description) {
        this.id = id;
        this.topTypeId = topTypeId;
        this.rankFrom = rankFrom;
        this.rankTo = rankTo;
        this.description = description;
        this.items = new ArrayList<>();
    }

    /**
     * Parse JSON details string into RewardItem list
     * @param jsonDetails JSON array string: [{"temp_id":441,"quantity":10,"options":[{"param":1,"id":30}]},...]
     */
    public void parseDetails(String jsonDetails) {
        if (jsonDetails == null || jsonDetails.isEmpty()) {
            this.items = new ArrayList<>();
            return;
        }

        try {
            // Parse JSON array into list of maps, then convert to RewardItem objects
            List<RewardItem> parsedItems = gson.fromJson(jsonDetails, 
                new TypeToken<List<RewardItem>>(){}.getType());
            this.items = parsedItems != null ? parsedItems : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Error parsing reward details JSON: " + e.getMessage());
            this.items = new ArrayList<>();
        }
    }

    /**
     * Check if a rank falls within this reward tier
     * @param rank Player's rank
     * @return true if rank is within rankFrom and rankTo (inclusive)
     */
    public boolean isRankInTier(int rank) {
        return rank >= rankFrom && rank <= rankTo;
    }

    @Override
    public String toString() {
        return "WeeklyTopReward{" +
                "id=" + id +
                ", topTypeId=" + topTypeId +
                ", rankFrom=" + rankFrom +
                ", rankTo=" + rankTo +
                ", items=" + items +
                ", description='" + description + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WeeklyTopReward that = (WeeklyTopReward) o;

        if (id != that.id) return false;
        if (topTypeId != that.topTypeId) return false;
        if (rankFrom != that.rankFrom) return false;
        if (rankTo != that.rankTo) return false;
        if (items != null ? !items.equals(that.items) : that.items != null) return false;
        return description != null ? description.equals(that.description) : that.description == null;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + topTypeId;
        result = 31 * result + rankFrom;
        result = 31 * result + rankTo;
        result = 31 * result + (items != null ? items.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }
}
