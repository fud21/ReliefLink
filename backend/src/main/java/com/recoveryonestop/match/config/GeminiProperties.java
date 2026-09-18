package com.recoveryonestop.match.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml 예시:
 *
 * gemini:
 *   api-key: ${GEMINI_API_KEY}
 *   base-url: https://generativelanguage.googleapis.com
 *   embedding-model: gemini-embedding-001
 *   embedding-dimension: 768
 *   generation-model: gemini-3.6-flash
 *
 * ⚠️ 2026-09-18: Postman으로 실제 embedContent 호출을 확인했다 — base-url/embedding-model
 * 조합으로 만든 URL, x-goog-api-key 헤더, 최상위 outputDimensionality 필드 모두 그대로
 * 동작하고 768차원 응답을 정상적으로 받았다({@link com.recoveryonestop.match.client.GeminiEmbeddingClient}
 * 참고). embedding-dimension 기본값 768은 이 프로젝트 규모(~4천여 건)에 맞춰 정한 값이다.
 *
 * ⚠️ 2026-09-18(3): generateContent(LLM 판정/추천이유) 실제 호출을 확인했다. 처음 시도한
 * gemini-2.5-flash는 404("no longer available to new users")였고, 에러 메시지가 직접
 * 알려준 gemini-3.6-flash로 교체해서 성공을 확인했다 — 그래서 기본값을 gemini-3.6-flash로
 * 잡는다({@link com.recoveryonestop.match.client.GeminiClassificationClient} 참고).
 */
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {

    private String apiKey = "";
    private String baseUrl = "https://generativelanguage.googleapis.com";
    private String embeddingModel = "gemini-embedding-001";
    private int embeddingDimension = 768;
    private String generationModel = "gemini-3.6-flash";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public int getEmbeddingDimension() {
        return embeddingDimension;
    }

    public void setEmbeddingDimension(int embeddingDimension) {
        this.embeddingDimension = embeddingDimension;
    }

    public String getGenerationModel() {
        return generationModel;
    }

    public void setGenerationModel(String generationModel) {
        this.generationModel = generationModel;
    }
}
