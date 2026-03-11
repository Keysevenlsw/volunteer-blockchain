package com.gzu.volunteerblockchain.controller;

import com.gzu.volunteerblockchain.service.PublicService;
import com.gzu.volunteerblockchain.vo.ApiResponse;
import com.gzu.volunteerblockchain.vo.InfoItemVO;
import com.gzu.volunteerblockchain.vo.ProjectItemVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
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
}
