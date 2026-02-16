package jdbc.daos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class BossDataDTO {
    private int bossId;
    private String bossName;
    private String displayName;
    private byte gender;
    
   
    private boolean notifyDisabled;
    private boolean zone01SpawnDisabled;
    
    private String specialClass;
    private boolean autoSpawn = true;
    private List<BossLevelDTO> levels;
}
