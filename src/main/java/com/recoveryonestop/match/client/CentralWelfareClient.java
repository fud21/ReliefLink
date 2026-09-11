package com.recoveryonestop.match.client;

import com.recoveryonestop.match.client.dto.BokjiroEnvelope;
import com.recoveryonestop.match.client.dto.CentralWelfareDetailItem;
import com.recoveryonestop.match.client.dto.CentralWelfareListItem;
import com.recoveryonestop.match.config.BokjiroProperties;
import org.springframework.core.ParameterizedTypeReference;
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
 * ⚠️ serviceKey는 반드시 "Decoding 키(일반 인증키)"를 그대로 넣는다.
 *    UriComponentsBuilder가 자동으로 한 번 더 인코딩하므로, Encoding 키를 넣으면 이중 인코딩 오류가 난다.
 *    (인수인계 문서 "실무 함정 #2")
 *
 * ⚠️ 오퍼레이션명(getNationalWelfarelist 등)과 파라미터명(callTp 등)은 실제 Swagger 승인 후 재검증 필요.
 */
@Component
public class CentralWelfareClient {

    private final RestClient restClient;
    private final BokjiroProperties props;

    public CentralWelfareClient(RestClient bokjiroRestClient, BokjiroProperties props) {
        this.restClient = bokjiroRestClient;
        this.props = props;
    }

    public List<CentralWelfareListItem> fetchListPage(int pageNo) {
        URI uri = UriComponentsBuilder
                .fromUriString(props.getBaseUrl() + "/getNationalWelfarelist")
                .queryParam("serviceKey", props.getServiceKey())
                .queryParam("callTp", "L")
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", props.getNumOfRows())
                .queryParam("type", "json")
                .build(true)
                .toUri();

        BokjiroEnvelope<CentralWelfareListItem> envelope = restClient.get()
                .uri(uri)
                .retrieve()
                .body(new ParameterizedTypeReference<BokjiroEnvelope<CentralWelfareListItem>>() {
                });

        if (envelope == null || envelope.response() == null || envelope.response().body() == null
                || envelope.response().body().items() == null) {
            return List.of();
        }
        return envelope.response().body().items().item();
    }

    public int fetchTotalCount() {
        URI uri = UriComponentsBuilder
                .fromUriString(props.getBaseUrl() + "/getNationalWelfarelist")
                .queryParam("serviceKey", props.getServiceKey())
                .queryParam("callTp", "L")
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 1)
                .queryParam("type", "json")
                .build(true)
                .toUri();

        BokjiroEnvelope<CentralWelfareListItem> envelope = restClient.get()
                .uri(uri)
                .retrieve()
                .body(new ParameterizedTypeReference<BokjiroEnvelope<CentralWelfareListItem>>() {
                });

        if (envelope == null || envelope.response() == null || envelope.response().body() == null) {
            return 0;
        }
        return envelope.response().body().totalCount();
    }

    public Optional<CentralWelfareDetailItem> fetchDetail(String servId) {
        URI uri = UriComponentsBuilder
                .fromUriString(props.getBaseUrl() + "/getNationalWelfaredetailed")
                .queryParam("serviceKey", props.getServiceKey())
                .queryParam("callTp", "D")
                .queryParam("servId", servId)
                .queryParam("type", "json")
                .build(true)
                .toUri();

        BokjiroEnvelope<CentralWelfareDetailItem> envelope = restClient.get()
                .uri(uri)
                .retrieve()
                .body(new ParameterizedTypeReference<BokjiroEnvelope<CentralWelfareDetailItem>>() {
                });

        if (envelope == null || envelope.response() == null || envelope.response().body() == null
                || envelope.response().body().items() == null
                || envelope.response().body().items().item().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(envelope.response().body().items().item().get(0));
    }
}
