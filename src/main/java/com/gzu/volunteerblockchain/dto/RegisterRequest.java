package com.gzu.volunteerblockchain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 100, message = "用户名长度不能超过 100")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过 100")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须在 6 到 50 之间")
    private String password;

    @Size(max = 50, message = "角色长度不能超过 50")
    private String role;

    @Size(max = 255, message = "组织名称长度不能超过 255")
    private String organizationName;

    @Size(max = 2000, message = "组织简介长度不能超过 2000")
    private String organizationDescription;
}
