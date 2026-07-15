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
    ORDER_ALREADY_CANCELED(
            HttpStatus.CONFLICT,
            "ORDER_003",
            "이미 취소된 주문입니다."
    ),
    ORDER_CANCEL_NOT_ALLOWED(
            HttpStatus.BAD_REQUEST,
            "ORDER_004",
            "현재 주문 상태에서는 취소할 수 없습니다."
    ),
    ORDER_STARTING_PAYMENT(
            HttpStatus.BAD_REQUEST,
            "ORDER_005",
            "현재 결제 진행중인 주문이 있습니다."
    ),
    TOO_MANY_ORDER_REQUESTS(
            HttpStatus.TOO_MANY_REQUESTS,
            "ORDER_006",
            "주문 요청이 너무 많습니다. 잠시 후 다시 시도해주세요."
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
    ),
    DUPLICATE_PAYMENT_REQUEST(
            HttpStatus.CONFLICT,
            "PAYMENT_003",
            "이미 처리 중이거나 처리된 결제 요청입니다."
    ),
    PAYMENT_CANCEL_FAILED(
            HttpStatus.BAD_REQUEST,
            "PAYMENT_004",
            "결제 취소에 실패했습니다."
    ),

    CART_NOT_FOUND(
            HttpStatus.BAD_REQUEST,
            "CART_001",
            "CART를 찾을 수 없습니다."
    ),
    CART_ITEM_NOT_FOUND(
            HttpStatus.BAD_REQUEST,
            "CART_ITEM_001",
            "ITEM을 찾을 수 없습니다."
    ),
    CART_ITEM_ALREADY_ADD(
            HttpStatus.BAD_REQUEST,
            "CART_ITEM_002",
            "ITEM이 이미 장바구니에 담겨있습니다."
    ),

    HAS_NO_CHECKOUT_TOKEN(
            HttpStatus.BAD_REQUEST,
            "CHECKOUT_TOKEN_001",
            "CHECKOUT_TOKEN이 없습니다."
    ),

    QUEUE_SOLD_OUT(
            HttpStatus.BAD_REQUEST,
            "QUEUE_001",
            "품절된 상품은 대기열에 진입할 수 없습니다."
    ),
    ADMISSION_TOKEN_REQUIRED(
            HttpStatus.FORBIDDEN,
            "QUEUE_002",
            "입장 토큰이 필요합니다."
    ),
    ADMISSION_TOKEN_INVALID(
            HttpStatus.FORBIDDEN,
            "QUEUE_003",
            "유효하지 않거나 만료된 입장 토큰입니다."
    )
    ;



    private final HttpStatus status;
    private final String code;
    private final String message;
}
