package com.courseplatform.backend.user;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.file.FileInfoResponse;
import com.courseplatform.backend.file.FileService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final FileService files = mock(FileService.class);
    private final UserService service = new UserService(users, files);
    private final AuthenticatedUser principal = new AuthenticatedUser(8, "student", UserRole.STUDENT);

    @Test
    void uploadsAvatarAndPersistsItsStablePreviewUrl() {
        User current = user(null, 2);
        User updated = user("/api/v1/public/avatars/10", 3);
        MockMultipartFile image = new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1});
        when(users.findById(8)).thenReturn(Optional.of(current));
        when(files.uploadAvatar(principal, image)).thenReturn(new FileInfoResponse(
                10, "avatar.png", "avatar.png", "image/png", 1,
                "/api/v1/files/10/preview", "hash", Instant.now()));
        when(users.updateAvatar(8, 2, "/api/v1/public/avatars/10")).thenReturn(updated);

        UserService.AvatarResponse response = service.updateAvatar(principal, image);

        verify(users).updateAvatar(8, 2, "/api/v1/public/avatars/10");
        assertThat(response.avatarUrl()).isEqualTo("/api/v1/public/avatars/10");
    }

    private User user(String avatarUrl, long version) {
        Instant now = Instant.parse("2026-09-20T00:00:00Z");
        return new User(8, "student", "hash", null, "学生", null, avatarUrl,
                UserRole.STUDENT, UserStatus.ACTIVE, version, now, now);
    }
}
