package com.courseplatform.backend.file;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.OSSClientBuilder;
import com.aliyun.sdk.service.oss2.credentials.EnvironmentVariableCredentialsProvider;
import com.aliyun.sdk.service.oss2.models.DeleteObjectRequest;
import com.aliyun.sdk.service.oss2.models.GetObjectRequest;
import com.aliyun.sdk.service.oss2.models.GetObjectResult;
import com.aliyun.sdk.service.oss2.models.PutObjectRequest;
import com.aliyun.sdk.service.oss2.transport.BinaryData;
import com.aliyun.sdk.service.oss2.utils.IOUtils;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "oss")
public class AliyunOssFileStorage implements FileStorage {
    private final OSSClient client;
    private final String bucket;
    private final String prefix;

    public AliyunOssFileStorage(
            @Value("${app.storage.oss.region}") String region,
            @Value("${app.storage.oss.bucket}") String bucket,
            @Value("${app.storage.oss.endpoint:}") String endpoint,
            @Value("${app.storage.oss.prefix:course-platform}") String prefix
    ) {
        requireValue(region, "OSS_REGION");
        requireValue(bucket, "OSS_BUCKET");
        requireValue(System.getenv("OSS_ACCESS_KEY_ID"), "OSS_ACCESS_KEY_ID");
        requireValue(System.getenv("OSS_ACCESS_KEY_SECRET"), "OSS_ACCESS_KEY_SECRET");
        OSSClientBuilder builder = OSSClient.newBuilder()
                .credentialsProvider(new EnvironmentVariableCredentialsProvider())
                .region(region);
        if (endpoint != null && !endpoint.isBlank()) builder.endpoint(endpoint.trim());
        this.client = builder.build();
        this.bucket = bucket.trim();
        this.prefix = normalizePrefix(prefix);
    }

    @Override
    public StoredObject store(MultipartFile multipart) throws IOException {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String objectKey = prefix + uuid.substring(0, 2) + "/" + uuid;
        MessageDigest digest = sha256();
        try (InputStream input = new DigestInputStream(multipart.getInputStream(), digest)) {
            client.putObject(PutObjectRequest.newBuilder()
                    .bucket(bucket)
                    .key(objectKey)
                    .body(BinaryData.fromStream(input))
                    .build());
            return new StoredObject(objectKey, HexFormat.of().formatHex(digest.digest()));
        } catch (RuntimeException exception) {
            throw new IOException("OSS upload failed", exception);
        }
    }

    @Override
    public Resource load(String objectKey) {
        try (GetObjectResult result = client.getObject(GetObjectRequest.newBuilder()
                .bucket(bucket).key(objectKey).build())) {
            byte[] content = IOUtils.toByteArray(result.body());
            return new ByteArrayResource(content) {
                @Override
                public String getFilename() {
                    return objectKey.substring(objectKey.lastIndexOf('/') + 1);
                }
            };
        } catch (Exception exception) {
            return new MissingOssResource(objectKey, exception);
        }
    }

    @Override
    public void delete(String objectKey) throws IOException {
        try {
            client.deleteObject(DeleteObjectRequest.newBuilder().bucket(bucket).key(objectKey).build());
        } catch (RuntimeException exception) {
            throw new IOException("OSS delete failed", exception);
        }
    }

    @PreDestroy
    public void close() throws Exception {
        client.close();
    }

    private static String normalizePrefix(String value) {
        String normalized = value == null ? "course-platform" : value.trim().replace('\\', '/');
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        if (normalized.contains("..")) throw new IllegalArgumentException("OSS_PREFIX 不能包含 ..");
        return normalized.isBlank() ? "" : normalized.endsWith("/") ? normalized : normalized + "/";
    }

    private static void requireValue(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " 未配置");
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static final class MissingOssResource extends ByteArrayResource {
        private final String description;

        private MissingOssResource(String objectKey, Exception cause) {
            super(new byte[0]);
            this.description = "OSS object unavailable: " + objectKey + " (" + cause.getClass().getSimpleName() + ")";
        }

        @Override
        public boolean exists() {
            return false;
        }

        @Override
        public String getDescription() {
            return description;
        }
    }
}
