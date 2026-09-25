package com.courseplatform.backend.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageTest {
    @TempDir
    Path directory;

    @Test
    void storesLoadsAndDeletesAnObject() throws Exception {
        LocalFileStorage storage = new LocalFileStorage(directory.toString());
        MockMultipartFile upload = new MockMultipartFile(
                "file", "lesson.txt", "text/plain", "course material".getBytes()
        );

        FileStorage.StoredObject object = storage.store(upload);

        assertThat(storage.load(object.objectKey()).getContentAsByteArray()).isEqualTo("course material".getBytes());
        assertThat(object.sha256()).hasSize(64);
        storage.delete(object.objectKey());
        assertThat(storage.load(object.objectKey()).exists()).isFalse();
    }

    @Test
    void rejectsObjectKeysOutsideStorageRoot() throws Exception {
        LocalFileStorage storage = new LocalFileStorage(directory.toString());

        assertThatThrownBy(() -> storage.load("../outside.txt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非法对象键");
    }
}
