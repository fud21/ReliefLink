package com.recoveryonestop.match.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegionCodeRepository extends JpaRepository<RegionCode, String> {

    Optional<RegionCode> findByFullName(String fullName);

    /**
     * ⚠️ 2026-09-16 추가: full_name에 유니크 제약이 없어서(원본 법정동코드 데이터에
     * 중복/이력 데이터가 섞여 있을 가능성 있음) findByFullName(Optional 단건)은
     * NonUniqueResultException 위험이 있다. 매핑 로직에서는 이 목록 버전을 써서
     * 여러 건이면 로그로 남기고 첫 번째만 채택하는 식으로 안전하게 처리한다.
     */
    List<RegionCode> findAllByFullName(String fullName);

    /** 매핑 로직에서 "경상남도 사천시" 같은 부분 문자열로 후보를 찾을 때 사용. */
    List<RegionCode> findByFullNameContaining(String keyword);
}
