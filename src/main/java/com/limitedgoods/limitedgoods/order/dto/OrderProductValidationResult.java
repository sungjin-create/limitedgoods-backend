package com.limitedgoods.limitedgoods.order.dto;

public record OrderProductValidationResult(
        Long admissionProductId
){
    public boolean requiresAdmissionToken(){
        return admissionProductId != null;
    }
}
