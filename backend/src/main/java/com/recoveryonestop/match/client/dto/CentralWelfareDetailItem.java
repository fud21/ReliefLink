package com.recoveryonestop.match.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 복지서비스 상세조회 1건. 목록조회의 servId로 재호출해서 얻는다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CentralWelfareDetailItem(
        @JsonProperty("servId") String servId,
        @JsonProperty("servNm") String servNm,
        @JsonProperty("tgtrDtlCn") String targetDetailContent,       // 지원대상 상세
        @JsonProperty("slctCritCn") String selectionCriteriaContent, // 선정기준
        @JsonProperty("alwServCn") String benefitContent,            // 지원내용
        @JsonProperty("aplyMtdCn") String applyMethodContent,        // 신청방법
        @JsonProperty("wlfareInfoOutlookUrl") String detailUrl
) {
}
