-- 법정동코드(행정안전부_행정표준코드_법정동코드, data.go.kr 15077871) 적재 테이블.
-- code(PK)는 10자리 법정동코드(region_cd). full_name으로 지자체복지서비스의
-- regionNameRaw("경상남도 사천시" 등) 텍스트와 매칭시키는 로직에서 사용할 예정(다음 단계).
CREATE TABLE region_code (
    code            VARCHAR(10) PRIMARY KEY,
    sido_cd         VARCHAR(2)  NOT NULL,
    sgg_cd          VARCHAR(3),
    umd_cd          VARCHAR(3),
    ri_cd           VARCHAR(2),
    full_name       VARCHAR(100) NOT NULL,
    lowest_name     VARCHAR(50),
    higher_code     VARCHAR(10),
    locat_order     INTEGER,
    last_synced_at  TIMESTAMP NOT NULL
);

CREATE INDEX idx_region_code_full_name ON region_code (full_name);
CREATE INDEX idx_region_code_sido_sgg ON region_code (sido_cd, sgg_cd);
