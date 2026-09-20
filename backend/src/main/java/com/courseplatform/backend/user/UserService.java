package com.courseplatform.backend.user;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.exception.BusinessException;
import com.courseplatform.backend.file.FileInfoResponse;
import com.courseplatform.backend.file.FileService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final FileService files;

    public UserService(UserRepository userRepository, FileService files) {
        this.userRepository = userRepository;
        this.files = files;
    }

    public AvatarResponse updateAvatar(AuthenticatedUser principal, MultipartFile multipart) {
        User current = requireActive(principal.id());
        FileInfoResponse uploaded = files.uploadAvatar(principal, multipart);
        String avatarUrl = "/api/v1/public/avatars/" + uploaded.fileId();
        try {
            User updated = userRepository.updateAvatar(current.id(), current.version(), avatarUrl);
            deletePreviousAvatar(current.avatarUrl(), principal);
            return new AvatarResponse(updated.avatarUrl());
        } catch (RuntimeException exception) {
            files.delete(uploaded.fileId(), principal);
            if (exception instanceof IllegalStateException) {
                throw new BusinessException(40904, exception.getMessage(), HttpStatus.CONFLICT);
            }
            throw exception;
        }
    }

    public UserResponse getCurrentUser(AuthenticatedUser principal) {
        return UserResponse.from(requireActive(principal.id()));
    }

    @Transactional
    public UserResponse updateCurrentUser(AuthenticatedUser principal, UpdateProfileRequest request) {
        User user = requireActive(principal.id());
        String email = normalizeNullable(request.email());
        if (userRepository.existsByEmail(email, user.id())) {
            throw new BusinessException(40902, "邮箱已被使用", HttpStatus.CONFLICT);
        }
        try {
            return UserResponse.from(userRepository.updateProfile(
                    user.id(), user.version(), normalizeNullable(request.nickname()), email,
                    normalizeNullable(request.bio())
            ));
        } catch (IllegalStateException exception) {
            throw new BusinessException(40903, exception.getMessage(), HttpStatus.CONFLICT);
        }
    }

    public User requireActive(long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(40401, "用户不存在", HttpStatus.NOT_FOUND));
        if (user.status() != UserStatus.ACTIVE) {
            throw new BusinessException(40301, "用户已被禁用", HttpStatus.FORBIDDEN);
        }
        return user;
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void deletePreviousAvatar(String avatarUrl, AuthenticatedUser principal) {
        Long fileId = avatarFileId(avatarUrl);
        if (fileId == null) return;
        try {
            files.delete(fileId, principal);
        } catch (BusinessException ignored) {
            // The new avatar is already active; a stale old file must not fail the request.
        }
    }

    private Long avatarFileId(String avatarUrl) {
        if (avatarUrl == null) return null;
        String prefix = "/api/v1/public/avatars/";
        if (!avatarUrl.startsWith(prefix)) return null;
        try {
            return Long.parseLong(avatarUrl.substring(prefix.length()));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public record AvatarResponse(String avatarUrl) {
    }
}
