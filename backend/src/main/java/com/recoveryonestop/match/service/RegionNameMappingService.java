package com.recoveryonestop.match.service;

import com.recoveryonestop.match.domain.BenefitProgram;
import com.recoveryonestop.match.domain.BenefitProgramRepository;
import com.recoveryonestop.match.domain.ProgramSource;
import com.recoveryonestop.match.domain.RegionCode;
import com.recoveryonestop.match.domain.RegionCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * BenefitProgram.regionNameRaw("경상남도 사천시" 같은 지자체복지서비스 원문 지역 텍스트)를
 * RegionCode.fullName과 매칭해서 BenefitProgram.regionCode를 채우는 단계.
 *
 * ⚠️ 2026-09-16: GET /api/admin/programs/region-names로 실제 regionNameRaw 262종을
 * 먼저 확인하고 설계함(추정 아님). 그 결과 아래 세 가지 특수 케이스를 실제로 확인했다.
 *
 *   1) "강원특별자치도 강원특별자치도교육청"처럼 시/군/구 자리에 "OO교육청"이 붙는 경우가
 *      있다 — 실제 법정동이 아니라서 region_code에 정확히 일치하는 행이 없다. 이런 건
 *      시/도 전체를 대상으로 하는 기관으로 보고, 맨 앞 시/도명만 떼어내 시/도 단위
 *      코드로 대체 매칭한다.
 *
 *   2) "인천광역시 동구" / "인천광역시 중구"는 2026-07-01 인천형 행정체제 개편으로
 *      폐지된 옛 구 이름이라서 최신 법정동코드에는 더 이상 존재하지 않는다(region_code는
 *      최신 데이터만 담고 있음). 실제 개편 내역(나무위키 "인천형 행정체제 개편" 문서,
 *      2026-09-16 확인)은 다음과 같다.
 *        - 동구 전역 → 제물포구로 통합 (1:1 개편, 모호함 없음) → 별칭으로 자동 치환.
 *        - 중구는 영종도 일대(영종동 등) → 영종구, 원도심권(신포동 등) → 제물포구로
 *          "분리" 개편됐다. regionNameRaw에는 "인천광역시 중구"까지만 있고 그 아래
 *          행정동 정보가 없어서, 영종구/제물포구 중 어디로 가야 하는지 이 데이터만으로는
 *          판단할 수 없다. 잘못 추정해서 엉뚱한 지역으로 노출시키는 것보다(재난지원
 *          서비스 특성상 지역을 잘못 안내하면 실사용자에게 해가 될 수 있음), 매칭 실패로
 *          남기고 사람이 프로그램별로 직접 확인하도록 한다.
 *
 *   3) "전남광주통합특별시"처럼 현재 법정동코드 원본 데이터에 아예 존재하지 않는
 *      지역명도 섞여 있다 — 이런 것도 마찬가지로 억지로 끼워맞추지 않고 매칭 실패로
 *      남긴다. 실패한 원문 목록은 그대로 결과에 담아 반환하므로, 이후 하나씩 원인을
 *      확인해서 규칙을 추가하면 된다.
 */
@Service
public class RegionNameMappingService {

    private static final Logger log = LoggerFactory.getLogger(RegionNameMappingService.class);
    private static final String EDU_OFFICE_MARKER = "교육청";

    /**
     * ⚠️ 2026-07-01 인천형 행정체제 개편으로 폐지된 옛 지명 → 현재 법정동코드상 지명.
     * 1:1로 명확히 대응되는 케이스만 여기 넣는다("인천광역시 중구"처럼 여러 신설구로
     * 쪼개진 애매한 케이스는 절대 넣지 말 것 — 클래스 주석 참고).
     */
    private static final Map<String, String> LEGACY_NAME_ALIASES = Map.of(
            "인천광역시 동구", "인천광역시 제물포구"
    );

    private final BenefitProgramRepository programRepository;
    private final RegionCodeRepository regionCodeRepository;

    public RegionNameMappingService(BenefitProgramRepository programRepository,
                                     RegionCodeRepository regionCodeRepository) {
        this.programRepository = programRepository;
        this.regionCodeRepository = regionCodeRepository;
    }

    @Transactional
    public MappingResult mapAllLocalPrograms() {
        List<BenefitProgram> programs = programRepository.findBySource(ProgramSource.LOCAL);

        int exactMatched = 0;
        int eduOfficeMatched = 0;
        int aliasMatched = 0;
        int alreadyMapped = 0;
        int skippedNoRawName = 0;
        int duplicateFullNameHits = 0;
        Set<String> unmatched = new TreeSet<>();

        for (BenefitProgram p : programs) {
            if (p.getRegionCode() != null) {
                alreadyMapped++;
                continue;
            }

            String raw = p.getRegionNameRaw();
            if (raw == null || raw.isBlank()) {
                skippedNoRawName++;
                continue;
            }
            String trimmed = raw.trim();

            List<RegionCode> exactCandidates = regionCodeRepository.findAllByFullName(trimmed);
            RegionCode exact = pickOne(exactCandidates, trimmed);
            if (exact != null) {
                duplicateFullNameHits += Math.max(0, exactCandidates.size() - 1);
                p.setRegionCode(exact.getCode());
                exactMatched++;
                continue;
            }

            String alias = LEGACY_NAME_ALIASES.get(trimmed);
            if (alias != null) {
                List<RegionCode> aliasCandidates = regionCodeRepository.findAllByFullName(alias);
                RegionCode aliasHit = pickOne(aliasCandidates, alias);
                if (aliasHit != null) {
                    duplicateFullNameHits += Math.max(0, aliasCandidates.size() - 1);
                    p.setRegionCode(aliasHit.getCode());
                    aliasMatched++;
                    log.info("옛 지명 별칭으로 매칭: regionNameRaw=\"{}\" → \"{}\" (code={})",
                            trimmed, alias, aliasHit.getCode());
                    continue;
                }
            }

            if (trimmed.contains(EDU_OFFICE_MARKER)) {
                String sidoOnly = trimmed.split(" ")[0];
                List<RegionCode> sidoCandidates = regionCodeRepository.findAllByFullName(sidoOnly);
                RegionCode sido = pickOne(sidoCandidates, sidoOnly);
                if (sido != null) {
                    duplicateFullNameHits += Math.max(0, sidoCandidates.size() - 1);
                    p.setRegionCode(sido.getCode());
                    eduOfficeMatched++;
                    continue;
                }
            }

            log.warn("법정동코드 매칭 실패, regionCode는 null로 유지: regionNameRaw=\"{}\"", trimmed);
            unmatched.add(trimmed);
        }

        programRepository.saveAll(programs);

        MappingResult result = new MappingResult(
                programs.size(), exactMatched, eduOfficeMatched, aliasMatched, alreadyMapped,
                skippedNoRawName, duplicateFullNameHits, unmatched);
        log.info("법정동코드 매핑 완료: 대상 {}건, 정확매칭 {}건, 교육청대체매칭 {}건, 옛지명별칭매칭 {}건, "
                        + "이미매핑됨 {}건, 원문없음 {}건, fullName중복 {}건, 매칭실패 {}종",
                result.totalPrograms(), result.exactMatched(), result.eduOfficeMatched(), result.aliasMatched(),
                result.alreadyMapped(), result.skippedNoRawName(), result.duplicateFullNameHits(),
                result.unmatchedRawNames().size());
        return result;
    }

    private RegionCode pickOne(List<RegionCode> candidates, String fullNameForLog) {
        if (candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() > 1) {
            log.warn("법정동코드 중복 매칭 발견 (fullName=\"{}\"): {}건 중 첫 번째(code={})를 사용함 — 원본 데이터 확인 필요",
                    fullNameForLog, candidates.size(), candidates.get(0).getCode());
        }
        return candidates.get(0);
    }

    public record MappingResult(
            int totalPrograms,
            int exactMatched,
            int eduOfficeMatched,
            int aliasMatched,
            int alreadyMapped,
            int skippedNoRawName,
            int duplicateFullNameHits,
            Set<String> unmatchedRawNames
    ) {
    }
}
