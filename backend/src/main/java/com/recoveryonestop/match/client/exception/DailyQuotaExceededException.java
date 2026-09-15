package com.recoveryonestop.match.client.exception;

/**
 * data.go.kr API의 일일 트래픽 한도(코드표 기준 1000건/일)를 초과했을 때 던지는 예외.
 *
 * ⚠️ 2026-09-13 실증 확인: 한도 초과 시 HTTP 429와 함께 아래 형태의 XML 바디가 온다.
 * {@code
 *   <errMsg>LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR</errMsg>
 *   <returnAuthMsg>일일 서비스 요청제한 횟수 초과 에러</returnAuthMsg>
 *   <returnReasonCode>22</returnReasonCode>
 * }
 * 이건 이 요청 1건만의 문제가 아니라 계정 전체가 그날 더 이상 호출 못 한다는 뜻이라,
 * 개별 item 실패로 취급해서 계속 진행하면 남은 요청이 전부 똑같이 실패하며 로그만 쌓인다.
 * → 이 예외를 만나면 {@link com.recoveryonestop.match.service.BenefitProgramIngestService}
 *   쪽에서 남은 키워드/항목 순회를 전부 건너뛰고 즉시 동기화를 중단한다(fail-fast).
 */
public class DailyQuotaExceededException extends RuntimeException {

    public DailyQuotaExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
