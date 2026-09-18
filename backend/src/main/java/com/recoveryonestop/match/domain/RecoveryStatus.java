package com.recoveryonestop.match.domain;

/** 신고 작성 시점의 복구 상태. 신고 화면의 "현재 피해 상태" select 값과 대응(피해 지속/임시 복구/복구 완료). */
public enum RecoveryStatus {
    ONGOING,
    TEMPORARY_RECOVERY,
    COMPLETED
}
