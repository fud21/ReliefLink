package com.recoveryonestop.match.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

/**
 * 지자체복지서비스 목록조회(LcgvWelfarelist) 실제 응답 루트 — {@code <wantedList>}.
 * 중앙부처와 마찬가지로 감싸는 envelope 없이 평평한 구조. (2026-09-13 실응답 확인)
 *
 * ⚠️ resultCode="40"(NO DATA FOUND)은 에러가 아니라 "이 조건으로는 결과 없음"이라는
 *    정상 응답이다. isSuccess()가 false를 돌려주되, 호출부에서 이걸 실패로 로그 찍지
 *    않도록 구분해서 처리할 것 (isNoData() 참고).
 *
 * ⚠️ 실증 확인됨(추정 아님): searchWrd 없이 지역 파라미터(ctpvNm/sggNm)만으로는
 *    목록이 비어서 나온다(resultCode=40). searchWrd가 사실상 필수로 동작한다.
 *    → 전수 적재는 "지역별 순회"가 아니라 "코드표 키워드로 순회 + servId upsert 중복제거"
 *      방식으로 가야 한다. ({@link com.recoveryonestop.match.service.BenefitProgramIngestService} 참고)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LocalWelfareListResponse(
        @JacksonXmlProperty(localName = "totalCount") int totalCount,
        @JacksonXmlProperty(localName = "pageNo") int pageNo,
        @JacksonXmlProperty(localName = "numOfRows") int numOfRows,
        @JacksonXmlProperty(localName = "resultCode") String resultCode,
        @JacksonXmlProperty(localName = "resultMessage") String resultMessage,
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "servList")
        List<LocalWelfareListItem> servList
) {

    public boolean isSuccess() {
        return "0".equals(resultCode);
    }

    /** "NO DATA FOUND" — 에러가 아니라 이 페이지/검색어 조건에 결과가 없다는 정상 응답. */
    public boolean isNoData() {
        return "40".equals(resultCode);
    }
}
