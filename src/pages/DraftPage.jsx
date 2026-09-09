import {
  Bot,
  CheckCircle2,
  CircleHelp,
  Download,
  FileSearch2,
  Plus,
  Save
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import PageHeader from "../components/PageHeader";
import SectionCard from "../components/SectionCard";

const documents = [
  ["피해사진 3장", true],
  ["신분증", true],
  ["통장 사본", true],
  ["거주 확인 서류", false],
  ["보험 가입 여부", false]
];

export default function DraftPage() {
  const navigate = useNavigate();

  return (
    <div className="page">
      <PageHeader
        eyebrow="신청서 초안"
        title="자동으로 채운 신청서와 준비서류를 확인하세요"
        description="실제 제출 전 사용자 확인이 필요한 항목은 별도로 표시합니다."
      />

      <div className="draft-grid">
        <SectionCard title="자동 작성 초안">
          <div className="compact-form">
            <label>신청인 <input defaultValue="홍길동" /></label>
            <label>주소 <input defaultValue="경상남도 거제시 장평1로 123" /></label>
            <label>피해유형 <input defaultValue="주택 침수" /></label>
            <label>발생일시 <input defaultValue="2026-08-12 15:10" /></label>
            <label>연락처 <input defaultValue="010-1234-5678" /></label>
          </div>
        </SectionCard>

        <SectionCard title="필요 증빙">
          <div className="document-check-list">
            {documents.map(([label, done]) => (
              <div className={`document-check ${done ? "done" : "pending"}`} key={label}>
                {done ? <CheckCircle2 size={18} /> : <CircleHelp size={18} />}
                <span>{label}</span>
                {!done && <button className="mini-btn">도움</button>}
              </div>
            ))}
          </div>
          <button className="ghost-btn"><Plus size={16} /> 서류 추가</button>
        </SectionCard>

        <SectionCard title="문서 도움">
          <div className="assistant-card">
            <div className="assistant-icon"><Bot size={24} /></div>
            <div>
              <strong>RAG 기반 제도 안내</strong>
              <p>신청요건과 제출서류를 해당 제도 원문 기준으로 안내합니다.</p>
            </div>
          </div>
          <div className="assistant-tip">
            <FileSearch2 size={18} />
            <span>“거주 확인 서류는 무엇을 제출해야 하나요?”처럼 질문할 수 있습니다.</span>
          </div>
        </SectionCard>
      </div>

      <div className="action-row right">
        <button className="secondary-btn"><Save size={17} /> 초안 저장</button>
        <button className="primary-btn" onClick={() => navigate("/tracking")}>
          <Download size={17} /> 제출 처리
        </button>
      </div>
    </div>
  );
}
