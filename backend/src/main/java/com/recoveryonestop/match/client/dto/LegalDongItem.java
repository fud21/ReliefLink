package com.recoveryonestop.match.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 법정동코드 목록조회(getStanReginCdList) 응답의 "row" 배열 원소 1건.
 * (2026-09-16 실응답 확인 — 대구광역시 서구 원대동3가 등 5건 샘플 기준)
 *
 * ⚠️ data.go.kr 문서의 요청/출력 파라미터 표에는 region_cd~locatadd_nm까지만 나와있었지만,
 *    실제 응답에는 locat_order, locat_rm, locathigh_cd, locallow_nm, adpt_de도 더 있었다.
 *    문서만 보고 짰으면 이 필드들을 놓쳤을 것.
 * ⚠️ locatadd_nm은 전체 주소명(예: "대구광역시 서구 원대동3가")이고, locallow_nm은 그중
 *    마지막 단위 이름만(예: "원대동3가")이다. 지자체복지서비스의 regionNameRaw는
 *    "경상남도 사천시"처럼 시도+시군구 2단어 형태라, 매핑 로직에서는 locatadd_nm 쪽을
 *    기준으로 비교해야 할 가능성이 높음 — 매핑 로직 설계 단계에서 재확인할 것.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LegalDongItem(
        @JsonProperty("region_cd") String regionCd,
        @JsonProperty("sido_cd") String sidoCd,
        @JsonProperty("sgg_cd") String sggCd,
        @JsonProperty("umd_cd") String umdCd,
        @JsonProperty("ri_cd") String riCd,
        @JsonProperty("locatjumin_cd") String locatjuminCd,
        @JsonProperty("locatjijuk_cd") String locatjijukCd,
        @JsonProperty("locatadd_nm") String locatAddNm,
        @JsonProperty("locat_order") Integer locatOrder,
        @JsonProperty("locat_rm") String locatRm,
        @JsonProperty("locathigh_cd") String locathighCd,
        @JsonProperty("locallow_nm") String locallowNm,
        @JsonProperty("adpt_de") String adptDe
) {

    /**
     * 시/도 + 시군구 단위 코드인지(읍면동·리 단위가 아닌지) 판별.
     * 지자체 제도 매칭에는 보통 이 단위가 필요함 — 읍면동 단위까지는 복지서비스 쪽
     * regionNameRaw("경상남도 사천시")에 대응하는 정보가 없어서 과할 수 있음.
     */
    public boolean isSggLevel() {
        return sggCd != null && !"000".equals(sggCd)
                && umdCd != null && umdCd.chars().allMatch(c -> c == '0');
    }
}
