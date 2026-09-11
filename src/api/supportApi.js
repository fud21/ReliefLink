import { apiFetch } from "./http";

export const getMatchedPrograms = (reportId) =>
  apiFetch(`/support-programs/matches?reportId=${reportId}`);

export const createApplicationDraft = (programId, reportId) =>
  apiFetch("/applications/drafts", {
    method: "POST",
    body: JSON.stringify({ programId, reportId })
  });

export const getApplicationStatus = (applicationId) =>
  apiFetch(`/applications/${applicationId}/status`);
