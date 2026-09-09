import {
  BellRing,
  ClipboardList,
  FilePenLine,
  Home,
  LifeBuoy,
  MapPinned,
  UserRound
} from "lucide-react";

import { NavLink } from "react-router-dom";

const items = [
  { to: "/", label: "홈", icon: Home },
  { to: "/report", label: "피해 신고", icon: ClipboardList },
  { to: "/matching", label: "제도 매칭", icon: MapPinned },
  { to: "/draft", label: "신청 서류", icon: FilePenLine },
  { to: "/tracking", label: "진행 추적", icon: LifeBuoy },
  { to: "/mypage", label: "마이페이지", icon: UserRound }
];

export default function Sidebar() {
  return (
    <aside className="sidebar">
      <div className="brand">
        <img
          src="/images/icon.png"
          alt="ReliefLink"
          className="brand-logo"
        />

        <span>ReliefLink</span>
      </div>

      <nav className="side-nav">
        {items.map(({ to, label, icon: Icon }, index) => (
          <NavLink
            key={`${label}-${index}`}
            to={to}
            end
            className={({ isActive }) =>
              `side-nav-item ${isActive ? "active" : ""}`
            }
          >
            <Icon size={18} strokeWidth={2} />
            <span>{label}</span>
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}