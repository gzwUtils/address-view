package kd.address.view.dto;

import lombok.Data;

@Data
public class GrowthCapsuleItemSaveDTO {

    private String userId;
    private String sourceKind;
    private Long sourceId;
    private String title;
    private String subtitle;
    private String note;
}
