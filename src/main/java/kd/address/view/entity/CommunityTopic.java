package kd.address.view.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommunityTopic {
    private Long id;
    private Long boardId;
    private String boardCode;
    private String boardName;
    private Long authorAccountId;
    private String authorNickname;
    private Long projectId;
    private String projectName;
    private String title;
    private String body;
    private Integer replyCount;
    private Integer nextFloor;
    private LocalDateTime lastReplyTime;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Boolean canEdit;
}
