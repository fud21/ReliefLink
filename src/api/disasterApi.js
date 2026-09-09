import { apiFetch } from "./http";

export const getDisasters = () => apiFetch("/disasters");
export const getDisasterDetail = (id) => apiFetch(`/disasters/${id}`);
