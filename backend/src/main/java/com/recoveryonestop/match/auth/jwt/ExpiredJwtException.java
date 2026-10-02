package com.recoveryonestop.match.auth.jwt;

public class ExpiredJwtException extends RuntimeException {

    public ExpiredJwtException() {
        super("만료된 JWT입니다.");
    }
}
