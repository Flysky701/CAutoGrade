package com.autograding.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 50, message = "用户名长度需要在4-50之间")
    private String username;

    private String code;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需要在6-64之间")
    private String password;

    @NotBlank(message = "角色不能为空")
    private String role;

    private String nickname;
}
