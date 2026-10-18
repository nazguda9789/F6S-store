package com.web.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.web.entity.CloudKey;
import com.web.repository.CloudKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CloudinaryService {
    @Autowired
    private CloudKeyRepository cloudKeyRepository;

    private Cloudinary getCloudNow() {
        CloudKey key = cloudKeyRepository.findCloudPrimary();
        if (key == null || !StringUtils.hasText(key.getCloudName())
                || !StringUtils.hasText(key.getApiKey()) || !StringUtils.hasText(key.getApiSecret())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Vui lòng cấu hình tài khoản Cloudinary chính trước khi tải ảnh.");
        }
        return new Cloudinary(ObjectUtils.asMap("cloud_name", key.getCloudName(),
                "api_key", key.getApiKey(), "api_secret", key.getApiSecret(), "secure", true));
    }

    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn tệp ảnh không rỗng.");
        }
        Cloudinary cloud = getCloudNow();
        Path temporary = null;
        try {
            temporary = Files.createTempFile("f6s-upload-", ".tmp");
            try (var input = file.getInputStream()) {
                Files.copy(input, temporary, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            Map result = cloud.uploader().upload(temporary.toFile(),
                    ObjectUtils.asMap("resource_type", "image"));
            Object url = result.get("secure_url");
            if (url == null) throw new IllegalStateException("Missing upload URL");
            return url.toString();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Không tải được ảnh. Vui lòng kiểm tra cấu hình Cloudinary và thử lại.");
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); }
                catch (java.io.IOException ex) {
                    org.slf4j.LoggerFactory.getLogger(CloudinaryService.class)
                            .warn("Could not remove temporary upload file.");
                }
            }
        }
    }

    public List<String> uploadMultiFile(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn ảnh.");
        }
        // Preserve order and report a failure instead of silently returning missing images.
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) urls.add(uploadFile(file));
        return urls;
    }
}
