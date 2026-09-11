package com.recoveryonestop.match.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml 예시:
 *
 * bokjiro:
 *   service-key: ${BOKJIRO_SERVICE_KEY}   # data.go.kr Decoding 키. 절대 재인코딩하지 말 것
 *   base-url: http://apis.data.go.kr/B554287/NationalWelfareInformationsV001
 *   local-base-url: http://apis.data.go.kr/B554287/LocalGovernmentWelfareInformations
 *   num-of-rows: 100
 */
@ConfigurationProperties(prefix = "bokjiro")
public class BokjiroProperties {

    private String serviceKey = "";
    private String baseUrl = "";
    private String localBaseUrl = "";
    private int numOfRows = 100;

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

    public String getLocalBaseUrl() {
        return localBaseUrl;
    }

    public void setLocalBaseUrl(String localBaseUrl) {
        this.localBaseUrl = localBaseUrl;
    }

    public int getNumOfRows() {
        return numOfRows;
    }

    public void setNumOfRows(int numOfRows) {
        this.numOfRows = numOfRows;
    }
}
