package com.recoveryonestop.match.domain;

/**
 * 피해 신고 화면(ReportPage)의 targetType 값을 그대로 받기 위한 enum.
 *
 * ⚠️ 2026-09-18: {@link ProgramCategory}와는 의도적으로 다른 enum이다. ProgramCategory에는
 * OTHER가 없고 대신 GENERAL이 있는데, 이름만 보고 "그냥 같은 개념이겠거니" 하고 서로
 * 바꿔 쓰면 안 된다 — GENERAL은 "이 제도는 대상 구분 없이 폭넓게 적용된다"는 제도 쪽
 * 와일드카드이고, 여기 OTHER는 "사용자의 피해가 주택/사업장/농업 중 어디에도 안 속한다"는
 * 신고 쪽 피해 유형이다(자세한 이유는 ProgramCategory 클래스 Javadoc 참고).
 *
 * MatchRequest는 프론트가 보내는 값을 그대로 받을 수 있도록 이 enum을 쓰고, MatchService가
 * 내부적으로 ProgramCategory로 변환한다(OTHER → 카테고리 필터 없음).
 */
public enum DamageTargetType {
    RESIDENTIAL,
    BUSINESS,
    FARM,
    OTHER
}
