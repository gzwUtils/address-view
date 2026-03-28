package kd.address.view.controller;

import kd.address.view.common.ApiResponse;
import kd.address.view.common.PageResponse;
import kd.address.view.dto.PortalResourceDTO;
import kd.address.view.dto.PortalResourceSaveDTO;
import kd.address.view.service.PortalCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/ai-assets")
public class AiAssetController {

    private final PortalCatalogService portalCatalogService;

    @GetMapping
    public ApiResponse<PageResponse<PortalResourceDTO>> getAiAssets(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return ApiResponse.success(portalCatalogService.getAiAssets(type, owner, status, page, size));
    }

    @PostMapping
    public ApiResponse<PortalResourceDTO> saveAiAsset(@RequestBody PortalResourceSaveDTO request) {
        return ApiResponse.success(portalCatalogService.saveAiAsset(request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> deleteAiAsset(@PathVariable Long id) {
        portalCatalogService.deleteAiAsset(id);
        return ApiResponse.success(Boolean.TRUE);
    }
}
