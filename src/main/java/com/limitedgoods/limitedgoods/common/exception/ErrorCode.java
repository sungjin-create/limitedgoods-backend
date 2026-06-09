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
    ),

    ORDER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "ORDER_001",
            "주문을 찾을 수 없습니다."
    ),

    INVALID_ORDER_STATUS(
            HttpStatus.BAD_REQUEST,
            "ORDER_002",
            "현재 주문 상태에서는 요청을 처리할 수 없습니다."
    ),

    PAYMENT_FAILED(
            HttpStatus.BAD_REQUEST,
            "PAYMENT_001",
            "결제에 실패했습니다."
    ),
    RESERVATION_EXPIRED(
            HttpStatus.BAD_REQUEST,
            "PAYMENT_002",
            "결제 유효기간이 지났습니다."
    )
    ;



    private final HttpStatus status;
    private final String code;
    private final String message;
}
