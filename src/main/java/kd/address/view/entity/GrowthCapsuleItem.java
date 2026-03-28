package kd.address.view.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GrowthCapsuleItem {

    private Long id;
    private Long capsuleId;
    private String sourceKind;
    private Long sourceId;
    private String title;
    private String subtitle;
    private String note;
    private String actionPlan;
    private String valueSummary;
    private String firstStep;
    private String sevenDayPlan;
    private String status;
    private Integer expectedMinutes;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
