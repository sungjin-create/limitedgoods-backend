package com.limitedgoods.limitedgoods.users.dto;

import lombok.Data;

@Data
public class UsersLoginRequest {
    private String email;
    private String password;
}
