package com.recoveryonestop.match.service;

import com.recoveryonestop.match.client.GeminiEmbeddingClient;
import com.recoveryonestop.match.domain.BenefitProgram;
import com.recoveryonestop.match.domain.BenefitProgramRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 기존 제도(~4천여 건)를 한 번에 임베딩해서 {@code benefit_program.embedding}을 채우는 배치.
 *
 * ⚠️ 이 클래스 전체를 {@code @Transactional}로 감싸지 않는다. 제도 1건마다 Gemini API를
 * 외부 호출(네트워크 왕복 + sleep)하는데, 그 전체를 하나의 트랜잭션/커넥션으로 묶으면
 * 수천 건 도는 동안 DB 커넥션을 계속 붙잡고 있게 되고, 중간에 예상 못 한 예외가 나면
 * 이미 저장된 임베딩까지 전부 롤백될 위험이 있다. 대신 {@link BenefitProgramRepository#save}
 * (Spring Data가 메서드 자체에 트랜잭션을 건다) 호출로 제도 1건씩 독립적으로 커밋한다.
 *
 * ⚠️ 실패한 건은 embeddingGenerated=false로 남으므로, 이 배치를 다시 돌리면
 * {@link BenefitProgramRepository#findByEmbeddingGeneratedFalse}가 실패했던 건만 다시
 * 대상으로 잡는다 — 별도 재시도 로직 없이 그냥 다시 실행하면 된다.
 */
@Service
public class ProgramEmbeddingBatchService {

    private static final Logger log = LoggerFactory.getLogger(ProgramEmbeddingBatchService.class);

    /**
     * Gemini 임베딩 입력 최대 길이(문자 수) 안전장치. gemini-embedding-001은 입력을
     * 최대 2048 토큰까지만 받는데, 한글은 토큰 하나에 보통 1~2자 정도라 여유 있게
     * 1500자로 잘라둔다. 실제로 토큰 초과 에러가 나는지는 아직 실증 확인 전이라,
     * 잘린 건은 로그로 남긴다.
     */
    private static final int MAX_INPUT_CHARS = 1500;

    /** Gemini 요율 제한을 피하기 위한 호출 간 대기. 법정동코드 클라이언트와 같은 패턴. */
    private static final long SLEEP_BETWEEN_CALLS_MS = 150;

    private final BenefitProgramRepository repository;
    private final GeminiEmbeddingClient geminiEmbeddingClient;

    public ProgramEmbeddingBatchService(BenefitProgramRepository repository,
                                         GeminiEmbeddingClient geminiEmbeddingClient) {
        this.repository = repository;
        this.geminiEmbeddingClient = geminiEmbeddingClient;
    }

    /**
     * @param limit null이면 대상 전체를 처리. 양수를 주면 그 개수만큼만 처리하고 멈춘다
     *              (전체(~4천여 건) 돌리기 전에 소수로 먼저 확인해볼 때 사용).
     */
    public EmbeddingBatchResult embedPending(Integer limit) {
        List<BenefitProgram> targets = repository.findByEmbeddingGeneratedFalse();

        if (limit != null && limit >= 0 && limit < targets.size()) {
            targets = targets.subList(0, limit);
        }

        log.info("제도 임베딩 배치 시작: 대상 {}건", targets.size());

        int succeeded = 0;
        int failed = 0;
        int skippedNoText = 0;
        List<Long> failedProgramIds = new ArrayList<>();

        for (BenefitProgram program : targets) {
            String text = buildEmbeddingText(program);

            if (text.isBlank()) {
                log.warn("임베딩할 텍스트가 없어서 건너뜀: programId={} name={}", program.getId(), program.getName());
                skippedNoText++;
                continue;
            }

            float[] vector = geminiEmbeddingClient.embed(text);

            if (vector == null) {
                failed++;
                failedProgramIds.add(program.getId());
                sleepQuietly(SLEEP_BETWEEN_CALLS_MS);
                continue;
            }

            program.setEmbedding(vector);
            program.setEmbeddingGenerated(true);
            repository.save(program);
            succeeded++;

            sleepQuietly(SLEEP_BETWEEN_CALLS_MS);
        }

        log.info("제도 임베딩 배치 완료: 성공 {}건, 실패 {}건, 텍스트 없어서 건너뜀 {}건",
                succeeded, failed, skippedNoText);

        return new EmbeddingBatchResult(targets.size(), succeeded, failed, skippedNoText, failedProgramIds);
    }

    /** 제도명 + 지원대상 텍스트 + 지원내용 텍스트를 합쳐서 임베딩용 텍스트를 만든다. */
    private String buildEmbeddingText(BenefitProgram program) {
        StringBuilder sb = new StringBuilder();
        appendIfPresent(sb, program.getName());
        appendIfPresent(sb, program.getTargetText());
        appendIfPresent(sb, program.getContentText());

        String text = sb.toString().trim();
        if (text.length() > MAX_INPUT_CHARS) {
            log.warn("임베딩 입력 텍스트가 길어서 잘림: programId={} ({}자 → {}자)",
                    program.getId(), text.length(), MAX_INPUT_CHARS);
            text = text.substring(0, MAX_INPUT_CHARS);
        }
        return text;
    }

    private void appendIfPresent(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append(value);
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public record EmbeddingBatchResult(
            int totalTargets,
            int succeeded,
            int failed,
            int skippedNoText,
            List<Long> failedProgramIds
    ) {
    }
}
