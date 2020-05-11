package ru.udya.sharedsession.redis;

import io.lettuce.core.RedisClient;
import ru.udya.sharedsession.config.RedisConfig;
import ru.udya.sharedsession.repository.SharedUserSessionRepository;

public class WebRedisSharedUserSessionRepository
        extends RedisSharedUserSessionRepository
        implements SharedUserSessionRepository {

    public WebRedisSharedUserSessionRepository(RedisConfig redisConfig,
                                               RedisClient redisClient) {
        super(redisConfig, redisClient);
    }
}
