import {
  AlertTriangle,
  CheckCircle2,
  FileCheck2,
  SearchCheck,
  Sparkles
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import PageHeader from "../components/PageHeader";
import SectionCard from "../components/SectionCard";
import { validationItems } from "../data/mockData";

const photos = ["🏠", "🚪", "🧱"];

export default function AnalysisPage() {
  const navigate = useNavigate();

  return (
    <div className="page">
      <PageHeader
        eyebrow="AI 분석"
        title="AI 피해 분석 + 공공데이터 교차검증"
        description=" 위치·재난·건축물 정보를 함께 확인합니다."
      />

      <div className="analysis-grid">
        <SectionCard title="업로드된 사진">
          <div className="uploaded-list">
            {photos.map((item, idx) => (
              <div className="uploaded-thumb" key={idx}>
                <span>{item}</span>
                <small>피해 사진 {idx + 1}</small>
              </div>
            ))}
          </div>
        </SectionCard>

        <SectionCard title="AI 분석 결과">
          <div className="result-list">
            <div><span>피해 유형</span><strong>주택 침수</strong></div>
            <div><span>피해 정도</span><strong className="orange-text">중간</strong></div>
            <div><span>가전도구 손상 감지</span><strong className="red-text">감지됨</strong></div>
          </div>

          <div className="confidence-block">
            <span>종합 일관성 점수</span>
            <div className="donut" style={{"--pct": "83%"}}>
              <strong>83%</strong>
            </div>
          </div>
        </SectionCard>

        <SectionCard title="공공데이터 교차검증">
          <div className="validation-list">
            {validationItems.map((item) => (
              <div className={`validation-row ${item.state}`} key={item.label}>
                {item.state === "ok"
                  ? <CheckCircle2 size={18} />
                  : <AlertTriangle size={18} />}
                <span>{item.label}</span>
              </div>
            ))}
          </div>
        </SectionCard>
      </div>

      <SectionCard title="판단 근거">
        <div className="evidence-box">
          <SearchCheck size={21} />
          <p>
            해당 지역 침수 이벤트와 신고 시각·위치가 일치하며, 촬영 이미지의
            실내 침수 흔적과 벽면 변색 패턴이 탐지되었습니다. 건축물 정보와
            피해 대상 입력값도 서로 모순되지 않아 종합 일관성 점수 83%로
            판단했습니다.
          </p>
        </div>
      </SectionCard>

      <div className="action-row right">
        <button className="secondary-btn" onClick={() => navigate("/report")}>
          신고 내용 수정
        </button>
        <button className="primary-btn" onClick={() => navigate("/matching")}>
          <FileCheck2 size={18} />
          지원제도 매칭
        </button>
      </div>
    </div>
  );
}
