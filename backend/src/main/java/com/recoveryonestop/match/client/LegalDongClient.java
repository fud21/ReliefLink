package com.recoveryonestop.match.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoveryonestop.match.client.dto.LegalDongItem;
import com.recoveryonestop.match.client.dto.LegalDongPageResult;
import com.recoveryonestop.match.config.LegalDongProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * 행정안전부_행정표준코드_법정동코드 (data.go.kr 15077871) 클라이언트.
 *
 * ⚠️ 2026-09-16 실증 확인됨: 응답이 흔한 "필드 나열형" JSON이 아니라
 *    {@code {"StanReginCd": [ {"head": [...]}, {"row": [...] } ]}} 형태다 — 배열 하나 안에
 *    서로 다른 모양의 객체(head/row)가 섞여있다. head 안에도 {"totalCount":...},
 *    {"numOfRows":...,"pageNo":...,"type":...}, {"RESULT":{...}}가 또 뒤섞여 있다.
 *    → 고정된 필드의 record로 한 번에 매핑하지 않고, JsonNode를 직접 순회해서 필요한
 *    값만 꺼내는 방식으로 짰다. 실제 데이터 1건(row 배열 원소)은 모양이 일정해서
 *    {@link LegalDongItem} record로 매핑한다.
 *
 * ⚠️ type=json 파라미터로 JSON 응답을 요청한다(기본값은 xml). serviceKey는 다른 API들과
 *    동일하게 이미 인코딩된 값을 그대로 쓴다.
 * ⚠️ 전체 약 20,560건(2026-09-16 기준)이라 numOfRows=1000이면 21번 호출로 끝난다.
 *    개발계정 하루 한도(10,000건)에 비해 여유가 커서, 지자체복지서비스 때처럼 할당량
 *    fail-fast/재시도 로직까지는 넣지 않았다 — 필요해지면 그때 추가.
 * ⚠️ 이 API는 지자체복지서비스와 제공기관이 다른(행정안전부 표준코드센터, 1741000)
 *    별도 API라 인증키/End Point가 bokjiro.* 설정과 완전히 무관하다.
 *    {@link LegalDongProperties} 참고.
 */
@Component
public class LegalDongClient {

    private static final Logger log = LoggerFactory.getLogger(LegalDongClient.class);

    private final RestClient restClient;
    private final LegalDongProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LegalDongClient(RestClient bokjiroRestClient, LegalDongProperties props) {
        this.restClient = bokjiroRestClient;
        this.props = props;
    }

    public LegalDongPageResult fetchPage(int pageNo) {
        URI uri = URI.create(props.getBaseUrl() + "/getStanReginCdList"
                + "?ServiceKey=" + props.getServiceKey()
                + "&pageNo=" + pageNo
                + "&numOfRows=" + props.getNumOfRows()
                + "&type=json");

        String body = restClient.get()
                .uri(uri)
                .retrieve()
                .body(String.class);

        if (body == null || body.isBlank()) {
            log.warn("법정동코드 목록조회 응답 없음 pageNo={}", pageNo);
            return new LegalDongPageResult(0, List.of());
        }

        return parse(body, pageNo);
    }

    public List<LegalDongItem> fetchAll() {
        LegalDongPageResult first = fetchPage(1);
        List<LegalDongItem> all = new ArrayList<>(first.items());
        if (first.totalCount() == 0) {
            return all;
        }

        int totalPages = (first.totalCount() + props.getNumOfRows() - 1) / props.getNumOfRows();
        log.info("법정동코드 총 {}건, {}페이지 수신 시작", first.totalCount(), totalPages);

        for (int page = 2; page <= totalPages; page++) {
            sleepQuietly(80);
            LegalDongPageResult result = fetchPage(page);
            all.addAll(result.items());
        }
        return all;
    }

    private LegalDongPageResult parse(String body, int pageNo) {
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (Exception e) {
            log.warn("법정동코드 목록조회 응답 파싱 실패 pageNo={}: {}", pageNo, e.getMessage());
            return new LegalDongPageResult(0, List.of());
        }

        JsonNode sections = root.path("StanReginCd");
        int totalCount = 0;
        List<LegalDongItem> items = new ArrayList<>();

        for (JsonNode section : sections) {
            JsonNode head = section.path("head");
            for (JsonNode headEntry : head) {
                if (headEntry.has("totalCount")) {
                    totalCount = headEntry.path("totalCount").asInt(0);
                }
                if (headEntry.has("RESULT")) {
                    JsonNode result = headEntry.path("RESULT");
                    String resultCode = result.path("resultCode").asText("");
                    if (!"INFO-0".equals(resultCode)) {
                        log.warn("법정동코드 목록조회 실패 pageNo={} resultCode={} resultMsg={}",
                                pageNo, resultCode, result.path("resultMsg").asText(""));
                    }
                }
            }

            JsonNode row = section.path("row");
            for (JsonNode rowItem : row) {
                try {
                    items.add(objectMapper.treeToValue(rowItem, LegalDongItem.class));
                } catch (Exception e) {
                    log.warn("법정동코드 항목 파싱 실패 pageNo={}: {}", pageNo, e.getMessage());
                }
            }
        }

        return new LegalDongPageResult(totalCount, items);
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
