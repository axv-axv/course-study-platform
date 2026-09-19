package com.courseplatform.backend.file;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.exception.BusinessException;
import com.courseplatform.backend.course.CourseService;
import com.courseplatform.backend.user.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class FileService {
    private final FileRepository files;
    private final LocalFileStorage storage;
    private final CourseService courses;
    private final long maxFileSize;

    public FileService(FileRepository files, LocalFileStorage storage, CourseService courses,
                       @Value("${app.storage.max-file-size-bytes}") long maxFileSize) {
        this.files = files;
        this.storage = storage;
        this.courses = courses;
        this.maxFileSize = maxFileSize;
    }

    public FileInfoResponse upload(AuthenticatedUser user, MultipartFile multipart) {
        if (multipart.isEmpty()) {
            throw new BusinessException(40030, "上传文件不能为空", HttpStatus.BAD_REQUEST);
        }
        if (multipart.getSize() > maxFileSize) {
            throw new BusinessException(41301, "文件超过大小限制", HttpStatus.PAYLOAD_TOO_LARGE);
        }
        String name = safeName(multipart.getOriginalFilename());
        String contentType = multipart.getContentType() == null || multipart.getContentType().isBlank()
                ? "application/octet-stream" : multipart.getContentType();
        try {
            LocalFileStorage.StoredObject object = storage.store(multipart);
            try {
                return FileInfoResponse.from(files.create(object.objectKey(), name, contentType,
                        multipart.getSize(), object.sha256(), user.id()));
            } catch (RuntimeException exception) {
                storage.delete(object.objectKey());
                throw exception;
            }
        } catch (IOException exception) {
            throw new BusinessException(50030, "文件存储失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public FileInfoResponse info(long id, AuthenticatedUser user) {
        return FileInfoResponse.from(requireAccessible(id, user));
    }

    public FileContent content(long id, AuthenticatedUser user) {
        StoredFile file = requireAccessible(id, user);
        Resource resource = storage.load(file.objectKey());
        if (!resource.exists()) {
            throw new BusinessException(40431, "文件内容不存在", HttpStatus.NOT_FOUND);
        }
        return new FileContent(file, resource);
    }

    public StoredFile requireOwned(long id, AuthenticatedUser user) {
        StoredFile file = require(id);
        if (file.uploaderId() != user.id() && user.role() != UserRole.ADMIN) {
            throw new BusinessException(40330, "只能使用自己上传的文件", HttpStatus.FORBIDDEN);
        }
        return file;
    }

    @Transactional
    public void delete(long id, AuthenticatedUser user) {
        StoredFile file = requireOwned(id, user);
        if (files.isReferenced(id)) {
            throw new BusinessException(40930, "文件仍被学习资料引用", HttpStatus.CONFLICT);
        }
        try {
            storage.delete(file.objectKey());
        } catch (IOException exception) {
            throw new BusinessException(50031, "文件删除失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        files.delete(id);
    }

    private StoredFile requireAccessible(long id, AuthenticatedUser user) {
        StoredFile file = require(id);
        if (file.uploaderId() == user.id() || user.role() == UserRole.ADMIN) return file;
        for (long courseId : files.findReferencedCourseIds(id)) {
            try {
                courses.requireViewable(courseId, user);
                return file;
            } catch (BusinessException ignored) {
                // Continue until an accessible reference is found.
            }
        }
        throw new BusinessException(40331, "无权访问该文件", HttpStatus.FORBIDDEN);
    }

    private StoredFile require(long id) {
        return files.findById(id)
                .orElseThrow(() -> new BusinessException(40430, "文件不存在", HttpStatus.NOT_FOUND));
    }

    private String safeName(String original) {
        String value = original == null || original.isBlank() ? "unnamed" : original.replace('\\', '/');
        value = value.substring(value.lastIndexOf('/') + 1).replaceAll("[\\r\\n\\u0000]", "_");
        return value.length() > 255 ? value.substring(value.length() - 255) : value;
    }

    public record FileContent(StoredFile metadata, Resource resource) {
    }
}
