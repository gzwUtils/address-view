package kd.address.view.dto;

import lombok.Data;

import java.util.List;

@Data
public class PortalResourceSaveDTO {

    private Long id;
    private String resourceCode;
    private String kind;
    private String title;
    private String name;
    private String excerpt;
    private String desc;
    private String category;
    private String type;
    private String owner;
    private String author;
    private String meta;
    private String date;
    private String status;
    private String version;
    private String updatedAt;
    private String entryUrl;
    private String coverImage;
    private String contentBody;
    private List<String> tags;
    private List<String> capabilities;
    private Integer sortOrder;
}
