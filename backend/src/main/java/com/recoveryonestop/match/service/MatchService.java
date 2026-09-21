package com.recoveryonestop.match.service;

import com.recoveryonestop.match.client.GeminiEmbeddingClient;
import com.recoveryonestop.match.client.dto.ProgramVerdict;
import com.recoveryonestop.match.domain.BenefitProgram;
import com.recoveryonestop.match.domain.BenefitProgramRepository;
import com.recoveryonestop.match.domain.BusinessDamageItem;
import com.recoveryonestop.match.domain.DamageLevel;
import com.recoveryonestop.match.domain.DamageTargetType;
import com.recoveryonestop.match.domain.HouseholdSize;
import com.recoveryonestop.match.domain.HousingType;
import com.recoveryonestop.match.domain.OperatingStatus;
import com.recoveryonestop.match.domain.ProgramCategory;
import com.recoveryonestop.match.domain.RecoveryStatus;
import com.recoveryonestop.match.dto.BusinessInfo;
import com.recoveryonestop.match.dto.HouseholdInfo;
import com.recoveryonestop.match.dto.MatchRequest;
import com.recoveryonestop.match.dto.MatchResponse;
import com.recoveryonestop.match.dto.MatchedProgram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 제도 매칭.
 * ① regionCode(없으면 damageAddress로 해석 시도)로 대상 지역(전국 + 해당 지역) 제도를 추린 뒤,
 * ② targetType이 있으면(OTHER가 아니면) {@link DefaultRequiredDocsProvider#classify}로 추정한
 *    카테고리가 같거나 GENERAL인 제도만 남기고,
 * ③ 사용자 입력(재난유형/피해대상/피해정도/주소/세대·사업장 정보)을 문장으로 합쳐 임베딩한 뒤,
 *    {@link ProgramSimilarityRanker}로 코사인 유사도 상위 {@value #TOP_N}건만 추린다.
 * ④ 상위 후보 전체를 한 번의 Gemini 호출({@link LlmClassificationService})로 일괄 판정해서
 *    "가능성 높음"/"확인 필요"와 자연어 추천 이유를 받아온다. 실패하면(fail-soft) 규칙
 *    기반 reason으로 대체한다.
 * ⑤ 각 제도에 {@link DeadlineCalculationService}로 deadlineRule을 파싱한 마감일/D-day를
 *    붙인다(인식 못 하는 형태면 null).
 *
 * ⚠️ 2026-09-18 기준: 사진 기반 멀티모달 분석은 아직 이 단계에 없다.
 */
@Service
public class MatchService {

    private static final Logger log = LoggerFactory.getLogger(MatchService.class);

    /**
     * 유사도 랭킹 후 최종적으로 남길 개수. 다음 단계(LLM 판정)에 넘길 후보 수이기도 해서,
     * 너무 크면 LLM 호출 비용/시간이 늘어난다 — 일단 20으로 잡고, 실제 써보면서 조정.
     */
    private static final int TOP_N = 20;

    private final BenefitProgramRepository repository;
    private final DefaultRequiredDocsProvider docsProvider;
    private final AddressToRegionCodeService addressToRegionCodeService;
    private final GeminiEmbeddingClient geminiEmbeddingClient;
    private final ProgramSimilarityRanker similarityRanker;
    private final DeadlineCalculationService deadlineCalculationService;
    private final LlmClassificationService llmClassificationService;

    public MatchService(BenefitProgramRepository repository,
                         DefaultRequiredDocsProvider docsProvider,
                         AddressToRegionCodeService addressToRegionCodeService,
                         GeminiEmbeddingClient geminiEmbeddingClient,
                         ProgramSimilarityRanker similarityRanker,
                         DeadlineCalculationService deadlineCalculationService,
                         LlmClassificationService llmClassificationService) {
        this.repository = repository;
        this.docsProvider = docsProvider;
        this.addressToRegionCodeService = addressToRegionCodeService;
        this.geminiEmbeddingClient = geminiEmbeddingClient;
        this.similarityRanker = similarityRanker;
        this.deadlineCalculationService = deadlineCalculationService;
        this.llmClassificationService = llmClassificationService;
    }

    @Transactional(readOnly = true)
    public MatchResponse match(MatchRequest request) {
        String regionCode = resolveRegionCode(request);
        ProgramCategory categoryFilter = toProgramCategory(request.targetType());

        List<BenefitProgram> candidates = repository.findByRegionCodeIsNullOrRegionCode(regionCode);

        List<BenefitProgram> categoryFiltered = new ArrayList<>();
        for (BenefitProgram program : candidates) {
            ProgramCategory category = docsProvider.classify(program);
            if (categoryFilter != null && category != categoryFilter && category != ProgramCategory.GENERAL) {
                continue;
            }
            categoryFiltered.add(program);
        }

        List<BenefitProgram> ranked = applySimilarityRanking(categoryFiltered, request);

        Map<Long, ProgramVerdict> verdictByProgramId = llmClassificationService.classify(ranked, request);

        List<MatchedProgram> matches = new ArrayList<>();
        for (BenefitProgram program : ranked) {
            ProgramCategory category = docsProvider.classify(program);
            matches.add(toMatchedProgram(program, category, request, verdictByProgramId));
        }

        return new MatchResponse(matches.size(), matches);
    }

    /**
     * 사용자 입력을 임베딩해서 유사도 상위 {@value #TOP_N}건으로 추린다. 임베딩할 텍스트가
     * 없거나(요청에 참고할 필드가 하나도 없음) Gemini 호출이 실패하면, 랭킹 없이
     * 지역+카테고리 필터 결과를 그대로 반환한다 — 임베딩이 안 된다고 매칭 자체가 실패하면
     * 안 된다는 원칙(GeminiEmbeddingClient와 동일한 fail-soft 방침).
     */
    private List<BenefitProgram> applySimilarityRanking(
            List<BenefitProgram> categoryFiltered,
            MatchRequest request
    ) {

        boolean hasEmbedding = categoryFiltered.stream()
                .anyMatch(p -> p.getEmbedding() != null);

        if (!hasEmbedding) {
            log.warn("저장된 임베딩이 없어 유사도 랭킹을 건너뜀");
            return categoryFiltered.stream()
                    .limit(TOP_N)
                    .toList();
        }

        String queryText = describeRequest(request);

        if (queryText.isBlank()) {
            return categoryFiltered;
        }

        float[] queryEmbedding =
                geminiEmbeddingClient.embed(queryText);

        if (queryEmbedding == null) {
            return categoryFiltered;
        }

        return similarityRanker.rankBySimilarity(
                categoryFiltered,
                queryEmbedding,
                TOP_N
        );
    }

    /** MatchRequest의 구조화된 필드들을 임베딩용 자연어 문장으로 합친다. */
    private String describeRequest(MatchRequest request) {
        StringBuilder sb = new StringBuilder();

        appendIfPresent(sb, request.disasterType() != null && !request.disasterType().isBlank()
                ? request.disasterType() + " 피해" : null);
        appendIfPresent(sb, targetTypeLabel(request.targetType()));
        appendIfPresent(sb, damageLevelLabel(request.damageLevel()));
        appendIfPresent(sb, recoveryStatusLabel(request.recoveryStatus()));
        appendIfPresent(sb, request.damageAddress());

        HouseholdInfo household = request.householdInfo();
        if (household != null) {
            appendIfPresent(sb, householdSizeLabel(household.householdSize()));
            appendIfPresent(sb, housingTypeLabel(household.housingType()));
        }

        BusinessInfo business = request.businessInfo();
        if (business != null) {
            appendIfPresent(sb, business.businessType());
            appendIfPresent(sb, operatingStatusLabel(business.operatingStatus()));
            if (business.damageItems() != null) {
                for (BusinessDamageItem item : business.damageItems()) {
                    appendIfPresent(sb, businessDamageItemLabel(item));
                }
            }
        }

        return sb.toString().trim();
    }

    private void appendIfPresent(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(value);
        }
    }

    private String targetTypeLabel(DamageTargetType targetType) {
        if (targetType == null) return null;
        return switch (targetType) {
            case RESIDENTIAL -> "주택/주거 피해";
            case BUSINESS -> "소상공인/사업장 피해";
            case FARM -> "농업/농작물 피해";
            case OTHER -> "기타 재산 피해";
        };
    }

    private String damageLevelLabel(DamageLevel damageLevel) {
        if (damageLevel == null) return null;
        return switch (damageLevel) {
            case LIGHT -> "경미한 피해";
            case MODERATE -> "중간 정도의 피해";
            case SEVERE -> "심각한 피해";
        };
    }

    private String recoveryStatusLabel(RecoveryStatus status) {
        if (status == null) return null;
        return switch (status) {
            case ONGOING -> "피해가 계속되고 있는 상태";
            case TEMPORARY_RECOVERY -> "임시 복구된 상태";
            case COMPLETED -> "복구가 완료된 상태";
        };
    }

    private String householdSizeLabel(HouseholdSize size) {
        if (size == null) return null;
        return switch (size) {
            case ONE -> "1인 가구";
            case TWO -> "2인 가구";
            case THREE -> "3인 가구";
            case FOUR_OR_MORE -> "4인 이상 가구";
        };
    }

    private String housingTypeLabel(HousingType housingType) {
        if (housingType == null) return null;
        return switch (housingType) {
            case OWN -> "자가 거주";
            case JEONSE -> "전세 거주";
            case WOLSE -> "월세 거주";
            case OTHER -> "기타 형태 거주";
        };
    }

    private String operatingStatusLabel(OperatingStatus status) {
        if (status == null) return null;
        return switch (status) {
            case NORMAL -> "정상 영업 중";
            case PARTIAL -> "부분 영업 중";
            case SUSPENDED -> "영업중단 상태";
        };
    }

    private String businessDamageItemLabel(BusinessDamageItem item) {
        if (item == null) return null;
        return switch (item) {
            case FACILITY -> "건물/내부시설 피해";
            case EQUIPMENT -> "영업용 장비 피해";
            case INVENTORY -> "상품/재고 피해";
            case SIGN -> "간판/외부시설 피해";
            case OTHER -> "기타 피해";
        };
    }

    private String resolveRegionCode(MatchRequest request) {
        if (request.regionCode() != null && !request.regionCode().isBlank()) {
            return request.regionCode();
        }
        return addressToRegionCodeService.resolve(request.damageAddress());
    }

    /**
     * DamageTargetType(프론트 값) → ProgramCategory(매칭 필터용) 변환.
     * OTHER는 "세 카테고리 중 어디에도 안 속함"이므로 카테고리 필터를 걸지 않는다(=null).
     */
    private ProgramCategory toProgramCategory(DamageTargetType targetType) {
        if (targetType == null) {
            return null;
        }
        return switch (targetType) {
            case RESIDENTIAL -> ProgramCategory.RESIDENTIAL;
            case BUSINESS -> ProgramCategory.BUSINESS;
            case FARM -> ProgramCategory.FARM;
            case OTHER -> null;
        };
    }

    private MatchedProgram toMatchedProgram(BenefitProgram program, ProgramCategory category, MatchRequest request,
                                             Map<Long, ProgramVerdict> verdictByProgramId) {
        DeadlineCalculationService.DeadlineResult deadline =
                deadlineCalculationService.calculate(program.getDeadlineRule(), request.occurredAt());

        ProgramVerdict llmVerdict = verdictByProgramId.get(program.getId());
        String verdict = llmVerdict != null ? llmVerdict.verdict() : null;
        String reason = llmVerdict != null ? llmVerdict.reason() : buildReason(program, category, request);

        return new MatchedProgram(
                program.getId(),
                program.getName(),
                program.getAgency(),
                verdict,
                reason,
                new ArrayList<>(program.getRequiredDocs()),
                program.getDeadlineRule(),
                deadline != null ? deadline.deadlineAt() : null,
                deadline != null ? deadline.dDay() : null,
                program.getSourceUrl()
        );
    }

    /** LLM 판정이 실패했을 때(fail-soft)만 쓰이는 규칙 기반 reason. */
    private String buildReason(BenefitProgram program, ProgramCategory category, MatchRequest request) {
        StringBuilder reason = new StringBuilder();

        if (program.getRegionCode() == null) {
            reason.append("전국 대상 제도");
        } else {
            reason.append("지역(").append(program.getRegionCode()).append(") 대상 제도");
        }

        if (request.targetType() != null) {
            ProgramCategory filter = toProgramCategory(request.targetType());
            if (filter != null && category == filter) {
                reason.append(", ").append(category).append(" 유형 일치");
            } else {
                reason.append(", ").append(category).append(" 유형(일반 대상으로 포함)");
            }
        }

        if (request.disasterType() != null && !request.disasterType().isBlank()) {
            reason.append(" [참고 재난유형: ").append(request.disasterType()).append("]");
        }

        return reason.toString();
    }
}
