package com.recoveryonestop.match.client;

import com.recoveryonestop.match.client.dto.LocalWelfareDetailItem;
import com.recoveryonestop.match.client.dto.LocalWelfareListItem;
import com.recoveryonestop.match.client.dto.LocalWelfareListResponse;
import com.recoveryonestop.match.client.exception.DailyQuotaExceededException;
import com.recoveryonestop.match.config.BokjiroProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 한국사회보장정보원_지자체복지서비스 (data.go.kr 15108347) 클라이언트.
 *
 * ⚠️ CentralWelfareClient와 필드명·동작이 다른 부분이 있어 그대로 복붙하지 않고 분리했다.
 *    자세한 차이는 {@link LocalWelfareListItem}, {@link LocalWelfareDetailItem} 주석 참고.
 *
 * ⚠️ 실증 확인됨(2026-09-13): 목록조회는 searchWrd 없이 지역 파라미터만으로는
 *    데이터가 안 나온다(resultCode=40 NO DATA FOUND). searchWrd가 사실상 필수로
 *    동작하므로, 전수 적재는 지역별이 아니라 {@link com.recoveryonestop.match.service.BenefitProgramIngestService}
 *    의 키워드 목록을 순회하는 방식으로 처리한다. 이 클라이언트 자체는 "키워드 1개로
 *    페이지 조회"만 책임진다.
 *
 * ⚠️ 2026-09-13 실행 중 실제로 만난 것: 일일 트래픽 한도(코드표 기준 1000건/일) 초과 시
 *    HTTP 429 + 본문에 {@code <errMsg>LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR</errMsg>}가
 *    온다. RestClient 기본 설정상 4xx는 {@link HttpClientErrorException}으로 던져지므로,
 *    이걸 낱개 실패로 삼켜서 계속 진행하면 남은 요청 전부가 예측 가능하게 똑같이 실패한다.
 *    → 429를 감지하면 {@link DailyQuotaExceededException}으로 바꿔 던져서, 호출부
 *    ({@link com.recoveryonestop.match.service.BenefitProgramIngestService})가 남은
 *    키워드/항목 순회를 즉시 중단(fail-fast)하도록 한다.
 *
 * ⚠️ 2026-09-13 한도 상향 후 실제로 만난 것: {@link ResourceAccessException}(내부 원인
 *    {@code ReadTimeoutException}) — data.go.kr 쪽 순간적인 응답 지연/끊김으로 보이는
 *    일시적 네트워크 오류. 할당량 초과와 달리 계정 전체 문제가 아니라 "그 요청 한 번"의
 *    문제라서, 짧은 대기 후 최대 3회까지 재시도하고, 그래도 안 되면 그 요청(페이지/항목)만
 *    포기하고 나머지 진행은 계속하게 한다. (전체 동기화를 fail-fast로 중단시키는
 *    {@link DailyQuotaExceededException}과는 다르게 취급함에 주의.)
 *
 * ⚠️ serviceKey 인코딩 규칙은 중앙부처와 동일: Encoding 키를 그대로(재인코딩 없이) 사용.
 * ⚠️ 2026-09-13 실행 중 발견된 버그를 고친 것: UriComponentsBuilder.build(true)는 "모든 값이
 *    이미 인코딩됐다"고 가정하고 아무것도 인코딩하지 않는다. serviceKey는 이미 인코딩된 값이라
 *    괜찮지만, searchWrd에 넣는 한글 검색어("영유아" 등)는 인코딩 안 된 원문이라 그대로 넘기면
 *    {@code IllegalArgumentException: Invalid character '영' for QUERY_PARAM}으로 터진다.
 *    → serviceKey는 원본 그대로, searchWrd 등 한글이 들어갈 수 있는 값은 URLEncoder로 직접
 *    인코딩해서 URI 문자열을 수동 조립하는 방식으로 우회한다.
 */
@Component
public class LocalWelfareClient {

    private static final Logger log = LoggerFactory.getLogger(LocalWelfareClient.class);

    private final RestClient restClient;
    private final BokjiroProperties props;

    public LocalWelfareClient(RestClient bokjiroRestClient, BokjiroProperties props) {
        this.restClient = bokjiroRestClient;
        this.props = props;
    }

    /**
     * @param srchKeyCode 001=서비스명, 002=서비스내용, 003=서비스명+서비스내용 (코드표 기준). 전수 적재 시
     *                    재현율을 높이려고 기본값으로 003을 쓴다.
     */
    public LocalWelfareListResponse fetchListPage(String searchWrd, int pageNo) {
        String encodedSearchWrd = URLEncoder.encode(searchWrd, StandardCharsets.UTF_8);
        URI uri = URI.create(props.getLocalBaseUrl() + "/LcgvWelfarelist"
                + "?serviceKey=" + props.getServiceKey()
                + "&pageNo=" + pageNo
                + "&numOfRows=" + props.getNumOfRows()
                + "&srchKeyCode=003"
                + "&searchWrd=" + encodedSearchWrd);

        LocalWelfareListResponse response = executeWithRetry(
                "목록조회 searchWrd=" + searchWrd + " pageNo=" + pageNo,
                () -> restClient.get().uri(uri).retrieve().body(LocalWelfareListResponse.class));

        if (response == null) {
            log.warn("지자체복지서비스 목록조회 응답 없음 searchWrd={} pageNo={}", searchWrd, pageNo);
            return new LocalWelfareListResponse(0, pageNo, props.getNumOfRows(), null, null, List.of());
        }
        if (!response.isSuccess() && !response.isNoData()) {
            log.warn("지자체복지서비스 목록조회 실패 searchWrd={} pageNo={} resultCode={} resultMessage={}",
                    searchWrd, pageNo, response.resultCode(), response.resultMessage());
        }
        return response;
    }

    /**
     * ⚠️ 일시적 네트워크 오류({@link ResourceAccessException})가 재시도까지 다 써도 안 풀리면,
     * 이 메서드가 예외를 밖으로 던지지 않고 그 시점까지 모은 항목만 반환한다 — 키워드 하나의
     * 네트워크 불운 때문에 {@link com.recoveryonestop.match.service.BenefitProgramIngestService}의
     * 전체 키워드 순회가 죽지 않도록. (할당량 초과는 다름 — {@link DailyQuotaExceededException}은
     * 그대로 밖으로 던져서 전체 동기화를 즉시 중단시킨다.)
     */
    public List<LocalWelfareListItem> fetchAllItemsForKeyword(String searchWrd) {
        LocalWelfareListResponse first;
        try {
            first = fetchListPage(searchWrd, 1);
        } catch (ResourceAccessException e) {
            log.warn("지자체복지서비스 검색어 '{}' 목록조회(1페이지) 반복 실패로 이 키워드는 건너뜀: {}",
                    searchWrd, e.getMessage());
            return List.of();
        }
        if (first.servList() == null || first.servList().isEmpty()) {
            return List.of();
        }

        List<LocalWelfareListItem> all = new java.util.ArrayList<>(first.servList());
        int totalPages = (first.totalCount() + props.getNumOfRows() - 1) / props.getNumOfRows();

        for (int page = 2; page <= totalPages; page++) {
            sleepQuietly(80);
            try {
                LocalWelfareListResponse response = fetchListPage(searchWrd, page);
                if (response.servList() != null) {
                    all.addAll(response.servList());
                }
            } catch (ResourceAccessException e) {
                log.warn("지자체복지서비스 검색어 '{}' {}페이지에서 반복 실패로 나머지 페이지는 건너뛰고 지금까지 모은 {}건만 사용: {}",
                        searchWrd, page, all.size(), e.getMessage());
                break;
            }
        }
        return all;
    }

    public Optional<LocalWelfareDetailItem> fetchDetail(String servId) {
        URI uri = UriComponentsBuilder
                .fromUriString(props.getLocalBaseUrl() + "/LcgvWelfaredetailed")
                .queryParam("serviceKey", props.getServiceKey())
                .queryParam("servId", servId)
                .build(true)
                .toUri();

        LocalWelfareDetailItem detail = executeWithRetry(
                "상세조회 servId=" + servId,
                () -> restClient.get().uri(uri).retrieve().body(LocalWelfareDetailItem.class));

        if (detail == null || !detail.isSuccess()) {
            log.warn("지자체복지서비스 상세조회 실패 servId={} resultCode={} resultMessage={}",
                    servId,
                    detail == null ? null : detail.resultCode(),
                    detail == null ? null : detail.resultMessage());
            return Optional.empty();
        }
        return Optional.of(detail);
    }

    /**
     * data.go.kr 호출 1건을 실행하면서 두 종류의 오류를 다르게 다룬다.
     * - 할당량 초과(HTTP 429): 재시도 의미 없음 → 즉시 {@link DailyQuotaExceededException}으로 변환해서 던짐.
     * - 일시적 네트워크 오류({@link ResourceAccessException}, 예: ReadTimeoutException): 계정 문제가
     *   아니라 그 요청 한 번의 문제라서, 0.5초 × 시도횟수만큼 대기하며 최대 {@code MAX_ATTEMPTS}번
     *   재시도. 그래도 안 되면 마지막 예외를 그대로 던져서 호출부가 "이 요청만" 실패 처리하게 한다.
     */
    private static final int MAX_ATTEMPTS = 3;

    private <T> T executeWithRetry(String context, Supplier<T> call) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return call.get();
            } catch (HttpClientErrorException e) {
                if (isQuotaExceeded(e)) {
                    throw new DailyQuotaExceededException("지자체복지서비스 일일 할당량 초과 (" + context + ")", e);
                }
                throw e;
            } catch (ResourceAccessException e) {
                if (attempt == MAX_ATTEMPTS) {
                    log.warn("지자체복지서비스 호출 실패(일시 오류, {}회 재시도 모두 실패) {}: {}", MAX_ATTEMPTS, context, e.getMessage());
                    throw e;
                }
                log.warn("지자체복지서비스 호출 일시 오류(재시도 {}/{}) {}: {}", attempt, MAX_ATTEMPTS, context, e.getMessage());
                sleepQuietly(500L * attempt);
            }
        }
        throw new IllegalStateException("unreachable");
    }

    /**
     * HTTP 429 + returnReasonCode=22(LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR)를
     * 일일 할당량 초과로 판단한다. 상태코드가 429가 아니어도 본문에 같은 에러 문구가 실려오는
     * 경우까지 방어적으로 같이 확인한다.
     */
    private boolean isQuotaExceeded(HttpClientErrorException e) {
        HttpStatusCode status = e.getStatusCode();
        if (status.value() == 429) {
            return true;
        }
        String body = e.getResponseBodyAsString();
        return body != null && body.contains("LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR");
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
