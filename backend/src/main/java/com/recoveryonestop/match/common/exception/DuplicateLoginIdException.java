package com.recoveryonestop.match.common.exception;

public class DuplicateLoginIdException extends RuntimeException {

    public DuplicateLoginIdException() {
        super("이미 사용 중인 로그인 아이디입니다.");
    }
}
