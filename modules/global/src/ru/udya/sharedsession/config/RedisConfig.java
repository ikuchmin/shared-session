package ru.udya.sharedsession.config;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Property;
import com.haulmont.cuba.core.config.Source;
import com.haulmont.cuba.core.config.SourceType;
import com.haulmont.cuba.core.config.defaults.DefaultInteger;
import com.haulmont.cuba.core.config.defaults.DefaultString;

@Source(type = SourceType.APP)
public interface RedisConfig extends Config {

    @Property("ss.redis.uri")
    @DefaultString("redis://localhost/0")
    String getRedisUri();

    @Property("ss.redis.host")
    @DefaultString("redis")
    String getRedisHost();

    @Property("ss.redis.port")
    @DefaultInteger(6379)
    Integer getRedisPort();

    @Property("ss.redis.password")
    @DefaultString("")
    String getRedisPassword();

    @Property("ss.redis.timeout")
    @DefaultInteger(10000)
    Integer getRedisTimeout();
}
