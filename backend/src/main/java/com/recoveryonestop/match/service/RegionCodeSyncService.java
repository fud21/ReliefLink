package com.recoveryonestop.match.service;

import com.recoveryonestop.match.client.LegalDongClient;
import com.recoveryonestop.match.client.dto.LegalDongItem;
import com.recoveryonestop.match.domain.RegionCode;
import com.recoveryonestop.match.domain.RegionCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 법정동코드 전수 적재. 전체 약 20,560건(2026-09-16 기준)을 numOfRows=1000 페이징으로
 * 21번쯤 호출하면 끝나서, 지자체복지서비스 때처럼 fail-fast/재시도 로직까지는
 * 필요 없다고 판단함(개발계정 하루 10,000건 한도에 비해 여유가 큼).
 *
 * ⚠️ 이건 "법정동코드 원본을 우리 DB(region_code)에 적재"까지만 하는 단계다.
 *    지자체복지서비스의 regionNameRaw 텍스트를 이 region_code랑 매칭해서
 *    BenefitProgram.regionCode를 채우는 로직은 별도 단계(다음 작업)로 남겨둔다.
 */
@Service
public class RegionCodeSyncService {

    private static final Logger log = LoggerFactory.getLogger(RegionCodeSyncService.class);

    private final LegalDongClient client;
    private final RegionCodeRepository repository;

    public RegionCodeSyncService(LegalDongClient client, RegionCodeRepository repository) {
        this.client = client;
        this.repository = repository;
    }

    @Transactional
    public void syncAllRegionCodes() {
        List<LegalDongItem> items = client.fetchAll();
        log.info("법정동코드 {}건 수신, DB 적재 시작", items.size());

        int success = 0;
        int failed = 0;
        for (LegalDongItem item : items) {
            try {
                upsertOne(item);
                success++;
            } catch (Exception e) {
                failed++;
                log.warn("법정동코드 적재 실패 region_cd={}: {}", item.regionCd(), e.getMessage());
            }
        }
        log.info("법정동코드 동기화 완료: 수신 {}건, 성공 {}건, 실패 {}건", items.size(), success, failed);
    }

    private void upsertOne(LegalDongItem item) {
        RegionCode entity = repository.findById(item.regionCd())
                .orElseGet(() -> new RegionCode(
                        item.regionCd(), item.sidoCd(), item.sggCd(), item.umdCd(), item.riCd(), item.locatAddNm()));

        entity.setFullName(item.locatAddNm());
        entity.setLowestName(item.locallowNm());
        entity.setHigherCode(item.locathighCd());
        entity.setLocatOrder(item.locatOrder());
        entity.setLastSyncedAt(LocalDateTime.now());

        repository.save(entity);
    }
}
