package kd.address.view.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GuestSession {
    private Long id;
    private Long accountId;
    private String tokenHash;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private LocalDateTime createTime;
}
