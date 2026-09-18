package com.recoveryonestop.match.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml 예시:
 *
 * legaldong:
 *   service-key: ${LEGALDONG_SERVICE_KEY}   # data.go.kr 15077871 전용 인증키(Encoding). bokjiro 쪽 키와 완전히 별개
 *   base-url: https://apis.data.go.kr/1741000/StanReginCd
 *   num-of-rows: 1000
 *
 * ⚠️ 행정안전부_행정표준코드_법정동코드(15077871)는 복지서비스 API들(B554287, 한국사회보장정보원)과
 *    제공기관 자체가 다른 API(1741000, 행정안전부 표준코드센터)라서 인증키/End Point가 bokjiro.*
 *    설정과 완전히 별개다. 절대 bokjiro.service-key를 재사용하면 안 된다.
 */
@ConfigurationProperties(prefix = "legaldong")
public class LegalDongProperties {

    private String serviceKey = "";
    private String baseUrl = "";
    private int numOfRows = 1000;

    public String getServiceKey() {
        return serviceKey;
    }

    public void setServiceKey(String serviceKey) {
        this.serviceKey = serviceKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getNumOfRows() {
        return numOfRows;
    }

    public void setNumOfRows(int numOfRows) {
        this.numOfRows = numOfRows;
    }
}
