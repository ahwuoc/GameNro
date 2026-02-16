package Top.weekly;

/**
 * Configuration for a weekly top type
 * Represents one type of top that rotates weekly (e.g., Boss, Power, DapDo, Nap)
 */
public class TopTypeConfig {
    public int id;
    public String name;           // "Top Săn Boss", "Top Sức Mạnh", etc.
    public int orderIndex;        // 0, 1, 2, 3 for rotation
    public String columnName;     // "pointboss", "power", "pointdapdo", "danap"

    public TopTypeConfig() {
    }

    public TopTypeConfig(int id, String name, int orderIndex, String columnName) {
        this.id = id;
        this.name = name;
        this.orderIndex = orderIndex;
        this.columnName = columnName;
    }

    @Override
    public String toString() {
        return "TopTypeConfig{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", orderIndex=" + orderIndex +
                ", columnName='" + columnName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TopTypeConfig that = (TopTypeConfig) o;

        if (id != that.id) return false;
        if (orderIndex != that.orderIndex) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return columnName != null ? columnName.equals(that.columnName) : that.columnName == null;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + orderIndex;
        result = 31 * result + (columnName != null ? columnName.hashCode() : 0);
        return result;
    }
}
