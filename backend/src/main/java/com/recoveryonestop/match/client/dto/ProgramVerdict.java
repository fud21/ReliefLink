package com.recoveryonestop.match.client.dto;

/**
 * Gemini 판정 결과 1건 — 매칭된 제도 하나에 대한 "가능성 높음"/"확인 필요" 판정과
 * 자연어 추천 이유. {@code verdict}는 항상 이 두 값 중 하나로 오도록 generateContent
 * 요청의 responseSchema에서 enum으로 강제했다(실증 확인됨).
 */
public record ProgramVerdict(Long programId, String verdict, String reason) {
}
