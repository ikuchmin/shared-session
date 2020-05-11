package ru.udya.sharedsession.redis;

import io.lettuce.core.RedisClient;
import ru.udya.sharedsession.repository.SharedUserSessionRepository;

public class WebRedisSharedUserSessionRepository
        extends RedisSharedUserSessionRepository
        implements SharedUserSessionRepository {

    public WebRedisSharedUserSessionRepository(RedisClient redisClient) {
        super(redisClient);
    }
}
