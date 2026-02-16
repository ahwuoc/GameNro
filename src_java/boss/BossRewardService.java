package boss;

import jdbc.daos.dto.BossLevelDTO;
import jdbc.daos.dto.BossRewardDTO;
import map.ItemMap;
import player.Player;
import services.ItemService;
import services.Service;
import services.TaskService;
import utils.Logger;
import utils.Util;

import java.util.List;

/**
 * Service for processing boss rewards from database configuration.
 * 
 * Requirements: 9.2, 9.3, 9.4, 9.5
 */
public class BossRewardService {
    
    private static BossRewardService instance;
    
    public static BossRewardService gI() {
        if (instance == null) {
            instance = new BossRewardService();
        }
        return instance;
    }
    
    /**
     * Process rewards for a boss kill.
     * 
     * @param boss The boss that was killed
     * @param player The player who killed the boss
     * @param levelDTO The level DTO containing reward configuration
     */
    public void processRewards(Boss boss, Player player, BossLevelDTO levelDTO) {
        if (levelDTO == null || levelDTO.getRewards() == null) {
            return;
        }
        
        List<BossRewardDTO> rewards = levelDTO.getRewards();
        for (BossRewardDTO reward : rewards) {
            processReward(boss, player, reward);
        }
    }
    
    /**
     * Process a single reward with loop support.
     * 
     * Requirements: 9.2, 9.3, 9.4, 9.5
     */
    private void processReward(Boss boss, Player player, BossRewardDTO reward) {
        if (reward == null) {
            return;
        }
        
        // Check condition if present
        if (!checkCondition(player, reward.getCondition())) {
            return;
        }
        
        // Get loop count (how many times to attempt drop)
        int loopCount = reward.getRandomLoopCount();
        
        for (int loop = 0; loop < loopCount; loop++) {
            // Check chance for each loop
            if (!reward.shouldDrop()) {
                continue;
            }
            
            // Get quantity for this drop
            int quantity = reward.getRandomQuantity();
            
            // Process based on reward type
            if (reward.isItemReward()) {
                dropItemWithOptions(boss, player, reward, quantity);
            } else if (reward.isTypeReward()) {
                processTypeReward(boss, player, reward, quantity);
            }
        }
    }
    
    /**
     * Drop an item reward with optional item options.
     * 
     * Requirements: 1.5, 9.2
     */
    private void dropItemWithOptions(Boss boss, Player player, BossRewardDTO reward, int quantity) {
        try {
            if (boss.zone == null) {
                return;
            }
            
            int itemId = reward.getItemId();
            long pickupPlayerId = reward.isPlayerOnly() ? player.id : -1;
            for (int i = 0; i < quantity; i++) {
                int x = boss.location.x + Util.nextInt(-15, 15) + (i * 5);
                int y = boss.zone.map.yPhysicInTop(boss.location.x, boss.location.y - 24);
                
                ItemMap itemMap = new ItemMap(boss.zone, itemId, 1, x, y, pickupPlayerId);
                itemMap.source = "BossRewardService (BossID: " + boss.id + ")";
                
                // Add item options if configured
                if (reward.getItemOptions() != null) {
                    for (int[] option : reward.getItemOptions()) {
                        if (option != null && option.length >= 2) {
                            int optionId = option[0];
                            int value = BossRewardDTO.getOptionValue(option);
                            itemMap.options.add(new item.Item.ItemOption(optionId, value));
                        }
                    }
                }
                
                Service.gI().dropItemMap(boss.zone, itemMap);
            }
        } catch (Exception e) {
            Logger.error("BossRewardService: Error dropping item: " + e.getMessage() + "\n");
        }
    }
    
    /**
     * Process type-based reward.
     * 
     * Requirements: 9.3
     */
    private void processTypeReward(Boss boss, Player player, BossRewardDTO reward, int quantity) {
        String type = reward.getType();
        if (type == null || boss.zone == null) {
            return;
        }
        
        // Determine who can pick up for item drops
        long pickupPlayerId = reward.isPlayerOnly() ? player.id : -1;
        
        try {
            switch (type.toUpperCase()) {
                case "DOT_LIEN":
                case "DO_THAN_LINH":
                    for (int i = 0; i < quantity; i++) {
                        ItemMap it = ItemService.gI().randDoTL(
                            boss.zone, 1, 
                            boss.location.x, 
                            boss.zone.map.yPhysicInTop(boss.location.x, boss.location.y - 24), 
                            pickupPlayerId
                        );
                        if (it != null) {
                            it.source = "BossRewardService (BossID: " + boss.id + ")";
                            Service.gI().dropItemMap(boss.zone, it);
                        }
                    }
                    break;
                case "GOLD":
                    player.inventory.gold += quantity;
                    break;
                case "EXP":
                    player.nPoint.tiemNang += quantity;
                    break;
                    
                case "RUBY":
                    player.inventory.ruby += quantity;
                    break;
                    
                default:
                    Logger.warning("BossRewardService: Unknown reward type: " + type + "\n");
                    break;
            }
        } catch (Exception e) {
            Logger.error("BossRewardService: Error processing type reward " + type + ": " + e.getMessage() + "\n");
        }
    }
    
   
    private boolean checkCondition(Player player, String condition) {
        if (condition == null || condition.isEmpty()) {
            return true;
        }
        
        try {
            if (condition.startsWith("TASK_")) {
                String[] parts = condition.split("_");
                if (parts.length >= 2) {
                    int taskId = Integer.parseInt(parts[1]);
                    int taskIndex = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
                    return TaskService.gI().getIdTask(player) == taskId;
                }
            }
        } catch (Exception e) {
            Logger.error("BossRewardService: Error checking condition " + condition + ": " + e.getMessage() + "\n");
        }
        
        return true;
    }
}
