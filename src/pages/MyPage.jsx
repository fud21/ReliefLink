import {
  AlertCircle,
  Building2,
  CheckCircle2,
  ChevronRight,
  FileCheck2,
  FileText,
  FolderOpen,
  HelpCircle,
  Home,
  Landmark,
  Phone,
  ShieldCheck,
  Store,
  UserRound,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import PageHeader from "../components/PageHeader";
import SectionCard from "../components/SectionCard";
import StatusPill from "../components/StatusPill";

const reports = [
  {
    id: "JPG-2026-0812-00125",
    type: "주택 침수",
    target: "개인/주거",
    address: "경상남도 거제시 장평1로 123",
    submittedAt: "2026.08.12",
    status: "보완서류 요청",
    statusTone: "orange",
    latestUpdate: "2026.09.08 · 거주 확인 서류 추가 제출 필요",
    completedStep: 3,
  },
  {
    id: "JPG-2026-0703-00042",
    type: "상가 침수",
    target: "소상공인·사업장",
    address: "경상남도 거제시 고현로 21",
    submittedAt: "2026.07.03",
    status: "처리 완료",
    statusTone: "green",
    latestUpdate: "2026.07.28 · 지원금 지급 완료",
    completedStep: 5,
  },
];

const trackingSteps = [
  "신고 접수",
  "서류 확인",
  "보완 요청",
  "기관 심사",
  "처리 완료",
];

const documents = [
  { label: "피해 사진 3장", state: "done", detail: "2026.08.12 제출" },
  { label: "신분증", state: "done", detail: "2026.08.12 제출" },
  { label: "통장 사본", state: "done", detail: "2026.08.12 제출" },
  {
    label: "거주 확인 서류",
    state: "required",
    detail: "추가 제출 필요",
  },
];

const contacts = [
  {
    icon: Home,
    title: "장평동 행정복지센터",
    description: "피해 신고 및 지원제도 관련 1차 문의",
    phone: "055-XXX-XXXX",
    primary: true,
  },
  {
    icon: Landmark,
    title: "거제시 재난안전 담당부서",
    description: "피해 사실 확인 및 재난 행정 절차 문의",
    phone: "055-XXX-XXXX",
  },
  {
    icon: Store,
    title: "소상공인 지원 창구",
    description: "사업장 피해 및 경영지원 제도 문의",
    phone: "기관 연결 예정",
  },
  {
    icon: ShieldCheck,
    title: "가입 보험사",
    description: "가입 상품의 재난 피해 보장 여부 확인",
    phone: "내 보험 정보 연결 예정",
  },
];

export default function MyPage() {
  const navigate = useNavigate();
  const activeReport = reports[0];

  return (
    <div className="page">
      <PageHeader
        eyebrow="마이페이지"
        title="내 신고와 지원 진행상황을 한 번에 확인하세요"
        description="제출한 피해 신고, 서류 보완 요청, 담당기관 정보를 모아 보여드립니다."
      />

      <SectionCard className="my-profile-card">
        <div className="my-profile">
          <div className="my-avatar">
            <UserRound size={28} />
          </div>

          <div className="my-profile-copy">
            <strong>홍길동</strong>
            <span>경상남도 거제시 · 개인/주거</span>
            <small>본인인증 완료</small>
          </div>

          <button type="button" className="secondary-btn compact">
            내 정보 수정
          </button>
        </div>
      </SectionCard>

      <div className="mypage-summary-grid">
        <div className="mypage-summary-card">
          <FileText size={20} />
          <span>전체 피해 신고</span>
          <strong>{reports.length}건</strong>
        </div>

        <div className="mypage-summary-card warning">
          <AlertCircle size={20} />
          <span>확인 필요</span>
          <strong>1건</strong>
        </div>

        <div className="mypage-summary-card">
          <FolderOpen size={20} />
          <span>보관 서류</span>
          <strong>4건</strong>
        </div>
      </div>

      <SectionCard
        title="내가 제출한 피해 신고"
        subtitle="신고별 현재 처리 상태와 최근 변경사항을 확인할 수 있습니다."
      >
        <div className="my-report-list">
          {reports.map((report) => (
            <article className="my-report-card" key={report.id}>
              <div className="my-report-top">
                <div>
                  <div className="my-report-title">
                    <strong>{report.type}</strong>
                    <StatusPill tone={report.statusTone}>{report.status}</StatusPill>
                  </div>
                  <span>{report.address}</span>
                </div>

                <button
                  type="button"
                  className="icon-link-button"
                  onClick={() => navigate("/tracking")}
                  aria-label="진행상황 보기"
                >
                  <ChevronRight size={19} />
                </button>
              </div>

              <div className="my-report-meta">
                <span>접수일 {report.submittedAt}</span>
                <span>접수번호 {report.id}</span>
                <span>{report.target}</span>
              </div>

              <div className="mini-progress">
                {trackingSteps.map((step, index) => {
                  const done = index < report.completedStep;
                  const current = index === report.completedStep - 1;

                  return (
                    <div
                      key={step}
                      className={`mini-progress-step ${done ? "done" : ""} ${
                        current ? "current" : ""
                      }`}
                    >
                      <span className="mini-progress-dot">
                        {done ? <CheckCircle2 size={13} /> : index + 1}
                      </span>
                      <small>{step}</small>
                    </div>
                  );
                })}
              </div>

              <div className="my-report-update">
                <AlertCircle size={16} />
                <span>{report.latestUpdate}</span>
              </div>

              <div className="my-report-actions">
                <button
                  type="button"
                  className="secondary-btn compact"
                  onClick={() => navigate("/tracking")}
                >
                  진행상황
                </button>
                <button
                  type="button"
                  className="ghost-btn compact"
                  onClick={() => navigate("/draft")}
                >
                  제출서류
                </button>
              </div>
            </article>
          ))}
        </div>
      </SectionCard>

      <div className="mypage-detail-grid">
        <SectionCard
          title="제출 서류함"
          subtitle={`${activeReport.type} · ${activeReport.id}`}
        >
          <div className="my-document-list">
            {documents.map((document) => (
              <div
                className={`my-document-row ${document.state}`}
                key={document.label}
              >
                <div className="my-document-icon">
                  {document.state === "done" ? (
                    <FileCheck2 size={18} />
                  ) : (
                    <AlertCircle size={18} />
                  )}
                </div>

                <div>
                  <strong>{document.label}</strong>
                  <span>{document.detail}</span>
                </div>

                <button type="button" className="mini-btn">
                  {document.state === "done" ? "확인" : "제출"}
                </button>
              </div>
            ))}
          </div>

          <button
            type="button"
            className="secondary-btn full"
            onClick={() => navigate("/draft")}
          >
            <FolderOpen size={17} />
            전체 서류 보기
          </button>
        </SectionCard>

        <SectionCard
          title="도움 받을 곳"
          subtitle="피해지역과 신고 유형을 기준으로 우선 문의할 기관을 안내합니다."
        >
          <div className="contact-list">
            {contacts.map(({ icon: Icon, title, description, phone, primary }) => (
              <div className={`contact-row ${primary ? "primary" : ""}`} key={title}>
                <div className="contact-icon">
                  <Icon size={19} />
                </div>

                <div className="contact-copy">
                  <div>
                    <strong>{title}</strong>
                    {primary && <span className="recommended-label">우선 문의</span>}
                  </div>
                  <span>{description}</span>
                  <small>
                    <Phone size={13} />
                    {phone}
                  </small>
                </div>
              </div>
            ))}
          </div>

          <div className="public-contact-note">
            <HelpCircle size={16} />
            <span>
              특정 민간 업체를 추천하기보다 담당 행정복지센터와 지자체 등 공공
              상담창구를 우선 안내합니다.
            </span>
          </div>
        </SectionCard>
      </div>
    </div>
  );
}
