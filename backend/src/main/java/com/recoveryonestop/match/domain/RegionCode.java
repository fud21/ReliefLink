package com.recoveryonestop.match.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 법정동코드(행정안전부 15077871) 적재 테이블.
 * 지자체복지서비스의 {@code BenefitProgram.regionNameRaw}("경상남도 사천시" 같은 텍스트)를
 * 실제 지역코드로 바꾸는 매핑 로직이 이 테이블을 조회 대상으로 삼는다
 * (매핑 로직 자체는 별도 서비스에서 다음 단계로 구현 예정).
 */
@Entity
@Table(name = "region_code")
public class RegionCode {

    /** region_cd. 10자리 법정동코드. */
    @Id
    @Column(name = "code", length = 10)
    private String code;

    @Column(name = "sido_cd", length = 2, nullable = false)
    private String sidoCd;

    @Column(name = "sgg_cd", length = 3)
    private String sggCd;

    @Column(name = "umd_cd", length = 3)
    private String umdCd;

    @Column(name = "ri_cd", length = 2)
    private String riCd;

    /** locatadd_nm. 전체 주소명(예: "대구광역시 서구 원대동3가") — 매핑 로직에서 문자열 비교용. */
    @Column(name = "full_name", length = 100, nullable = false)
    private String fullName;

    /** locallow_nm. 마지막 단위 이름만(예: "원대동3가"). */
    @Column(name = "lowest_name", length = 50)
    private String lowestName;

    /** locathigh_cd. 상위 행정구역 코드. */
    @Column(name = "higher_code", length = 10)
    private String higherCode;

    @Column(name = "locat_order")
    private Integer locatOrder;

    @Column(name = "last_synced_at", nullable = false)
    private LocalDateTime lastSyncedAt = LocalDateTime.now();

    protected RegionCode() {
        // JPA용
    }

    public RegionCode(String code, String sidoCd, String sggCd, String umdCd, String riCd, String fullName) {
        this.code = code;
        this.sidoCd = sidoCd;
        this.sggCd = sggCd;
        this.umdCd = umdCd;
        this.riCd = riCd;
        this.fullName = fullName;
    }

    public String getCode() {
        return code;
    }

    public String getSidoCd() {
        return sidoCd;
    }

    public void setSidoCd(String sidoCd) {
        this.sidoCd = sidoCd;
    }

    public String getSggCd() {
        return sggCd;
    }

    public void setSggCd(String sggCd) {
        this.sggCd = sggCd;
    }

    public String getUmdCd() {
        return umdCd;
    }

    public void setUmdCd(String umdCd) {
        this.umdCd = umdCd;
    }

    public String getRiCd() {
        return riCd;
    }

    public void setRiCd(String riCd) {
        this.riCd = riCd;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getLowestName() {
        return lowestName;
    }

    public void setLowestName(String lowestName) {
        this.lowestName = lowestName;
    }

    public String getHigherCode() {
        return higherCode;
    }

    public void setHigherCode(String higherCode) {
        this.higherCode = higherCode;
    }

    public Integer getLocatOrder() {
        return locatOrder;
    }

    public void setLocatOrder(Integer locatOrder) {
        this.locatOrder = locatOrder;
    }

    public LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(LocalDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }
}
