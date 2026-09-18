package com.recoveryonestop.match.client;

import com.recoveryonestop.match.client.dto.GeminiEmbedContentRequest;
import com.recoveryonestop.match.client.dto.GeminiEmbedContentResponse;
import com.recoveryonestop.match.config.GeminiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;

/**
 * Gemini 임베딩(gemini-embedding-001) 클라이언트.
 *
 * ⚠️ 2026-09-18 실증 확인됨(Postman 직접 호출): 요청은
 * {@code POST {base-url}/v1beta/models/{model}:embedContent}, 인증은 쿼리 파라미터가 아니라
 * {@code x-goog-api-key} 헤더, outputDimensionality는 요청 최상위 필드. 응답은
 * {@code {"embedding":{"values":[...]}}} 뿐이고 문서에 나오는 shape/usageMetadata는 없다.
 * 자세한 내용은 {@link GeminiEmbedContentRequest}, {@link GeminiEmbedContentResponse} 참고.
 *
 * ⚠️ 아직 배치 임베딩 서비스(제도 ~4천여 건 전체 적재)는 만들지 않았다 — 이 클라이언트는
 * 텍스트 1건을 임베딩하는 최소 단위만 담당한다. 배치로 돌릴 때는 LegalDongClient처럼
 * 호출 간 sleep을 넣어 요율 제한을 지키는 로직이 다음 단계에서 필요하다.
 */
@Component
public class GeminiEmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiEmbeddingClient.class);

    private final RestClient restClient;
    private final GeminiProperties props;

    public GeminiEmbeddingClient(RestClient bokjiroRestClient, GeminiProperties props) {
        this.restClient = bokjiroRestClient;
        this.props = props;
    }

    /**
     * 텍스트 1건을 임베딩한다. 실패하거나 응답이 비정상이면(값이 없거나 차원이 안 맞으면)
     * null을 반환한다 — 절대 잘못된/불완전한 벡터를 그대로 반환하지 않는다.
     */
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            log.warn("Gemini 임베딩 요청 텍스트가 비어있음");
            return null;
        }

        URI uri = URI.create(props.getBaseUrl() + "/v1beta/models/" + props.getEmbeddingModel() + ":embedContent");

        GeminiEmbedContentRequest request = new GeminiEmbedContentRequest(
                "models/" + props.getEmbeddingModel(),
                new GeminiEmbedContentRequest.Content(List.of(new GeminiEmbedContentRequest.Part(text))),
                props.getEmbeddingDimension()
        );

        GeminiEmbedContentResponse response;
        try {
            response = restClient.post()
                    .uri(uri)
                    .header("x-goog-api-key", props.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GeminiEmbedContentResponse.class);
        } catch (Exception e) {
            log.warn("Gemini 임베딩 호출 실패: {}", e.getMessage());
            return null;
        }

        if (response == null || response.embedding() == null || response.embedding().values() == null) {
            log.warn("Gemini 임베딩 응답이 비정상(embedding/values 없음)");
            return null;
        }

        List<Float> values = response.embedding().values();
        if (values.size() != props.getEmbeddingDimension()) {
            log.warn("Gemini 임베딩 차원 불일치: 기대 {}, 실제 {} — null 반환",
                    props.getEmbeddingDimension(), values.size());
            return null;
        }

        float[] vector = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            vector[i] = values.get(i);
        }
        return vector;
    }
}
