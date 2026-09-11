package com.recoveryonestop.match.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BenefitProgramRepository extends JpaRepository<BenefitProgram, Long> {

    Optional<BenefitProgram> findBySourceAndServId(ProgramSource source, String servId);

    /**
     * 매칭 1차 후보군: 전국 대상(regionCode = null) + 해당 지역 대상 제도.
     * 실제 유사도(RAG) 랭킹은 이 결과를 대상으로 W3에서 붙인다.
     */
    List<BenefitProgram> findByRegionCodeIsNullOrRegionCode(String regionCode);
}
