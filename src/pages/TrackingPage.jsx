import {
  AlertCircle,
  Building2,
  Check,
  FileText,
  MessageCircle,
  Phone
} from "lucide-react";
import PageHeader from "../components/PageHeader";
import SectionCard from "../components/SectionCard";

const steps = [
  ["신고 접수", "2026-08-12", true],
  ["서류 제출", "제출됨", true],
  ["주민센터 검토", "진행 중", true],
  ["보완서류 요청", "필요", false],
  ["지급 심사", "대기", false],
  ["완료", "", false]
];

export default function TrackingPage() {
  return (
    <div className="page">
      <PageHeader
        eyebrow="진행상태 추적"
        title="신고부터 지원금 처리까지 한 번에 확인"
        description="기관 진행상태와 추가 보완 요청을 놓치지 않도록 정리합니다."
      />

      <SectionCard>
        <div className="stepper">
          {steps.map(([label, sub, active], idx) => (
            <div className={`step ${active ? "active" : ""}`} key={label}>
              <div className="step-circle">
                {active ? <Check size={16} /> : idx + 1}
              </div>
              <strong>{label}</strong>
              <span>{sub}</span>
            </div>
          ))}
        </div>
      </SectionCard>

      <div className="tracking-grid">
        <SectionCard title="제출 서류">
          <div className="tracking-card-row">
            <FileText size={20} />
            <div><strong>주민센터</strong><span>피해신고서, 사진 3건 제출 완료</span></div>
          </div>
          <div className="tracking-card-row">
            <FileText size={20} />
            <div><strong>복지로</strong><span>신청서 접수 준비</span></div>
          </div>
        </SectionCard>

        <SectionCard title="담당 기관">
          <div className="agency-box">
            <Building2 size={22} />
            <div>
              <strong>거제시 장평동 주민센터</strong>
              <span><Phone size={14} /> 055-123-4567</span>
              <span>접수번호 JPG-2026-0812-00125</span>
            </div>
          </div>
        </SectionCard>

        <SectionCard title="보완 서류 필요" className="danger-card">
          <div className="danger-box">
            <AlertCircle size={20} />
            <div>
              <strong>거주 확인 서류</strong>
              <span>추가 제출이 필요합니다.</span>
            </div>
          </div>
          <button className="secondary-btn full">상세보기</button>
        </SectionCard>
      </div>

      <div className="tracking-bottom-grid">
        <SectionCard title="1:1 상담 연결">
          <div className="chat-line">
            <MessageCircle size={19} />
            <span>담당자와 진행상태를 상담할 수 있습니다.</span>
            <button className="mini-btn">상담 연결</button>
          </div>
        </SectionCard>

        <SectionCard title="진행 메모">
          <p className="muted">
            필요한 보완서류를 제출하면 담당기관 검토 후 지급심사 단계로 이동합니다.
          </p>
        </SectionCard>
      </div>
    </div>
  );
}
