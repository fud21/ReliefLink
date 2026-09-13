package com.recoveryonestop.match.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(BokjiroProperties.class)
public class RestClientConfig {

    @Bean
    public RestClient bokjiroRestClient() {
        // baseUrl은 여기 고정하지 않는다. 중앙부처/지자체 URL이 다르므로 호출부에서 전체 URI를 조립해서 넘긴다.
        // 목록조회(callTp=L)는 XML로 응답하고, 상세조회(callTp=D, type=json)는 아직 JSON을 쓰고 있으므로
        // 기본 컨버터(JSON 포함)는 그대로 등록하고 XML 컨버터를 추가한다.
        // (Spring Framework 7 / Jackson 3 기준 JacksonXmlHttpMessageConverter 사용.
        //  옛 MappingJackson2XmlHttpMessageConverter는 제거 예정(deprecated for removal))
        return RestClient.builder()
                .configureMessageConverters(converters -> converters
                        .registerDefaults()
                        .withXmlConverter(new JacksonXmlHttpMessageConverter()))
                .build();
    }
}
