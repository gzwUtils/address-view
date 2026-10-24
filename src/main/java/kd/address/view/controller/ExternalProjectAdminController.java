package kd.address.view.controller;

import jakarta.servlet.http.HttpServletRequest;
import kd.address.view.common.ApiResponse;
import kd.address.view.service.AdminSessionService;
import kd.address.view.service.ExternalProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/external-projects")
public class ExternalProjectAdminController {
    private final AdminSessionService admin;
    private final ExternalProjectService projects;

    public record HighlightRequest(String featuredIntro, String desktopDownloadUrl, String desktopVersion) {}

    /** 平台自写突出介绍与桌面端下载信息；同步源不会覆盖这些字段。 */
    @PostMapping("/{id}/highlight")
    public ApiResponse<Boolean> updateHighlight(@PathVariable Long id,
                                                @RequestBody HighlightRequest body,
                                                HttpServletRequest request) {
        admin.requireAdmin(request);
        projects.updateHighlight(id, body.featuredIntro(), body.desktopDownloadUrl(), body.desktopVersion());
        return ApiResponse.success(true);
    }
}
