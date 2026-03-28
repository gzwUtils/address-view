package kd.address.view.dto;

import lombok.Data;

import java.util.List;

@Data
public class GrowthCapsuleOverviewDTO {

    private Long capsuleId;
    private String userId;
    private String capsuleName;
    private String tagline;
    private String aiBrief;
    private Integer totalItems;
    private Integer todoItems;
    private Integer inProgressItems;
    private Integer doneItems;
    private Integer streakDays;
    private Integer todayCheckins;
    private String todaySuggestion;
    private List<GrowthCapsuleItemDTO> items;
}
