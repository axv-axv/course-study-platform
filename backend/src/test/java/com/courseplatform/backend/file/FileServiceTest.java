package com.courseplatform.backend.file;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.exception.BusinessException;
import com.courseplatform.backend.course.CourseService;
import com.courseplatform.backend.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.core.io.ByteArrayResource;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileServiceTest {
    private final FileRepository files = mock(FileRepository.class);
    private final CourseService courses = mock(CourseService.class);
    private final AuthenticatedUser uploader = new AuthenticatedUser(7, "teacher", UserRole.TEACHER);

    @Test
    void rejectsOversizedUpload(@TempDir Path root) throws Exception {
        FileService service = new FileService(files, new LocalFileStorage(root.toString()), courses, 3);
        MockMultipartFile multipart = new MockMultipartFile("file", "demo.txt", "text/plain", "four".getBytes());

        assertThatThrownBy(() -> service.upload(uploader, multipart))
                .isInstanceOf(BusinessException.class).hasMessage("文件超过大小限制");
    }

    @Test
    void referencedFileCannotBeDeleted(@TempDir Path root) throws Exception {
        FileService service = new FileService(files, new LocalFileStorage(root.toString()), courses, 100);
        StoredFile file = new StoredFile(5, "aa/object", "demo.txt", "text/plain", 4, "hash", 7, Instant.now());
        when(files.findById(5)).thenReturn(java.util.Optional.of(file));
        when(files.isReferenced(5)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(5, uploader))
                .isInstanceOf(BusinessException.class).hasMessage("文件仍被学习资料引用");
    }

    @Test
    void rejectsNonImageAvatar(@TempDir Path root) throws Exception {
        FileService service = new FileService(files, new LocalFileStorage(root.toString()), courses, 100);
        MockMultipartFile text = new MockMultipartFile("file", "avatar.txt", "text/plain", new byte[]{1});

        assertThatThrownBy(() -> service.uploadAvatar(uploader, text))
                .isInstanceOf(BusinessException.class).hasMessage("头像必须是图片文件");
    }

    @Test
    void downloadIncrementsResourcesUsingTheFile() {
        FileStorage storage = mock(FileStorage.class);
        StoredFile stored = new StoredFile(1, "key", "lesson.pdf", "application/pdf", 3, "hash", 7,
                Instant.parse("2026-09-20T00:00:00Z"));
        when(files.findById(1)).thenReturn(Optional.of(stored));
        when(storage.load("key")).thenReturn(new ByteArrayResource(new byte[]{1, 2, 3}));
        FileService service = new FileService(files, storage, courses, 100);

        service.downloadContent(1, uploader);

        verify(files).incrementResourceDownloads(1);
    }
}
