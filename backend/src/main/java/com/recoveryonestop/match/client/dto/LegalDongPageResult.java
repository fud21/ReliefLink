package com.recoveryonestop.match.client.dto;

import java.util.List;

/**
 * LegalDongClient 내부용 페이지 결과 홀더. data.go.kr 응답 자체의 타입이 아니라,
 * {@code {"StanReginCd": [{"head": [...]}, {"row": [...]}]}} 구조를 순회해서 뽑아낸 값을
 * 담는 용도다 (head의 totalCount + row의 항목 리스트).
 */
public record LegalDongPageResult(int totalCount, List<LegalDongItem> items) {
}
