import {
  useEffect,
  useRef,
  useState,
} from "react";

import { loadKakaoMap } from "./loadKakaoMap";

/* 더미 데이터 */
const disasters = [
  {
    id: 1,
    title: "경주 화재",
    type: "fire",
    lat: 35.8562,
    lng: 129.2247,
  },
  {
    id: 2,
    title: "포항 산사태",
    type: "landslide",
    lat: 36.019,
    lng: 129.3435,
  },
  {
    id: 3,
    title: "제주 정전",
    type: "power",
    lat: 33.4996,
    lng: 126.5312,
  },
];

export default function KakaoMap() {
  const mapRef = useRef(null);

  const [mapReady, setMapReady] =
    useState(false);

  const [error, setError] =
    useState("");

  useEffect(() => {
    let map = null;
    let resizeObserver = null;
    let cancelled = false;

    const markers = [];
    const infoWindows = [];

    loadKakaoMap()
      .then((kakao) => {
        if (
          cancelled ||
          !mapRef.current
        ) {
          return;
        }

        const container =
          mapRef.current;

        console.log(
          "[KakaoMap] container:",
          container.offsetWidth,
          container.offsetHeight
        );

        const center =
          new kakao.maps.LatLng(
            36.2,
            127.8
          );

        map =
          new kakao.maps.Map(
            container,
            {
              center,
              level: 13,
            }
          );
        requestAnimationFrame(() => {
          if (!map) return;

          map.relayout();
          map.setCenter(center);
        });

        const timer =
          setTimeout(() => {
            if (!map) return;

            map.relayout();
            map.setCenter(center);
          }, 100);

        disasters.forEach(
          (item) => {
            const position =
              new kakao.maps.LatLng(
                item.lat,
                item.lng
              );

            const marker =
              new kakao.maps.Marker({
                position,
                map,
              });

            const infoWindow =
              new kakao.maps.InfoWindow({
                content: `
                  <div
                    style="
                      padding:8px 10px;
                      font-size:12px;
                      font-weight:700;
                      white-space:nowrap;
                    "
                  >
                    ${item.title}
                  </div>
                `,
              });

            kakao.maps.event.addListener(
              marker,
              "click",
              () => {
                infoWindow.open(
                  map,
                  marker
                );
              }
            );

            markers.push(marker);
            infoWindows.push(
              infoWindow
            );
          }
        );

        resizeObserver =
          new ResizeObserver(() => {
            if (!map) return;

            map.relayout();
          });

        resizeObserver.observe(
          container
        );

        setMapReady(true);

        return () =>
          clearTimeout(timer);
      })
      .catch((err) => {
        console.error(
          "[KakaoMap] 지도 생성 오류:",
          err
        );

        if (!cancelled) {
          setError(err.message);
        }
      });

    return () => {
      cancelled = true;

      resizeObserver?.disconnect();

      markers.forEach(
        (marker) =>
          marker.setMap(null)
      );
    };
  }, []);

  return (
    <div className="map-wrap">
      <div
        ref={mapRef}
        className="kakao-map"
      />

      {error && (
        <div className="map-error">
          {error}
        </div>
      )}
    </div>
  );
}