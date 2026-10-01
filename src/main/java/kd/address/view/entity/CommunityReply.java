package kd.address.view.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommunityReply {
    private Long id;
    private Long topicId;
    private Long authorAccountId;
    private String authorNickname;
    private Long replyToId;
    private Integer floorNo;
    private String body;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Boolean canEdit;
}
