package ru.udya.sharedsession.redis.repository;

import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId;

import java.util.UUID;

public interface RedisSessionIdMappingRepository {

    String KEY_PREFIX = "shared:session-id-mapping";

    String KEY_PATTERN = KEY_PREFIX + ":" + "%s";

    RedisSharedUserSessionId findSharedIdByCubaSessionId(UUID cubaUserSessionId);

    void createSessionIdMapping(UUID cubaUserSessionId, RedisSharedUserSessionId redisSharedUserSessionId);
}
