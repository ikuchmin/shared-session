**Development:** In Progress

## Overview

Shared Session AppComponent saves user sessions into external storage (now it is Redis).
It helps you:
- to configure cluster configuration for CUBA.platform apps
- to make SOA or microservices architecture


## Build

```shell script
./gradlew clean assemble distribute 
```


## Installation

1. Build component. Component isn't published to the maven repository. Because you must build it by yourself

2. Using manual installation in Cuba Studio Marketplace to add project to your project

3. Configure basic properties:
    - `ss.redis.host`
    - `ss.redis.port`
    
4. Declare overridden CUBA components as primary in spring.xml configurations 
   
   **Core** *spring.xml:*
   ```xml
   <beans>
      <bean id="cuba_UserSessionManager" class="ru.udya.sharedsession.sys.SharedUserSessionManager" primary="true"/>
      <bean id="cuba_UserSessions" class="ru.udya.sharedsession.security.RedisUserSessions" primary="true"/>
   </beans>
   ```

   **Web** *web-spring.xml:*   
   ```xml
   <bean>
       <bean id="cuba_Connection"
          class="ru.udya.sharedsession.client.SharedSessionConnectionImpl"
          scope="vaadin" primary="true"/>
   </bean>
   ```

## Overridden components

### Core

- cuba_UserSessionManager -> ru.udya.sharedsession.sys.SharedUserSessionManager
- cuba_UserSessions -> ru.udya.sharedsession.security.RedisUserSessions

### Web

cuba_Connection -> ru.udya.sharedsession.client.SharedSessionConnectionImpl

## Documentation

### Redis

#### How we find shared session by CUBA.platform session Id

If you go deep to CUBA Remote Invocation implementation you will find that
CubaRemoteInvocation contains sessionId as UUID. It restricts us to use SharedSessionId
there because SharedSessionId is a String (Redis implementation widely use it).

As you can see RedisSharedUserSessionRepositoryImpl contains findIdByCubaUserSessionId.
The method uses key scan in Redis and because it has complexity O(N) (too slow). It is one
of the reason wrapping RedisSharedUserSessionRepositoryImpl by RedisSharedUserSessionRepositoryCached.

Let see how findIdByCubaUserSessionId works in the Cached version:

1. Finds cuba session id in local cache
2. Finds cuba session id in shared redis cache
3. Use implementation in RedisSharedUserSessionRepositoryImpl


#### RedisSharedTokenStore
The table shows how keys and values formed and store in the redis. A key consists of a key prefix and a key value

| Key. Prefix       | Key. Value                  | Value                       | Calling method                    |
| ----------------- | --------------------------- | --------------------------- | --------------------------------- |
| ACCESS            | OAuth2AccessToken#getValue  | OAuth2AccessToken           | readAccessToken                   |
| AUTH              | OAuth2AccessToken#getValue  | OAuth2Authentication        | readAuthentication                |
| REFRESH_AUTH      | OAuth2RefreshToken#getValue | OAuth2Authentication        | readAuthenticationForRefreshToken |
| REFRESH           | OAuth2RefreshToken#getValue | OAuth2RefreshToken          | readRefreshToken                  |
| ACCESS_TO_REFRESH | OAuth2AccessToken#getValue  | OAuth2RefreshToken#getValue |                                   |            
| REFRESH_TO_ACCESS | OAuth2RefreshToken#getValue | OAuth2AccessToken#getValue  |                                   |