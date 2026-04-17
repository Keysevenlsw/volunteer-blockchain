package com.gzu.volunteerblockchain.service;

import com.gzu.volunteerblockchain.dto.PlatformRequests;
import com.gzu.volunteerblockchain.vo.PlatformVOs;
import java.util.List;

public interface ProductService {

    PlatformVOs.ProductVO createProduct(PlatformRequests.ProductSaveRequest request);

    PlatformVOs.ProductVO updateProduct(Integer id, PlatformRequests.ProductSaveRequest request);

    void deleteProduct(Integer id);

    PlatformVOs.ProductVO submitProduct(Integer id);

    List<PlatformVOs.ProductVO> listVolunteerProducts();

    List<PlatformVOs.ProductVO> listOrganizationProducts();

    PlatformVOs.RedemptionVO redeemProduct(PlatformRequests.RedemptionRequest request);

    PlatformVOs.RedemptionVO updateRedemptionStatus(Integer id, PlatformRequests.RedemptionStatusRequest request);

    List<PlatformVOs.RedemptionVO> listMyRedemptions();

    List<PlatformVOs.RedemptionVO> listOrganizationRedemptions();
}
