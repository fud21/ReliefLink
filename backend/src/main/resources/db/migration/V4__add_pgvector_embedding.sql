-- pgvector 확장 활성화 + benefit_program에 임베딩 컬럼 추가.
--
-- 2026-09-18: 임베딩 모델은 Gemini gemini-embedding-001, outputDimensionality=768로 결정
-- (계정에 남은 유료 크레딧 사용. 프로젝트 규모(~4천여 건 제도)에는 기본 3072차원이 과해서
-- Matryoshka 축소 차원 중 768을 선택함 — V1 마이그레이션 주석에 남아있던 "vector(1536)"은
-- 확정 전 임시 메모였고 실제로는 적용된 적 없음, 이 마이그레이션이 처음 적용).
CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE benefit_program ADD COLUMN embedding vector(768);

-- ⚠️ 아직 배치 임베딩 전이라 데이터가 비어있는 상태. 유사도 검색용 인덱스(ivfflat/hnsw)는
-- 지금 만들지 않는다 — 현재 규모(~4천여 건)면 인덱스 없이 순차 스캔으로도 충분히 빠르고,
-- ivfflat류 인덱스는 데이터가 어느 정도 채워진 뒤에 만드는 게 권장 방식이라, 배치 임베딩을
-- 끝낸 뒤 실제로 느리면 그때 별도 마이그레이션으로 추가한다.
