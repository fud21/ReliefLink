package com.recoveryonestop.match.service;

import com.recoveryonestop.match.domain.BenefitProgram;
import com.recoveryonestop.match.domain.BenefitProgramRepository;
import com.recoveryonestop.match.domain.ProgramCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * requiredDocs가 비어있는 제도들에 카테고리 기반 기본 체크리스트를 채워 넣는다.
 * (LLM 추출 대신 규칙 기반으로 대체 — 외부 API 호출/비용 없음)
 */
@Service
public class RequiredDocsAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(RequiredDocsAssignmentService.class);

    private final BenefitProgramRepository repository;
    private final DefaultRequiredDocsProvider docsProvider;

    public RequiredDocsAssignmentService(BenefitProgramRepository repository,
                                          DefaultRequiredDocsProvider docsProvider) {
        this.repository = repository;
        this.docsProvider = docsProvider;
    }

    @Transactional
    public void assignMissingRequiredDocs() {
        List<BenefitProgram> targets = repository.findNeedingDocsExtraction();
        log.info("구비서류 기본 체크리스트 적용 대상 {}건", targets.size());

        int assigned = 0;
        for (BenefitProgram program : targets) {
            ProgramCategory category = docsProvider.classify(program);
            List<String> docs = docsProvider.getDefaultDocs(category);
            program.setRequiredDocs(new ArrayList<>(docs));
            repository.save(program);
            assigned++;
        }

        log.info("구비서류 기본 체크리스트 적용 완료: {}건", assigned);
    }
}
