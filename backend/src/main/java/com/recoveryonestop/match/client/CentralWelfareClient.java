package com.recoveryonestop.match.client;

import com.recoveryonestop.match.client.dto.CentralWelfareDetailItem;
import com.recoveryonestop.match.client.dto.CentralWelfareListItem;
import com.recoveryonestop.match.client.dto.CentralWelfareListResponse;
import com.recoveryonestop.match.config.BokjiroProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * 한국사회보장정보원_중앙부처복지서비스 (data.go.kr 15090532) 클라이언트.
 * 목록조회 → servId 리스트 확보 → 상세조회 반복 호출 순서.
 *
 * ⚠️ serviceKey는 반드시 "Encoding 키(URL 인코딩된 인증키)"를 그대로 넣고 build(true)로 이미 인코딩된
 *    값을 그대로 사용해야 한다. Decoding 키 + build()를 쓰면 "+"가 공백으로 깨지는 등 실제로는 실패한다.
 *    (data.go.kr 미리보기가 만들어낸 성공 URL 기준으로 확인함)
 *
 * ⚠️ 목록조회 시 srchKeyCode 파라미터는 값과 무관하게 반드시 존재해야 한다(없으면
 *    INVALID_REQUEST_PARAMETER_ERROR). 검색어(searchWrd) 없이 호출할 때는 빈 값으로 넣으면 된다.
 *    lifeArray/trgterIndvdlArray/intrsThemaArray/age/onapPsbltYn/orderBy는 전부 선택 파라미터로,
 *    전체 목록을 받으려면 넣지 않는다(넣으면 인구통계 조건으로 필터링됨).
 *
 * ⚠️ 목록조회(callTp=L)·상세조회(callTp=D) 모두 실응답 확인 결과 XML이라 type 파라미터 없이 호출하고
 *    {@link CentralWelfareListResponse} / {@link CentralWelfareDetailItem}로 직접 파싱한다.
 *    (둘 다 감싸는 envelope 없이 평평한 구조)
 */
@Component
public class CentralWelfareClient {

    private static final Logger log = LoggerFactory.getLogger(CentralWelfareClient.class);

    private final RestClient restClient;
    private final BokjiroProperties props;

    public CentralWelfareClient(RestClient bokjiroRestClient, BokjiroProperties props) {
        this.restClient = bokjiroRestClient;
        this.props = props;
    }

    public List<CentralWelfareListItem> fetchListPage(int pageNo) {
        URI uri = UriComponentsBuilder
                .fromUriString(props.getBaseUrl() + "/NationalWelfarelistV001")
                .queryParam("serviceKey", props.getServiceKey())
                .queryParam("callTp", "L")
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", props.getNumOfRows())
                .queryParam("srchKeyCode", "")
                .build(true)
                .toUri();

        CentralWelfareListResponse response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(CentralWelfareListResponse.class);

        if (response == null || !response.isSuccess()) {
            log.warn("중앙부처복지서비스 목록조회 실패 pageNo={} resultCode={} resultMessage={}",
                    pageNo,
                    response == null ? null : response.resultCode(),
                    response == null ? null : response.resultMessage());
            return List.of();
        }
        return response.servList() != null ? response.servList() : List.of();
    }

    public int fetchTotalCount() {
        URI uri = UriComponentsBuilder
                .fromUriString(props.getBaseUrl() + "/NationalWelfarelistV001")
                .queryParam("serviceKey", props.getServiceKey())
                .queryParam("callTp", "L")
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 1)
                .queryParam("srchKeyCode", "")
                .build(true)
                .toUri();

        CentralWelfareListResponse response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(CentralWelfareListResponse.class);

        if (response == null || !response.isSuccess()) {
            log.warn("중앙부처복지서비스 전체 건수 조회 실패 resultCode={} resultMessage={}",
                    response == null ? null : response.resultCode(),
                    response == null ? null : response.resultMessage());
            return 0;
        }
        return response.totalCount();
    }

    public Optional<CentralWelfareDetailItem> fetchDetail(String servId) {
        URI uri = UriComponentsBuilder
                .fromUriString(props.getBaseUrl() + "/NationalWelfaredetailedV001")
                .queryParam("serviceKey", props.getServiceKey())
                .queryParam("callTp", "D")
                .queryParam("servId", servId)
                .build(true)
                .toUri();

        CentralWelfareDetailItem detail = restClient.get()
                .uri(uri)
                .retrieve()
                .body(CentralWelfareDetailItem.class);

        if (detail == null || !detail.isSuccess()) {
            log.warn("중앙부처복지서비스 상세조회 실패 servId={} resultCode={} resultMessage={}",
                    servId,
                    detail == null ? null : detail.resultCode(),
                    detail == null ? null : detail.resultMessage());
            return Optional.empty();
        }
        return Optional.of(detail);
    }
}
