package com.recoveryonestop.match.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 복지서비스 목록조회 1건.
 * servId / servNm은 복지로 실서비스 URL(bokjiro.go.kr ... servId=...&servNm=...)에서 확인된 필드명.
 * 나머지(jurMnofNm 등)는 data.go.kr 유사 API의 관행적 명명이므로 ⚠️ Swagger 승인 후 재검증 필요.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CentralWelfareListItem(
        @JsonProperty("servId") String servId,
        @JsonProperty("servNm") String servNm,
        @JsonProperty("servDgst") String servDgst,       // 서비스 요약
        @JsonProperty("jurMnofNm") String jurMnofNm,     // 소관부처명
        @JsonProperty("jurOrgNm") String jurOrgNm,       // 소관기관명
        @JsonProperty("lastModYmd") String lastModYmd    // yyyyMMdd
) {
}
