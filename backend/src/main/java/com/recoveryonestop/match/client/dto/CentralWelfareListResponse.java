package com.recoveryonestop.match.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

/**
 * 중앙부처복지서비스 목록조회(getNationalWelfarelist) 실제 응답 루트 — {@code <wantedList>}.
 *
 * data.go.kr 관행적인 envelope(response &gt; header/body &gt; items &gt; item[]) 구조와 달리
 * 평평한 구조이며, {@code <servList>} 엘리먼트가 감싸는 태그 없이 {@code totalCount}만큼 반복된다.
 * 상세조회 응답({@link CentralWelfareDetailItem})도 같은 방식(평평한 구조, envelope 없음).
 *
 * (읽기 전용 DTO라 루트 엘리먼트명을 강제할 필요가 없어 {@code @JacksonXmlRootElement}는 붙이지 않는다.
 *  Jackson은 역직렬화 시 최상위 엘리먼트명을 검증하지 않고 그 자식들만 필드에 매핑한다.)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CentralWelfareListResponse(
        @JacksonXmlProperty(localName = "totalCount") int totalCount,
        @JacksonXmlProperty(localName = "pageNo") int pageNo,
        @JacksonXmlProperty(localName = "numOfRows") int numOfRows,
        @JacksonXmlProperty(localName = "resultCode") String resultCode,
        @JacksonXmlProperty(localName = "resultMessage") String resultMessage,
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "servList")
        List<CentralWelfareListItem> servList
) {

    public boolean isSuccess() {
        return "0".equals(resultCode);
    }
}
