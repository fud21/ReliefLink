package com.recoveryonestop.match.controller;

import com.recoveryonestop.match.service.BenefitProgramIngestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

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

    public AdminSyncController(BenefitProgramIngestService ingestService) {
        this.ingestService = ingestService;
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
}
