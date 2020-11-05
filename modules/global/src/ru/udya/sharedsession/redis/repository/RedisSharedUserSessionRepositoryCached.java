package ru.udya.sharedsession.redis.repository;

import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.entity.contracts.Ids;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSession;
import ru.udya.sharedsession.redis.domain.RedisSharedUserSessionId;
import ru.udya.sharedsession.repository.SharedUserSessionRepository;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Primary
@Component(SharedUserSessionRepository.NAME)
public class RedisSharedUserSessionRepositoryCached
        implements RedisSharedUserSessionRepository {

    protected RedisSharedUserSessionRepositoryImpl redisSharedUserSessionRepositoryImpl;

    public RedisSharedUserSessionRepositoryCached(
            RedisSharedUserSessionRepositoryImpl redisSharedUserSessionRepositoryImpl) {

        this.redisSharedUserSessionRepositoryImpl = redisSharedUserSessionRepositoryImpl;
    }

    @Override
    public RedisSharedUserSession findById(RedisSharedUserSessionId sharedId) {
        return redisSharedUserSessionRepositoryImpl.findById(sharedId);
    }

    @Override
    public List<RedisSharedUserSession> findAllByUser(Id<User, UUID> userId) {
        return redisSharedUserSessionRepositoryImpl.findAllByUser(userId);
    }

    @Override
    public List<RedisSharedUserSessionId> findAllKeysByUser(Id<User, UUID> userId) {
        return redisSharedUserSessionRepositoryImpl.findAllKeysByUser(userId);
    }

    @Override
    public List<RedisSharedUserSessionId> findAllKeysByUsers(Ids<User, UUID> userId) {
        return null;
    }

    @Override
    public RedisSharedUserSession createByCubaUserSession(UserSession cubaUserSession) {
        return redisSharedUserSessionRepositoryImpl.createByCubaUserSession(cubaUserSession);
    }

    @Override
    public void save(RedisSharedUserSession sharedUserSession) {
        redisSharedUserSessionRepositoryImpl.save(sharedUserSession);
    }

    @Override
    public RedisSharedUserSession updateByFn(RedisSharedUserSessionId redisSharedUserSessionId,
                                             Consumer<RedisSharedUserSession> updateFn) {
        return redisSharedUserSessionRepositoryImpl.updateByFn(redisSharedUserSessionId, updateFn);
    }
}
