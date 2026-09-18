package com.recoveryonestop.match.controller;

import com.recoveryonestop.match.client.GeminiEmbeddingClient;
import com.recoveryonestop.match.domain.BenefitProgram;
import com.recoveryonestop.match.domain.BenefitProgramRepository;
import com.recoveryonestop.match.domain.ProgramSource;
import com.recoveryonestop.match.domain.RegionCode;
import com.recoveryonestop.match.domain.RegionCodeRepository;
import com.recoveryonestop.match.service.BenefitProgramIngestService;
import com.recoveryonestop.match.service.ProgramEmbeddingBatchService;
import com.recoveryonestop.match.service.RegionCodeSyncService;
import com.recoveryonestop.match.service.RegionNameMappingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * ⚠️ 임시 테스트용 엔드포인트. 배치(원래 주 1회 스케줄러로만 도는 로직)를 수동으로 즉시
 * 실행해서 확인하려고 만든 것. 인증/권한 체크 없음.
 *
 * {@code @Profile("local")}로 로컬 개발 프로파일에서만 빈이 등록되도록 막아뒀다 —
 * application.yml에 spring.profiles.active: local을 넣지 않고 배포하면 이 컨트롤러
 * 자체가 존재하지 않는다. 지자체 연동까지 전부 끝나면 이 파일 자체를 삭제할 것.
 */
@Profile("local")
@RestController
public class AdminSyncController {

    private static final Logger log = LoggerFactory.getLogger(AdminSyncController.class);

    private final BenefitProgramIngestService ingestService;
    private final BenefitProgramRepository repository;
    private final RegionCodeSyncService regionCodeSyncService;
    private final RegionCodeRepository regionCodeRepository;
    private final RegionNameMappingService regionNameMappingService;
    private final GeminiEmbeddingClient geminiEmbeddingClient;
    private final ProgramEmbeddingBatchService programEmbeddingBatchService;

    public AdminSyncController(BenefitProgramIngestService ingestService,
                                BenefitProgramRepository repository,
                                RegionCodeSyncService regionCodeSyncService,
                                RegionCodeRepository regionCodeRepository,
                                RegionNameMappingService regionNameMappingService,
                                GeminiEmbeddingClient geminiEmbeddingClient,
                                ProgramEmbeddingBatchService programEmbeddingBatchService) {
        this.ingestService = ingestService;
        this.repository = repository;
        this.regionCodeSyncService = regionCodeSyncService;
        this.regionCodeRepository = regionCodeRepository;
        this.regionNameMappingService = regionNameMappingService;
        this.geminiEmbeddingClient = geminiEmbeddingClient;
        this.programEmbeddingBatchService = programEmbeddingBatchService;
    }

    @PostMapping("/api/admin/sync-central")
    public String syncCentral() {
        log.info("[임시 테스트 엔드포인트] 중앙부처복지서비스 수동 동기화 요청 수신");
        ingestService.syncAllCentralPrograms();
        return "OK";
    }

    @PostMapping("/api/admin/sync-local")
    public String syncLocal() {
        log.info("[임시 테스트 엔드포인트] 지자체복지서비스 수동 동기화 요청 수신");
        ingestService.syncAllLocalPrograms();
        return "OK";
    }

    /**
     * ⚠️ 2026-09-15 추가: 동기화가 실제로 DB에 저장까지 됐는지 눈으로 확인하기 위한
     * 임시 조회용 엔드포인트. 정부 API를 다시 호출하는 게 아니라 우리 DB(benefit_program)를
     * 그대로 조회해서 JSON으로 돌려준다 — "우리가 만드는 API는 기본적으로 JSON으로
     * 응답한다"를 보여주는 제일 간단한 예시이기도 함.
     */
    @GetMapping("/api/admin/programs/summary")
    public Map<String, Object> programsSummary() {
        long total = repository.count();
        long central = repository.countBySource(ProgramSource.CENTRAL);
        long local = repository.countBySource(ProgramSource.LOCAL);

        List<Map<String, Object>> samples = repository.findAll(PageRequest.of(0, 5)).stream()
                .map(this::toSample)
                .toList();

        return Map.of(
                "totalCount", total,
                "centralCount", central,
                "localCount", local,
                "samples", samples
        );
    }

    private Map<String, Object> toSample(BenefitProgram p) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("source", p.getSource());
        m.put("servId", p.getServId());
        m.put("name", p.getName());
        m.put("agency", p.getAgency());
        m.put("regionCode", p.getRegionCode());
        m.put("regionNameRaw", p.getRegionNameRaw());
        return m;
    }

    @PostMapping("/api/admin/sync-region-codes")
    public String syncRegionCodes() {
        log.info("[임시 테스트 엔드포인트] 법정동코드 수동 동기화 요청 수신");
        regionCodeSyncService.syncAllRegionCodes();
        return "OK";
    }

    /** DB에 실제로 몇 건, 어떤 값으로 들어갔는지 확인용. */
    @GetMapping("/api/admin/region-codes/summary")
    public Map<String, Object> regionCodesSummary() {
        long total = regionCodeRepository.count();

        List<Map<String, Object>> samples = regionCodeRepository.findAll(PageRequest.of(0, 5)).stream()
                .map(r -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("code", r.getCode());
                    m.put("sidoCd", r.getSidoCd());
                    m.put("sggCd", r.getSggCd());
                    m.put("umdCd", r.getUmdCd());
                    m.put("fullName", r.getFullName());
                    m.put("lowestName", r.getLowestName());
                    return m;
                })
                .toList();

        return Map.of(
                "totalCount", total,
                "samples", samples
        );
    }

    /**
     * ⚠️ 2026-09-16 추가: 법정동코드 매핑 로직을 규칙 기반으로 짜기 전에,
     * 지자체복지서비스 regionNameRaw가 실제로 어떤 형태("경상남도 사천시"처럼 시/도+시/군/구가
     * 붙어있는지, "사천시"처럼 단독인지 등)로 들어와 있는지 먼저 눈으로 확인하기 위한
     * 임시 조회용 엔드포인트. data.go.kr API 때와 같은 원칙 — 절대 형태를 추정하지 않는다.
     */
    @GetMapping("/api/admin/programs/region-names")
    public Map<String, Object> regionNamesRaw() {
        List<String> names = repository.findDistinctRegionNameRaw(ProgramSource.LOCAL);
        return Map.of(
                "distinctCount", names.size(),
                "names", names
        );
    }

    /**
     * ⚠️ 2026-09-16 추가: 실제 regionNameRaw 값들을 보고 설계한 매핑 로직을 지금 바로
     * 실행하는 엔드포인트. RegionNameMappingService.MappingResult를 그대로 응답으로
     * 돌려주므로, 몇 건이 정확 매칭됐고 몇 건이 실패했는지(그리고 실패한 원문 목록까지)
     * 한 번에 확인할 수 있다.
     */
    @PostMapping("/api/admin/map-region-codes")
    public RegionNameMappingService.MappingResult mapRegionCodes() {
        log.info("[임시 테스트 엔드포인트] regionNameRaw → regionCode 매핑 요청 수신");
        return regionNameMappingService.mapAllLocalPrograms();
    }

    /**
     * ⚠️ 2026-09-18 추가: GeminiEmbeddingClient가 실제로 붙는지 확인하기 위한 임시
     * 테스트용 엔드포인트. 벡터 전체(768개 숫자)를 다 돌려주면 응답이 쓸데없이 커서,
     * 차원 수와 앞부분 5개 값만 보여준다 — "제대로 붙었는지"만 확인하는 용도다.
     * 실패(null)면 어느 API 호출이 문제였는지는 서버 로그의 WARN 메시지에서 확인한다.
     */
    @PostMapping("/api/admin/test-embedding")
    public Map<String, Object> testEmbedding(
            @RequestParam(defaultValue = "경상남도 거제시 주택 침수 피해") String text) {
        log.info("[임시 테스트 엔드포인트] Gemini 임베딩 테스트 요청 수신: \"{}\"", text);

        float[] vector = geminiEmbeddingClient.embed(text);
        if (vector == null) {
            return Map.of(
                    "success", false,
                    "message", "임베딩 실패 - 서버 로그의 WARN 메시지 확인 필요"
            );
        }

        float[] preview = Arrays.copyOf(vector, Math.min(5, vector.length));
        return Map.of(
                "success", true,
                "text", text,
                "dimension", vector.length,
                "preview", preview
        );
    }

    /**
     * ⚠️ 2026-09-18 추가: 기존 제도(~4천여 건) 임베딩 배치를 수동으로 즉시 실행하는
     * 엔드포인트. 전체를 한 번에 돌리면 시간이 꽤 걸리니(건당 Gemini 호출 + 150ms 대기),
     * limit 쿼리 파라미터로 소수만 먼저 확인해볼 수 있다 — 예:
     * {@code POST /api/admin/embed-programs?limit=5}. limit 없이 호출하면 대상 전체를 처리한다.
     * 실패한 건은 embeddingGenerated가 false로 남아서, 다시 호출하면 실패한 것만 재시도된다.
     */
    @PostMapping("/api/admin/embed-programs")
    public ProgramEmbeddingBatchService.EmbeddingBatchResult embedPrograms(
            @RequestParam(required = false) Integer limit) {
        log.info("[임시 테스트 엔드포인트] 제도 임베딩 배치 요청 수신 (limit={})", limit);
        return programEmbeddingBatchService.embedPending(limit);
    }

    /**
     * ⚠️ 2026-09-18(2) 추가: D-day 계산 로직을 설계하기 전에, deadlineRule 원문이 실제로
     * 어떤 형태(상대 기한 "OOO일 이내"인지, 절대 날짜인지, "예산 소진 시까지"처럼 계산
     * 불가능한 형태인지 등)로 들어와 있는지 절대 추정하지 말고 이 결과로 먼저 눈으로
     * 확인하기 위한 조회용 엔드포인트. regionNameRaw 때와 같은 원칙.
     */
    @GetMapping("/api/admin/programs/deadline-rules")
    public Map<String, Object> deadlineRulesRaw() {
        long total = repository.count();
        long withDeadline = repository.countByDeadlineRuleIsNotNull();
        List<String> distinct = repository.findDistinctDeadlineRule();

        return Map.of(
                "totalPrograms", total,
                "withDeadlineRuleCount", withDeadline,
                "distinctCount", distinct.size(),
                "samples", distinct
        );
    }
}
