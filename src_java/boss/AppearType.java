package boss;


public enum AppearType {
    DEFAULT_APPEAR,
    APPEAR_WITH_ANOTHER,
    ANOTHER_LEVEL,
    CALL_BY_ANOTHER;
    public static AppearType fromString(String value) {
        if (value == null || value.isEmpty()) {
            return DEFAULT_APPEAR;
        }
        
        try {
            return AppearType.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            String normalized = value.toUpperCase().trim().replace("-", "_").replace(" ", "_");
            try {
                return AppearType.valueOf(normalized);
            } catch (IllegalArgumentException ex) {
                return DEFAULT_APPEAR;
            }
        }
    }
}
