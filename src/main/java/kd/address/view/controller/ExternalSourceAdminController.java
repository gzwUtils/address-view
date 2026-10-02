package kd.address.view.controller;

import jakarta.servlet.http.HttpServletRequest;
import kd.address.view.common.ApiResponse;
import kd.address.view.entity.ExternalSource;
import kd.address.view.service.AdminSessionService;
import kd.address.view.service.ExternalSourceService;
import kd.address.view.service.ExternalSourceSync;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/external-sources")
public class ExternalSourceAdminController {
    private final AdminSessionService admin;
    private final ExternalSourceService sources;
    private final ExternalSourceSync sync;

    @GetMapping
    public ApiResponse<List<ExternalSource>> list(HttpServletRequest request) {
        admin.requireAdmin(request);
        return ApiResponse.success(sources.list());
    }

    @PostMapping
    public ApiResponse<ExternalSource> save(@RequestBody ExternalSource source, HttpServletRequest request) {
        admin.requireAdmin(request);
        return ApiResponse.success(sources.save(source));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id, HttpServletRequest request) {
        admin.requireAdmin(request);
        sources.delete(id);
        return ApiResponse.success(true);
    }

    @PostMapping("/{id}/run")
    public ApiResponse<ExternalSource> runNow(@PathVariable Long id, HttpServletRequest request) {
        admin.requireAdmin(request);
        return ApiResponse.success(sync.runNow(id));
    }
}
