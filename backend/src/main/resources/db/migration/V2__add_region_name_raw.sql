-- 지자체복지서비스 API는 법정동코드가 아니라 "경상남도"/"사천시" 같은 텍스트로 지역을 준다.
-- 문자열→법정동코드 매핑 테이블이 아직 없어(인수인계 문서 미결사항 #5) 원문만 우선 보관.
-- region_code는 매핑 테이블이 생기기 전까지 지자체 제도도 NULL로 남는다.
ALTER TABLE benefit_program ADD COLUMN region_name_raw VARCHAR(100);
