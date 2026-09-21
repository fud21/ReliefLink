package com.recoveryonestop.match.disaster.controller;

import com.recoveryonestop.match.disaster.dto.DisasterAlertResponse;
import com.recoveryonestop.match.disaster.service.DisasterService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/disasters")
public class DisasterController {

    private final DisasterService disasterService;

    public DisasterController(DisasterService disasterService) {
        this.disasterService = disasterService;
    }

    /**
     * 예:
     * GET /api/disasters
     * GET /api/disasters?date=2026-09-21
     * GET /api/disasters?date=2026-09-21&region=경상북도
     */
    @GetMapping
    public List<DisasterAlertResponse> getDisasters(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String region
    ) {
        return disasterService.getAlerts(date, region);
    }
}
