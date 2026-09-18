package com.recoveryonestop.match.service;

import com.recoveryonestop.match.domain.BenefitProgram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 지역+카테고리로 거른 후보군을, 사용자 입력 임베딩과의 코사인 유사도로 top-N만 추린다.
 *
 * ⚠️ 2026-09-18 설계 결정: pgvector의 {@code <=>} 연산자로 DB에서 직접 유사도 검색을 하지
 * 않고, 이미 지역+카테고리로 거른(보통 수십~수백 건 수준) 후보군을 애플리케이션 메모리에서
 * 코사인 유사도 계산해 정렬한다. 이유:
 * 1) 카테고리 필터({@link DefaultRequiredDocsProvider#classify})가 DB 컬럼이 아니라
 *    제도명/텍스트 키워드로 매번 계산하는 값이라, region+category+similarity를 SQL 한 방에
 *    합치기가 어렵다 — category 필터는 여전히 Java 쪽에서 한다.
 * 2) 이 프로젝트 규모(전체 ~4천여 건, 지역+카테고리로 거르면 보통 그보다 훨씬 적음)에서는
 *    Java에서 벡터(768차원) 코사인 유사도를 수백 건 계산하는 비용이 무시할 만하다.
 * 3) Hibernate HQL의 벡터 거리 함수(cosine_distance 등) 이름/동작을 아직 실제로 검증하지
 *    않았다 — 검증 안 된 채로 SQL에 끼워 넣는 것보다, 이미 검증된 float[] 코사인 계산을
 *    직접 하는 쪽이 이 프로젝트의 "실증 확인 후 코드 작성" 원칙에 맞는다.
 *
 * 전체 제도 수가 훨씬 커지면(수만 건 이상) 이 방식은 느려질 수 있고, 그때는 pgvector
 * 인덱스(ivfflat/hnsw) + DB 레벨 유사도 검색으로 바꾸는 걸 고려해야 한다.
 */
@Component
public class ProgramSimilarityRanker {

    private static final Logger log = LoggerFactory.getLogger(ProgramSimilarityRanker.class);

    /**
     * @param candidates     지역+카테고리로 이미 거른 후보군.
     * @param queryEmbedding 사용자 입력을 임베딩한 벡터.
     * @param topN           최종적으로 남길 개수.
     * @return 유사도 높은 순으로 정렬된 상위 topN개. embedding이 없는(아직 배치 임베딩이
     *         안 된) 제도는 순위를 매길 수 없으므로 결과에서 제외되고, 몇 건 제외됐는지
     *         로그로 남긴다.
     */
    public List<BenefitProgram> rankBySimilarity(List<BenefitProgram> candidates, float[] queryEmbedding, int topN) {
        List<BenefitProgram> withEmbedding = candidates.stream()
                .filter(p -> p.getEmbedding() != null)
                .toList();

        int skipped = candidates.size() - withEmbedding.size();
        if (skipped > 0) {
            log.warn("유사도 순위에서 제외됨(임베딩 없음): {}건 / 전체 후보 {}건 — 배치 임베딩이 아직 안 된 제도로 추정",
                    skipped, candidates.size());
        }

        return withEmbedding.stream()
                .sorted(Comparator.comparingDouble(
                        (BenefitProgram p) -> cosineSimilarity(queryEmbedding, p.getEmbedding())).reversed())
                .limit(Math.max(topN, 0))
                .toList();
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            log.warn("임베딩 차원이 서로 다름(a={}, b={}) — 유사도 0으로 처리", a.length, b.length);
            return 0.0;
        }

        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
