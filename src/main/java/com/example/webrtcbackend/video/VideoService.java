package com.example.webrtcbackend.video;

import com.example.webrtcbackend.video.dto.PresignRequest;
import com.example.webrtcbackend.video.dto.PresignResponse;
import com.example.webrtcbackend.video.dto.VideoMetadataDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class VideoService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("mp4", "mov", "mkv");
    private static final long MAX_FILE_SIZE = 1024L * 1024L * 1024L; // 1 GB
    private static final Duration PRESIGN_EXPIRY = Duration.ofMinutes(10);

    private final S3Presigner presigner;
    private final S3Client s3Client;
    private final String bucketName;
    private final String endpoint;
    private final VideoMetadataRepository repository;

    public VideoService(S3Presigner presigner,
                        S3Client s3Client,
                        @Value("${r2.bucket-name}") String bucketName,
                        @Value("${r2.endpoint}") String endpoint,
                        VideoMetadataRepository repository) {
        this.presigner = presigner;
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.endpoint = endpoint;
        this.repository = repository;
    }

    public PresignResponse createPresignedUrl(PresignRequest request) {

        validateFilename(request.getFileName());

        if (request.getFileSize() != null && request.getFileSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(BAD_REQUEST, "file size exceeds limit");
        }

        String uuid = UUID.randomUUID().toString();

        String safeName = request.getFileName()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-zA-Z0-9._-]", "");

        String objectKey = String.format(
                "videos/%d/%d/%s-%s",
                request.getCourseId(),
                request.getLessonId(),
                uuid,
                safeName
        );

        PutObjectRequest por = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder()
                        .putObjectRequest(por)
                        .signatureDuration(PRESIGN_EXPIRY)
                        .build();

        PresignedPutObjectRequest presigned =
                presigner.presignPutObject(presignRequest);

        return new PresignResponse(
                presigned.url().toString(),
                objectKey,
                Instant.now().plus(PRESIGN_EXPIRY)
        );
    }

    public VideoMetadata saveMetadata(VideoMetadataDto dto) {
        if (dto.getFileSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(BAD_REQUEST, "file size exceeds limit");
        }
        validateFilename(dto.getOriginalFileName());

        // verify object exists and size matches
        try {
            HeadObjectRequest headReq = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(dto.getObjectKey())
                    .build();
            HeadObjectResponse headRes = s3Client.headObject(headReq);
            long actualSize = headRes.contentLength();
            if (actualSize > MAX_FILE_SIZE) {
                throw new ResponseStatusException(BAD_REQUEST, "uploaded file too large");
            }
            // override provided size with actual
            dto.setFileSize(actualSize);
        } catch (NoSuchKeyException e) {
            throw new ResponseStatusException(BAD_REQUEST, "object not found in storage");
        } catch (SdkException e) {
            throw new ResponseStatusException(BAD_REQUEST, "error checking object: " + e.getMessage());
        }

        VideoMetadata entity = new VideoMetadata();
        entity.setCourseId(dto.getCourseId());
        entity.setLessonId(dto.getLessonId());
        entity.setOriginalFileName(dto.getOriginalFileName());
        entity.setObjectKey(dto.getObjectKey());
        entity.setFileSize(dto.getFileSize());
        entity.setUploadTime(Instant.now());
        entity.setVideoUrl(buildVideoUrl(dto.getObjectKey()));

        return repository.save(entity);
    }

    private String buildVideoUrl(String objectKey) {
        // endpoint already contains bucket path for R2 according to configuration
        // ensure no trailing slash
        String base = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        return base + "/" + objectKey;
    }

    private void validateFilename(String filename) {
        String ext = getExtension(filename);
        if (!ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
            throw new ResponseStatusException(BAD_REQUEST, "invalid video file type");
        }
    }

    private String getExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx == -1 || idx == filename.length() - 1) {
            return "";
        }
        return filename.substring(idx + 1);
    }

    public List<VideoMetadata> getListVideo(Long courseId, Long lessonId) {
        return repository.findByCourseIdAndLessonId(courseId, lessonId);
    }

    public String generateVideoUrl(String key) {

        GetObjectRequest objectRequest =
                GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();

        GetObjectPresignRequest presignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(30))
                        .getObjectRequest(objectRequest)
                        .build();

        PresignedGetObjectRequest presignedRequest =
                presigner.presignGetObject(presignRequest);

        return presignedRequest.url().toString();
    }
}
