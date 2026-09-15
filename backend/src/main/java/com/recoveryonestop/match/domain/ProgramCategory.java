package com.recoveryonestop.match.domain;

/**
 * 구비서류 기본 체크리스트를 정하기 위한 단순 분류.
 * damage_case의 subject_type(주택/사업장/농작물)과는 별개로, "이 제도가 대략 어떤 대상을
 * 위한 것인지" 제도명·내용 텍스트의 키워드로 추정하는 용도다. 정밀한 판정이 아니라
 * 기본 체크리스트를 고르기 위한 참고용 분류이므로, 오분류가 있어도 크게 문제되지 않는다
 * (사용자가 화면에서 최종 확인하는 구조).
 */
public enum ProgramCategory {
    HOUSING,      // 주택/주거 관련
    BUSINESS,     // 소상공인/사업장 관련
    AGRICULTURE,  // 농업/농작물 관련
    GENERAL       // 위에 해당 안 되거나 판단 불가
}
