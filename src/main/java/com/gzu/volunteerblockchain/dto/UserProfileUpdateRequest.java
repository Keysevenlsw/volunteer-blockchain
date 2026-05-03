package com.gzu.volunteerblockchain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserProfileUpdateRequest {

    @Size(min = 2, max = 100, message = "用户名长度必须在 2 到 100 之间")
    private String username;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过 100")
    private String email;

}
