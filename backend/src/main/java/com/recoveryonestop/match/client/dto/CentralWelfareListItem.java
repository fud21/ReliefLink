package com.recoveryonestop.match.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * 복지서비스 목록조회(getNationalWelfarelist) 1건 — {@code <servList>} 엘리먼트.
 * 실제 API 응답(XML) 샘플로 필드 확정. (2026-09-13 실응답 확인)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CentralWelfareListItem(
        @JacksonXmlProperty(localName = "servId") String servId,
        @JacksonXmlProperty(localName = "servNm") String servNm,
        @JacksonXmlProperty(localName = "servDgst") String servDgst,                   // 서비스 요약
        @JacksonXmlProperty(localName = "jurMnofNm") String jurMnofNm,                 // 소관부처명
        @JacksonXmlProperty(localName = "jurOrgNm") String jurOrgNm,                   // 소관기관명
        @JacksonXmlProperty(localName = "servDtlLink") String servDtlLink,             // 상세페이지 URL (목록조회에서 바로 확보)
        @JacksonXmlProperty(localName = "svcfrstRegTs") String svcfrstRegTs,           // 최초등록일시 (yyyyMMdd)
        @JacksonXmlProperty(localName = "inqNum") String inqNum,                       // 조회수
        @JacksonXmlProperty(localName = "sprtCycNm") String sprtCycNm,                 // 지원주기명 (예: "1회성")
        @JacksonXmlProperty(localName = "srvPvsnNm") String srvPvsnNm,                 // 서비스제공방법명 (예: "전자바우처(바우처)")
        @JacksonXmlProperty(localName = "onapPsbltYn") String onapPsbltYn,             // 온라인신청 가능여부 (Y/N)
        @JacksonXmlProperty(localName = "rprsCtadr") String rprsCtadr,                 // 대표문의처
        @JacksonXmlProperty(localName = "lifeArray") String lifeArray,                 // 생애주기 (콤마 구분 문자열)
        @JacksonXmlProperty(localName = "trgterIndvdlArray") String trgterIndvdlArray, // 대상특성 (콤마 구분 문자열)
        @JacksonXmlProperty(localName = "intrsThemaArray") String intrsThemaArray      // 관심주제 (콤마 구분 문자열)
) {
}
