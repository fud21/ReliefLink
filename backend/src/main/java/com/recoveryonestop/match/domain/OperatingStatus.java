package com.recoveryonestop.match.domain;

/** 현재 영업 상태. 신고 화면의 "현재 영업 상태" select 값과 대응(정상 영업/부분 영업/영업중단). */
public enum OperatingStatus {
    NORMAL,
    PARTIAL,
    SUSPENDED
}
