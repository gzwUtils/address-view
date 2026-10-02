package kd.address.view.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExternalProject {
    private Long id;
    private String sourcePlatform;
    private Long sourceRepoId;
    private String fullName;
    private String sourceUrl;
    private String description;
    private String language;
    private String licenseSpdx;
    private Integer starCount;
    private Integer forkCount;
    private LocalDateTime repoCreatedAt;
    private LocalDateTime syncedAt;
    private Integer displayRank;
    private Boolean featured;
}
