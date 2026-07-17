package com.limitedgoods.limitedgoods.product.entity;

public enum ProductStatus {
    DRAFT,      //임시저장
    PREPARING,  //준비 중
    SCHEDULED,  //판매 예정
    ACTIVE,     //판매 가능
    PAUSED,     //판매 중지
    HIDDEN,     //비공개
    ARCHIVED    //운영종료/보관
}
