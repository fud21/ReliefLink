package com.recoveryonestop.match.disaster.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoveryonestop.match.config.SafetyDataProperties;
import com.recoveryonestop.match.disaster.dto.DisasterAlertResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 재난안전데이터공유플랫폼의 행정안전부 긴급재난문자 API(DSSP-IF-00247) 클라이언트.
 *
 * 공식 응답 wrapper가 변경되더라도 핵심 row(SN/MSG_CN/RCPTN_RGN_NM)를 찾을 수 있도록
 * JsonNode를 재귀 탐색한다. API 장애 시 홈 전체를 죽이지 않도록 빈 목록을 반환한다.
 *
 * regionCodes는 여기서는 채우지 않고 빈 리스트로 둔다 — region 원문을 법정동코드로
 * 바꾸는 건 DisasterService가 AddressToRegionCodeService로 담당한다(이 클라이언트는
 * 원본 API 응답을 그대로 옮기는 역할만 한다).
 *
 * ⚠️ 2026-10-02: HTTP 상태코드는 200이면서 바디 안에 {"header":{"resultCode":"32",...},
 * "body":null} 같은 애플리케이션 레벨 에러를 담아 보내는 경우가 실제로 있었다(IP 미등록
 * 에러). 이런 응답은 예외를 던지지 않아서 기존에는 조용히 빈 목록으로만 처리되고
 * 로그에 아무 흔적도 안 남았다 — 그래서 디버깅할 때 "API가 실패한 건지, 그냥 오늘
 * 재난문자가 없는 건지" 구분이 안 됐다. header는 있는데 body가 없는/null인 경우를
 * 에러 응답으로 보고 경고 로그를 남기도록 했다.
 */
@Component
public class SafetyDisasterClient {

    private static final Logger log = LoggerFactory.getLogger(SafetyDisasterClient.class);
    private static final DateTimeFormatter REQUEST_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final RestClient restClient;
    private final SafetyDataProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SafetyDisasterClient(RestClient bokjiroRestClient, SafetyDataProperties props) {
        this.restClient = bokjiroRestClient;
        this.props = props;
    }

    public List<DisasterAlertResponse> fetch(LocalDate date, String region) {
        if (props.getServiceKey() == null || props.getServiceKey().isBlank()) {
            log.warn("SAFETY_DATA_SERVICE_KEY가 없어 긴급재난문자 API를 호출하지 않음");
            return List.of();
        }

        StringBuilder url = new StringBuilder(props.getBaseUrl())
                .append("?serviceKey=").append(props.getServiceKey())
                .append("&returnType=json")
                .append("&pageNo=1")
                .append("&numOfRows=").append(props.getNumOfRows());

        if (date != null) {
            url.append("&crtDt=").append(date.format(REQUEST_DATE));
        }
        if (region != null && !region.isBlank()) {
            url.append("&rgnNm=")
                    .append(URLEncoder.encode(region.trim(), StandardCharsets.UTF_8));
        }

        try {
            String body = restClient.get()
                    .uri(URI.create(url.toString()))
                    .retrieve()
                    .body(String.class);

            if (body == null || body.isBlank()) {
                return List.of();
            }
            return parse(body);
        } catch (Exception e) {
            log.warn("긴급재난문자 API 호출 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private List<DisasterAlertResponse> parse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);

            warnIfErrorResponse(root);

            List<JsonNode> rows = new ArrayList<>();
            collectRows(root, rows);

            List<DisasterAlertResponse> results = new ArrayList<>();
            for (JsonNode row : rows) {
                results.add(new DisasterAlertResponse(
                        text(row, "SN"),
                        text(row, "DST_SE_NM"),
                        text(row, "RCPTN_RGN_NM"),
                        text(row, "MSG_CN"),
                        text(row, "EMRG_STEP_NM"),
                        text(row, "CRT_DT"),
                        text(row, "RCPTN_RGN_ID"),
                        text(row, "DST_SE_ID"),
                        text(row, "EMRG_STEP_ID"),
                        List.of()
                ));
            }
            return results;
        } catch (Exception e) {
            log.warn("긴급재난문자 API JSON 파싱 실패: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * {"header":{"resultCode":"32","resultMsg":"UNREGISTERED IP ERROR","errorMsg":"등록되지
     * 않은 IP"},"body":null}처럼 HTTP 200이지만 애플리케이션 레벨 에러를 담은 응답을
     * header는 있는데 body가 없는/null인 모양으로 감지해서 경고 로그를 남긴다. 예외를
     * 던지지는 않는다 — 이후 collectRows()가 어차피 빈 목록을 반환하므로 호출부 동작은
     * 그대로 "빈 목록"이고, 이 로그는 그 원인을 콘솔에서 바로 알아볼 수 있게 해주는
     * 용도다.
     */
    private void warnIfErrorResponse(JsonNode root) {
        JsonNode header = root.get("header");
        if (header == null) {
            return;
        }
        JsonNode bodyNode = root.get("body");
        boolean bodyMissing = bodyNode == null || bodyNode.isNull();
        if (bodyMissing) {
            log.warn("긴급재난문자 API가 에러 응답을 반환함: resultCode={}, resultMsg={}, errorMsg={}",
                    text(header, "resultCode"), text(header, "resultMsg"), text(header, "errorMsg"));
        }
    }

    /** 응답 wrapper 모양에 덜 의존하도록 핵심 필드를 가진 객체를 재귀적으로 찾는다. */
    private void collectRows(JsonNode node, List<JsonNode> rows) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }

        if (node.isObject()) {
            if (node.has("SN") && (node.has("MSG_CN") || node.has("RCPTN_RGN_NM"))) {
                rows.add(node);
                return;
            }
            node.elements().forEachRemaining(child -> collectRows(child, rows));
            return;
        }

        if (node.isArray()) {
            node.elements().forEachRemaining(child -> collectRows(child, rows));
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
