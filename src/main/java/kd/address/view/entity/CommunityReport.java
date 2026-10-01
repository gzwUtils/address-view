package kd.address.view.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommunityReport {
    private Long id;
    private Long reporterAccountId;
    private String targetType;
    private Long targetId;
    private Long topicId;
    private String reason;
    private String status;
    private LocalDateTime createTime;
}
