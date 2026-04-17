package com.gzu.volunteerblockchain.controller;

import com.gzu.volunteerblockchain.service.PublicService;
import com.gzu.volunteerblockchain.vo.ApiResponse;
import com.gzu.volunteerblockchain.vo.InfoItemVO;
import com.gzu.volunteerblockchain.vo.PlatformVOs;
import com.gzu.volunteerblockchain.vo.ProjectItemVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final PublicService publicService;

    public PublicController(PublicService publicService) {
        this.publicService = publicService;
    }

    @GetMapping("/info")
    public ApiResponse<List<InfoItemVO>> info() {
        return ApiResponse.success(publicService.getInfoList());
    }

    @GetMapping("/completed-projects")
    public ApiResponse<List<ProjectItemVO>> completedProjects(
        @RequestParam(value = "limit", defaultValue = "6") int limit
    ) {
        return ApiResponse.success(publicService.getCompletedProjects(limit));
    }

    @GetMapping("/evidences")
    public ApiResponse<List<PlatformVOs.EvidenceVO>> evidences(
        @RequestParam(value = "limit", defaultValue = "20") Integer limit,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "organizationId", required = false) Integer organizationId,
        @RequestParam(value = "keyword", required = false) String keyword
    ) {
        return ApiResponse.success(publicService.listPublicEvidences(limit, status, organizationId, keyword));
    }

    @GetMapping("/evidences/{bizType}/{bizId}")
    public ApiResponse<PlatformVOs.EvidenceVO> evidenceDetail(@PathVariable String bizType, @PathVariable Integer bizId) {
        return ApiResponse.success(publicService.getPublicEvidence(bizType, bizId));
    }
}
