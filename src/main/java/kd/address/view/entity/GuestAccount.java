package kd.address.view.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GuestAccount {
    private Long id;
    private String publicId;
    private String nickname;
    private String recoveryHash;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
