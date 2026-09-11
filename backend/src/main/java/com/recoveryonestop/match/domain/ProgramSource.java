package com.recoveryonestop.match.domain;

public enum ProgramSource {
    CENTRAL,     // 중앙부처복지서비스 (15090532)
    LOCAL,       // 지자체복지서비스 (15108347)
    RULE_TABLE   // 재난지원금 단가 등, 우리가 직접 하드코딩하는 규정
}
