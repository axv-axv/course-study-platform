package com.courseplatform.backend.file;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.exception.BusinessException;
import com.courseplatform.backend.course.CourseService;
import com.courseplatform.backend.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
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
}
