package com.recoveryonestop.match.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * 지자체복지서비스 목록조회(LcgvWelfarelist) 1건 — {@code <servList>} 엘리먼트.
 * 실제 API 응답(XML) 샘플로 필드 확정. (2026-09-13, searchWrd=노인 검색 실응답 확인)
 *
 * ⚠️ 중앙부처복지서비스 목록 항목과 필드명이 다르다 — 그대로 재사용하면 안 됨.
 *    - 소관기관: jurMnofNm/jurOrgNm(중앙) 대신 bizChrDeptNm(지자체) 하나만 옴
 *    - 지역: ctpvNm(시도명)/sggNm(시군구명)이 추가로 옴 — 법정동코드 아닌 텍스트
 *    - svcfrstRegTs/onapPsbltYn/rprsCtadr 필드는 지자체 목록에는 없음 (대신 lastModYmd가 옴)
 * ⚠️ lifeNmArray/trgterIndvdlNmArray/intrsThemaNmArray/aplyMtdNm은 값이 없으면
 *    엘리먼트 자체가 응답에서 생략된다 (null 허용 전제로 매핑).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LocalWelfareListItem(
        @JacksonXmlProperty(localName = "servId") String servId,
        @JacksonXmlProperty(localName = "servNm") String servNm,
        @JacksonXmlProperty(localName = "servDgst") String servDgst,
        @JacksonXmlProperty(localName = "bizChrDeptNm") String bizChrDeptNm,     // 담당부서명 (중앙부처의 jurMnofNm 대응)
        @JacksonXmlProperty(localName = "ctpvNm") String ctpvNm,                 // 시도명 (텍스트, 법정동코드 아님)
        @JacksonXmlProperty(localName = "sggNm") String sggNm,                   // 시군구명 (텍스트, 법정동코드 아님)
        @JacksonXmlProperty(localName = "servDtlLink") String servDtlLink,
        @JacksonXmlProperty(localName = "lastModYmd") String lastModYmd,         // 최종수정일 (yyyyMMdd)
        @JacksonXmlProperty(localName = "inqNum") String inqNum,
        @JacksonXmlProperty(localName = "sprtCycNm") String sprtCycNm,
        @JacksonXmlProperty(localName = "srvPvsnNm") String srvPvsnNm,
        @JacksonXmlProperty(localName = "aplyMtdNm") String aplyMtdNm,           // 값 있을 때만 옴
        @JacksonXmlProperty(localName = "lifeNmArray") String lifeNmArray,
        @JacksonXmlProperty(localName = "trgterIndvdlNmArray") String trgterIndvdlNmArray,
        @JacksonXmlProperty(localName = "intrsThemaNmArray") String intrsThemaNmArray
) {
}
