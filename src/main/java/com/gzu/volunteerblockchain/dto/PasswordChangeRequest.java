package com.gzu.volunteerblockchain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PasswordChangeRequest {

    @NotBlank(message = "原密码不能为空")
    @Size(min = 6, max = 50, message = "原密码长度必须在 6 到 50 之间")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 50, message = "新密码长度必须在 6 到 50 之间")
    private String newPassword;
}
