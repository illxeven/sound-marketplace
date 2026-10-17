package com.soundmarketplace.service.impl;

import com.soundmarketplace.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {

    private final Path rootLocation;

    public FileStorageServiceImpl(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            log.error("Could not initialize upload storage location", e);
        }
    }

    @Override
    public String storeFile(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty or null file");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        if (originalFilename.contains("..")) {
            throw new IllegalArgumentException("Filename contains invalid path sequence: " + originalFilename);
        }

        try {
            Path targetDir = this.rootLocation.resolve(subDirectory).normalize();
            Files.createDirectories(targetDir);

            String fileExtension = "";
            int extIdx = originalFilename.lastIndexOf('.');
            if (extIdx >= 0) {
                fileExtension = originalFilename.substring(extIdx);
            }

            String storedFileName = UUID.randomUUID() + "_" + originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");
            Path destinationFile = targetDir.resolve(storedFileName).normalize();

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Successfully stored uploaded file: {}", destinationFile);
            return "/" + subDirectory + "/" + storedFileName;
        } catch (IOException e) {
            log.error("Failed to store uploaded file: {}", originalFilename, e);
            throw new RuntimeException("Could not store file " + originalFilename + ". Please try again!", e);
        }
    }

    @Override
    public org.springframework.core.io.Resource loadAsResource(String filePath) {
        if (filePath == null || filePath.isBlank()) return null;
        try {
            String cleanRelative = filePath.startsWith("/") ? filePath.substring(1) : filePath;
            Path file = this.rootLocation.resolve(cleanRelative).normalize();
            if (!Files.exists(file)) {
                // Check if it's directly inside tracks directory
                file = this.rootLocation.resolve("tracks").resolve(cleanRelative).normalize();
            }
            if (Files.exists(file) && Files.isReadable(file)) {
                return new org.springframework.core.io.UrlResource(file.toUri());
            } else {
                log.warn("File does not exist or is not readable: {}", file);
                return null;
            }
        } catch (java.net.MalformedURLException e) {
            log.error("Malformed URL for file path: {}", filePath, e);
            return null;
        }
    }

    @Override
    public void deleteFile(String filePath) {
        if (filePath == null || filePath.isBlank()) return;
        try {
            String cleanRelative = filePath.startsWith("/") ? filePath.substring(1) : filePath;
            Path file = this.rootLocation.resolve(cleanRelative).normalize();
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Could not delete file at: {}", filePath, e);
        }
    }
}
