package com.limitedgoods.limitedgoods.users.dto;

import lombok.Data;

@Data
public class UsersSignUpRequest {
    private String name;
    private String email;
    private String password;
    private String confirmPassword;
}
