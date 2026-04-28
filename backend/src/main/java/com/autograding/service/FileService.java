package com.autograding.service;

import com.autograding.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final Path uploadDir;

    public FileService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            log.error("Failed to create upload directory: {}", this.uploadDir, e);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(400, "文件大小不能超过10MB");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new BusinessException(400, "文件名不能为空");
        }

        String sanitized = originalName.replaceAll("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]", "_");
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String storedName = datePrefix + "_" + UUID.randomUUID().toString().substring(0, 8) + "_" + sanitized;

        try {
            Path targetPath = this.uploadDir.resolve(storedName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("File stored: {}", storedName);
            return storedName;
        } catch (IOException e) {
            log.error("Failed to store file: {}", originalName, e);
            throw new BusinessException(500, "文件上传失败");
        }
    }

    public Path getFile(String fileName) {
        Path filePath = this.uploadDir.resolve(fileName).normalize();
        if (!filePath.startsWith(this.uploadDir)) {
            throw new BusinessException(403, "非法的文件路径");
        }
        if (!Files.exists(filePath)) {
            throw new BusinessException(404, "文件不存在");
        }
        return filePath;
    }

    public boolean deleteFile(String fileName) {
        try {
            Path filePath = this.uploadDir.resolve(fileName).normalize();
            if (!filePath.startsWith(this.uploadDir)) {
                return false;
            }
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", fileName, e);
            return false;
        }
    }
}
