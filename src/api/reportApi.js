import { apiFetch } from "./http";

export const createDamageReport = (payload) =>
  apiFetch("/damage-reports", {
    method: "POST",
    body: JSON.stringify(payload)
  });

// Spring Boot 예: @RequestPart("request") DamageReportRequest + @RequestPart("files") List<MultipartFile>
export const createDamageReportWithPhotos = ({ request, files }) => {
  const formData = new FormData();
  formData.append(
    "request",
    new Blob([JSON.stringify(request)], { type: "application/json" })
  );

  files.forEach((file) => formData.append("files", file));

  return apiFetch("/damage-reports", {
    method: "POST",
    body: formData
  });
};

export const getDamageReport = (id) =>
  apiFetch(`/damage-reports/${id}`);

export const requestDamageAnalysis = (reportId) =>
  apiFetch(`/damage-reports/${reportId}/analysis`, {
    method: "POST"
  });
