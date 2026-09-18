package com.recoveryonestop.match.service;

import com.recoveryonestop.match.client.GeminiClassificationClient;
import com.recoveryonestop.match.client.dto.ProgramVerdict;
import com.recoveryonestop.match.domain.BenefitProgram;
import com.recoveryonestop.match.dto.BusinessInfo;
import com.recoveryonestop.match.dto.HouseholdInfo;
import com.recoveryonestop.match.dto.MatchRequest;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 유사도 랭킹까지 끝난 최종 후보들을 한 번의 Gemini 호출로 일괄 판정("가능성 높음"/
 * "확인 필요")하고 추천 이유를 자연어로 받아온다.
 *
 * ⚠️ 2026-09-18(3) 설계 결정: 후보 건당 LLM을 호출하지 않고, 후보 전체(최대 TOP_N=20건)를
 * 프롬프트 하나에 담아 딱 1번만 호출한다. 건당 호출하면 응답이 최대 수십 초까지 느려질 수
 * 있어서(gemini-3.6-flash 실증 호출에서 건당 내부 reasoning 토큰이 1,000개 이상 나옴)
 * 매칭 요청-응답 흐름에 안 맞는다.
 */
@Component
public class LlmClassificationService {

    private static final int PROGRAM_DETAIL_MAX_CHARS = 200;

    private final GeminiClassificationClient client;

    public LlmClassificationService(GeminiClassificationClient client) {
        this.client = client;
    }

    /**
     * @return programId → 판정 결과 맵. 호출/파싱 실패, 혹은 응답에 안 나온 programId는
     *         맵에 없음 — 호출부(MatchService)는 없는 경우 규칙 기반 reason으로
     *         fail-soft 해야 한다.
     */
    public Map<Long, ProgramVerdict> classify(List<BenefitProgram> candidates, MatchRequest request) {
        if (candidates.isEmpty()) {
            return Map.of();
        }

        String prompt = buildPrompt(candidates, request);
        List<ProgramVerdict> results = client.classify(prompt);

        Map<Long, ProgramVerdict> byProgramId = new LinkedHashMap<>();
        for (ProgramVerdict verdict : results) {
            if (verdict.programId() != null) {
                byProgramId.put(verdict.programId(), verdict);
            }
        }
        return byProgramId;
    }

    private String buildPrompt(List<BenefitProgram> candidates, MatchRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("[사용자 피해 상황]\n");
        sb.append(describeSituation(request));
        sb.append("\n\n[후보 제도 목록]\n");

        int i = 1;
        for (BenefitProgram program : candidates) {
            sb.append(i++).append(". programId=").append(program.getId())
                    .append(", ").append(program.getName())
                    .append("(").append(program.getAgency() != null ? program.getAgency() : "기관 미상").append("): ");

            String detail = firstNonBlank(program.getTargetText(), program.getContentText());
            sb.append(detail != null ? truncate(detail, PROGRAM_DETAIL_MAX_CHARS) : "상세 설명 없음");
            sb.append('\n');
        }

        return sb.toString();
    }

    private String describeSituation(MatchRequest request) {
        StringBuilder sb = new StringBuilder();
        appendField(sb, request.damageAddress());
        if (request.disasterType() != null && !request.disasterType().isBlank()) {
            appendField(sb, request.disasterType() + "로 인한 피해");
        }
        if (request.targetType() != null) {
            appendField(sb, "피해유형: " + request.targetType());
        }
        if (request.damageLevel() != null) {
            appendField(sb, "피해정도: " + request.damageLevel());
        }
        if (request.recoveryStatus() != null) {
            appendField(sb, "복구상태: " + request.recoveryStatus());
        }

        HouseholdInfo household = request.householdInfo();
        if (household != null) {
            appendField(sb, "가구: " + household.householdSize() + " / " + household.housingType());
        }

        BusinessInfo business = request.businessInfo();
        if (business != null) {
            appendField(sb, "사업장: " + business.businessType() + " / " + business.operatingStatus());
        }

        return sb.toString();
    }

    private void appendField(StringBuilder sb, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append(", ");
        }
        sb.append(value);
    }

    private String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }

    private String truncate(String text, int maxChars) {
        return text.length() > maxChars ? text.substring(0, maxChars) : text;
    }
}
