package com.gzu.volunteerblockchain.controller;

import com.gzu.volunteerblockchain.common.RoleAllowed;
import com.gzu.volunteerblockchain.common.RoleConstants;
import com.gzu.volunteerblockchain.dto.PlatformRequests;
import com.gzu.volunteerblockchain.service.ProductService;
import com.gzu.volunteerblockchain.vo.ApiResponse;
import com.gzu.volunteerblockchain.vo.PlatformVOs;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @RoleAllowed(RoleConstants.ORGANIZATION_ADMIN)
    @PostMapping("/products")
    public ApiResponse<PlatformVOs.ProductVO> createProduct(@Valid @RequestBody PlatformRequests.ProductSaveRequest request) {
        return ApiResponse.success("商品草稿创建成功", productService.createProduct(request));
    }

    @RoleAllowed(RoleConstants.ORGANIZATION_ADMIN)
    @PutMapping("/products/{id}")
    public ApiResponse<PlatformVOs.ProductVO> updateProduct(
        @PathVariable Integer id,
        @Valid @RequestBody PlatformRequests.ProductSaveRequest request
    ) {
        return ApiResponse.success("商品更新成功", productService.updateProduct(id, request));
    }

    @RoleAllowed(RoleConstants.ORGANIZATION_ADMIN)
    @DeleteMapping("/products/{id}")
    public ApiResponse<Void> deleteProduct(@PathVariable Integer id) {
        productService.deleteProduct(id);
        return ApiResponse.success("商品删除成功", null);
    }

    @RoleAllowed(RoleConstants.ORGANIZATION_ADMIN)
    @PostMapping("/products/{id}/submit")
    public ApiResponse<PlatformVOs.ProductVO> submitProduct(@PathVariable Integer id) {
        return ApiResponse.success("商品已提交审核", productService.submitProduct(id));
    }

    @RoleAllowed(RoleConstants.VOLUNTEER)
    @GetMapping("/products/volunteer")
    public ApiResponse<List<PlatformVOs.ProductVO>> volunteerProducts() {
        return ApiResponse.success(productService.listVolunteerProducts());
    }

    @RoleAllowed(RoleConstants.ORGANIZATION_ADMIN)
    @GetMapping("/products/organization")
    public ApiResponse<List<PlatformVOs.ProductVO>> organizationProducts() {
        return ApiResponse.success(productService.listOrganizationProducts());
    }

    @RoleAllowed(RoleConstants.VOLUNTEER)
    @PostMapping("/redemptions")
    public ApiResponse<PlatformVOs.RedemptionVO> redeemProduct(@Valid @RequestBody PlatformRequests.RedemptionRequest request) {
        return ApiResponse.success("兑换申请已提交", productService.redeemProduct(request));
    }

    @RoleAllowed(RoleConstants.VOLUNTEER)
    @GetMapping("/redemptions/mine")
    public ApiResponse<List<PlatformVOs.RedemptionVO>> myRedemptions() {
        return ApiResponse.success(productService.listMyRedemptions());
    }

    @RoleAllowed(RoleConstants.ORGANIZATION_ADMIN)
    @GetMapping("/redemptions/organization")
    public ApiResponse<List<PlatformVOs.RedemptionVO>> organizationRedemptions() {
        return ApiResponse.success(productService.listOrganizationRedemptions());
    }

    @RoleAllowed(RoleConstants.ORGANIZATION_ADMIN)
    @PostMapping("/redemptions/{id}/status")
    public ApiResponse<PlatformVOs.RedemptionVO> updateRedemptionStatus(
        @PathVariable Integer id,
        @Valid @RequestBody PlatformRequests.RedemptionStatusRequest request
    ) {
        return ApiResponse.success("兑换状态更新成功", productService.updateRedemptionStatus(id, request));
    }
}
