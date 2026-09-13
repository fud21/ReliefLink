package com.recoveryonestop.match.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "benefit_program",
        uniqueConstraints = @UniqueConstraint(name = "uk_benefit_program_source_serv", columnNames = {"source", "serv_id"}),
        indexes = @Index(name = "idx_benefit_program_region", columnList = "region_code")
)
public class BenefitProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProgramSource source;

    /**
     * 복지로 원본 서비스 ID (예: 중앙부처 API의 servId).
     * RULE_TABLE 소스는 우리가 부여한 임의 코드(예: "RULE_FLOOD_2026") 사용.
     * 재적재 시 upsert 키로 사용.
     */
    @Column(name = "serv_id", nullable = false, length = 50)
    private String servId;

    @Column(nullable = false, length = 300)
    private String name;

    @Column(length = 200)
    private String agency;

    /** 지자체 제도만 값 존재. 전국 대상(중앙부처)은 null */
    @Column(name = "region_code", length = 10)
    private String regionCode;

    @Column(name = "target_text")
    private String targetText;

    @Column(name = "content_text")
    private String contentText;

    @Column(name = "apply_text")
    private String applyText;

    /**
     * 원본 API는 보통 "구비서류"를 별도 필드로 안 주고 신청방법 텍스트에 섞여 있음.
     * 1차 적재 단계에서는 비워두고, 매칭/서류안내 단계(W3)에서 채우는 걸 권장.
     */
    @ElementCollection
    @CollectionTable(name = "benefit_program_required_docs", joinColumns = @JoinColumn(name = "program_id"))
    @Column(name = "doc_name", length = 200)
    private List<String> requiredDocs = new ArrayList<>();

    @Column(name = "deadline_rule", length = 500)
    private String deadlineRule;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    /** pgvector 붙이기 전 단계 표시용 플래그. 임베딩 컬럼은 W3에서 추가 예정 */
    @Column(name = "embedding_generated", nullable = false)
    private boolean embeddingGenerated = false;

    @Column(name = "raw_last_mod_date")
    private LocalDate rawLastModDate;

    @Column(name = "last_synced_at", nullable = false)
    private LocalDateTime lastSyncedAt = LocalDateTime.now();

    protected BenefitProgram() {
        // JPA용
    }

    public BenefitProgram(ProgramSource source, String servId, String name) {
        this.source = source;
        this.servId = servId;
        this.name = name;
    }

    // --- getters / setters ---

    public Long getId() {
        return id;
    }

    public ProgramSource getSource() {
        return source;
    }

    public void setSource(ProgramSource source) {
        this.source = source;
    }

    public String getServId() {
        return servId;
    }

    public void setServId(String servId) {
        this.servId = servId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAgency() {
        return agency;
    }

    public void setAgency(String agency) {
        this.agency = agency;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public void setRegionCode(String regionCode) {
        this.regionCode = regionCode;
    }

    public String getTargetText() {
        return targetText;
    }

    public void setTargetText(String targetText) {
        this.targetText = targetText;
    }

    public String getContentText() {
        return contentText;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
    }

    public String getApplyText() {
        return applyText;
    }

    public void setApplyText(String applyText) {
        this.applyText = applyText;
    }

    public List<String> getRequiredDocs() {
        return requiredDocs;
    }

    public void setRequiredDocs(List<String> requiredDocs) {
        this.requiredDocs = requiredDocs;
    }

    public String getDeadlineRule() {
        return deadlineRule;
    }

    public void setDeadlineRule(String deadlineRule) {
        this.deadlineRule = deadlineRule;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public boolean isEmbeddingGenerated() {
        return embeddingGenerated;
    }

    public void setEmbeddingGenerated(boolean embeddingGenerated) {
        this.embeddingGenerated = embeddingGenerated;
    }

    public LocalDate getRawLastModDate() {
        return rawLastModDate;
    }

    public void setRawLastModDate(LocalDate rawLastModDate) {
        this.rawLastModDate = rawLastModDate;
    }

    public LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(LocalDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }
}
