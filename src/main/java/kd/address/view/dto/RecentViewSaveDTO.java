package kd.address.view.dto;

import lombok.Data;

@Data
public class RecentViewSaveDTO {

    private String clientId;
    private String kind;
    private Long targetId;
    private String title;
    private String subtitle;
}
