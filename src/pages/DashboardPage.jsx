import {
  AlarmClock,
  Bell,
  ChevronRight,
  Flame,
  MapPin,
  Users
} from "lucide-react";

import { useNavigate } from "react-router-dom";

import KakaoMap from "../components/KakaoMap";
import SectionCard from "../components/SectionCard";
import { realtimeAlerts } from "../data/mockData";

export default function DashboardPage() {
  const navigate = useNavigate();

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
          <KakaoMap />
        </SectionCard>

        <SectionCard
          title="실시간 재난 알림"
          className="alerts-card"
        >
          <div className="alert-list">
            {realtimeAlerts.map((item) => (
              <div
                className="alert-row"
                key={`${item.type}-${item.time}`}
              >
                <div className={`alert-icon ${item.tone}`}>
                  {item.tone === "red"
                    ? <Flame size={17} />
                    : <MapPin size={17} />}
                </div>

                <div className="alert-copy">
                  <strong>{item.type}</strong>
                  <span>{item.place}</span>
                </div>

                <div className="alert-time">
                  {item.time}
                </div>
              </div>
            ))}
          </div>

          <button className="text-button">
            더보기
            <ChevronRight size={16} />
          </button>
        </SectionCard>
      </div>

      <div className="stat-grid">
        <div className="stat-card">
          <div className="stat-icon">
            <Bell size={20} />
          </div>
          <span>오늘 재난 알림</span>
          <strong>4건</strong>
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