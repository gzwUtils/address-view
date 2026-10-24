package kd.address.view.entity;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Project {

    private Long id;
    private String projectName;
    private String shortName;
    private String platformUrl;
    private String backgroundImage;
    private String category;
    private String type;
    private String description;
    private String featuredIntro;
    private String desktopDownloadUrl;
    private String desktopDownloadUrlX64;
    private String desktopVersion;
    private String ownerId;
    private String ownerName;
    private Long ownerAccountId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
