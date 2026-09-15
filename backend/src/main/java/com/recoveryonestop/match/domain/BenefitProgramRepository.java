package com.recoveryonestop.match.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BenefitProgramRepository extends JpaRepository<BenefitProgram, Long> {

    Optional<BenefitProgram> findBySourceAndServId(ProgramSource source, String servId);

    /**
     * 매칭 1차 후보군: 전국 대상(regionCode = null) + 해당 지역 대상 제도.
     * 실제 유사도(RAG) 랭킹은 이 결과를 대상으로 W3에서 붙인다.
     */
    List<BenefitProgram> findByRegionCodeIsNullOrRegionCode(String regionCode);

    /**
     * 구비서류 기본 체크리스트 적용 대상: 신청방법 텍스트는 있지만 requiredDocs가 비어있는 제도.
     */
    @Query("SELECT p FROM BenefitProgram p WHERE p.applyText IS NOT NULL AND p.requiredDocs IS EMPTY")
    List<BenefitProgram> findNeedingDocsExtraction();
}
