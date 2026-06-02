package com.limitedgoods.limitedgoods.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ErrorCode {

    USER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "USER_001",
            "사용자를 찾을 수 없습니다."
    ),

    DUPLICATE_EMAIL(
            HttpStatus.CONFLICT,
            "USER_002",
            "이미 사용 중인 이메일입니다."
    ),

    INVALID_INPUT(
            HttpStatus.BAD_REQUEST,
            "COMMON_001",
            "잘못된 요청입니다."
    ),

    INVALID_PRODUCT_ID(
            HttpStatus.BAD_REQUEST,
            "PRODUCT_001",
                    "Id에 해당하는 상품이 없습니다."
    ),
    INSUFFICIENT_STOCK(
            HttpStatus.BAD_REQUEST,
            "PRODUCT_002",
            "상품의 재고보다 요청한 수량이 더 많습니다."
    );



    private final HttpStatus status;
    private final String code;
    private final String message;
}
