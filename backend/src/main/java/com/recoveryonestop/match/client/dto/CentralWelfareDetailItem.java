package com.recoveryonestop.match.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

/**
 * 복지서비스 상세조회(getNationalWelfaredetailed) 실제 응답 루트 — {@code <wantedDtl>}.
 * 목록조회와 마찬가지로 감싸는 envelope 없이 평평한 구조. (2026-09-13 실응답 확인)
 *
 * basfrmList, inqplCtadrList, inqplHmpgReldList, baslawList 등은 당장 안 써서 매핑하지 않는다.
 * (Jackson XML은 매핑되지 않은 엘리먼트는 그냥 무시하므로 에러 없음)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CentralWelfareDetailItem(
        @JacksonXmlProperty(localName = "servId") String servId,
        @JacksonXmlProperty(localName = "servNm") String servNm,
        @JacksonXmlProperty(localName = "jurMnofNm") String jurMnofNm,                       // 소관부처명
        @JacksonXmlProperty(localName = "tgtrDtlCn") String targetDetailContent,             // 지원대상 상세
        @JacksonXmlProperty(localName = "slctCritCn") String selectionCriteriaContent,       // 선정기준
        @JacksonXmlProperty(localName = "alwServCn") String benefitContent,                  // 지원내용
        @JacksonXmlProperty(localName = "wlfareInfoOutlCn") String summaryContent,           // 복지정보 요약
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "applmetList")
        List<ApplyMethodEntry> applmetList,                                                  // 신청/조사/결정/지급/사후관리 기관 목록
        @JacksonXmlProperty(localName = "resultCode") String resultCode,
        @JacksonXmlProperty(localName = "resultMessage") String resultMessage
) {

    public boolean isSuccess() {
        return "0".equals(resultCode);
    }

    /**
     * {@code <applmetList>} 1건. 신청기관/조사기관/결정기관/지급기관/사후관리기관 등이
     * servSeCode로 구분되어 여러 건 반복된다.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApplyMethodEntry(
            @JacksonXmlProperty(localName = "servSeCode") String servSeCode,
            @JacksonXmlProperty(localName = "servSeDetailLink") String servSeDetailLink,
            @JacksonXmlProperty(localName = "servSeDetailNm") String servSeDetailNm
    ) {
    }
}
