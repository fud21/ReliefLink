package com.recoveryonestop.match.client.dto;

import java.util.List;

/**
 * generateContent 응답 바디 — 필요한 것만 매핑.
 *
 * ⚠️ 2026-09-18(3) 실증 확인됨(Postman): 실제 응답의 candidates[0].content.parts[0]에는
 * text 외에 thoughtSignature(내부 reasoning 서명)도 같이 오지만 안 쓰므로 매핑하지
 * 않는다 — Spring Boot 기본 Jackson 설정은 모르는 필드를 그냥 무시하므로 에러 없음.
 * usageMetadata/modelVersion/responseId/finishReason도 지금은 안 써서 뺐다.
 */
public record GeminiGenerateContentResponse(
        List<Candidate> candidates
) {
    public record Candidate(Content content) {
    }

    public record Content(List<Part> parts) {
    }

    public record Part(String text) {
    }
}
