package com.limitedgoods.limitedgoods.notification.service;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.common.response.PageResponse;
import com.limitedgoods.limitedgoods.notification.dto.AdminEmailDeliveryResponse;
import com.limitedgoods.limitedgoods.notification.entity.EmailDelivery;
import com.limitedgoods.limitedgoods.notification.repository.EmailDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminEmailDeliveryService {

    private final EmailDeliveryRepository repository;

    @Transactional(readOnly = true)
    public PageResponse<AdminEmailDeliveryResponse> findDead(Pageable pageable) {
        Page<AdminEmailDeliveryResponse> page =
                repository.findByStatus(EmailDelivery.Status.DEAD, pageable)
                        .map(AdminEmailDeliveryResponse::from);

        return PageResponse.from(page);
    }

    @Transactional
    public void requeue(Long deliveryId) {
        EmailDelivery delivery = repository
                .findByIdForUpdate(deliveryId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.EMAIL_DELIVERY_NOT_FOUND));

        if (delivery.getStatus() != EmailDelivery.Status.DEAD) {
            throw new BusinessException(ErrorCode.EMAIL_DELIVERY_NOT_DEAD);
        }

        delivery.requeueDead(LocalDateTime.now());
    }
}
