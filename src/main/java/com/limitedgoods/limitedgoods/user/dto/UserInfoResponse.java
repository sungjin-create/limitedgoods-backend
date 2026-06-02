package com.limitedgoods.limitedgoods.user.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserInfoResponse {
    private String email;
    private String name;
    private String role;
    private LocalDateTime createdAt;
}
