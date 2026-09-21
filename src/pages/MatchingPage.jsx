import {
  CalendarClock,
  CheckCircle2,
  FileText,
  Lightbulb
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

import { matchPrograms } from "../api/supportApi";
import PageHeader from "../components/PageHeader";
import SectionCard from "../components/SectionCard";
import StatusPill from "../components/StatusPill";

function getStatus(program) {
  return program.verdict || "확인 필요";
}

function getTone(program) {
  return getStatus(program) === "가능성 높음" ? "green" : "blue";
}

function formatDDay(value) {
  if (value == null) return "-";
  if (value === 0) return "D-Day";
  if (value > 0) return `D-${value}`;
  return `D+${Math.abs(value)}`;
}

export default function MatchingPage() {
  const navigate = useNavigate();
  const [programs, setPrograms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;

    const raw = sessionStorage.getItem("relieflink.matchRequest");
    if (!raw) {
      setError("피해 신고 정보가 없습니다. 피해 신고를 먼저 입력해주세요.");
      setLoading(false);
      return () => {};
    }

    let request;
    try {
      request = JSON.parse(raw);
    } catch {
      setError("저장된 피해 신고 정보를 읽을 수 없습니다.");
      setLoading(false);
      return () => {};
    }

    matchPrograms(request)
      .then((response) => {
        if (!cancelled) {
          setPrograms(Array.isArray(response?.matches) ? response.matches : []);
        }
      })
      .catch((err) => {
        console.error("[MatchingPage] 매칭 API 호출 실패:", err);
        if (!cancelled) setError(err.message);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const possibleCount = useMemo(
    () => programs.filter((item) => getStatus(item) === "가능성 높음").length,
    [programs]
  );

  const urgentCount = useMemo(
    () => programs.filter((item) => item.dDay != null && item.dDay >= 0 && item.dDay <= 7).length,
    [programs]
  );

  const representativeReason =
    programs.find((item) => item.reason)?.reason ||
    "피해 신고 정보를 기준으로 지원제도를 조회합니다.";

  return (
    <div className="page">
      <PageHeader
        eyebrow="지원제도 매칭"
        title="내 피해 상황에 맞는 지원제도를 골라드립니다"
        description="피해 신고 정보와 지원제도 조건을 기준으로 매칭 결과를 표시합니다."
      />

      <div className="matching-grid">
        <SectionCard title="맞춤 지원제도 추천">
          {loading && <p>지원제도를 매칭하고 있습니다.</p>}

          {!loading && error && (
            <div className="reason-box">
              <Lightbulb size={20} />
              <div>
                <strong>매칭 정보를 불러오지 못했습니다.</strong>
                <p>{error}</p>
                <button className="secondary-btn" onClick={() => navigate("/report")}>
                  피해 신고로 이동
                </button>
              </div>
            </div>
          )}

          {!loading && !error && programs.length === 0 && (
            <p>현재 조건에 맞는 지원제도를 찾지 못했습니다.</p>
          )}

          {!loading && !error && programs.length > 0 && (
            <>
              <div className="program-table">
                <div className="table-row table-head-row">
                  <span>제도명</span><span>가능성</span><span>마감일</span><span>D-day</span>
                </div>

                {programs.map((item) => (
                  <div className="table-row" key={item.programId ?? item.name}>
                    <strong>{item.name}</strong>
                    <span>
                      <StatusPill tone={getTone(item)}>
                        {getStatus(item)}
                      </StatusPill>
                    </span>
                    <span>{item.deadlineAt || "계산 불가"}</span>
                    <b className="dday">{formatDDay(item.dDay)}</b>
                  </div>
                ))}
              </div>

              <div className="reason-box">
                <Lightbulb size={20} />
                <div>
                  <strong>추천 이유 요약</strong>
                  <p>{representativeReason}</p>
                </div>
              </div>
            </>
          )}
        </SectionCard>

        <SectionCard title="신청 현황 요약">
          <div className="summary-grid">
            <div className="summary-tile">
              <CheckCircle2 />
              <span>신청 가능성 높음</span>
              <strong>{possibleCount}건</strong>
            </div>
            <div className="summary-tile">
              <FileText />
              <span>전체 추천</span>
              <strong>{programs.length}건</strong>
            </div>
            <div className="summary-tile">
              <CalendarClock />
              <span>7일 내 마감</span>
              <strong>{urgentCount}건</strong>
            </div>
          </div>

          <button
            className="primary-btn full"
            disabled={loading || !!error || programs.length === 0}
            onClick={() => navigate("/draft")}
          >
            서류 준비 시작
          </button>
        </SectionCard>
      </div>
    </div>
  );
}
