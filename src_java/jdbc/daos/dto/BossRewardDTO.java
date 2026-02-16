package jdbc.daos.dto;

/**
 * DTO for boss reward configuration.
 * Represents a single reward that can drop from a boss.
 * 
 * Requirements: 9.1
 */
public class BossRewardDTO {
    
    /**
     * Item ID for specific item drops.
     * Null if using type-based reward.
     */
    private Integer itemId;
    
    /**
     * Special reward type.
     * Examples: "DOT_LIEN", "GOLD", "EXP"
     * Null if using itemId.
     */
    private String type;
    
    /**
     * Quantity range [min, max].
     * Random value between min and max will be used.
     */
    private int[] quantity;
    
    /**
     * Drop chance percentage (1-100).
     */
    private int chance;
    
    /**
     * Optional condition for the reward.
     * Example: "TASK_31_0" means player must be on task 31.
     */
    private String condition;
    
    /**
     * Item options to add to dropped item.
     * Format: [[optionId, minValue, maxValue], [optionId, fixedValue], ...]
     * Example: [[30, 1, 30]] means option 30 with random value 1-30
     * Requirements: 1.5
     */
    private int[][] itemOptions;
    
    /**
     * Whether only the player who killed the boss can pick up this reward.
     * true = only killer can pick (default)
     * false = anyone can pick (public drop)
     */
    private boolean playerOnly = true;
    
    /**
     * Number of times to drop this reward [min, max].
     * [1,1] = drop 1 time, [1,10] = random 1-10 times
     * Each loop drops quantity items with chance check.
     * Default: [1,1] (drop once)
     */
    private int[] loop;
    
    // Getters and Setters
    
    public Integer getItemId() {
        return itemId;
    }
    
    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public int[] getQuantity() {
        return quantity;
    }
    
    public void setQuantity(int[] quantity) {
        this.quantity = quantity;
    }
    
    public int getChance() {
        return chance;
    }
    
    public void setChance(int chance) {
        this.chance = chance;
    }
    
    public String getCondition() {
        return condition;
    }
    
    public void setCondition(String condition) {
        this.condition = condition;
    }
    
    public int[][] getItemOptions() {
        return itemOptions;
    }
    
    public void setItemOptions(int[][] itemOptions) {
        this.itemOptions = itemOptions;
    }
    
    public boolean isPlayerOnly() {
        return playerOnly;
    }
    
    public void setPlayerOnly(boolean playerOnly) {
        this.playerOnly = playerOnly;
    }
    
    public int[] getLoop() {
        return loop;
    }
    
    public void setLoop(int[] loop) {
        this.loop = loop;
    }
    
    /**
     * Get random loop count within range.
     * Default: 1 if not set
     */
    public int getRandomLoopCount() {
        if (loop == null || loop.length == 0) {
            return 1;
        }
        if (loop.length == 1) {
            return loop[0];
        }
        int min = loop[0];
        int max = loop[1];
        return min + (int)(Math.random() * (max - min + 1));
    }
    
    /**
     * Get option value from option config.
     * If option has 2 elements [optionId, value], returns fixed value.
     * If option has 3 elements [optionId, min, max], returns random value in range.
     * Requirements: 1.5
     */
    public static int getOptionValue(int[] option) {
        if (option == null || option.length < 2) {
            return 0;
        }
        if (option.length == 2) {
            return option[1];
        }
        // option.length >= 3: [optionId, min, max]
        int min = option[1];
        int max = option[2];
        return min + (int)(Math.random() * (max - min + 1));
    }
    
    /**
     * Check if this is an item-based reward.
     */
    public boolean isItemReward() {
        return itemId != null && itemId > 0;
    }
    
    /**
     * Check if this is a type-based reward.
     */
    public boolean isTypeReward() {
        return type != null && !type.isEmpty();
    }
    
    /**
     * Get random quantity within range.
     */
    public int getRandomQuantity() {
        if (quantity == null || quantity.length == 0) {
            return 1;
        }
        if (quantity.length == 1) {
            return quantity[0];
        }
        int min = quantity[0];
        int max = quantity.length > 1 ? quantity[1] : quantity[0];
        return min + (int)(Math.random() * (max - min + 1));
    }
    
    /**
     * Check if reward should drop based on chance.
     */
    public boolean shouldDrop() {
        if (chance >= 100) {
            return true;
        }
        return Math.random() * 100 < chance;
    }
}
