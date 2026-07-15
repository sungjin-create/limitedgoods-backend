package com.limitedgoods.limitedgoods.queue.listener;

import com.limitedgoods.limitedgoods.queue.service.QueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdmissionTokenExpiredListener implements MessageListener {

    // 만료 감지 대상: "admission:track:{userId}:{productId}"
    private static final String TRACK_PREFIX = "admission:track:";

    private final QueueService queueService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = new String(message.getBody(), StandardCharsets.UTF_8);

        if (!expiredKey.startsWith(TRACK_PREFIX)) {
            return;
        }

        // "admission:track:{userId}:{productId}" 파싱
        String suffix = expiredKey.substring(TRACK_PREFIX.length());
        String[] parts = suffix.split(":");

        if (parts.length != 2) {
            log.warn("입장 토큰 만료 키 파싱 실패: key={}", expiredKey);
            return;
        }

        try {
            Long userId    = Long.parseLong(parts[0]);
            Long productId = Long.parseLong(parts[1]);
            queueService.removeFromQueue(userId, productId);
        } catch (NumberFormatException e) {
            log.error("입장 토큰 만료 처리 실패: key={}", expiredKey, e);
        }
    }
}