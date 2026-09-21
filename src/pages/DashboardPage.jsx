import {
  AlarmClock,
  Bell,
  ChevronRight,
  Flame,
  MapPin,
  Users
} from "lucide-react";

import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

import { getDisasters } from "../api/disasterApi";
import KakaoMap from "../components/KakaoMap";
import SectionCard from "../components/SectionCard";

function getAlertTone(item) {
  if (item.emergencyLevel === "위급재난") return "red";
  if (item.emergencyLevel === "긴급재난") return "orange";
  return "yellow";
}

function formatAlertTime(value) {
  if (!value) return "-";

  const match = String(value).match(/(\d{2}):(\d{2})(?::\d{2})?/);
  return match ? `${match[1]}:${match[2]}` : String(value);
}

export default function DashboardPage() {
  const navigate = useNavigate();
  const [disasters, setDisasters] = useState([]);
  const [disasterLoading, setDisasterLoading] = useState(true);
  const [disasterError, setDisasterError] = useState("");

  useEffect(() => {
    let cancelled = false;

    getDisasters()
      .then((data) => {
        if (!cancelled) {
          setDisasters(Array.isArray(data) ? data : []);
        }
      })
      .catch((error) => {
        console.error("[Dashboard] 재난 API 호출 실패:", error);
        if (!cancelled) setDisasterError(error.message);
      })
      .finally(() => {
        if (!cancelled) setDisasterLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const realtimeAlerts = useMemo(
    () => disasters.slice(0, 5),
    [disasters]
  );

  return (
    <div className="page">
      <section className="home-hero">
        <div className="home-hero-copy">
          <div className="eyebrow">
            재난 피해 사후지원 원스톱 서비스
          </div>

          <h1>
            재난 이후,
            <br />
            필요한 지원까지 연결합니다.
          </h1>

          <p>
            실시간 재난 현황부터 피해 신고,
            AI·공공데이터 검증,
            지원제도 매칭과 신청 절차까지
            ReliefLink에서 한 번에 확인하세요.
          </p>

          <div className="home-hero-actions">
            <button
              className="primary-btn"
              onClick={() => navigate("/report")}
            >
              피해 신고 시작
            </button>

            <button
              className="secondary-btn"
              onClick={() => navigate("/matching")}
            >
              지원제도 보기
            </button>
          </div>
        </div>

        <div className="home-hero-image-wrap">
          <img
            src="/images/home.png"
            alt="ReliefLink 재난 피해 지원 서비스"
            className="home-hero-image"
          />
        </div>
      </section>

      <div className="dashboard-grid">
        <SectionCard className="map-card">
          <KakaoMap disasters={disasters} />
        </SectionCard>

        <SectionCard
          title="실시간 재난 알림"
          className="alerts-card"
        >
          <div className="alerts-overview">
            <span className={`live-dot ${disasterError ? "error" : ""}`} />
            <span>
              {disasterLoading
                ? "재난 알림 확인 중"
                : disasterError
                  ? "재난 알림 연결 오류"
                  : `오늘 ${disasters.length}건`}
            </span>
          </div>

          <div className="alert-list">
            {disasterLoading && (
              <div className="alert-state">
                <div className="alert-state-icon">
                  <Bell size={22} />
                </div>
                <strong>재난 정보를 불러오는 중입니다.</strong>
                <span>잠시만 기다려 주세요.</span>
              </div>
            )}

            {!disasterLoading && disasterError && (
              <div className="alert-state error">
                <div className="alert-state-icon">
                  <Bell size={22} />
                </div>
                <strong>재난 정보를 불러오지 못했습니다.</strong>
                <span>{disasterError}</span>
              </div>
            )}

            {!disasterLoading && !disasterError && realtimeAlerts.length === 0 && (
              <div className="alert-state">
                <div className="alert-state-icon">
                  <Bell size={22} />
                </div>
                <strong>현재 조회된 재난 알림이 없습니다.</strong>
                <span>
                  새 재난문자가 수집되면 이 영역에 표시됩니다.
                </span>
              </div>
            )}

            {realtimeAlerts.map((item) => {
              const tone = getAlertTone(item);

              return (
                <div
                  className="alert-row"
                  key={item.id || `${item.type}-${item.occurredAt}-${item.region}`}
                  title={item.message || ""}
                >
                  <div className={`alert-icon ${tone}`}>
                    {tone === "red"
                      ? <Flame size={17} />
                      : <MapPin size={17} />}
                  </div>

                  <div className="alert-copy">
                    <strong>{item.type || "재난 알림"}</strong>
                    <span>{item.region || "지역 정보 없음"}</span>
                  </div>

                  <div className="alert-time">
                    {formatAlertTime(item.occurredAt)}
                  </div>
                </div>
              );
            })}
          </div>

          {realtimeAlerts.length > 0 && (
            <div className="alerts-footer">
              <span>최근 {realtimeAlerts.length}건 표시</span>
              <button
                className="text-button"
                onClick={() => navigate("/disasters")}
              >
                더보기
                <ChevronRight size={16} />
              </button>
            </div>
          )}
        </SectionCard>
      </div>

      <div className="stat-grid">
        <div className="stat-card">
          <div className="stat-icon">
            <Bell size={20} />
          </div>
          <span>오늘 재난 알림</span>
          <strong>{disasterLoading ? "-" : `${disasters.length}건`}</strong>
        </div>

        <div className="stat-card">
          <div className="stat-icon">
            <Users size={20} />
          </div>
          <span>내 지역 피해신고</span>
          <strong>12건</strong>
        </div>

        <div className="stat-card">
          <div className="stat-icon">
            <AlarmClock size={20} />
          </div>
          <span>마감 임박 제도</span>
          <strong>3건</strong>
        </div>
      </div>
    </div>
  );
}
