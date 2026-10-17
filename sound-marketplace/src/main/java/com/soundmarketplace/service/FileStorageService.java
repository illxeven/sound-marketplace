package com.soundmarketplace.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storeFile(MultipartFile file, String subDirectory);
    Resource loadAsResource(String filePath);
    void deleteFile(String filePath);
}
