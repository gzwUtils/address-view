package kd.address.view.controller;

import jakarta.servlet.http.HttpServletRequest;
import kd.address.view.common.ApiResponse;
import kd.address.view.common.PageResponse;
import kd.address.view.dto.ReplyRequest;
import kd.address.view.dto.TopicRequest;
import kd.address.view.dto.ReportRequest;
import kd.address.view.entity.CommunityBoard;
import kd.address.view.entity.CommunityReply;
import kd.address.view.entity.CommunityTopic;
import kd.address.view.entity.CommunityReport;
import kd.address.view.entity.GuestAccount;
import kd.address.view.service.GuestIdentityService;
import kd.address.view.service.ReplyService;
import kd.address.view.service.TopicService;
import kd.address.view.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community")
public class CommunityController {
    private final TopicService topics;
    private final ReplyService replies;
    private final GuestIdentityService identities;
    private final ReportService reports;

    @GetMapping("/boards")
    public ApiResponse<List<CommunityBoard>> boards() { return ApiResponse.success(topics.boards()); }

    @GetMapping("/topics")
    public ApiResponse<PageResponse<CommunityTopic>> list(
            @RequestParam(required = false) String board,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        GuestAccount account = identities.optionalCurrent(request);
        return ApiResponse.success(topics.list(board, projectId, keyword, sort, page, size,
                account == null ? null : account.getId()));
    }

    @GetMapping("/topics/{id}")
    public ApiResponse<CommunityTopic> get(@PathVariable Long id, HttpServletRequest request) {
        GuestAccount account = identities.optionalCurrent(request);
        return ApiResponse.success(topics.get(id, account == null ? null : account.getId()));
    }

    @PostMapping("/topics")
    public ApiResponse<CommunityTopic> create(@RequestBody TopicRequest body, HttpServletRequest request) {
        GuestAccount account = identities.requireCurrent(request);
        return ApiResponse.success(topics.create(body, account.getId(), request.getRemoteAddr()));
    }

    @PatchMapping("/topics/{id}")
    public ApiResponse<CommunityTopic> update(@PathVariable Long id, @RequestBody TopicRequest body,
                                                HttpServletRequest request) {
        GuestAccount account = identities.requireCurrent(request);
        return ApiResponse.success(topics.update(id, body, account.getId()));
    }

    @DeleteMapping("/topics/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id, HttpServletRequest request) {
        GuestAccount account = identities.requireCurrent(request);
        topics.delete(id, account.getId());
        return ApiResponse.success(true);
    }

    @GetMapping("/topics/{id}/replies")
    public ApiResponse<PageResponse<CommunityReply>> replies(@PathVariable Long id,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        GuestAccount account = identities.optionalCurrent(request);
        return ApiResponse.success(replies.list(id, page, size, account == null ? null : account.getId()));
    }

    @GetMapping("/topics/{id}/replies/new-count")
    public ApiResponse<Long> newReplyCount(@PathVariable Long id,
            @RequestParam(defaultValue = "0") Long afterId) {
        return ApiResponse.success(replies.countAfter(id, afterId));
    }

    @PostMapping("/topics/{id}/replies")
    public ApiResponse<CommunityReply> reply(@PathVariable Long id, @RequestBody ReplyRequest body,
                                               HttpServletRequest request) {
        GuestAccount account = identities.requireCurrent(request);
        return ApiResponse.success(replies.create(id, body, account.getId(), request.getRemoteAddr()));
    }

    @PatchMapping("/replies/{id}")
    public ApiResponse<CommunityReply> updateReply(@PathVariable Long id, @RequestBody ReplyRequest body,
                                                     HttpServletRequest request) {
        GuestAccount account = identities.requireCurrent(request);
        return ApiResponse.success(replies.update(id, body.body(), account.getId()));
    }

    @DeleteMapping("/replies/{id}")
    public ApiResponse<Boolean> deleteReply(@PathVariable Long id, HttpServletRequest request) {
        GuestAccount account = identities.requireCurrent(request);
        replies.delete(id, account.getId());
        return ApiResponse.success(true);
    }

    @PostMapping("/reports")
    public ApiResponse<CommunityReport> report(@RequestBody ReportRequest body, HttpServletRequest request) {
        GuestAccount account = identities.requireCurrent(request);
        return ApiResponse.success(reports.submit(account.getId(), body, request.getRemoteAddr()));
    }
}
