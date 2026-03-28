package kd.address.view.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLog {

    private Long id;
    private String module;
    private String action;
    private String targetType;
    private String targetName;
    private String operatorName;
    private String detail;
    private LocalDateTime createTime;
}
