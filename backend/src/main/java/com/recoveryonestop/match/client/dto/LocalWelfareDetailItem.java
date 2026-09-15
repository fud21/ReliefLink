package com.recoveryonestop.match.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

/**
 * 지자체복지서비스 상세조회(LcgvWelfaredetailed) 실제 응답 루트 — {@code <wantedDtl>}.
 * 감싸는 envelope 없이 평평한 구조. (2026-09-13, servId=WLF00001138 실응답 확인)
 *
 * ⚠️ 중앙부처 상세조회와 필드명이 다르다 — 그대로 재사용하면 안 됨.
 *    - 지원대상: tgtrDtlCn(중앙) 대신 sprtTrgtCn(지자체)
 *    - 신청방법: applmetList 반복 리스트(중앙) 대신 aplyMtdCn 단일 텍스트(지자체)
 * ⚠️ ctpvNm/sggNm이 비어있으면 null이 아니라 문자열 "-"로 온다(전국 단위 제도도
 *    지자체 API로 조회될 수 있음 확인됨). "-"는 파싱 시 "전국"(null)으로 취급할 것.
 * ⚠️ enfcBgngYmd/enfcEndYmd는 신청 마감일이 아니라 "제도 시행 시작/종료일"이다.
 *    enfcEndYmd="99991231"이면 "종료일 미정(무기한)"이라는 관행적 표기.
 * ⚠️ inqplCtadrList/inqplHmpgReldList/baslawList/basfrmList는 전부 같은 3필드
 *    (wlfareInfoDtlCd/wlfareInfoReldCn/wlfareInfoReldNm)를 감싸는 태그만 다르게
 *    반복하는 구조라 {@link InfoLinkEntry} 하나로 공용 매핑한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LocalWelfareDetailItem(
        @JacksonXmlProperty(localName = "resultCode") String resultCode,
        @JacksonXmlProperty(localName = "resultMessage") String resultMessage,
        @JacksonXmlProperty(localName = "servId") String servId,
        @JacksonXmlProperty(localName = "servNm") String servNm,
        @JacksonXmlProperty(localName = "enfcBgngYmd") String enfcBgngYmd,
        @JacksonXmlProperty(localName = "enfcEndYmd") String enfcEndYmd,
        @JacksonXmlProperty(localName = "bizChrDeptNm") String bizChrDeptNm,
        @JacksonXmlProperty(localName = "ctpvNm") String ctpvNm,
        @JacksonXmlProperty(localName = "sggNm") String sggNm,
        @JacksonXmlProperty(localName = "servDgst") String servDgst,
        @JacksonXmlProperty(localName = "sprtTrgtCn") String sprtTrgtCn,       // 지원대상 상세 (중앙부처의 tgtrDtlCn 대응)
        @JacksonXmlProperty(localName = "slctCritCn") String slctCritCn,       // 선정기준
        @JacksonXmlProperty(localName = "alwServCn") String alwServCn,         // 지원내용
        @JacksonXmlProperty(localName = "aplyMtdCn") String aplyMtdCn,         // 신청방법 (단일 텍스트, 중앙부처와 다름)
        @JacksonXmlProperty(localName = "inqNum") String inqNum,
        @JacksonXmlProperty(localName = "lastModYmd") String lastModYmd,
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "inqplCtadrList")
        List<InfoLinkEntry> inqplCtadrList,
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "inqplHmpgReldList")
        List<InfoLinkEntry> inqplHmpgReldList,
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "baslawList")
        List<InfoLinkEntry> baslawList,
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "basfrmList")
        List<InfoLinkEntry> basfrmList
) {

    public boolean isSuccess() {
        return "0".equals(resultCode);
    }

    /** 문의처/홈페이지/근거법령/서식파일 목록에 공통으로 쓰이는 3필드 구조. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InfoLinkEntry(
            @JacksonXmlProperty(localName = "wlfareInfoDtlCd") String wlfareInfoDtlCd,
            @JacksonXmlProperty(localName = "wlfareInfoReldCn") String wlfareInfoReldCn,
            @JacksonXmlProperty(localName = "wlfareInfoReldNm") String wlfareInfoReldNm
    ) {
    }
}
