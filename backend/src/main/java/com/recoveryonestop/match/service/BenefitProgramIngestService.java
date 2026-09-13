package com.recoveryonestop.match.service;

import com.recoveryonestop.match.client.CentralWelfareClient;
import com.recoveryonestop.match.client.dto.CentralWelfareDetailItem;
import com.recoveryonestop.match.client.dto.CentralWelfareListItem;
import com.recoveryonestop.match.config.BokjiroProperties;
import com.recoveryonestop.match.domain.BenefitProgram;
import com.recoveryonestop.match.domain.BenefitProgramRepository;
import com.recoveryonestop.match.domain.ProgramSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BenefitProgramIngestService {

    private static final Logger log = LoggerFactory.getLogger(BenefitProgramIngestService.class);
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CentralWelfareClient client;
    private final BenefitProgramRepository repository;
    private final BokjiroProperties props;

    public BenefitProgramIngestService(CentralWelfareClient client,
                                        BenefitProgramRepository repository,
                                        BokjiroProperties props) {
        this.client = client;
        this.repository = repository;
        this.props = props;
    }

    /**
     * 주 1회 배치 진입점. (인수인계 문서: "제도 데이터는 실시간 호출 X, 주 1회 배치 적재")
     * 전체 페이지를 순회하며 목록을 받고, 각 항목의 상세를 호출해 합친 뒤 upsert.
     * 대량 상세조회이므로 요청 간 딜레이를 둬서 초당 트래픽 제한(에러코드 23)을 피한다.
     */
    public void syncAllCentralPrograms() {
        int total = client.fetchTotalCount();
        int totalPages = (total + props.getNumOfRows() - 1) / props.getNumOfRows();
        log.info("중앙부처복지서비스 총 {}건, {}페이지 동기화 시작", total, totalPages);

        int success = 0;
        int failed = 0;

        for (int page = 1; page <= totalPages; page++) {
            List<CentralWelfareListItem> listItems = client.fetchListPage(page);
            for (CentralWelfareListItem item : listItems) {
                try {
                    upsertOne(item);
                    success++;
                } catch (Exception e) {
                    failed++;
                    log.warn("적재 실패 servId={}: {}", item.servId(), e.getMessage());
                }
                sleepQuietly(80); // 초당 호출 제한 방어. 값은 트래픽 정책 확인 후 조정
            }
        }
        log.info("중앙부처복지서비스 동기화 완료: 성공 {}건, 실패 {}건", success, failed);
    }

    @Transactional
    public void upsertOne(CentralWelfareListItem listItem) {
        Optional<CentralWelfareDetailItem> detailOpt = client.fetchDetail(listItem.servId());

        BenefitProgram entity = repository.findBySourceAndServId(ProgramSource.CENTRAL, listItem.servId())
                .orElseGet(() -> new BenefitProgram(ProgramSource.CENTRAL, listItem.servId(), listItem.servNm()));

        entity.setName(listItem.servNm());
        entity.setAgency(listItem.jurMnofNm() != null ? listItem.jurMnofNm() : listItem.jurOrgNm());
        entity.setRegionCode(null); // 중앙부처는 전국 대상
        entity.setSourceUrl(listItem.servDtlLink()); // 상세조회가 아니라 목록조회에서 바로 확보

        detailOpt.ifPresentOrElse(detail -> {
            entity.setTargetText(detail.targetDetailContent() != null ? detail.targetDetailContent() : listItem.servDgst());
            entity.setContentText(detail.benefitContent());
            entity.setApplyText(buildApplyText(detail.applmetList()));
        }, () -> entity.setTargetText(listItem.servDgst()));

        // requiredDocs는 여기서 채우지 않는다. applyText 자유텍스트에서 서류를 뽑는 건
        // 별도 파서/LLM 추출 단계(W3 매칭 로직과 함께)에서 처리하는 게 정확도가 더 나옴.

        if (listItem.svcfrstRegTs() != null) {
            try {
                entity.setRawLastModDate(LocalDate.parse(listItem.svcfrstRegTs(), YMD));
            } catch (Exception ignored) {
                // 형식이 다르면 무시.
            }
        }
        entity.setLastSyncedAt(LocalDateTime.now());

        repository.save(entity);
    }

    /**
     * applmetList(신청/조사/결정/지급/사후관리 기관 목록) 각 항목을 "{servSeDetailNm}: {servSeDetailLink}"
     * 형태로 줄바꿈해서 이어붙인다.
     */
    private String buildApplyText(List<CentralWelfareDetailItem.ApplyMethodEntry> applmetList) {
        if (applmetList == null || applmetList.isEmpty()) {
            return null;
        }
        return applmetList.stream()
                .map(entry -> entry.servSeDetailNm() + ": " + entry.servSeDetailLink())
                .collect(Collectors.joining("\n"));
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
