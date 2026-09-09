export const realtimeAlerts = [
  { type: "지진", place: "경주시 동부", time: "15:30", tone: "red" },
  { type: "산사태", place: "포항 북구", time: "14:10", tone: "orange" },
  { type: "정전", place: "제주 제주시", time: "12:05", tone: "yellow" }
];

export const programs = [
  {
    name: "재난지원금",
    status: "가능성 높음",
    deadline: "2026-09-17",
    dday: "D-14",
    tone: "green"
  },
  {
    name: "중소벤처 긴급지원",
    status: "가능성 높음",
    deadline: "2026-09-24",
    dday: "D-21",
    tone: "green"
  },
  {
    name: "지방세 감면",
    status: "확인 필요",
    deadline: "2026-10-01",
    dday: "D-28",
    tone: "blue"
  },
  {
    name: "건강보험료 경감",
    status: "가능성 높음",
    deadline: "2026-10-05",
    dday: "D-32",
    tone: "green"
  }
];

export const validationItems = [
  { label: "동일 재난 이벤트 일치", state: "ok" },
  { label: "기상청 강우량 일치", state: "ok" },
  { label: "신고 위치와 행정구역 일치", state: "ok" },
  { label: "건축물대장 용도 일치", state: "ok" },
  { label: "추가 기준정보 필요", state: "warn" }
];
