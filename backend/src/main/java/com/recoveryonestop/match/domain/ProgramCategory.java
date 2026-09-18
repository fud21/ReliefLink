package com.recoveryonestop.match.domain;

/**
 * 구비서류 기본 체크리스트를 정하기 위한 단순 분류.
 * 프론트엔드 피해신고 화면의 targetType(RESIDENTIAL/BUSINESS/FARM/OTHER)과 이름을 맞춤
 * (2026-09-16) — 팀원이 만들 damage_report의 subject_type과 그대로 대응시켜서 헷갈리지
 * 않게 하기 위함. "이 제도가 대략 어떤 대상을 위한 것인지" 제도명·내용 텍스트의 키워드로
 * 추정하는 용도다. 정밀한 판정이 아니라 기본 체크리스트를 고르기 위한 참고용 분류이므로,
 * 오분류가 있어도 크게 문제되지 않는다(사용자가 화면에서 최종 확인하는 구조).
 *
 * ⚠️ GENERAL은 프론트의 "OTHER"(기타 재산 피해)로 이름을 맞추지 않았다. 의미가 다르기
 * 때문이다 — GENERAL은 "이 제도는 대상 구분 없이 폭넓게 적용된다"는 와일드카드이고
 * (MatchService에서 subjectType과 무관하게 항상 후보에 포함시키는 용도), 프론트의 OTHER는
 * "사용자의 피해가 주택/사업장/농업 중 어디에도 안 속한다"는 구체적인 피해 유형 하나다.
 * 이름이 같으면 오히려 헷갈린다.
 */
public enum ProgramCategory {
    RESIDENTIAL,  // 주택/주거 관련
    BUSINESS,     // 소상공인/사업장 관련
    FARM,         // 농업/농작물 관련
    GENERAL       // 대상 구분 없이 폭넓게 적용되는 제도 (위 세 카테고리에 안 걸리거나 판단 불가)
}
