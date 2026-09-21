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
                        text(row, "EMRG_STEP_ID")
                ));
            }
            return results;
        } catch (Exception e) {
            log.warn("긴급재난문자 API JSON 파싱 실패: {}", e.getMessage());
            return List.of();
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
