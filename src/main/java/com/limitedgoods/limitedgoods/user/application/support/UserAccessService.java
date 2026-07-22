package com.limitedgoods.limitedgoods.user.application.support;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.user.entity.User;
import com.limitedgoods.limitedgoods.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserAccessService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.USER_NOT_FOUND)
                );
    }
}