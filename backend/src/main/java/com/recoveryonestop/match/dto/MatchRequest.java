package com.recoveryonestop.match.dto;

import com.recoveryonestop.match.domain.ProgramCategory;

/**
 * POST /api/match 요청.
 *
 * @param regionCode   법정동코드. null이면 전국 대상 제도만 후보가 된다
 *                      ({@link com.recoveryonestop.match.domain.BenefitProgramRepository#findByRegionCodeIsNullOrRegionCode}).
 * @param disasterType 재난유형. 매칭 필터링에는 쓰지 않는 참고용 값(응답의 reason에 표시됨).
 * @param subjectType  신청 주체 유형. null이면 카테고리 필터링 없이 지역 후보군 전체를 반환한다.
 */
public record MatchRequest(
        String regionCode,
        String disasterType,
        ProgramCategory subjectType
) {
}
