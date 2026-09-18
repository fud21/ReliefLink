package com.recoveryonestop.match.dto;

import com.recoveryonestop.match.domain.HouseholdSize;
import com.recoveryonestop.match.domain.HousingType;

/**
 * targetType=RESIDENTIAL일 때의 추가 정보. 그 외 targetType이면 null이어도 된다.
 *
 * @param householdSize 세대 정보(1인 가구/2인 가구/3인 가구/4인 이상).
 * @param housingType   주거 형태(자가/전세/월세/기타).
 */
public record HouseholdInfo(
        HouseholdSize householdSize,
        HousingType housingType
) {
}
