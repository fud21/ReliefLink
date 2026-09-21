package com.recoveryonestop.match.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 재난안전데이터공유플랫폼 행정안전부 긴급재난문자 API 설정.
 *
 * application.yml:
 * safetydata:
 *   service-key: ${SAFETY_DATA_SERVICE_KEY}
 *   base-url: https://www.safetydata.go.kr/V2/api/DSSP-IF-00247
 *   num-of-rows: 50
 */
@ConfigurationProperties(prefix = "safetydata")
public class SafetyDataProperties {

    private String serviceKey = "";
    private String baseUrl = "";
    private int numOfRows = 50;

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
