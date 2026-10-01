package kd.address.view.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommunityBoard {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Integer sortOrder;
    private String status;
    private Long topicCount;
    private LocalDateTime lastActivity;
}
