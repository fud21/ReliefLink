package com.recoveryonestop.match.auth.jwt;

public class InvalidJwtException extends RuntimeException {

    public InvalidJwtException() {
        super("유효하지 않은 JWT입니다.");
    }
}
