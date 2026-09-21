package com.recoveryonestop.match.disaster.service;

import com.recoveryonestop.match.disaster.client.SafetyDisasterClient;
import com.recoveryonestop.match.disaster.dto.DisasterAlertResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class DisasterService {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final SafetyDisasterClient safetyDisasterClient;

    public DisasterService(SafetyDisasterClient safetyDisasterClient) {
        this.safetyDisasterClient = safetyDisasterClient;
    }

    public List<DisasterAlertResponse> getAlerts(LocalDate date, String region) {
        LocalDate targetDate = date != null ? date : LocalDate.now(KOREA_ZONE);
        return safetyDisasterClient.fetch(targetDate, region);
    }
}
