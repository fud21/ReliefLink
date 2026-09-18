package com.recoveryonestop.match.client.dto;

import java.util.List;

/**
 * POST .../models/{model}:embedContent 요청 바디.
 *
 * ⚠️ 2026-09-18 실증 확인됨: outputDimensionality는 (일부 문서에 나오는 것처럼)
 * "embedContentConfig" 하위가 아니라 최상위 필드로 바로 넣어야 실제로 동작한다.
 * Postman으로 이 형태 그대로 호출해서 768차원 응답을 직접 확인했다.
 */
public record GeminiEmbedContentRequest(
        String model,
        Content content,
        Integer outputDimensionality
) {
    public record Content(List<Part> parts) {
    }

    public record Part(String text) {
    }
}
