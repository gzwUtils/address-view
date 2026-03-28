package kd.address.view.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecentViewHistory {

    private Long id;
    private String clientId;
    private String kind;
    private Long targetId;
    private String title;
    private String subtitle;
    private LocalDateTime viewTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
