package com.recoveryonestop.match.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * ⚠️ 2026-09-18(4) 수정: bokjiroRestClient에 명시적 타임아웃이 전혀 없어서, 콘솔 로그에서
 * 확인된 io.netty.handler.timeout.ReadTimeoutException(Gemini 판정 호출 100% 실패)의
 * 원인이었다 — 기존 코드는 어떤 ClientHttpRequestFactory를 쓰는지조차 명시하지 않고
 * RestClient.builder().build()의 기본값(내부적으로 reactor-netty를 골라 쓰고 있었음,
 * 로그의 r.netty.http.client.HttpClientConnect로 확인됨)에 맡기고 있었는데, 그 기본
 * read timeout이 gemini-3.6-flash의 무거운 reasoning("thinking") 응답 시간보다 짧았다.
 *
 * 고쳐서 JdkClientHttpRequestFactory(java.net.http.HttpClient 기반, Spring 6.1+ 표준
 * 컴포넌트, 별도 의존성 불필요)를 명시적으로 구성하고 connect/read 타임아웃을 넉넉하게
 * 잡았다. 이 빈은 GeminiEmbeddingClient/GeminiClassificationClient뿐 아니라 중앙부처·
 * 지자체 복지 API 호출에도 공유되는데, 타임아웃을 "늘리는" 방향의 수정이라 기존 호출들이
 * 더 빨리 실패하게 되는 회귀는 없다(오히려 더 관대해짐) — 진짜 응답이 없는 경우에도
 * readTimeout(90초) 안에는 여전히 실패로 확정되어 fail-soft 로직이 정상 동작한다.
 *
 * readTimeout=90초는 실증 측정값이 아니라 보수적으로 잡은 값이다(Postman 실증 호출에서
 * usageMetadata.thoughtsTokenCount가 3건 판정에 1,140까지 나왔던 것을 감안해, 실제
 * 배치(최대 20건)가 더 오래 걸릴 수 있다고 보고 여유를 크게 뒀다). 재현 시 90초로도
 * 부족하면 늘려야 한다는 신호이니 콘솔 로그로 확인할 것.
 */
@Configuration
@EnableConfigurationProperties({BokjiroProperties.class, LegalDongProperties.class, GeminiProperties.class})
public class RestClientConfig {

    @Bean
    public RestClient bokjiroRestClient() {
        // baseUrl은 여기 고정하지 않는다. 중앙부처/지자체 URL이 다르므로 호출부에서 전체 URI를 조립해서 넘긴다.
        // 목록조회(callTp=L)는 XML로 응답하고, 상세조회(callTp=D, type=json)는 아직 JSON을 쓰고 있으므로
        // 기본 컨버터(JSON 포함)는 그대로 등록하고 XML 컨버터를 추가한다.
        // (Spring Framework 7 / Jackson 3 기준 JacksonXmlHttpMessageConverter 사용.
        //  옛 MappingJackson2XmlHttpMessageConverter는 제거 예정(deprecated for removal))
        HttpClient jdkHttpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(jdkHttpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(90));

        return RestClient.builder()
                .requestFactory(requestFactory)
                .configureMessageConverters(converters -> converters
                        .registerDefaults()
                        .withXmlConverter(new JacksonXmlHttpMessageConverter()))
                .build();
    }
}
