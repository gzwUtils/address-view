package kd.address.view.dto;

import lombok.Data;

import java.util.List;

@Data
public class OpsWorkbenchDTO {
    private Integer projectCount;
    private Integer categoryCount;
    private Integer aiCount;
    private Integer onlineAiCount;
    private Integer articleCount;
    private Integer lifeCount;
    private Integer contentCount;
    private List<OperationLogDTO> recentLogs;
}
