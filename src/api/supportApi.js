import { apiFetch } from "./http";

// 현재 BE에서 실제 구현되어 있는 사용자용 매칭 API.
// DB의 DamageReport/reportId 연동이 완료되기 전에는 ReportPage 입력값을 직접 전달한다.
export const matchPrograms = (payload) =>
  apiFetch("/match", {
    method: "POST",
    body: JSON.stringify(payload)
  });

// DB 담당자 통합 이후 reportId 기반 오케스트레이션 API가 생기면 사용할 예정.
export const getMatchedPrograms = (reportId) =>
  apiFetch(`/support-programs/matches?reportId=${reportId}`);

export const createApplicationDraft = (programId, reportId) =>
  apiFetch("/applications/drafts", {
    method: "POST",
    body: JSON.stringify({ programId, reportId })
  });

export const getApplicationStatus = (applicationId) =>
  apiFetch(`/applications/${applicationId}/status`);
