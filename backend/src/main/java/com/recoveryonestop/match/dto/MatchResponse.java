package com.recoveryonestop.match.dto;

import java.util.List;

/** POST /api/match 응답. */
public record MatchResponse(
        int totalCount,
        List<MatchedProgram> matches
) {
}
