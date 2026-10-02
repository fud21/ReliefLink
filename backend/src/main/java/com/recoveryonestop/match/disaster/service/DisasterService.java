package com.recoveryonestop.match.disaster.service;

import com.recoveryonestop.match.disaster.client.SafetyDisasterClient;
import com.recoveryonestop.match.disaster.dto.DisasterAlertResponse;
import com.recoveryonestop.match.service.AddressToRegionCodeService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class DisasterService {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final SafetyDisasterClient safetyDisasterClient;
    private final AddressToRegionCodeService addressToRegionCodeService;

    public DisasterService(SafetyDisasterClient safetyDisasterClient,
                            AddressToRegionCodeService addressToRegionCodeService) {
        this.safetyDisasterClient = safetyDisasterClient;
        this.addressToRegionCodeService = addressToRegionCodeService;
    }

    public List<DisasterAlertResponse> getAlerts(LocalDate date, String region) {
        LocalDate targetDate = date != null ? date : LocalDate.now(KOREA_ZONE);
        List<DisasterAlertResponse> alerts = safetyDisasterClient.fetch(targetDate, region);

        return alerts.stream()
                .map(this::withRegionCodes)
                .toList();
    }

    /** region(원문 수신 지역 텍스트)을 법정동코드로 변환해 regionCodes를 채운 새 응답을 만든다. */
    private DisasterAlertResponse withRegionCodes(DisasterAlertResponse alert) {
        List<String> regionCodes = addressToRegionCodeService.resolveMultiple(alert.region());
        return new DisasterAlertResponse(
                alert.id(),
                alert.type(),
                alert.region(),
                alert.message(),
                alert.emergencyLevel(),
                alert.occurredAt(),
                alert.regionId(),
                alert.disasterTypeId(),
                alert.emergencyStepId(),
                regionCodes
        );
    }
}
