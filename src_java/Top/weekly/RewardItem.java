package Top.weekly;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single item in a reward tier
 */
public class RewardItem {
    public int tempId;                    // Item template ID
    public int quantity;
    public List<ItemOption> options;

    public RewardItem() {
        this.options = new ArrayList<>();
    }

    public RewardItem(int tempId, int quantity) {
        this.tempId = tempId;
        this.quantity = quantity;
        this.options = new ArrayList<>();
    }

    public RewardItem(int tempId, int quantity, List<ItemOption> options) {
        this.tempId = tempId;
        this.quantity = quantity;
        this.options = options != null ? options : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "RewardItem{" +
                "tempId=" + tempId +
                ", quantity=" + quantity +
                ", options=" + options +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RewardItem that = (RewardItem) o;

        if (tempId != that.tempId) return false;
        if (quantity != that.quantity) return false;
        return options != null ? options.equals(that.options) : that.options == null;
    }

    @Override
    public int hashCode() {
        int result = tempId;
        result = 31 * result + quantity;
        result = 31 * result + (options != null ? options.hashCode() : 0);
        return result;
    }
}
