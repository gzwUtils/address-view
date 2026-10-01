package kd.address.view.service;

import kd.address.view.entity.Project;
import kd.address.view.mapper.ProjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectServiceTest {

    private ProjectMapper mapper;
    private ProjectService service;

    @BeforeEach
    void setUp() {
        mapper = mock(ProjectMapper.class);
        service = new ProjectService(mapper);
    }

    @Test
    void filtersProjectsByKeywordAcrossNameAndDescription() {
        Project first = project("订单中心", "内部交易系统");
        Project second = project("知识平台", "订单迁移复盘入口");
        Project third = project("监控平台", "查看服务指标");
        when(mapper.findAll()).thenReturn(List.of(first, second, third));

        assertEquals(List.of("订单中心", "知识平台"), service.getProjects(null, "  订单 ").stream()
                .map(item -> item.getProjectName()).toList());
        assertEquals(3, service.getProjects(null, " ").size());
    }

    @Test
    void combinesCategoryAndCaseInsensitiveKeyword() {
        Project project = project("AI Workspace", "团队能力入口");
        when(mapper.findByCategory("研发")).thenReturn(List.of(project));

        assertEquals(1, service.getProjects("研发", "workspace").size());
        assertEquals(0, service.getProjects("研发", "missing").size());
    }

    private Project project(String name, String description) {
        Project project = new Project();
        project.setProjectName(name);
        project.setDescription(description);
        return project;
    }
}
