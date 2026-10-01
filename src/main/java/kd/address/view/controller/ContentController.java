package kd.address.view.controller;

import kd.address.view.common.ApiResponse;
import kd.address.view.common.PageResponse;
import kd.address.view.dto.PortalResourceDTO;
import kd.address.view.dto.PortalResourceSaveDTO;
import kd.address.view.dto.TagCountDTO;
import kd.address.view.service.PortalCatalogService;
import kd.address.view.service.AdminSessionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/content")
public class ContentController {

    private final PortalCatalogService portalCatalogService;
    private final AdminSessionService admin;

    @GetMapping("/resources")
    public ApiResponse<PageResponse<PortalResourceDTO>> getResources(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return ApiResponse.success(portalCatalogService.getResources(keyword, type, tag, page, size));
    }

    @GetMapping("/tags")
    public ApiResponse<List<TagCountDTO>> getTags() {
        return ApiResponse.success(portalCatalogService.getTags());
    }

    @PostMapping("/resources")
    public ApiResponse<PortalResourceDTO> saveResource(@RequestBody PortalResourceSaveDTO request, HttpServletRequest servletRequest) {
        admin.requireAdmin(servletRequest);
        return ApiResponse.success(portalCatalogService.saveResource(request));
    }

    @DeleteMapping("/resources/{id}")
    public ApiResponse<Boolean> deleteResource(@PathVariable Long id, HttpServletRequest request) {
        admin.requireAdmin(request);
        portalCatalogService.deleteResource(id);
        return ApiResponse.success(Boolean.TRUE);
    }
}
