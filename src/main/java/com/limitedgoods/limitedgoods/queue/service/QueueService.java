package com.limitedgoods.limitedgoods.queue.service;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.queue.dto.QueueAdmissionResult;
import com.limitedgoods.limitedgoods.product.service.ProductSoldOutCacheService;
import com.limitedgoods.limitedgoods.queue.dto.QueueStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {

    private static final String QUEUE_PREFIX = "queue:product:";
    private static final int    ACTIVE_WINDOW = 50;

    private final RedisTemplate<String, String> redisTemplate;
    private final ProductSoldOutCacheService productSoldOutCacheService;
    private final AdmissionTokenService admissionTokenService;

    /**
     * 대기열 진입
     * 이미 등록된 경우 기존 순번 유지
     */
    public QueueStatusResponse enterQueue(Long userId, Long productId) {
        //SoldOut인 경우 큐 진입 차단
        if (productSoldOutCacheService.isSoldOut(productId)) {
            throw new BusinessException(ErrorCode.QUEUE_SOLD_OUT);
        }

        QueueAdmissionResult result = admissionTokenService
                        .enterQueueAndIssueToken(userId, productId, ACTIVE_WINDOW);

        if (result.admitted()) {
            return QueueStatusResponse.admitted(
                    result.admissionToken()
            );
        }

        return QueueStatusResponse.waiting(
                result.position()
        );
    }

    /**
     * 대기 상태 폴링
     */
    public QueueStatusResponse getStatus(Long userId, Long productId) {
        return enterQueue(
                userId,
                productId
        );
    }

    /**
     * 대기열에서 제거
     * 호출 시점: 주문 생성 완료, 입장 토큰 TTL 만료
     */
    public void removeFromQueue(Long userId, Long productId) {
        redisTemplate.opsForZSet().remove(QUEUE_PREFIX + productId, userId.toString());
        log.info("대기열 제거 userId={}, productId={}", userId, productId);
    }

}
