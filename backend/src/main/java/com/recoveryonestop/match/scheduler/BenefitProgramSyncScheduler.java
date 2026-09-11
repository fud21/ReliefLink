package com.recoveryonestop.match.scheduler;

import com.recoveryonestop.match.service.BenefitProgramIngestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BenefitProgramSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(BenefitProgramSyncScheduler.class);

    private final BenefitProgramIngestService ingestService;

    public BenefitProgramSyncScheduler(BenefitProgramIngestService ingestService) {
        this.ingestService = ingestService;
    }

    /** 매주 월요일 04:00 실행 (심야 배치). cron 값은 application.yml로 빼서 조정 가능하게 해도 됨 */
    @Scheduled(cron = "0 0 4 * * MON")
    public void weeklySync() {
        log.info("복지제도 주간 배치 시작");
        ingestService.syncAllCentralPrograms();
        log.info("복지제도 주간 배치 종료");
    }
}
