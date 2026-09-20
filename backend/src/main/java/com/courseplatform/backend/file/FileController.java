package com.courseplatform.backend.file;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.api.ApiResponse;
import org.slf4j.MDC;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpRange;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {
    private final FileService service;

    public FileController(FileService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FileInfoResponse> upload(@AuthenticationPrincipal AuthenticatedUser user,
                                                 @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(service.upload(user, file), MDC.get("traceId"));
    }

    @GetMapping("/{id}")
    public ApiResponse<FileInfoResponse> info(@PathVariable long id, @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(service.info(id, user), MDC.get("traceId"));
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<ResourceRegion> preview(@PathVariable long id, @AuthenticationPrincipal AuthenticatedUser user,
                                                   @RequestHeader HttpHeaders requestHeaders) throws IOException {
        FileService.FileContent content = service.content(id, user);
        Resource resource = content.resource();
        long length = resource.contentLength();
        List<HttpRange> ranges = requestHeaders.getRange();
        ResourceRegion region = ranges.isEmpty() ? new ResourceRegion(resource, 0, length)
                : ranges.getFirst().toResourceRegion(resource);
        HttpHeaders headers = streamHeaders(content.metadata(), true);
        return ResponseEntity.status(ranges.isEmpty() ? HttpStatus.OK : HttpStatus.PARTIAL_CONTENT)
                .headers(headers).body(region);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable long id, @AuthenticationPrincipal AuthenticatedUser user) {
        FileService.FileContent content = service.downloadContent(id, user);
        return ResponseEntity.ok().headers(streamHeaders(content.metadata(), false)).body(content.resource());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id, @AuthenticationPrincipal AuthenticatedUser user) {
        service.delete(id, user);
    }

    private HttpHeaders streamHeaders(StoredFile file, boolean inline) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(file.contentType()));
        headers.setContentDisposition((inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(file.originalName(), StandardCharsets.UTF_8).build());
        headers.setCacheControl(CacheControl.noStore());
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.set("Referrer-Policy", "no-referrer");
        return headers;
    }
}
