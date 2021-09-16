package ru.udya.sharedsession.configuration;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.udya.sharedsession.config.RedisConfig;

import java.time.Duration;

@Configuration
public class SharedUserSessionConfiguration {

    @Bean(destroyMethod = "shutdown")
    public ClientResources clientResources() {
        return DefaultClientResources.create();
    }

    @Bean(destroyMethod = "shutdown")
    public RedisClient redisClient(ClientResources clientResources, RedisConfig redisConfig) {

        ClientOptions resp3 = ClientOptions.builder()
                .protocolVersion(ProtocolVersion.RESP3).build();

        var redisUri= new RedisURI(redisConfig.getRedisHost(),
                redisConfig.getRedisPort(),
                Duration.ofMillis(redisConfig.getRedisTimeout()));

        RedisClient redisClient = RedisClient.create(clientResources, redisUri);

        redisClient.setOptions(resp3);

        return redisClient;
    }
}
