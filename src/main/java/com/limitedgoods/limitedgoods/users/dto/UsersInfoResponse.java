package com.limitedgoods.limitedgoods.users.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UsersInfoResponse {
    private String email;
    private String name;
    private String role;
    private LocalDateTime createdAt;
}
