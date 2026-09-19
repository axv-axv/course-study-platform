package com.courseplatform.backend.user;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
}
