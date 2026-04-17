package com.gzu.volunteerblockchain.vo;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserProfileVO {

    private Integer userId;
    private String username;
    private String email;
    private String role;
    private String primaryRole;
    private List<String> roles;
    private List<String> permissions;
    private Integer organizationId;
    private String organizationName;
    private Integer totalPoints;
}
