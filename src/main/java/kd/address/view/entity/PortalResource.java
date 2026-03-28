package kd.address.view.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PortalResource {

    private Long id;
    private String resourceCode;
    private String kind;
    private String title;
    private String name;
    private String excerpt;
    private String description;
    private String category;
    private String type;
    private String owner;
    private String author;
    private String meta;
    private String resourceDate;
    private String status;
    private String version;
    private String updatedAt;
    private String entryUrl;
    private String coverImage;
    private String contentBody;
    private String tags;
    private String capabilities;
    private Integer sortOrder;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
