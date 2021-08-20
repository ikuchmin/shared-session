package ru.udya.sharedsession.redis.repository;

import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId;

import java.util.UUID;

public interface RedisCubaUserSessionIdOnSharedUserSessionIdMappingRepository {

    RedisSharedUserSessionId findRedisSharedUserSessionIdByCubaUserSessionId(UUID cubaUserSessionId);

    void createCubaUserSessionIdOnSharedUserSessionIdMapping(UUID cubaUserSessionId, RedisSharedUserSessionId redisSharedUserSessionId);
}
