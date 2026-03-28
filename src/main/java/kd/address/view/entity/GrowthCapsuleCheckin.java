package kd.address.view.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GrowthCapsuleCheckin {

    private Long id;
    private Long itemId;
    private String content;
    private String mood;
    private LocalDateTime createTime;
}
