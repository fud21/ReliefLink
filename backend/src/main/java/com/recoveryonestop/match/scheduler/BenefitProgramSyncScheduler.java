package com.recoveryonestop.match.scheduler;

import com.recoveryonestop.match.service.BenefitProgramIngestService;
import com.recoveryonestop.match.service.RequiredDocsAssignmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BenefitProgramSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(BenefitProgramSyncScheduler.class);

    private final BenefitProgramIngestService ingestService;
    private final RequiredDocsAssignmentService docsAssignmentService;

    public BenefitProgramSyncScheduler(BenefitProgramIngestService ingestService,
                                        RequiredDocsAssignmentService docsAssignmentService) {
        this.ingestService = ingestService;
        this.docsAssignmentService = docsAssignmentService;
    }

    /**
     * 매주 월요일 04:00 실행 (심야 배치).
     * ① 복지제도 API 적재 → ② 새로 들어온 제도들에 카테고리 기반 기본 구비서류 체크리스트 적용.
     */
    @Scheduled(cron = "0 0 4 * * MON")
    public void weeklySync() {
        log.info("복지제도 주간 배치 시작");
        ingestService.syncAllCentralPrograms();

        log.info("구비서류 기본 체크리스트 적용 시작");
        docsAssignmentService.assignMissingRequiredDocs();

        log.info("복지제도 주간 배치 종료");
    }
}
