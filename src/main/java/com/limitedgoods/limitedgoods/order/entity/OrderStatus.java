package com.limitedgoods.limitedgoods.order.entity;


public enum OrderStatus {
    CREATED, //주문 생성
    PAYMENT_PENDING, //결제 진행
    PAYMENT_APPROVED, //외부 결제는 승인됨
    PAID, //내부 확정까지 끝남
    PAYMENT_FAILED, //결제 실패
    CANCELED, //주문 취소
    COMPLETED, // 주문 완료
    EXPIRED // 주문 만료
}
