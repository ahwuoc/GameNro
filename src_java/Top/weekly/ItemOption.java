package Top.weekly;

/**
 * Represents an option/attribute for a reward item
 */
public class ItemOption {
    public int id;
    public int param;

    public ItemOption() {
    }

    public ItemOption(int id, int param) {
        this.id = id;
        this.param = param;
    }

    @Override
    public String toString() {
        return "ItemOption{" +
                "id=" + id +
                ", param=" + param +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ItemOption that = (ItemOption) o;

        if (id != that.id) return false;
        return param == that.param;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + param;
        return result;
    }
}
