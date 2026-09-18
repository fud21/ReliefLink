package com.recoveryonestop.match.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BenefitProgramRepository extends JpaRepository<BenefitProgram, Long> {

    Optional<BenefitProgram> findBySourceAndServId(ProgramSource source, String servId);

    /** DB에 실제로 몇 건 들어있는지 소스별로 확인하는 용도 (예: 관리용 요약 엔드포인트). */
    long countBySource(ProgramSource source);

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

    /**
     * ⚠️ 2026-09-16 추가: regionNameRaw → regionCode 매핑 로직을 짜기 전에,
     * "경상남도 사천시" / "사천시" / "경남 사천시" 중 실제로 어떤 형태로 들어와 있는지
     * 절대 추정하지 말고 이 결과로 먼저 눈으로 확인할 것.
     */
    @Query("SELECT DISTINCT p.regionNameRaw FROM BenefitProgram p " +
            "WHERE p.source = :source AND p.regionNameRaw IS NOT NULL ORDER BY p.regionNameRaw")
    List<String> findDistinctRegionNameRaw(@Param("source") ProgramSource source);

    /** regionNameRaw → regionCode 매핑 로직이 순회할 대상(지자체 제도 전체). */
    List<BenefitProgram> findBySource(ProgramSource source);

    /**
     * ⚠️ 2026-09-18 추가: pgvector 배치 임베딩 대상(아직 embedding이 안 채워진 제도 전체).
     * 실패한 건은 embeddingGenerated가 계속 false로 남기 때문에, 배치를 다시 돌리면
     * 실패했던 건만 자동으로 재시도된다 — 별도 재시도 로직이 필요 없다.
     */
    List<BenefitProgram> findByEmbeddingGeneratedFalse();

    /**
     * ⚠️ 2026-09-18(2) 추가: D-day 계산 로직을 짜기 전에, deadlineRule 텍스트가 실제로
     * 몇 건이나 채워져 있는지 먼저 확인하는 용도.
     */
    long countByDeadlineRuleIsNotNull();

    /**
     * ⚠️ 2026-09-18(2) 추가: deadlineRule 원문이 실제로 어떤 형태(상대 기한 "OOO일 이내"인지,
     * 절대 날짜인지, "예산 소진 시까지"처럼 계산이 아예 불가능한 형태인지 등)로 들어와
     * 있는지 절대 추정하지 말고 이 결과로 먼저 눈으로 확인할 것.
     */
    @Query("SELECT DISTINCT p.deadlineRule FROM BenefitProgram p " +
            "WHERE p.deadlineRule IS NOT NULL ORDER BY p.deadlineRule")
    List<String> findDistinctDeadlineRule();
}
