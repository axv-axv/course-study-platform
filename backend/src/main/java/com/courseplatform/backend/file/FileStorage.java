package com.courseplatform.backend.file;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileStorage {
    StoredObject store(MultipartFile multipart) throws IOException;

    Resource load(String objectKey);

    void delete(String objectKey) throws IOException;

    record StoredObject(String objectKey, String sha256) {
    }
}
