import { CalendarClock, CheckCircle2, FileText, Lightbulb } from "lucide-react";
import { useNavigate } from "react-router-dom";
import PageHeader from "../components/PageHeader";
import SectionCard from "../components/SectionCard";
import StatusPill from "../components/StatusPill";
import { programs } from "../data/mockData";

export default function MatchingPage() {
  const navigate = useNavigate();

  return (
    <div className="page">
      <PageHeader
        eyebrow="지원제도 매칭"
        title="내 피해 상황에 맞는 지원제도를 골라드립니다"
        description="AI 분석 결과와 신청자 조건을 기준으로 매칭 가능성을 표시합니다."
      />

      <div className="matching-grid">
        <SectionCard title="맞춤 지원제도 추천">
          <div className="program-table">
            <div className="table-row table-head-row">
              <span>제도명</span><span>가능성</span><span>마감일</span><span>D-day</span>
            </div>
            {programs.map((item) => (
              <div className="table-row" key={item.name}>
                <strong>{item.name}</strong>
                <span><StatusPill tone={item.tone}>{item.status}</StatusPill></span>
                <span>{item.deadline}</span>
                <b className="dday">{item.dday}</b>
              </div>
            ))}
          </div>

          <div className="reason-box">
            <Lightbulb size={20} />
            <div>
              <strong>추천 이유 요약</strong>
              <p>
                침수 피해로 주거 기능이 일부 제한된 상황과 현재 피해 분석 결과를
                기준으로, 즉시 신청 가능한 제도와 확인이 필요한 제도를 구분했습니다.
              </p>
            </div>
          </div>
        </SectionCard>

        <SectionCard title="신청 현황 요약">
          <div className="summary-grid">
            <div className="summary-tile"><CheckCircle2 /><span>신청 가능 제도</span><strong>4건</strong></div>
            <div className="summary-tile"><FileText /><span>우선 신청</span><strong>2건</strong></div>
            <div className="summary-tile"><CalendarClock /><span>마감 임박</span><strong>1건</strong></div>
          </div>
          <button className="primary-btn full" onClick={() => navigate("/draft")}>
            서류 준비 시작
          </button>
        </SectionCard>
      </div>
    </div>
  );
}
