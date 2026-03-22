package com.ainovel.infrastructure.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RedissonProperties.class)
public class RedissonConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RedissonClient redissonClient(RedissonProperties properties) {
        Config config = new Config();
        String address = Objects.requireNonNull(properties.address(), "ainovel.redisson.address must be configured");
        var server = config.useSingleServer()
                .setAddress(address)
                .setDatabase(properties.database())
                .setConnectTimeout((int) properties.connectTimeout().toMillis())
                .setTimeout((int) properties.timeout().toMillis());

        if (properties.username() != null && !properties.username().isBlank()) {
            server.setUsername(properties.username());
        }
        if (properties.password() != null && !properties.password().isBlank()) {
            server.setPassword(properties.password());
        }

        return Redisson.create(config);
    }
}
