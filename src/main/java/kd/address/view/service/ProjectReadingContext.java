package kd.address.view.service;

import java.util.List;

public record ProjectReadingContext(String name, String description, String sourceUrl,
                                    String branch, String version, String language,
                                    String license, List<ProjectReadingSource> sources) {}
