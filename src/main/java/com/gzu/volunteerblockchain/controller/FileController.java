package com.gzu.volunteerblockchain.controller;

import com.gzu.volunteerblockchain.common.RoleAllowed;
import com.gzu.volunteerblockchain.common.RoleConstants;
import com.gzu.volunteerblockchain.service.StorageService;
import com.gzu.volunteerblockchain.vo.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final StorageService storageService;

    public FileController(StorageService storageService) {
        this.storageService = storageService;
    }

    @RoleAllowed({RoleConstants.VOLUNTEER, RoleConstants.ORGANIZATION_ADMIN, RoleConstants.SYSTEM_ADMIN})
    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> upload(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(Map.of("path", storageService.store(file)));
    }
}
