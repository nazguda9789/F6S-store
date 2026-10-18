package com.web.service;

import com.web.repository.CloudKeyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CloudinaryServiceTests {
    @Test void missingPrimaryConfigurationReturnsClearError() {
        CloudinaryService service = new CloudinaryService();
        ReflectionTestUtils.setField(service, "cloudKeyRepository", mock(CloudKeyRepository.class));
        var file = new MockMultipartFile("image", "photo.png", "image/png", new byte[]{1});
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, assertThrows(ResponseStatusException.class,
                () -> service.uploadFile(file)).getStatusCode());
    }
    @Test void emptyFileIsRejected() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> new CloudinaryService().uploadFile(new MockMultipartFile("image", new byte[0])))
                .getStatusCode());
    }
    @Test void multiUploadPreservesInputOrder() {
        CloudinaryService service = spy(new CloudinaryService());
        var first = new MockMultipartFile("a", new byte[]{1});
        var second = new MockMultipartFile("b", new byte[]{2});
        doReturn("first-url").when(service).uploadFile(first);
        doReturn("second-url").when(service).uploadFile(second);
        assertEquals(List.of("first-url", "second-url"), service.uploadMultiFile(List.of(first, second)));
    }
    @Test void multiUploadDoesNotSilentlyDropFailedFiles() {
        CloudinaryService service = spy(new CloudinaryService());
        var file = new MockMultipartFile("image", new byte[]{1});
        doThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY)).when(service).uploadFile(file);
        assertThrows(ResponseStatusException.class, () -> service.uploadMultiFile(List.of(file)));
    }
}
