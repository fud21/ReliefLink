package com.recoveryonestop.match.service;

import com.recoveryonestop.match.client.CentralWelfareClient;
import com.recoveryonestop.match.client.LocalWelfareClient;
import com.recoveryonestop.match.client.dto.CentralWelfareDetailItem;
import com.recoveryonestop.match.client.dto.CentralWelfareListItem;
import com.recoveryonestop.match.client.dto.LocalWelfareDetailItem;
import com.recoveryonestop.match.client.dto.LocalWelfareListItem;
import com.recoveryonestop.match.client.exception.DailyQuotaExceededException;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BenefitProgramIngestService {

    private static final Logger log = LoggerFactory.getLogger(BenefitProgramIngestService.class);
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 지자체복지서비스 전수 적재용 검색어 목록.
     * ⚠️ 실증 확인됨(2026-09-13): 목록조회는 searchWrd 없이 지역 파라미터만으로는 데이터가
     * 안 나온다(resultCode=40). "전 지역 훑기"가 불가능해서 코드표(생애주기/가구상황/관심주제)
     * 텍스트를 검색어로 순회하는 방식으로 대체한다. servId가 같은 제도는 여러 키워드에
     * 중복으로 잡히는데, upsertOneLocal이 (source, servId) 유니크 키로 덮어써서 중복은 자동 제거됨.
     * 코드표에 없는 제도명·내용은 이 방식으로도 못 찾을 수 있음 — 알려진 한계.
     */
    private static final List<String> LOCAL_SYNC_KEYWORDS = List.of(
            "영유아", "아동", "청소년", "청년", "중장년", "노년", "임신", "출산",
            "다문화", "탈북민", "다자녀", "보훈대상자", "장애인", "저소득", "한부모", "조손",
            "신체건강", "정신건강", "생활지원", "주거", "일자리", "문화", "여가",
            "안전", "위기", "보육", "교육", "입양", "위탁", "보호", "돌봄", "서민금융", "법률",
            "재난", "수해", "침수", "이재민", "긴급지원"
    );

    private final CentralWelfareClient client;
    private final LocalWelfareClient localClient;
    private final BenefitProgramRepository repository;
    private final BokjiroProperties props;

    public BenefitProgramIngestService(CentralWelfareClient client,
                                        LocalWelfareClient localClient,
                                        BenefitProgramRepository repository,
                                        BokjiroProperties props) {
        this.client = client;
        this.localClient = localClient;
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
     * 지자체복지서비스 전수 적재 진입점. {@link #LOCAL_SYNC_KEYWORDS}를 순회하며 검색하고,
     * servId 기준 upsert로 중복을 제거한다. 상세조회까지 같이 호출하므로 대량 호출량이
     * 발생함 — 트래픽 제한(1000건/일, 코드표 기준) 고려해서 딜레이를 둔다.
     *
     * ⚠️ 2026-09-13 fail-fast 추가: 일일 할당량을 넘기면(429) 남은 요청도 전부 똑같이
     * 실패하는 게 실증으로 확인돼서, {@link DailyQuotaExceededException}을 만나는 즉시
     * 남은 키워드/항목 순회를 전부 건너뛰고 동기화를 중단한다. 지금까지 처리된 건은
     * 이미 upsert(커밋)돼 있으므로 유실되지 않는다 — 다음 실행(내일 등) 때 이어서 처리됨.
     */
    public void syncAllLocalPrograms() {
        Set<String> seenServIds = new LinkedHashSet<>();
        int success = 0;
        int failed = 0;

        for (String keyword : LOCAL_SYNC_KEYWORDS) {
            List<LocalWelfareListItem> items;
            try {
                items = localClient.fetchAllItemsForKeyword(keyword);
            } catch (DailyQuotaExceededException e) {
                log.warn("지자체복지서비스 동기화 중단(일일 할당량 초과, 검색어='{}' 목록조회 단계): {}. "
                                + "지금까지 고유 {}건, 성공 {}건, 실패 {}건 처리됨. 내일 다시 실행하거나 한도 상향을 신청할 것.",
                        keyword, e.getMessage(), seenServIds.size(), success, failed);
                return;
            }
            log.info("지자체복지서비스 검색어 '{}' → {}건", keyword, items.size());

            for (LocalWelfareListItem item : items) {
                if (!seenServIds.add(item.servId())) {
                    continue; // 이미 다른 키워드에서 처리한 servId
                }
                try {
                    upsertOneLocal(item);
                    success++;
                } catch (DailyQuotaExceededException e) {
                    log.warn("지자체복지서비스 동기화 중단(일일 할당량 초과, servId='{}' 상세조회 단계): {}. "
                                    + "지금까지 고유 {}건, 성공 {}건, 실패 {}건 처리됨. 내일 다시 실행하거나 한도 상향을 신청할 것.",
                            item.servId(), e.getMessage(), seenServIds.size(), success, failed);
                    return;
                } catch (Exception e) {
                    failed++;
                    log.warn("지자체복지서비스 적재 실패 servId={}: {}", item.servId(), e.getMessage());
                }
                sleepQuietly(80);
            }
        }
        log.info("지자체복지서비스 동기화 완료: 고유 {}건, 성공 {}건, 실패 {}건", seenServIds.size(), success, failed);
    }

    @Transactional
    public void upsertOneLocal(LocalWelfareListItem listItem) {
        Optional<LocalWelfareDetailItem> detailOpt = localClient.fetchDetail(listItem.servId());

        BenefitProgram entity = repository.findBySourceAndServId(ProgramSource.LOCAL, listItem.servId())
                .orElseGet(() -> new BenefitProgram(ProgramSource.LOCAL, listItem.servId(), listItem.servNm()));

        entity.setName(listItem.servNm());
        entity.setAgency(listItem.bizChrDeptNm());
        entity.setSourceUrl(listItem.servDtlLink());

        // ⚠️ 법정동코드 매핑 테이블이 아직 없어 regionCode는 비워두고 원문만 보관.
        // BenefitProgram.regionNameRaw 주석 참고.
        entity.setRegionCode(null);
        entity.setRegionNameRaw(joinRegionName(listItem.ctpvNm(), listItem.sggNm()));

        detailOpt.ifPresentOrElse(detail -> {
            entity.setTargetText(detail.sprtTrgtCn() != null ? detail.sprtTrgtCn() : listItem.servDgst());
            entity.setContentText(detail.alwServCn());
            entity.setApplyText(detail.aplyMtdCn());
        }, () -> entity.setTargetText(listItem.servDgst()));

        if (listItem.lastModYmd() != null) {
            try {
                entity.setRawLastModDate(LocalDate.parse(listItem.lastModYmd(), YMD));
            } catch (Exception ignored) {
                // 형식이 다르면 무시.
            }
        }
        entity.setLastSyncedAt(LocalDateTime.now());

        repository.save(entity);
    }

    /**
     * ctpvNm/sggNm이 "-"로 오면 전국 단위 제도라는 뜻(실응답으로 확인됨) — 이 경우 원문도 비워둔다.
     */
    private String joinRegionName(String ctpvNm, String sggNm) {
        boolean ctpvBlank = ctpvNm == null || ctpvNm.isBlank() || "-".equals(ctpvNm);
        boolean sggBlank = sggNm == null || sggNm.isBlank() || "-".equals(sggNm);
        if (ctpvBlank && sggBlank) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (!ctpvBlank) {
            sb.append(ctpvNm);
        }
        if (!sggBlank) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(sggNm);
        }
        return sb.toString();
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
