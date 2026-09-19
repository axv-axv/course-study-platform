package com.courseplatform.backend.learning;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.api.ApiResponse;
import com.courseplatform.backend.common.api.PageResult;
import com.courseplatform.backend.resource.ResourceType;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class LearningController {
    private final LearningService service;

    public LearningController(LearningService service) {
        this.service = service;
    }

    @PostMapping("/resources/{resourceId}/favorite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void favorite(@PathVariable long resourceId, @AuthenticationPrincipal AuthenticatedUser user) {
        service.favorite(resourceId, user);
    }

    @DeleteMapping("/resources/{resourceId}/favorite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfavorite(@PathVariable long resourceId, @AuthenticationPrincipal AuthenticatedUser user) {
        service.unfavorite(resourceId, user);
    }

    @GetMapping("/resources/{resourceId}/favorite")
    public ApiResponse<Map<String, Boolean>> favoriteStatus(@PathVariable long resourceId,
                                                             @AuthenticationPrincipal AuthenticatedUser user) {
        return ok(Map.of("favorite", service.favoriteStatus(resourceId, user)));
    }

    @GetMapping("/users/me/favorites")
    public ApiResponse<PageResult<FavoriteResponse>> favorites(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long courseId, @RequestParam(required = false) ResourceType type,
            @RequestParam(required = false) String keyword
    ) {
        return ok(service.favorites(user, page, size, courseId, type, keyword));
    }

    @PutMapping("/resources/{resourceId}/progress")
    public ApiResponse<ProgressResponse> updateProgress(@PathVariable long resourceId,
                                                         @AuthenticationPrincipal AuthenticatedUser user,
                                                         @Valid @RequestBody UpdateProgressRequest request) {
        return ok(service.updateProgress(resourceId, user, request));
    }

    @GetMapping("/resources/{resourceId}/progress")
    public ApiResponse<ProgressResponse> progress(@PathVariable long resourceId,
                                                   @AuthenticationPrincipal AuthenticatedUser user) {
        return ok(service.progress(resourceId, user));
    }

    @GetMapping("/courses/{courseId}/progress")
    public ApiResponse<CourseProgressResponse> courseProgress(@PathVariable long courseId,
                                                               @AuthenticationPrincipal AuthenticatedUser user) {
        return ok(service.courseProgress(courseId, user));
    }

    @PostMapping("/resources/{resourceId}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<NoteResponse> createNote(@PathVariable long resourceId,
                                                 @AuthenticationPrincipal AuthenticatedUser user,
                                                 @Valid @RequestBody CreateNoteRequest request) {
        return ok(service.createNote(resourceId, user, request));
    }

    @GetMapping("/resources/{resourceId}/notes")
    public ApiResponse<List<NoteResponse>> resourceNotes(@PathVariable long resourceId,
                                                          @AuthenticationPrincipal AuthenticatedUser user) {
        return ok(service.resourceNotes(resourceId, user));
    }

    @GetMapping("/users/me/notes")
    public ApiResponse<PageResult<NoteResponse>> notes(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long courseId, @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) String keyword
    ) {
        return ok(service.notes(user, page, size, courseId, resourceId, keyword));
    }

    @PatchMapping("/notes/{id}")
    public ApiResponse<NoteResponse> updateNote(@PathVariable long id, @AuthenticationPrincipal AuthenticatedUser user,
                                                 @Valid @RequestBody UpdateNoteRequest request) {
        return ok(service.updateNote(id, user, request));
    }

    @DeleteMapping("/notes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNote(@PathVariable long id, @AuthenticationPrincipal AuthenticatedUser user) {
        service.deleteNote(id, user);
    }

    @GetMapping("/users/me/recent-learning")
    public ApiResponse<List<RecentLearningResponse>> recent(@AuthenticationPrincipal AuthenticatedUser user) {
        return ok(service.recent(user));
    }

    @GetMapping("/users/me/learning-dashboard")
    public ApiResponse<LearningDashboardResponse> dashboard(@AuthenticationPrincipal AuthenticatedUser user) {
        return ok(service.dashboard(user));
    }

    private <T> ApiResponse<T> ok(T data) {
        return ApiResponse.success(data, MDC.get("traceId"));
    }
}
