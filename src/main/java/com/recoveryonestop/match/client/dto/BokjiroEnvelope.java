package com.recoveryonestop.match.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * data.go.kr 공통 응답 봉투(envelope) 구조.
 * 대부분의 data.go.kr REST API가 이 형태(response > header/body > items > item[])를 따른다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BokjiroEnvelope<T>(
        @JsonProperty("response") BokjiroResponse<T> response
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BokjiroResponse<T>(
            @JsonProperty("header") BokjiroHeader header,
            @JsonProperty("body") BokjiroBody<T> body
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BokjiroHeader(
            @JsonProperty("resultCode") String resultCode,
            @JsonProperty("resultMsg") String resultMsg
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BokjiroBody<T>(
            @JsonProperty("items") BokjiroItems<T> items,
            @JsonProperty("numOfRows") int numOfRows,
            @JsonProperty("pageNo") int pageNo,
            @JsonProperty("totalCount") int totalCount
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BokjiroItems<T>(
            @JsonProperty("item") List<T> item
    ) {
    }
}
