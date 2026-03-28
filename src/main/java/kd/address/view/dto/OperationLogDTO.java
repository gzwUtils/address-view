package kd.address.view.dto;

import lombok.Data;

@Data
public class OperationLogDTO {
    private Long id;
    private String module;
    private String action;
    private String targetType;
    private String targetName;
    private String operatorName;
    private String detail;
    private String createTime;
}
