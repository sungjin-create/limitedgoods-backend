package com.limitedgoods.limitedgoods.queue.service;

import com.limitedgoods.limitedgoods.common.exception.BusinessException;
import com.limitedgoods.limitedgoods.common.exception.ErrorCode;
import com.limitedgoods.limitedgoods.order.service.SoldOutCacheService;
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
    private final SoldOutCacheService soldOutCacheService;
    private final AdmissionTokenService admissionTokenService;

    /**
     * 대기열 진입
     * 이미 등록된 경우 기존 순번 유지
     */
    public QueueStatusResponse enter(Long userId, Long productId) {
        //SoldOut인 경우 큐 진입 x
        if (soldOutCacheService.isSoldOut(productId)) {
            throw new BusinessException(ErrorCode.QUEUE_SOLD_OUT);
        }

        String queueKey = QUEUE_PREFIX + productId;

        // NX 옵션: 이미 있으면 기존 score(순번) 유지
        redisTemplate.opsForZSet().addIfAbsent(
                queueKey,
                userId.toString(),
                System.currentTimeMillis()
        );

        return checkAndIssue(userId, productId, queueKey);
    }

    /**
     * 대기 상태 폴링
     */
    public QueueStatusResponse getStatus(Long userId, Long productId) {
        String queueKey = QUEUE_PREFIX + productId;
        Long rank = redisTemplate.opsForZSet().rank(queueKey, userId.toString());

        if (rank == null) {
            // 대기열에 없으면 재진입 처리
            return enter(userId, productId);
        }

        return checkAndIssue(userId, productId, queueKey);
    }

    /**
     * 대기열에서 제거
     * 호출 시점: 주문 생성 완료, 입장 토큰 TTL 만료
     */
    public void removeFromQueue(Long userId, Long productId) {
        redisTemplate.opsForZSet().remove(QUEUE_PREFIX + productId, userId.toString());
        log.info("대기열 제거 userId={}, productId={}", userId, productId);
    }

    private QueueStatusResponse checkAndIssue(Long userId, Long productId, String queueKey) {
        Long rank = redisTemplate.opsForZSet().rank(queueKey, userId.toString());

        if (rank == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        if (rank < ACTIVE_WINDOW) {
            // rank 0~49 → 입장 가능
            String token = admissionTokenService.issueToken(userId, productId);
            return QueueStatusResponse.admitted(token);
        }

        // rank 50 이상 → 대기 (1-based 위치로 변환)
        int position = (int) (rank - ACTIVE_WINDOW + 1);
        return QueueStatusResponse.waiting(position);
    }
}
