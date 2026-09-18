package com.recoveryonestop.match.client.dto;

import java.util.List;
import java.util.Map;

/**
 * POST .../models/{model}:generateContent 요청 바디.
 *
 * ⚠️ 2026-09-18(3) 실증 확인됨(Postman): systemInstruction/contents/generationConfig
 * 구조 그대로 동작한다. generationConfig.responseSchema는 표준 JSON Schema를 그대로
 * Map으로 넣으면 된다 — 재귀적인 JSON Schema를 record로 표현하면 오히려 읽기 어려워지고,
 * 이 프로젝트에서는 스키마가 고정값이라 Map이 더 단순하다.
 */
public record GeminiGenerateContentRequest(
        SystemInstruction systemInstruction,
        List<Content> contents,
        GenerationConfig generationConfig
) {
    public record SystemInstruction(List<Part> parts) {
    }

    public record Content(String role, List<Part> parts) {
    }

    public record Part(String text) {
    }

    public record GenerationConfig(String responseMimeType, Map<String, Object> responseSchema) {
    }
}
