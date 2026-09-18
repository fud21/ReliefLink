package com.recoveryonestop.match.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link com.recoveryonestop.match.domain.BenefitProgram#getDeadlineRule() deadlineRule} 원문을
 * 파싱해서 실제 마감일(deadlineAt)과 D-day를 계산한다.
 *
 * ⚠️ 2026-09-18(2) 설계 결정: {@code GET /api/admin/programs/deadline-rules}로 실제 DB를
 * 확인한 결과, 전체 4,068건 중 deadlineRule이 채워진 건 "피해 발생일로부터 10일 이내"
 * (재난지원금, programId 1) 단 1건뿐이었다. 그래서 지금은 "OOO일 이내"라는 상대 기한
 * 패턴 하나만 정규식으로 인식한다. 이 패턴에 안 맞는 문구(절대 날짜, "예산 소진 시까지" 등)는
 * 아직 실제 데이터에서 본 적이 없으므로 절대 추정해서 처리하지 않고 계산 불가(null)로
 * fail-soft 처리한다 — 나중에 deadlineRule이 더 채워져서 다른 형태가 실제로 나타나면,
 * 그때 그 실제 문구를 보고 패턴을 추가할 것 (이 프로젝트의 "검증 후 코드 작성" 원칙).
 */
@Component
public class DeadlineCalculationService {

    private static final Logger log = LoggerFactory.getLogger(DeadlineCalculationService.class);

    /** "피해 발생일로부터 10일 이내" 같은 문구에서 일수(10)만 뽑아낸다. */
    private static final Pattern RELATIVE_DAYS_PATTERN = Pattern.compile("(\\d+)\\s*일\\s*이내");

    /**
     * @param deadlineRule 제도의 deadlineRule 원문. null/빈 문자열이거나 인식 못 하는
     *                     형태면 결과 자체가 null.
     * @param occurredAt   사용자가 입력한 피해 발생 시각(기준일). null이면 계산 불가.
     * @return 계산된 마감일/D-day. 계산할 수 없으면 null.
     */
    public DeadlineResult calculate(String deadlineRule, LocalDateTime occurredAt) {
        if (deadlineRule == null || deadlineRule.isBlank() || occurredAt == null) {
            return null;
        }

        Matcher matcher = RELATIVE_DAYS_PATTERN.matcher(deadlineRule);
        if (!matcher.find()) {
            log.warn("deadlineRule을 인식하지 못해 D-day 계산을 건너뜀: \"{}\"", deadlineRule);
            return null;
        }

        int days = Integer.parseInt(matcher.group(1));
        LocalDate deadlineAt = occurredAt.toLocalDate().plusDays(days);
        long dDay = ChronoUnit.DAYS.between(LocalDate.now(), deadlineAt);

        return new DeadlineResult(deadlineAt, dDay);
    }

    /**
     * @param deadlineAt 계산된 마감일.
     * @param dDay       오늘 기준 남은 일수. 0이면 오늘이 마감일, 음수면 이미 마감 지남.
     */
    public record DeadlineResult(LocalDate deadlineAt, long dDay) {
    }
}
