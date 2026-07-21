package com.limitedgoods.limitedgoods.common.config;

import com.limitedgoods.limitedgoods.queue.listener.AdmissionTokenExpiredListener;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@RequiredArgsConstructor
public class RedisExpirationConfig {

    private final RedisConnectionFactory connectionFactory;
    private final AdmissionTokenExpiredListener admissionTokenExpiredListener;

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
                admissionTokenExpiredListener,
                new PatternTopic("__keyevent@0__:expired")
        );
        return container;
    }
}
