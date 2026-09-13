package com.recoveryonestop.match.service;

import com.recoveryonestop.match.domain.BenefitProgram;
import com.recoveryonestop.match.domain.BenefitProgramRepository;
import com.recoveryonestop.match.domain.ProgramCategory;
import com.recoveryonestop.match.dto.MatchRequest;
import com.recoveryonestop.match.dto.MatchResponse;
import com.recoveryonestop.match.dto.MatchedProgram;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 제도 매칭 1차 후보군 조회.
 * ① regionCode로 대상 지역(전국 + 해당 지역) 제도를 추린 뒤,
 * ② subjectType이 있으면 {@link DefaultRequiredDocsProvider#classify}로 추정한 카테고리가
 *    같거나 GENERAL인 제도만 남긴다.
 * 실제 유사도(RAG) 랭킹은 이 후보군을 대상으로 이후 단계에서 붙인다.
 */
@Service
public class MatchService {

    private final BenefitProgramRepository repository;
    private final DefaultRequiredDocsProvider docsProvider;

    public MatchService(BenefitProgramRepository repository, DefaultRequiredDocsProvider docsProvider) {
        this.repository = repository;
        this.docsProvider = docsProvider;
    }

    @Transactional(readOnly = true)
    public MatchResponse match(MatchRequest request) {
        List<BenefitProgram> candidates = repository.findByRegionCodeIsNullOrRegionCode(request.regionCode());

        List<MatchedProgram> matches = new ArrayList<>();
        for (BenefitProgram program : candidates) {
            ProgramCategory category = docsProvider.classify(program);

            if (request.subjectType() != null
                    && category != request.subjectType()
                    && category != ProgramCategory.GENERAL) {
                continue;
            }

            matches.add(toMatchedProgram(program, category, request));
        }

        return new MatchResponse(matches.size(), matches);
    }

    private MatchedProgram toMatchedProgram(BenefitProgram program, ProgramCategory category, MatchRequest request) {
        return new MatchedProgram(
                program.getId(),
                program.getName(),
                program.getAgency(),
                buildReason(program, category, request),
                new ArrayList<>(program.getRequiredDocs()),
                program.getDeadlineRule(),
                program.getSourceUrl()
        );
    }

    private String buildReason(BenefitProgram program, ProgramCategory category, MatchRequest request) {
        StringBuilder reason = new StringBuilder();

        if (program.getRegionCode() == null) {
            reason.append("전국 대상 제도");
        } else {
            reason.append("지역(").append(program.getRegionCode()).append(") 대상 제도");
        }

        if (request.subjectType() != null) {
            if (category == request.subjectType()) {
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
