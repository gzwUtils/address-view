package kd.address.view.dto;

import lombok.Data;

@Data
public class RecentViewDTO {

    private Long id;
    private String clientId;
    private String kind;
    private Long targetId;
    private String title;
    private String subtitle;
    private String viewTime;
}
