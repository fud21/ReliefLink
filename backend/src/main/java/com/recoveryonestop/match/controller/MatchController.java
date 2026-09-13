package com.recoveryonestop.match.controller;

import com.recoveryonestop.match.dto.MatchRequest;
import com.recoveryonestop.match.dto.MatchResponse;
import com.recoveryonestop.match.service.MatchService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping("/api/match")
    public MatchResponse match(@RequestBody MatchRequest request) {
        return matchService.match(request);
    }
}
