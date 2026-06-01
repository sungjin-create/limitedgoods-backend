package com.limitedgoods.limitedgoods.users.controller;

import com.limitedgoods.limitedgoods.common.response.ApiResponse;
import com.limitedgoods.limitedgoods.users.dto.UsersLoginRequest;
import com.limitedgoods.limitedgoods.users.dto.UsersLoginResponse;
import com.limitedgoods.limitedgoods.users.dto.UsersSignUpRequest;
import com.limitedgoods.limitedgoods.users.dto.UsersSignUpResponse;
import com.limitedgoods.limitedgoods.users.service.UsersService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UsersController {

    private final UsersService usersService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse> userSignUp(@RequestBody UsersSignUpRequest usersSignUpRequest) {

        String userEmail = usersService.signUpUsers(usersSignUpRequest);

        return ResponseEntity.ok(ApiResponse.success(new UsersSignUpResponse(userEmail)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> userLogin(@RequestBody UsersLoginRequest usersLoginRequest) {
        return ResponseEntity.ok(ApiResponse.success(usersService.loginUsers(usersLoginRequest)));
    }

    @GetMapping("/info")
    public ResponseEntity<ApiResponse> userInfo(Authentication authentication) {
        String email = authentication.getName();



        return ResponseEntity.ok(ApiResponse.success(usersService.getUserInfo(email)));
    }

    @GetMapping("/test")
    public ResponseEntity<ApiResponse> test() {

        return ResponseEntity.ok(ApiResponse.success("test"));
    }

}
