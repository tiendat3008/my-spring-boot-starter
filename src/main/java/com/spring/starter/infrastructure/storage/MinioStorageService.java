package com.spring.starter.infrastructure.storage;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.spring.starter.common.config.MinioProperties;
import com.spring.starter.common.config.StorageProperties;
import com.spring.starter.common.exception.AppException;
import com.spring.starter.common.exception.ErrorCode;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Http.Method;

/**
 * {@link StorageService} implementation backed by MinIO (S3-compatible).
 *
 * <p>
 * In dev, connects to the local MinIO container.
 * In prod, set MINIO_ENDPOINT to the AWS S3 regional endpoint and provide S3
 * credentials.
 */
@Service
public class MinioStorageService implements StorageService {

    private static final Logger logger = LoggerFactory.getLogger(MinioStorageService.class);

    private final MinioClient minioClient;
    private final String bucket;
    private final String endpoint;

    public MinioStorageService(
            MinioClient minioClient,
            StorageProperties storageProperties,
            MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.bucket = storageProperties.bucket();
        this.endpoint = minioProperties.endpoint();
    }

    @Override
    public String upload(String objectKey, InputStream inputStream, String contentType, long sizeBytes) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(inputStream, sizeBytes, -1L)
                            .contentType(contentType)
                            .build());
            var url = "%s/%s/%s".formatted(endpoint, bucket, objectKey);
            logger.debug("Uploaded object: {}", url);
            return url;
        } catch (Exception ex) {
            logger.error("Failed to upload object: {}", objectKey, ex);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build());
            logger.debug("Deleted object: {}", objectKey);
        } catch (Exception ex) {
            logger.error("Failed to delete object: {}", objectKey, ex);
            throw new AppException(ErrorCode.FILE_DELETE_FAILED);
        }
    }

    @Override
    public String generatePresignedUrl(String objectKey, int expirySeconds) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(expirySeconds, TimeUnit.SECONDS)
                            .build());
        } catch (Exception ex) {
            logger.error("Failed to generate presigned URL for: {}", objectKey, ex);
            throw new AppException(ErrorCode.PRESIGNED_URL_GENERATION_FAILED);
        }
    }
}
