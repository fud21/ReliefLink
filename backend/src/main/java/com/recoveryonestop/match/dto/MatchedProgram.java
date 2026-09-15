package com.recoveryonestop.match.dto;

import java.util.List;

/**
 * 매칭된 제도 1건. {@code reason}은 왜 이 제도가 후보에 포함됐는지에 대한 간단한 설명이며,
 * 정밀한 판정이 아니라 사용자가 화면에서 참고할 수 있도록 돕는 문구다.
 */
public record MatchedProgram(
        Long programId,
        String name,
        String agency,
        String reason,
        List<String> requiredDocs,
        String deadlineRule,
        String sourceUrl
) {
}
