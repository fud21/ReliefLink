package com.recoveryonestop.match.client;

import com.recoveryonestop.match.client.dto.GeminiGenerateContentRequest;
import com.recoveryonestop.match.client.dto.GeminiGenerateContentResponse;
import com.recoveryonestop.match.client.dto.ProgramVerdict;
import com.recoveryonestop.match.config.GeminiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Gemini 텍스트 생성(gemini-3.6-flash) 클라이언트 — 매칭된 제도 후보들을 한 번에
 * "가능성 높음"/"확인 필요"로 판정하고 추천 이유를 자연어로 생성한다.
 *
 * ⚠️ 2026-09-18(3) 실증 확인됨(Postman 직접 호출):
 * - 처음 시도한 gemini-2.5-flash는 404("no longer available to new users") — 에러
 *   메시지가 알려준 gemini-3.6-flash로 교체해서 성공 확인. 인증은 임베딩과 동일하게
 *   x-goog-api-key 헤더.
 * - generationConfig.responseSchema로 구조화된 JSON 출력을 요청하면, 실제로
 *   candidates[0].content.parts[0].text 안에 그 스키마 그대로의 JSON 문자열이 온다
 *   (=JSON 안의 JSON이라 이 text를 다시 한번 파싱해야 함).
 * - parts[0]에는 text 외에 thoughtSignature(내부 reasoning 서명)도 같이 오지만
 *   안 쓰므로 매핑하지 않는다.
 * - usageMetadata.thoughtsTokenCount가 1,140까지 찍힌 걸로 봐서 gemini-3.6-flash가
 *   기본적으로 내부 reasoning("thinking")을 꽤 많이 쓴다. 매칭 요청마다 호출 1번이라
 *   지금은 문제 없지만, 나중에 응답 속도/비용이 문제되면 thinkingConfig로 줄이는 걸
 *   검토할 것 — 아직 그 옵션 자체는 실증 확인 안 했으므로 지금은 손대지 않는다.
 *
 * ⚠️ ObjectMapper는 tools.jackson.databind 패키지에서 가져온다 — 이 프로젝트가 이미
 * XML 매핑에 tools.jackson.dataformat.xml.annotation을 쓰고 있는 걸로 봐서 Jackson 3.x
 * 계열로 추정한 것이다(Spring Boot 4.0.8 기본 BOM). 만약 컴파일 에러(cannot find symbol)가
 * 나면 com.fasterxml.jackson.databind.ObjectMapper로 바꿔야 한다는 뜻이니 그렇게 수정할 것.
 *
 * ⚠️ fail-soft: 호출 실패/응답 이상/JSON 파싱 실패 시 빈 리스트를 반환한다. 판정이
 * 안 됐다고 매칭 자체가 실패하면 안 된다는 이 프로젝트의 원칙(GeminiEmbeddingClient와 동일).
 */
@Component
public class GeminiClassificationClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClassificationClient.class);

    private static final String SYSTEM_INSTRUCTION = """
            당신은 재난 피해 복구 지원 서비스의 제도 매칭 보조원입니다. 사용자의 피해 상황과, \
            이미 지역·유형·유사도로 1차 선별된 복지제도 후보 목록이 주어집니다. 각 제도에 대해 \
            사용자가 실제로 지원 대상에 해당할 가능성이 높은지("가능성 높음") 아니면 조건을 더 \
            확인해봐야 하는지("확인 필요")를 판정하고, 이유를 한국어 1~2문장으로 친절하게 \
            작성하세요. 당신은 최종 판정자가 아니라 초안 작성자입니다 — 확신이 서지 않으면 \
            "확인 필요"를 선택하세요.""";

    /** Postman으로 실제 검증한 responseSchema 그대로. */
    private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "results", Map.of(
                            "type", "array",
                            "items", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "programId", Map.of("type", "integer"),
                                            "verdict", Map.of("type", "string", "enum", List.of("가능성 높음", "확인 필요")),
                                            "reason", Map.of("type", "string")
                                    ),
                                    "required", List.of("programId", "verdict", "reason")
                            )
                    )
            ),
            "required", List.of("results")
    );

    private final RestClient restClient;
    private final GeminiProperties props;
    private final ObjectMapper objectMapper;

    public GeminiClassificationClient(RestClient bokjiroRestClient, GeminiProperties props, ObjectMapper objectMapper) {
        this.restClient = bokjiroRestClient;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    /**
     * @param userPrompt 사용자 피해 상황 + 후보 제도 목록을 담은 프롬프트 텍스트
     *                   ({@link com.recoveryonestop.match.service.LlmClassificationService}에서 생성).
     * @return 판정 결과 목록. 호출/파싱이 실패하면 빈 리스트(=판정 없음).
     */
    public List<ProgramVerdict> classify(String userPrompt) {
        if (userPrompt == null || userPrompt.isBlank()) {
            log.warn("Gemini 판정 요청 프롬프트가 비어있음");
            return List.of();
        }

        URI uri = URI.create(props.getBaseUrl() + "/v1beta/models/" + props.getGenerationModel() + ":generateContent");

        GeminiGenerateContentRequest request = new GeminiGenerateContentRequest(
                new GeminiGenerateContentRequest.SystemInstruction(
                        List.of(new GeminiGenerateContentRequest.Part(SYSTEM_INSTRUCTION))),
                List.of(new GeminiGenerateContentRequest.Content("user",
                        List.of(new GeminiGenerateContentRequest.Part(userPrompt)))),
                new GeminiGenerateContentRequest.GenerationConfig("application/json", RESPONSE_SCHEMA)
        );

        GeminiGenerateContentResponse response;
        try {
            response = restClient.post()
                    .uri(uri)
                    .header("x-goog-api-key", props.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GeminiGenerateContentResponse.class);
        } catch (Exception e) {
            log.warn("Gemini 판정 호출 실패: {}", e.getMessage());
            return List.of();
        }

        String text = extractText(response);
        if (text == null || text.isBlank()) {
            log.warn("Gemini 판정 응답에서 텍스트를 찾지 못함");
            return List.of();
        }

        try {
            ClassificationResult parsed = objectMapper.readValue(text, ClassificationResult.class);
            return parsed.results() != null ? parsed.results() : List.of();
        } catch (Exception e) {
            log.warn("Gemini 판정 응답 JSON 파싱 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private String extractText(GeminiGenerateContentResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            return null;
        }
        GeminiGenerateContentResponse.Content content = response.candidates().get(0).content();
        if (content == null || content.parts() == null || content.parts().isEmpty()) {
            return null;
        }
        return content.parts().get(0).text();
    }

    /** {@code candidates[0].content.parts[0].text} 안의 JSON 문자열을 파싱하기 위한 내부 래퍼. */
    private record ClassificationResult(List<ProgramVerdict> results) {
    }
}
