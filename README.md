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