package com.courseplatform.backend.rag;

import com.courseplatform.backend.auth.AuthenticatedUser;
import com.courseplatform.backend.common.exception.BusinessException;
import com.courseplatform.backend.course.CourseService;
import com.courseplatform.backend.resource.AiIndexStatus;
import com.courseplatform.backend.resource.ResourceRepository;
import com.courseplatform.backend.resource.ResourceResponse;
import com.courseplatform.backend.resource.ResourceType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;

@Service
public class RagIndexService {
    private static final Set<ResourceType> SUPPORTED_TYPES = EnumSet.of(
            ResourceType.PDF, ResourceType.DOC, ResourceType.PPT, ResourceType.MARKDOWN, ResourceType.TXT
    );

    private final ResourceRepository resources;
    private final CourseService courses;
    private final RagIndexRepository indexes;

    public RagIndexService(ResourceRepository resources, CourseService courses, RagIndexRepository indexes) {
        this.resources = resources;
        this.courses = courses;
        this.indexes = indexes;
    }

    public AiIndexStatusResponse status(long resourceId, AuthenticatedUser user) {
        ResourceResponse resource = requireResource(resourceId);
        courses.requireViewable(resource.courseId(), user);
        return response(resource);
    }

    @Transactional
    public AiIndexStatusResponse reindex(long resourceId, AuthenticatedUser user) {
        ResourceResponse resource = requireResource(resourceId);
        courses.requireManageable(resource.courseId(), user);
        if (!SUPPORTED_TYPES.contains(resource.resourceType())) {
            throw new BusinessException(40070, "该资料类型暂不支持建立 AI 索引", HttpStatus.BAD_REQUEST);
        }
        if (resource.fileId() == null) {
            throw new BusinessException(40071, "建立 AI 索引需要关联本地文件", HttpStatus.BAD_REQUEST);
        }
        if (resource.aiIndexStatus() != AiIndexStatus.PROCESSING) {
            indexes.enqueue(resourceId);
        }
        ResourceResponse updated = resources.findById(resourceId).orElseThrow();
        return response(updated);
    }

    @Transactional
    public void delete(long resourceId, AuthenticatedUser user) {
        ResourceResponse resource = requireResource(resourceId);
        courses.requireManageable(resource.courseId(), user);
        indexes.deleteIndex(resourceId);
    }

    private ResourceResponse requireResource(long resourceId) {
        return resources.findById(resourceId)
                .orElseThrow(() -> new BusinessException(40440, "学习资料不存在", HttpStatus.NOT_FOUND));
    }

    private AiIndexStatusResponse response(ResourceResponse resource) {
        return new AiIndexStatusResponse(resource.id(), resource.aiIndexStatus(),
                resource.indexedAt(), resource.aiIndexError());
    }
}
