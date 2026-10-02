package com.recoveryonestop.match.common.exception;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Refresh Token이 유효하지 않습니다.");
    }
}
