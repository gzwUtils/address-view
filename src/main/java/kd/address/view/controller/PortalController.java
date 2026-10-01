package kd.address.view.controller;

import kd.address.view.common.ApiResponse;
import kd.address.view.dto.OpsWorkbenchDTO;
import kd.address.view.dto.PortalOverviewDTO;
import kd.address.view.dto.RecentViewDTO;
import kd.address.view.dto.RecentViewSaveDTO;
import kd.address.view.service.PortalCatalogService;
import kd.address.view.service.AdminSessionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/portal")
public class PortalController {

    private final PortalCatalogService portalCatalogService;
    private final AdminSessionService admin;

    @GetMapping("/overview")
    public ApiResponse<PortalOverviewDTO> getOverview() {
        return ApiResponse.success(portalCatalogService.getOverview());
    }

    @GetMapping("/ops-workbench")
    public ApiResponse<OpsWorkbenchDTO> getOpsWorkbench(HttpServletRequest request) {
        admin.requireAdmin(request);
        return ApiResponse.success(portalCatalogService.getOpsWorkbench());
    }

    @GetMapping("/recent-views")
    public ApiResponse<List<RecentViewDTO>> getRecentViews(@RequestParam String clientId) {
        return ApiResponse.success(portalCatalogService.getRecentViews(clientId));
    }

    @PostMapping("/recent-views")
    public ApiResponse<RecentViewDTO> saveRecentView(@RequestBody RecentViewSaveDTO request) {
        return ApiResponse.success(portalCatalogService.saveRecentView(request));
    }
}
