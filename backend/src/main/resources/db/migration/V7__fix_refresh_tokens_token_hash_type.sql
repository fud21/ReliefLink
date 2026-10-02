-- V6에서 token_hash를 CHAR(64)로 만들었는데, RefreshToken 엔티티는 VARCHAR 타입을 기대해서
-- ddl-auto: validate 단계에서 "found [bpchar], but expecting [char(64) (Types#VARCHAR)]"로
-- 막히는 문제를 고친다. CHAR(64)는 고정 길이라 짧은 값이 뒤에 공백으로 채워지는 반면,
-- 실제 저장되는 토큰 해시는 항상 64자 고정 길이라 값 자체는 동일하게 유지된다.
ALTER TABLE refresh_tokens ALTER COLUMN token_hash TYPE VARCHAR(64);
