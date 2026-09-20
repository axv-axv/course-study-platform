package com.courseplatform.backend.user;

import com.courseplatform.backend.file.FileService;
import com.courseplatform.backend.file.StoredFile;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/public/avatars")
public class AvatarController {
    private final FileService files;

    public AvatarController(FileService files) {
        this.files = files;
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> avatar(@PathVariable long fileId) {
        FileService.FileContent content = files.avatarContent(fileId);
        StoredFile metadata = content.metadata();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.contentType()))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(content.resource());
    }
}
