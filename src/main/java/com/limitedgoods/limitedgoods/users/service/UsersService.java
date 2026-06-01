package com.limitedgoods.limitedgoods.users.service;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.common.jwt.JwtUtil;
import com.limitedgoods.limitedgoods.users.dto.UsersInfoResponse;
import com.limitedgoods.limitedgoods.users.dto.UsersLoginRequest;
import com.limitedgoods.limitedgoods.users.dto.UsersLoginResponse;
import com.limitedgoods.limitedgoods.users.dto.UsersSignUpRequest;
import com.limitedgoods.limitedgoods.users.entity.UserRole;
import com.limitedgoods.limitedgoods.users.entity.Users;
import com.limitedgoods.limitedgoods.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class UsersService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public String signUpUsers(UsersSignUpRequest usersSignUpRequest) {
        String usersName = usersSignUpRequest.getName();
        String usersEmail = usersSignUpRequest.getEmail();
        String usersPassword = usersSignUpRequest.getPassword();

        checkSignUpUsers(usersName, usersEmail, usersPassword);

        Users signUpUser = new Users();
        signUpUser.setName(usersName);
        signUpUser.setEmail(usersEmail);
        signUpUser.setPassword(passwordEncoder.encode(usersPassword));
        signUpUser.setCreatedAt(LocalDateTime.now());
        signUpUser.setRole(UserRole.USER);

        usersRepository.save(signUpUser);
        return usersEmail;
    }

    private void checkSignUpUsers(String usersName, String usersEmail, String usersPassword) {
        if(usersName == null || usersEmail == null || usersPassword == null ||
                usersName.isEmpty() || usersEmail.isEmpty() || usersPassword.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        if (usersRepository.existsByEmail(usersEmail)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
    }

    @Transactional(readOnly = true)
    public UsersLoginResponse loginUsers(UsersLoginRequest usersLoginRequest) {
        String email = usersLoginRequest.getEmail();
        String password = usersLoginRequest.getPassword();

        if (email == null || password == null || email.isBlank() || password.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole());
        return new UsersLoginResponse(accessToken);
    }

    public UsersInfoResponse getUserInfo(String email) {

        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return UsersInfoResponse.builder().name(user.getName()).email(user.getEmail()).createdAt(user.getCreatedAt()).build();
    }
}
