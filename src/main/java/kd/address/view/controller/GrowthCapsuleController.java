package kd.address.view.controller;

import kd.address.view.common.ApiResponse;
import kd.address.view.dto.GrowthCapsuleCheckinDTO;
import kd.address.view.dto.GrowthCapsuleCheckinSaveDTO;
import kd.address.view.dto.GrowthCapsuleItemDTO;
import kd.address.view.dto.GrowthCapsuleItemSaveDTO;
import kd.address.view.dto.GrowthCapsuleOverviewDTO;
import kd.address.view.service.GrowthCapsuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/growth-capsule")
public class GrowthCapsuleController {

    private final GrowthCapsuleService growthCapsuleService;

    @GetMapping
    public ApiResponse<GrowthCapsuleOverviewDTO> getOverview(@RequestParam String userId) {
        return ApiResponse.success(growthCapsuleService.getOverview(userId));
    }

    @PostMapping("/items")
    public ApiResponse<GrowthCapsuleItemDTO> addItem(@RequestBody GrowthCapsuleItemSaveDTO request) {
        return ApiResponse.success(growthCapsuleService.addItem(request));
    }

    @PostMapping("/items/{itemId}/status")
    public ApiResponse<GrowthCapsuleItemDTO> updateStatus(@PathVariable Long itemId,
                                                          @RequestParam String status,
                                                          @RequestParam(required = false) String note) {
        return ApiResponse.success(growthCapsuleService.updateStatus(itemId, status, note));
    }

    @PostMapping("/items/{itemId}/checkins")
    public ApiResponse<GrowthCapsuleCheckinDTO> addCheckin(@PathVariable Long itemId,
                                                           @RequestBody GrowthCapsuleCheckinSaveDTO request) {
        return ApiResponse.success(growthCapsuleService.addCheckin(itemId, request));
    }
}
