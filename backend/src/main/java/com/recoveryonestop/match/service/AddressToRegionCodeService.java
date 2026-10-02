package com.recoveryonestop.match.service;

import com.recoveryonestop.match.domain.RegionCode;
import com.recoveryonestop.match.domain.RegionCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
 *
 * ⚠️ 2026-10-02: 긴급재난문자(RCPTN_RGN_NM)는 "서울특별시 강남구,서초구"처럼 쉼표로
 * 여러 지역을 한 문자열에 나열하고, 두 번째 조각부터는 시/도를 생략하는 경우가 있다.
 * resolveMultiple()은 그 포맷을 다루기 위해 추가했다 — 앞 조각에서 본 시/도를 이후
 * 시/도 없는 조각에 그대로 적용하고, 공유할 시/도를 전혀 모르는 조각(= 첫 조각부터
 * 시/도가 없음)은 추측하지 않고 그 조각만 건너뛴다.
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

    /**
     * 재난문자 수신 지역(RCPTN_RGN_NM)처럼 쉼표로 여러 지역이 나열되고, 두 번째 조각부터
     * 시/도가 생략될 수 있는 원문 문자열을 법정동코드 목록으로 변환한다.
     *
     * 예: "서울특별시 강남구,서초구" → [강남구 코드, 서초구 코드]
     *
     * 일부 조각만 매칭에 실패하면 그 조각만 조용히 건너뛰고(로그만 남김) 나머지 유효한
     * 지역코드는 그대로 반환한다 — 하나가 애매하다고 전체를 버리는 것보다, 매칭에 성공한
     * 지역코드만이라도 살리는 쪽이 안전하다고 판단했다. 전부 못 찾으면 빈 리스트를
     * 반환한다(null이 아님 — 호출부에서 null 체크 없이 바로 쓸 수 있게).
     */
    public List<String> resolveMultiple(String rawRegionText) {
        if (rawRegionText == null || rawRegionText.isBlank()) {
            return List.of();
        }

        List<String> codes = new ArrayList<>();
        String sharedSido = null;

        for (String rawSegment : rawRegionText.split(",")) {
            String segment = rawSegment.trim();
            if (segment.isEmpty()) {
                continue;
            }

            String[] tokens = segment.split("\\s+");
            String candidateAddress;
            if (tokens.length >= 2) {
                sharedSido = tokens[0];
                candidateAddress = segment;
            } else if (sharedSido != null) {
                candidateAddress = sharedSido + " " + segment;
            } else {
                log.warn("재난문자 지역명에서 시/도를 추출할 수 없어 건너뜀: \"{}\" (전체 원문: \"{}\")",
                        segment, rawRegionText);
                continue;
            }

            String code = resolve(candidateAddress);
            if (code != null && !codes.contains(code)) {
                codes.add(code);
            }
        }

        return codes;
    }
}
