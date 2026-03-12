package com.example.webrtcbackend.video;

import com.example.webrtcbackend.video.dto.PresignRequest;
import com.example.webrtcbackend.video.dto.VideoMetadataDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.time.Instant;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VideoServiceTest {
    private S3Presigner presigner;
    private S3Client s3Client;
    private VideoMetadataRepository repo;
    private VideoService service;

    @BeforeEach
    void setUp() {
        presigner = mock(S3Presigner.class);
        s3Client = mock(S3Client.class);
        repo = mock(VideoMetadataRepository.class);
        service = new VideoService(presigner, s3Client, "bucket", "https://endpoint/", repo);
    }

    @Test
    void presignUrl_containsCourseAndLesson() {
        PresignRequest req = new PresignRequest();
        req.setCourseId(1L);
        req.setLessonId(2L);
        req.setFileName("lecture.mp4");

        PresignedPutObjectRequest preg = mock(PresignedPutObjectRequest.class);
        when(preg.url()).thenReturn(URI.create("https://fake/upload"));
        when(presigner.presignPutObject(any())).thenReturn(preg);

        var resp = service.createPresignedUrl(req);
        assertNotNull(resp.getUploadUrl());
        assertTrue(resp.getObjectKey().startsWith("videos/1/2/"));
        assertTrue(resp.getObjectKey().endsWith("-lecture.mp4"));
    }

    @Test
    void presignUrl_invalidExtension() {
        PresignRequest req = new PresignRequest();
        req.setCourseId(1L); req.setLessonId(2L); req.setFileName("foo.txt");
        assertThrows(Exception.class, () -> service.createPresignedUrl(req));
    }

    @Test
    void saveMetadata_objectNotFound() {
        VideoMetadataDto dto = new VideoMetadataDto();
        dto.setCourseId(1L); dto.setLessonId(2L);
        dto.setOriginalFileName("a.mp4");
        dto.setObjectKey("videos/1/2/foo.mp4");
        dto.setFileSize(100L);
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(NoSuchKeyException.builder().build());
        assertThrows(Exception.class, () -> service.saveMetadata(dto));
    }
}
