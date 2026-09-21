import {
  useEffect,
  useRef,
  useState,
} from "react";

import { loadKakaoMap } from "./loadKakaoMap";

function escapeHtml(value = "") {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function getPrimaryRegion(region = "") {
  const first = region
    .split(",")
    .map((item) => item.trim())
    .find(Boolean);

  if (!first || first === "전국") return null;

  return first
    .replace(/\s*전체\s*$/, "")
    .replace(/\([^)]*\)/g, "")
    .trim();
}

export default function KakaoMap({ disasters = [] }) {
  const mapRef = useRef(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let map = null;
    let resizeObserver = null;
    let cancelled = false;
    const markers = [];
    const infoWindows = [];

    loadKakaoMap()
      .then((kakao) => {
        if (cancelled || !mapRef.current) return;

        const container = mapRef.current;
        const center = new kakao.maps.LatLng(36.2, 127.8);

        map = new kakao.maps.Map(container, {
          center,
          level: 13,
        });

        requestAnimationFrame(() => {
          if (!map) return;
          map.relayout();
          map.setCenter(center);
        });

        setTimeout(() => {
          if (!map) return;
          map.relayout();
          map.setCenter(center);
        }, 100);

        const services = kakao.maps.services;
        const geocoder = services?.Geocoder
          ? new services.Geocoder()
          : null;

        if (!geocoder && disasters.some((item) => !item.latitude || !item.longitude)) {
          console.warn("[KakaoMap] services 라이브러리가 없어 지역명 좌표 변환을 할 수 없습니다.");
        }

        const addMarker = (item, position) => {
          if (cancelled || !map) return;

          const marker = new kakao.maps.Marker({
            position,
            map,
          });

          const infoWindow = new kakao.maps.InfoWindow({
            content: `
              <div style="padding:8px 10px;font-size:12px;line-height:1.5;min-width:150px;">
                <strong>${escapeHtml(item.type || "재난 알림")}</strong><br />
                <span>${escapeHtml(item.region || "지역 정보 없음")}</span>
              </div>
            `,
          });

          kakao.maps.event.addListener(marker, "click", () => {
            infoWindow.open(map, marker);
          });

          markers.push(marker);
          infoWindows.push(infoWindow);
        };

        disasters.forEach((item) => {
          if (item.latitude != null && item.longitude != null) {
            addMarker(
              item,
              new kakao.maps.LatLng(item.latitude, item.longitude)
            );
            return;
          }

          const regionQuery = getPrimaryRegion(item.region);
          if (!regionQuery || !geocoder) return;

          geocoder.addressSearch(regionQuery, (result, status) => {
            if (cancelled) return;

            if (status !== kakao.maps.services.Status.OK || !result?.[0]) {
              console.warn("[KakaoMap] 지역 좌표 검색 실패:", regionQuery);
              return;
            }

            addMarker(
              item,
              new kakao.maps.LatLng(
                Number(result[0].y),
                Number(result[0].x)
              )
            );
          });
        });

        resizeObserver = new ResizeObserver(() => {
          if (map) map.relayout();
        });
        resizeObserver.observe(container);
      })
      .catch((err) => {
        console.error("[KakaoMap] 지도 생성 오류:", err);
        if (!cancelled) setError(err.message);
      });

    return () => {
      cancelled = true;
      resizeObserver?.disconnect();
      infoWindows.forEach((infoWindow) => infoWindow.close());
      markers.forEach((marker) => marker.setMap(null));
    };
  }, [disasters]);

  return (
    <div className="map-wrap">
      <div ref={mapRef} className="kakao-map" />
      {error && <div className="map-error">{error}</div>}
    </div>
  );
}
