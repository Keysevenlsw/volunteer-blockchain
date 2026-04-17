package com.gzu.volunteerblockchain.service.impl;

import com.gzu.volunteerblockchain.exception.BusinessException;
import com.gzu.volunteerblockchain.service.StorageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StorageServiceImpl implements StorageService {

    @Value("${storage.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null) {
            int lastDot = originalName.lastIndexOf('.');
            if (lastDot >= 0) {
                extension = originalName.substring(lastDot);
            }
        }

        String relativeDir = LocalDate.now().toString();
        String filename = UUID.randomUUID().toString().replace("-", "") + extension;
        Path basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path targetDir = basePath.resolve(relativeDir).normalize();
        Path targetFile = targetDir.resolve(filename).normalize();

        if (!targetFile.startsWith(basePath)) {
            throw new BusinessException("非法文件路径");
        }

        try {
            Files.createDirectories(targetDir);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BusinessException("文件保存失败: " + ex.getMessage());
        }

        return "/uploads/" + relativeDir + "/" + filename;
    }
}
