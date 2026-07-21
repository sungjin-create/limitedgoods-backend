package com.limitedgoods.limitedgoods.order.policy;

public record OrderProductValidationResult(
        Long admissionProductId
){
    public boolean requiresAdmissionToken(){
        return admissionProductId != null;
    }
}
