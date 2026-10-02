package com.recoveryonestop.match.disaster.dto;

import java.util.List;

/**
 * 홈 화면 재난 알림/지도에서 사용하는 ReliefLink용 DTO.
 * 원본 재난문자 API 필드명을 그대로 프론트에 노출하지 않고 필요한 값만 정규화한다.
 *
 * latitude/longitude는 원본 긴급재난문자 API에 없으므로 포함하지 않는다.
 * 프론트 Kakao Maps SDK에서 region을 주소 검색해 지도 마커를 생성한다.
 *
 * regionCodes는 region(원문 수신 지역 텍스트, 예: "서울특별시 강남구,서초구")을
 * AddressToRegionCodeService.resolveMultiple()로 법정동코드 목록으로 변환한 결과다.
 * 못 찾은 지역은 추측하지 않고 조용히 빠지며, 전부 못 찾으면 빈 리스트가 된다
 * (null이 아님 — 프론트에서 null 체크 없이 바로 써도 된다).
 */
public record DisasterAlertResponse(
        String id,
        String type,
        String region,
        String message,
        String emergencyLevel,
        String occurredAt,
        String regionId,
        String disasterTypeId,
        String emergencyStepId,
        List<String> regionCodes
) {
}
