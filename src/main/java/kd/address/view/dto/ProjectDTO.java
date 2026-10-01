package kd.address.view.dto;


import lombok.Data;

@Data
public class ProjectDTO {
    private Long id;
    private String projectName;
    private String shortName;
    private String platformUrl;
    private String backgroundImage;
    private String category;
    private String type;
    private String description;
    private String ownerId;
    private String ownerName;
    private Long ownerAccountId;
    private Boolean canEdit;
}
