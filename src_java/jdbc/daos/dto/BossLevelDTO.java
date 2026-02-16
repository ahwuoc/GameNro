package jdbc.daos.dto;

import boss.AppearType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for boss level data from database JSON.
 * Maps directly to the JSON structure in the 'levels' field.
 * 
 * Each boss can have multiple levels/forms (e.g., Black Goku -> Super Black Goku).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BossLevelDTO {
    
    /**
     * Level index (0 = base form, 1+ = transformed forms).
     */
    private int level;
    
    /**
     * Display name for this level/form.
     */
    private String name;
    
    /**
     * Base damage value.
     */
    private long dame;
    
  
    private long[] hp;
    

    private short[] outfit;
    

    private int[] mapJoin;
    

    private int[][] skills;
    

    private String[] textS;
    

    private String[] textM;
    
 
    private String[] textE;
    

    private AppearType appearType;
    

    private int secondsRest;
    
 
    private int[] bossesAppearTogether;
    
   
    private List<BossRewardDTO> rewards;
    
  
    private Long damageCap;
    
    
    private Integer eventPoints;
    
  
    private String dodgeText;
    

    private Integer damageDivisor;
    

    private Integer autoLeaveTime;
}
