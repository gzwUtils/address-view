package kd.address.view.dto;

import lombok.Data;

import java.util.List;

@Data
public class PortalAiCapabilityDTO {
    private String type;
    private String title;
    private String desc;
    private List<String> tags;
}
