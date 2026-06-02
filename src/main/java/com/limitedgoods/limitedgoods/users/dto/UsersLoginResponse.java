package com.limitedgoods.limitedgoods.users.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UsersLoginResponse {
    private String accessToken;
}
