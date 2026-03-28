package kd.address.view.dto;

import lombok.Data;

import java.util.List;

@Data
public class GrowthCapsuleItemDTO {

    private Long id;
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
    private String createdAt;
    private String updatedAt;
    private Integer checkinCount;
    private List<GrowthCapsuleCheckinDTO> recentCheckins;
}
