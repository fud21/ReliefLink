package com.recoveryonestop.match.dto;

import com.recoveryonestop.match.domain.BusinessDamageItem;
import com.recoveryonestop.match.domain.BusinessPremisesType;
import com.recoveryonestop.match.domain.ExpectedSuspensionPeriod;
import com.recoveryonestop.match.domain.OperatingStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * targetType=BUSINESS일 때의 추가 정보. 그 외 targetType이면 null이어도 된다.
 *
 * @param businessName             사업장명.
 * @param businessType             업종(음식점업/도·소매업/서비스업/숙박업/제조업/기타 등, 화면 select 값 그대로).
 * @param premisesType             사업장 형태(자가/임차/기타).
 * @param operatingStatus          현재 영업 상태(정상 영업/부분 영업/영업중단).
 * @param suspensionStartDate      영업중단 시작일. 영업중단이 아니면 null일 수 있다.
 * @param expectedSuspensionPeriod 예상 영업중단 기간.
 * @param damageItems              피해 항목(건물·내부시설/영업용 장비/상품·재고/간판·외부시설/기타), 복수 선택 가능.
 */
public record BusinessInfo(
        String businessName,
        String businessType,
        BusinessPremisesType premisesType,
        OperatingStatus operatingStatus,
        LocalDate suspensionStartDate,
        ExpectedSuspensionPeriod expectedSuspensionPeriod,
        List<BusinessDamageItem> damageItems
) {
}
