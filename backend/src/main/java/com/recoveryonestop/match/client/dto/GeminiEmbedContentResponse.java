package com.recoveryonestop.match.client.dto;

import java.util.List;

/**
 * embedContent 응답 바디.
 *
 * ⚠️ 2026-09-18 실증 확인됨: 공식 문서에는 embedding.shape, usageMetadata 같은 필드도
 * 같이 온다고 나오지만, 실제 응답에는 {@code embedding.values}만 있고 그 외 필드는 없다.
 * 존재하지 않는 필드를 읽으려 하지 않도록, 여기 정의한 필드만 딱 매핑한다.
 */
public record GeminiEmbedContentResponse(
        Embedding embedding
) {
    public record Embedding(List<Float> values) {
    }
}
