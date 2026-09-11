package com.recoveryonestop.match.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(BokjiroProperties.class)
public class RestClientConfig {

    @Bean
    public RestClient bokjiroRestClient() {
        // baseUrl은 여기 고정하지 않는다. 중앙부처/지자체 URL이 다르므로 호출부에서 전체 URI를 조립해서 넘긴다.
        return RestClient.builder().build();
    }
}
