package kd.address.view.service;

import kd.address.view.entity.ExternalProject;

public interface ProjectReadingContextReader {
    boolean supports(ExternalProject project);

    ProjectReadingContext read(ExternalProject project);
}
