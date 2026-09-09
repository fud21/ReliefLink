import { Navigate, Route, Routes } from "react-router-dom";

import Layout from "./components/Layout";

import AnalysisPage from "./pages/AnalysisPage";
import DashboardPage from "./pages/DashboardPage";
import DraftPage from "./pages/DraftPage";
import MatchingPage from "./pages/MatchingPage";
import MyPage from "./pages/MyPage";
import ReportPage from "./pages/ReportPage";
import TrackingPage from "./pages/TrackingPage";

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        {/* 홈 */} 
        <Route
          path="/"
          element={<DashboardPage />}
        />

        {/* 재난 현황 */}
        <Route
          path="/disasters"
          element={<DashboardPage />}
        />

        {/* 피해 신고 */}
        <Route
          path="/report"
          element={<ReportPage />}
        />

        {/* AI 분석 */}
        <Route
          path="/analysis"
          element={<AnalysisPage />}
        />

        {/* 지원제도 매칭 */}
        <Route
          path="/matching"
          element={<MatchingPage />}
        />

        {/* 신청 서류 */}
        <Route
          path="/draft"
          element={<DraftPage />}
        />

        {/* 진행 추적 */}
        <Route
          path="/tracking"
          element={<TrackingPage />}
        />

        {/* 마이페이지 */}
        <Route
          path="/mypage"
          element={<MyPage />}
        />
      </Route>

      <Route
        path="*"
        element={<Navigate to="/" replace />}
      />
    </Routes>
  );
}