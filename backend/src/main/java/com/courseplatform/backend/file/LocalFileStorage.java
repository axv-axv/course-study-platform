package com.courseplatform.backend.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalFileStorage implements FileStorage {
    private final Path root;

    public LocalFileStorage(@Value("${app.storage.root}") String root) throws IOException {
        this.root = Path.of(root).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
    }

    @Override
    public StoredObject store(MultipartFile multipart) throws IOException {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String objectKey = uuid.substring(0, 2) + "/" + uuid;
        Path target = resolve(objectKey);
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), "upload-", ".tmp");
        MessageDigest digest = sha256();
        try (InputStream input = new DigestInputStream(multipart.getInputStream(), digest);
             OutputStream output = Files.newOutputStream(temporary)) {
            input.transferTo(output);
        }
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temporary);
        }
        return new StoredObject(objectKey, HexFormat.of().formatHex(digest.digest()));
    }

    @Override
    public Resource load(String objectKey) {
        return new FileSystemResource(resolve(objectKey));
    }

    @Override
    public void delete(String objectKey) throws IOException {
        Files.deleteIfExists(resolve(objectKey));
    }

    private Path resolve(String objectKey) {
        Path path = root.resolve(objectKey).normalize();
        if (!path.startsWith(root)) throw new IllegalArgumentException("非法对象键");
        return path;
    }

    private MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

}
