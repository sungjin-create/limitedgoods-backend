package com.limitedgoods.limitedgoods.user.dto;

import lombok.Data;

@Data
public class UserSignUpRequest {
    private String name;
    private String email;
    private String password;
    private String confirmPassword;
}
