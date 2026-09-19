package com.courseplatform.backend.search;

import com.courseplatform.backend.common.api.PageResult;
import com.courseplatform.backend.course.CourseResponse;
import com.courseplatform.backend.resource.ResourceResponse;
import com.courseplatform.backend.resource.TagResponse;

public record UnifiedSearchResponse(
        PageResult<CourseResponse> courses,
        PageResult<ResourceResponse> resources,
        PageResult<TagResponse> tags
) {
}
