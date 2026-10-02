package com.recoveryonestop.match.service;

import com.recoveryonestop.match.domain.RegionCode;
import com.recoveryonestop.match.domain.RegionCodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * safetydata.go.kr가 IP 미등록으로 막혀 있어서 실제 긴급재난문자 API로 끝까지 테스트할 수
 * 없는 동안에도, region 문자열 → 법정동코드 변환 로직 자체는 네트워크/DB 연결 없이 이
 * 테스트로 검증할 수 있다. RegionCodeRepository를 mock으로 대체해서 resolve()/
 * resolveMultiple()의 분기 로직만 순수하게 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class AddressToRegionCodeServiceTest {

    @Mock
    private RegionCodeRepository regionCodeRepository;

    @InjectMocks
    private AddressToRegionCodeService service;

    @Test
    void resolve_null이나_빈문자열은_null을_반환한다() {
        assertNull(service.resolve(null));
        assertNull(service.resolve("   "));
    }

    @Test
    void resolve_토큰이_하나뿐이면_null을_반환한다() {
        assertNull(service.resolve("강남구"));
    }

    @Test
    void resolve_정상_매칭되면_코드를_반환한다() {
        when(regionCodeRepository.findAllByFullName("서울특별시 강남구"))
                .thenReturn(List.of(regionCode("1168000000", "서울특별시 강남구")));

        assertEquals("1168000000", service.resolve("서울특별시 강남구"));
    }

    @Test
    void resolve_중복매칭이면_첫번째를_반환한다() {
        when(regionCodeRepository.findAllByFullName("서울특별시 강남구"))
                .thenReturn(List.of(
                        regionCode("1168000000", "서울특별시 강남구"),
                        regionCode("1168000001", "서울특별시 강남구")));

        assertEquals("1168000000", service.resolve("서울특별시 강남구"));
    }

    @Test
    void resolve_매칭실패면_null을_반환한다() {
        when(regionCodeRepository.findAllByFullName("서울특별시 없는구"))
                .thenReturn(List.of());

        assertNull(service.resolve("서울특별시 없는구"));
    }

    @Test
    void resolveMultiple_null이나_빈문자열은_빈리스트를_반환한다() {
        assertTrue(service.resolveMultiple(null).isEmpty());
        assertTrue(service.resolveMultiple("   ").isEmpty());
    }

    @Test
    void resolveMultiple_쉼표로_나열되고_뒤쪽은_시도가_생략된_경우_앞쪽_시도를_공유한다() {
        when(regionCodeRepository.findAllByFullName("서울특별시 강남구"))
                .thenReturn(List.of(regionCode("1168000000", "서울특별시 강남구")));
        when(regionCodeRepository.findAllByFullName("서울특별시 서초구"))
                .thenReturn(List.of(regionCode("1165000000", "서울특별시 서초구")));

        List<String> codes = service.resolveMultiple("서울특별시 강남구,서초구");

        assertEquals(List.of("1168000000", "1165000000"), codes);
    }

    @Test
    void resolveMultiple_첫조각부터_시도가_없으면_그조각만_건너뛴다() {
        when(regionCodeRepository.findAllByFullName("서울특별시 서초구"))
                .thenReturn(List.of(regionCode("1165000000", "서울특별시 서초구")));

        // "강남구"는 시/도 정보 없이 시작해서 공유할 시/도를 모르니 건너뛰고,
        // 그다음 "서울특별시 서초구"는 자체적으로 시/도를 포함하므로 정상 매칭된다.
        List<String> codes = service.resolveMultiple("강남구,서울특별시 서초구");

        assertEquals(List.of("1165000000"), codes);
    }

    @Test
    void resolveMultiple_중복으로_매칭된_코드는_한번만_담는다() {
        when(regionCodeRepository.findAllByFullName("서울특별시 강남구"))
                .thenReturn(List.of(regionCode("1168000000", "서울특별시 강남구")));

        List<String> codes = service.resolveMultiple("서울특별시 강남구,강남구");

        assertEquals(List.of("1168000000"), codes);
    }

    @Test
    void resolveMultiple_일부만_매칭되면_매칭된_것만_반환한다() {
        when(regionCodeRepository.findAllByFullName("서울특별시 강남구"))
                .thenReturn(List.of(regionCode("1168000000", "서울특별시 강남구")));
        when(regionCodeRepository.findAllByFullName("서울특별시 없는구"))
                .thenReturn(List.of());

        List<String> codes = service.resolveMultiple("서울특별시 강남구,없는구");

        assertEquals(List.of("1168000000"), codes);
    }

    private RegionCode regionCode(String code, String fullName) {
        return new RegionCode(code, fullName.substring(0, 2), null, null, null, fullName);
    }
}
