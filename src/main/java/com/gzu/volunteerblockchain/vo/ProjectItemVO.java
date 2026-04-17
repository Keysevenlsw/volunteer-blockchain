package com.gzu.volunteerblockchain.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProjectItemVO {

    private Integer id;
    private String title;
    private String description;
    private String location;
    private String endDate;
    private String image;
    private String organizationName;
    private String volunteerName;
    private String reviewerName;
    private String reviewTime;
    private String evidenceStatus;
    private String txHash;
    private String digest;
    private Boolean verified;
}
