package com.spring.starter.infrastructure.storage;

import java.io.InputStream;

/**
 * Storage service abstraction — decouples application code from the underlying
 * object storage.
 * Implementations: {@link MinioStorageService} (dev), AWS S3 (prod — same
 * interface).
 */
public interface StorageService {

    /**
     * Uploads an object to the configured bucket and returns the public URL.
     *
     * @param objectKey   the path/key within the bucket (e.g.
     *                    "kyc/userId/front.jpg")
     * @param inputStream the content to upload
     * @param contentType the MIME type (e.g. "image/jpeg")
     * @param sizeBytes   the exact byte size of the content (-1 if unknown)
     * @return the full URL to access the uploaded object
     */
    String upload(String objectKey, InputStream inputStream, String contentType, long sizeBytes);

    /**
     * Deletes an object from the bucket.
     *
     * @param objectKey the path/key within the bucket
     */
    void delete(String objectKey);

    /**
     * Generates a pre-signed URL for temporary private access (e.g. KYC documents).
     *
     * @param objectKey     the path/key within the bucket
     * @param expirySeconds how long the URL should be valid
     * @return a time-limited URL
     */
    String generatePresignedUrl(String objectKey, int expirySeconds);
}
