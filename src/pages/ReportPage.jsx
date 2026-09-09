import {
  AlertTriangle,
  Building2,
  Camera,
  CheckCircle2,
  CircleHelp,
  Home,
  ImagePlus,
  Info,
  ShieldCheck,
  Sparkles,
  Store,
  Tractor,
} from "lucide-react";

import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

import PageHeader from "../components/PageHeader";
import SectionCard from "../components/SectionCard";

const targetOptions = [
  {
    value: "RESIDENTIAL",
    label: "개인 / 주거",
    description: "주택, 아파트 등 주거 공간 피해",
    icon: Home,
  },
  {
    value: "BUSINESS",
    label: "소상공인·사업장",
    description: "상가, 점포, 사업장 피해",
    icon: Store,
  },
  {
    value: "FARM",
    label: "농·축산업",
    description: "농지, 작물, 시설물 피해",
    icon: Tractor,
  },
  {
    value: "OTHER",
    label: "기타",
    description: "기타 재산 및 시설 피해",
    icon: Building2,
  },
];

const photoGuides = {
  RESIDENTIAL: [
    {
      id: "residential-outside",
      title: "건물 외부",
      description: "건물 전체와 주변 피해 상황이 보이도록 촬영해주세요.",
      icon: "🏠",
    },
    {
      id: "residential-entrance",
      title: "출입구",
      description: "출입구와 침수·파손 흔적이 함께 보이도록 촬영해주세요.",
      icon: "🚪",
    },
    {
      id: "residential-damage",
      title: "주요 피해 부위",
      description: "가장 피해가 큰 부분을 가까이에서 촬영해주세요.",
      icon: "🧱",
    },
  ],

  BUSINESS: [
    {
      id: "business-outside",
      title: "사업장 외부",
      description: "간판과 사업장 전경이 함께 보이도록 촬영해주세요.",
      icon: "🏪",
    },
    {
      id: "business-inside",
      title: "사업장 내부 전체",
      description: "내부 전체의 피해 범위를 확인할 수 있도록 촬영해주세요.",
      icon: "🛒",
    },
    {
      id: "business-equipment",
      title: "시설·장비 피해",
      description: "POS, 냉장고, 기계 등 영업용 장비 피해를 촬영해주세요.",
      icon: "🧰",
    },
    {
      id: "business-inventory",
      title: "상품·재고 피해",
      description: "판매상품, 원재료 등 재고 피해를 촬영해주세요.",
      icon: "📦",
    },
  ],

  FARM: [
    {
      id: "farm-overall",
      title: "농지·시설 전체",
      description: "피해 범위를 확인할 수 있는 전체 사진을 촬영해주세요.",
      icon: "🌾",
    },
    {
      id: "farm-crop",
      title: "작물 피해",
      description: "침수·파손 등 작물 피해를 가까이에서 촬영해주세요.",
      icon: "🌱",
    },
    {
      id: "farm-facility",
      title: "시설물 피해",
      description: "비닐하우스, 축사 등 시설 피해를 촬영해주세요.",
      icon: "🏚️",
    },
  ],

  OTHER: [
    {
      id: "other-overall",
      title: "피해 현장 전체",
      description: "피해 위치와 주변 환경이 함께 보이도록 촬영해주세요.",
      icon: "📍",
    },
    {
      id: "other-main",
      title: "주요 피해 부위",
      description: "피해 내용을 확인할 수 있도록 촬영해주세요.",
      icon: "📷",
    },
    {
      id: "other-detail",
      title: "세부 피해",
      description: "파손·침수 부위의 세부 상태를 촬영해주세요.",
      icon: "🔎",
    },
  ],
};

const damageLevelGuide = {
  RESIDENTIAL: {
    LIGHT: "일부 손상이 있으나 현재 거주와 일상생활이 가능한 상태",
    MODERATE: "일부 공간 사용이 제한되거나 수리·교체가 필요한 상태",
    SEVERE: "거주가 어렵거나 안전상 위험이 있어 즉시 조치가 필요한 상태",
  },
  BUSINESS: {
    LIGHT: "일부 시설·재고에 피해가 있으나 정상 영업이 가능한 상태",
    MODERATE: "시설·장비 피해로 부분 영업 또는 단기 영업중단이 필요한 상태",
    SEVERE: "핵심 설비·재고 피해로 정상 영업이 어려운 상태",
  },
  FARM: {
    LIGHT: "일부 작물·시설에 피해가 있으나 생산 활동을 계속할 수 있는 상태",
    MODERATE: "생산량이나 시설 운영에 영향을 주어 복구가 필요한 상태",
    SEVERE: "생산 지속이 어렵거나 주요 시설을 사용할 수 없는 상태",
  },
  OTHER: {
    LIGHT: "일부 손상이 있으나 대상의 주요 기능을 사용할 수 있는 상태",
    MODERATE: "기능이 일부 제한되고 수리·교체가 필요한 상태",
    SEVERE: "정상 사용이 어렵거나 안전상 위험이 있는 상태",
  },
};

const damageLevelLabels = {
  LIGHT: "경미",
  MODERATE: "중간",
  SEVERE: "심각",
};

function FieldHint({ children, tone = "default" }) {
  return (
    <div className={`field-hint ${tone}`}>
      <Info size={14} />
      <span>{children}</span>
    </div>
  );
}

export default function ReportPage() {
  const navigate = useNavigate();

  const [targetType, setTargetType] = useState("RESIDENTIAL");
  const [damageLevel, setDamageLevel] = useState("MODERATE");
  const [photos, setPhotos] = useState({});
  const [businessDamageTypes, setBusinessDamageTypes] = useState([]);

  const currentPhotoGuides = photoGuides[targetType];

  const currentUploadedPhotoCount = useMemo(
    () => currentPhotoGuides.filter((guide) => photos[guide.id]).length,
    [currentPhotoGuides, photos]
  );

  const currentDamageGuide = damageLevelGuide[targetType];

  useEffect(() => {
    return () => {
      Object.values(photos).forEach((photo) => {
        if (photo?.preview) URL.revokeObjectURL(photo.preview);
      });
    };
  }, []);

  const handlePhotoChange = (guideId, event) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setPhotos((prev) => {
      if (prev[guideId]?.preview) {
        URL.revokeObjectURL(prev[guideId].preview);
      }

      return {
        ...prev,
        [guideId]: {
          file,
          preview: URL.createObjectURL(file),
        },
      };
    });
  };

  const toggleBusinessDamage = (value) => {
    setBusinessDamageTypes((prev) =>
      prev.includes(value)
        ? prev.filter((item) => item !== value)
        : [...prev, value]
    );
  };

  return (
    <div className="page">
      <PageHeader
        eyebrow="피해 신고"
        title="현장 정보를 입력하고 피해 사진을 등록하세요"
        description="피해 대상에 따라 필요한 정보와 촬영 가이드를 맞춤 제공합니다."
      />

      <div className="report-notice">
        <ShieldCheck size={20} />
        <div>
          <strong>신고 전 확인해주세요</strong>
          <p>
            입력한 정보와 사진은 피해 분석과 지원제도 매칭을 돕기 위한 자료입니다.
            최종 피해 인정 및 지원 여부는 담당 기관의 확인 결과에 따라 달라질 수 있습니다.
          </p>
        </div>
      </div>

      <SectionCard
        title="피해 대상"
        subtitle="피해 대상에 따라 필요한 신고 정보와 촬영 항목이 달라집니다."
      >
        <div className="target-selector">
          {targetOptions.map(({ value, label, description, icon: Icon }) => {
            const selected = targetType === value;

            return (
              <button
                key={value}
                type="button"
                className={`target-card ${selected ? "selected" : ""}`}
                onClick={() => setTargetType(value)}
              >
                <div className="target-icon">
                  <Icon size={21} />
                </div>

                <div>
                  <strong>{label}</strong>
                  <span>{description}</span>
                </div>
              </button>
            );
          })}
        </div>
      </SectionCard>

      <SectionCard title="피해 신고 정보">
        <div className="form-grid">
          <label>
            재난 유형
            <select defaultValue="침수">
              <option>침수</option>
              <option>화재</option>
              <option>산사태</option>
              <option>지진</option>
              <option>태풍</option>
              <option>폭설</option>
              <option>기타</option>
            </select>
            <small className="input-help">
              현재 피해의 직접적인 원인이 된 재난을 선택해주세요.
            </small>
          </label>

          <label>
            피해 정도
            <select
              value={damageLevel}
              onChange={(event) => setDamageLevel(event.target.value)}
            >
              <option value="LIGHT">경미</option>
              <option value="MODERATE">중간</option>
              <option value="SEVERE">심각</option>
            </select>
            <small className="input-help">
              현재 사용·거주·영업 가능 여부를 기준으로 선택해주세요.
            </small>
          </label>

          <label>
            발생 시각
            <input type="datetime-local" defaultValue="2026-08-12T15:10" />
            <small className="input-help">
              피해가 발생했거나 처음 확인한 시각을 입력해주세요.
            </small>
          </label>

          <label>
            현재 피해 상태
            <select defaultValue="피해 지속">
              <option>피해 지속</option>
              <option>임시 복구</option>
              <option>복구 완료</option>
            </select>
            <small className="input-help">
              신고를 작성하는 현재 시점의 복구 상태를 선택해주세요.
            </small>
          </label>

          <label className="span-2">
            피해 현장 주소
            <input
              type="text"
              defaultValue="경상남도 거제시 장평1로 123"
            />
            <small className="input-help">
              거주지 주소가 아니라 실제 피해가 발생한 현장 주소를 입력해주세요.
            </small>
          </label>

          {targetType === "RESIDENTIAL" && (
            <>
              <label>
                세대 정보
                <select defaultValue="3인 가구">
                  <option>1인 가구</option>
                  <option>2인 가구</option>
                  <option>3인 가구</option>
                  <option>4인 이상</option>
                </select>
                <small className="input-help">
                  일부 지원제도는 가구 규모를 기준으로 지원 범위가 달라질 수 있습니다.
                </small>
              </label>

              <label>
                주거 형태
                <select defaultValue="자가">
                  <option>자가</option>
                  <option>전세</option>
                  <option>월세</option>
                  <option>기타</option>
                </select>
                <small className="input-help">
                  피해 주택의 점유 형태를 선택해주세요.
                </small>
              </label>
            </>
          )}
        </div>

        <div className="damage-guide-box">
          <div className="damage-guide-head">
            <CircleHelp size={17} />
            <strong>피해 정도 선택 기준</strong>
          </div>

          <div className="damage-guide-grid">
            {Object.entries(currentDamageGuide).map(([level, description]) => (
              <button
                key={level}
                type="button"
                className={`damage-guide-item ${
                  damageLevel === level ? "active" : ""
                }`}
                onClick={() => setDamageLevel(level)}
              >
                <span className={`damage-level-dot ${level.toLowerCase()}`} />
                <strong>{damageLevelLabels[level]}</strong>
                <p>{description}</p>
              </button>
            ))}
          </div>

          <FieldHint>
            위 기준은 사용자의 입력을 돕기 위한 안내입니다. 실제 피해 등급이나
            지원 대상 여부를 확정하는 기준이 아닙니다.
          </FieldHint>
        </div>
      </SectionCard>

      {targetType === "BUSINESS" && (
        <SectionCard
          title="사업장 피해 정보"
          subtitle="지원제도 매칭을 위해 사업장 및 영업 피해 정보를 추가로 확인합니다."
          className="business-card"
        >
          <div className="form-grid">
            <label>
              사업장명
              <input type="text" placeholder="예: Relief 카페" />
              <small className="input-help">
                피해가 발생한 실제 사업장명을 입력해주세요.
              </small>
            </label>

            <label>
              업종
              <select defaultValue="">
                <option value="" disabled>
                  업종 선택
                </option>
                <option>음식점업</option>
                <option>도·소매업</option>
                <option>서비스업</option>
                <option>숙박업</option>
                <option>제조업</option>
                <option>기타</option>
              </select>
              <small className="input-help">
                가장 가까운 주 업종을 선택해주세요.
              </small>
            </label>

            <label>
              사업장 형태
              <select defaultValue="임차">
                <option>자가</option>
                <option>임차</option>
                <option>기타</option>
              </select>
            </label>

            <label>
              현재 영업 상태
              <select defaultValue="영업중단">
                <option>정상 영업</option>
                <option>부분 영업</option>
                <option>영업중단</option>
              </select>
              <small className="input-help">
                재난 피해로 인해 현재 정상 영업이 가능한지 선택해주세요.
              </small>
            </label>

            <label>
              영업중단 시작일
              <input type="date" defaultValue="2026-08-12" />
            </label>

            <label>
              예상 영업중단 기간
              <select defaultValue="3~7일">
                <option>1~2일</option>
                <option>3~7일</option>
                <option>1~2주</option>
                <option>2주 이상</option>
                <option>확인 불가</option>
              </select>
              <small className="input-help">
                현재 예상 기간이며 이후 진행상황에서 수정할 수 있습니다.
              </small>
            </label>
          </div>

          <div className="business-damage-section">
            <span className="label-title">피해 항목</span>
            <p className="field-help">
              실제 피해가 발생한 항목을 모두 선택하세요.
            </p>

            <div className="damage-check-grid">
              {[
                { value: "FACILITY", label: "건물·내부시설" },
                { value: "EQUIPMENT", label: "영업용 장비" },
                { value: "INVENTORY", label: "상품·재고" },
                { value: "SIGN", label: "간판·외부시설" },
                { value: "OTHER", label: "기타" },
              ].map(({ value, label }) => (
                <label
                  key={value}
                  className={`damage-check ${
                    businessDamageTypes.includes(value) ? "checked" : ""
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={businessDamageTypes.includes(value)}
                    onChange={() => toggleBusinessDamage(value)}
                  />
                  <span>{label}</span>
                </label>
              ))}
            </div>
          </div>
        </SectionCard>
      )}

      <SectionCard
        title="현장 촬영 가이드"
        subtitle="피해 대상에 맞는 사진을 촬영하면 피해 검증과 지원제도 매칭에 활용됩니다."
      >
        <div className="photo-safety-notice">
          <AlertTriangle size={18} />
          <div>
            <strong>안전이 확보된 위치에서만 촬영해주세요.</strong>
            <span>
              얼굴, 주민등록번호, 카드번호 등 불필요한 개인정보가 사진에 포함되지
              않도록 확인해주세요.
            </span>
          </div>
        </div>

        <div
          className={`photo-guide-grid ${
            currentPhotoGuides.length === 4 ? "four" : ""
          }`}
        >
          {currentPhotoGuides.map((photo, index) => {
            const uploaded = photos[photo.id];

            return (
              <label
                className={`photo-slot ${uploaded ? "uploaded" : ""}`}
                key={photo.id}
              >
                <input
                  type="file"
                  accept="image/*"
                  capture="environment"
                  hidden
                  onChange={(event) => handlePhotoChange(photo.id, event)}
                />

                <div className="photo-number">{index + 1}</div>

                {uploaded ? (
                  <img
                    src={uploaded.preview}
                    alt={photo.title}
                    className="photo-preview"
                  />
                ) : (
                  <div className="photo-emoji">{photo.icon}</div>
                )}

                <strong>{photo.title}</strong>
                <small className="photo-description">{photo.description}</small>

                <span>
                  <Camera size={15} />
                  {uploaded ? "다시 선택" : "촬영 / 업로드"}
                </span>
              </label>
            );
          })}
        </div>

        <div className="report-meta">
          <span>
            <CheckCircle2 size={16} />
            현재 위치 확인
          </span>

          <span>경상남도 거제시 장평1로 123</span>

          <span>
            <ImagePlus size={16} />
            업로드 사진 {currentUploadedPhotoCount}건
          </span>
        </div>

        <button
          className="primary-btn full"
          onClick={() => navigate("/analysis")}
        >
          <Sparkles size={18} />
          AI 분석 요청
        </button>
      </SectionCard>
    </div>
  );
}


