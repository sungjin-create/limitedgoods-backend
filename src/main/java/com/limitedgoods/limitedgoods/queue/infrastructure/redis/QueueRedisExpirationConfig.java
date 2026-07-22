package com.limitedgoods.limitedgoods.queue.infrastructure.redis;

import com.limitedgoods.limitedgoods.queue.listener.AdmissionTokenExpiredListener;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@RequiredArgsConstructor
public class QueueRedisExpirationConfig {

    private final RedisConnectionFactory connectionFactory;
    private final AdmissionTokenExpiredListener admissionTokenExpiredListener;

    @Bean
    public RedisMessageListenerContainer
    queueRedisMessageListenerContainer() {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();

        container.setConnectionFactory(connectionFactory);

        container.addMessageListener(
                admissionTokenExpiredListener,
                new PatternTopic("__keyevent@*__:expired")
        );

        return container;
    }
}