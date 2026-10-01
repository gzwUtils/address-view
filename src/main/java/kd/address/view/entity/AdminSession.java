package kd.address.view.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AdminSession {
    private Long id;
    private String tokenHash;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private LocalDateTime createTime;
}
