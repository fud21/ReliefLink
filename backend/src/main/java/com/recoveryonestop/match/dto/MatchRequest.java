package com.recoveryonestop.match.dto;

import com.recoveryonestop.match.domain.DamageLevel;
import com.recoveryonestop.match.domain.DamageTargetType;
import com.recoveryonestop.match.domain.RecoveryStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * POST /api/match 요청.
 *
 * ⚠️ 2026-09-18 확장: 이 서비스는 피해신고(DamageReport) 저장을 책임지지 않는다
 * (팀 내 역할 분담 — 저장/reportId 발급은 팀원 쪽 담당). reportId를 받는 대신, 매칭에
 * 필요한 값들을 구조화된 필드로 그대로 입력받는다. 프론트의
 * "GET /support-programs/matches?reportId=..." 같은 reportId 기반 조회를 맡을 팀원의
 * 오케스트레이션 레이어가 DamageReport를 조회해서 이 필드들을 채운 뒤 이 API를 호출하는
 * 구조를 가정한다.
 *
 * @param regionCode      법정동코드. 직접 주면 damageAddress보다 우선한다. 이것도
 *                         damageAddress도 없으면(또는 주소로 해석에 실패하면) null로 취급되고,
 *                         이 경우 전국 대상 제도만 후보가 된다
 *                         ({@link com.recoveryonestop.match.domain.BenefitProgramRepository#findByRegionCodeIsNullOrRegionCode}).
 * @param damageAddress   피해 현장 주소(자유 텍스트, 예: "경상남도 거제시 장평1로 123").
 *                         regionCode가 없을 때, 앞의 "시/도 + 시/군/구" 부분만 추출해서
 *                         region_code 테이블과 문자열 매칭하는 데 쓰인다(도로명주소 API를
 *                         새로 붙이지 않기로 한 결정에 따른 구현 — 애매하면 절대 추측하지
 *                         않고 null로 남긴다).
 * @param disasterType    재난유형(화면 select 값 그대로: 침수/화재/산사태/지진/태풍/폭설/기타).
 *                         매칭 필터링에는 쓰지 않는 참고용 값(추천 이유 문구 등에 활용).
 * @param targetType      피해 대상 유형(프론트 값 그대로: RESIDENTIAL/BUSINESS/FARM/OTHER).
 *                         내부적으로 ProgramCategory로 변환되어 필터링에 쓰인다. OTHER는
 *                         "세 카테고리 중 어디에도 안 속함"이라는 뜻이라 카테고리 필터를
 *                         걸지 않는다(=null 취급). {@link DamageTargetType} Javadoc 참고 —
 *                         ProgramCategory.GENERAL과는 의미가 다르므로 혼용하지 않는다.
 * @param damageLevel     피해 정도(LIGHT/MODERATE/SEVERE).
 * @param recoveryStatus  신고 작성 시점의 복구 상태(ONGOING/TEMPORARY_RECOVERY/COMPLETED). null 허용.
 * @param occurredAt      피해 발생(또는 최초 확인) 시각. deadlineRule 기반 D-day 계산의 기준일이 된다.
 * @param householdInfo   targetType=RESIDENTIAL일 때만 의미 있음. 그 외에는 null이어도 된다.
 * @param businessInfo    targetType=BUSINESS일 때만 의미 있음. 그 외에는 null이어도 된다.
 * @param photoUrls        피해 사진 URL 목록(추후 멀티모달 LLM 판정 단계에서 사용 예정).
 *                         사진이 없으면 빈 리스트. ⚠️ 사진 파일 자체의 저장/호스팅은 이
 *                         서비스의 책임이 아니다 — 팀원의 저장 레이어가 만든, 이 서버가
 *                         접근 가능한 URL을 그대로 받는다는 가정이다.
 */
public record MatchRequest(
        String regionCode,
        String damageAddress,
        String disasterType,
        DamageTargetType targetType,
        DamageLevel damageLevel,
        RecoveryStatus recoveryStatus,
        LocalDateTime occurredAt,
        HouseholdInfo householdInfo,
        BusinessInfo businessInfo,
        List<String> photoUrls
) {
}
