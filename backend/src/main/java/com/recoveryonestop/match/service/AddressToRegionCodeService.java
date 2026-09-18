package com.recoveryonestop.match.service;

import com.recoveryonestop.match.domain.RegionCode;
import com.recoveryonestop.match.domain.RegionCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 피해 현장 주소(자유 텍스트)에서 "시/도 + 시/군/구"만 추출해서 region_code 테이블과 매칭한다.
 *
 * ⚠️ 2026-09-18: 새 도로명주소 API를 붙이지 않고, 이미 있는 region_code 테이블(시/군/구
 * 단위까지만 확보된 상태) 수준의 문자열 매칭만 쓰기로 한 결정("1번" 안)에 따른 구현이다.
 * 그래서 시/군/구보다 더 상세한 주소 구분(읍/면/동 등)은 애초에 구분하지 않는다 — 어차피
 * BenefitProgram.regionCode도 그 이상 세밀하게 쓰이지 않는다.
 *
 * 애매하거나 못 찾으면 절대 추측하지 않고 null을 반환한다. regionCode가 null이면
 * MatchService는 전국 대상 제도만 후보로 남긴다 — 틀린 지역 제도를 잘못 보여주는 것보다
 * 안전한 쪽을 택한다는, 이 프로젝트 전반의 원칙을 그대로 따른다.
 */
@Service
public class AddressToRegionCodeService {

    private static final Logger log = LoggerFactory.getLogger(AddressToRegionCodeService.class);

    private final RegionCodeRepository regionCodeRepository;

    public AddressToRegionCodeService(RegionCodeRepository regionCodeRepository) {
        this.regionCodeRepository = regionCodeRepository;
    }

    public String resolve(String damageAddress) {
        if (damageAddress == null || damageAddress.isBlank()) {
            return null;
        }

        String[] tokens = damageAddress.trim().split("\\s+");
        if (tokens.length < 2) {
            log.warn("피해 현장 주소에서 시/도+시/군/구를 추출할 수 없음(토큰 부족): \"{}\"", damageAddress);
            return null;
        }

        String sidoSigungu = tokens[0] + " " + tokens[1];
        List<RegionCode> candidates = regionCodeRepository.findAllByFullName(sidoSigungu);

        if (candidates.isEmpty()) {
            log.warn("피해 현장 주소로 지역코드 매칭 실패: \"{}\" (추출값: \"{}\")", damageAddress, sidoSigungu);
            return null;
        }
        if (candidates.size() > 1) {
            log.warn("피해 현장 주소 지역코드 중복 매칭 (추출값=\"{}\"): {}건 중 첫 번째(code={})를 사용함 — "
                            + "원본 데이터 확인 필요",
                    sidoSigungu, candidates.size(), candidates.get(0).getCode());
        }

        return candidates.get(0).getCode();
    }
}
