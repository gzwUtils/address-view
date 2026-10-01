package kd.address.view.controller;

import jakarta.servlet.http.HttpServletRequest;
import kd.address.view.common.ApiResponse;
import kd.address.view.common.PageResponse;
import kd.address.view.entity.CommunityTopic;
import kd.address.view.entity.GuestAccount;
import kd.address.view.service.GuestIdentityService;
import kd.address.view.service.TopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MyDiscussionsController {
    private final TopicService topics;
    private final GuestIdentityService identities;

    @GetMapping("/api/me/discussions")
    public ApiResponse<PageResponse<CommunityTopic>> mine(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size, HttpServletRequest request) {
        GuestAccount account = identities.requireCurrent(request);
        return ApiResponse.success(topics.mine(account.getId(), page, size));
    }
}
