package com.recoveryonestop.match.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 매칭된 제도 1건.
 *
 * ⚠️ 2026-09-18(3) 추가: {@code verdict}/{@code reason}은 가능하면
 * {@link com.recoveryonestop.match.service.LlmClassificationService}가 생성한
 * LLM 판정·자연어 추천 이유다. LLM 호출이 실패하면(fail-soft) {@code verdict}는 null이고
 * {@code reason}은 규칙 기반 문구(지역/카테고리 매칭 사유)로 대체된다 — 둘 다 최종
 * 판정이 아니라 사용자가 참고할 수 있도록 돕는 문구다.
 *
 * ⚠️ 2026-09-18(2) 추가: {@code deadlineAt}/{@code dDay}는 {@code deadlineRule} 원문을
 * {@link com.recoveryonestop.match.service.DeadlineCalculationService}로 파싱해서 계산한
 * 값이다. deadlineRule이 없거나 인식 못 하는 형태면 둘 다 null(계산 불가) — 프론트는
 * null일 때 D-day 배지를 아예 표시하지 않아야 한다.
 */
public record MatchedProgram(
        Long programId,
        String name,
        String agency,
        String verdict,
        String reason,
        List<String> requiredDocs,
        String deadlineRule,
        LocalDate deadlineAt,
        Long dDay,
        String sourceUrl
) {
}
