package com.recoveryonestop.match.service;

import com.recoveryonestop.match.domain.BenefitProgram;
import com.recoveryonestop.match.domain.ProgramCategory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 제도명/지원대상 텍스트의 키워드로 대략적인 카테고리를 추정하고,
 * 카테고리별 기본 구비서류 체크리스트를 돌려준다.
 *
 * ⚠️ 이건 정밀한 판정이 아니라 "대충 이 정도는 보통 필요하다"는 기본값이다.
 * 실제 서비스에서는 화면에 "AI/규칙 기반 추정 목록이며 실제 제출 서류는 다를 수 있습니다"
 * 같은 안내를 같이 보여주는 걸 권장한다.
 */
@Component
public class DefaultRequiredDocsProvider {

    private static final List<String> HOUSING_DOCS = List.of(
            "신분증 사본",
            "주민등록등본",
            "피해사실확인서",
            "거주 확인 서류 (임대차계약서 등)"
    );

    private static final List<String> BUSINESS_DOCS = List.of(
            "신분증 사본",
            "사업자등록증",
            "피해사실확인서",
            "통장 사본"
    );

    private static final List<String> AGRICULTURE_DOCS = List.of(
            "신분증 사본",
            "농지원부 또는 농업경영체 등록확인서",
            "피해사실확인서",
            "통장 사본"
    );

    private static final List<String> GENERAL_DOCS = List.of(
            "신분증 사본",
            "피해사실확인서",
            "통장 사본"
    );

    private static final Map<ProgramCategory, List<String>> DOCS_BY_CATEGORY = Map.of(
            ProgramCategory.HOUSING, HOUSING_DOCS,
            ProgramCategory.BUSINESS, BUSINESS_DOCS,
            ProgramCategory.AGRICULTURE, AGRICULTURE_DOCS,
            ProgramCategory.GENERAL, GENERAL_DOCS
    );

    // 순서가 중요하다 — 사업장/농업 키워드가 더 구체적이므로 먼저 검사하고, 주택은 마지막에 검사한다.
    // (예: "농업인 주택개량 지원"처럼 여러 키워드가 겹치는 제도명 대비)
    private static final List<String> BUSINESS_KEYWORDS = List.of(
            "소상공인", "사업자", "사업장", "중소기업", "자영업", "상공인"
    );

    private static final List<String> AGRICULTURE_KEYWORDS = List.of(
            "농업", "농작물", "농지", "농가", "축산", "어업", "어가"
    );

    private static final List<String> HOUSING_KEYWORDS = List.of(
            "주택", "주거", "가구", "임대", "전세", "월세"
    );

    public ProgramCategory classify(BenefitProgram program) {
        String haystack = String.join(" ",
                nullSafe(program.getName()),
                nullSafe(program.getTargetText()),
                nullSafe(program.getAgency())
        );

        if (containsAny(haystack, BUSINESS_KEYWORDS)) {
            return ProgramCategory.BUSINESS;
        }
        if (containsAny(haystack, AGRICULTURE_KEYWORDS)) {
            return ProgramCategory.AGRICULTURE;
        }
        if (containsAny(haystack, HOUSING_KEYWORDS)) {
            return ProgramCategory.HOUSING;
        }
        return ProgramCategory.GENERAL;
    }

    public List<String> getDefaultDocs(ProgramCategory category) {
        return DOCS_BY_CATEGORY.getOrDefault(category, GENERAL_DOCS);
    }

    private boolean containsAny(String haystack, List<String> keywords) {
        return keywords.stream().anyMatch(haystack::contains);
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
