let kakaoMapPromise = null;

export function loadKakaoMap() {
  if (window.kakao?.maps?.Map) {
    return Promise.resolve(window.kakao);
  }

  if (kakaoMapPromise) {
    return kakaoMapPromise;
  }

  kakaoMapPromise = new Promise((resolve, reject) => {
    const appKey = import.meta.env.VITE_KAKAO_MAP_KEY;

    console.log(
      "[loadKakaoMap] key loaded:",
      !!appKey
    );

    if (!appKey) {
      reject(
        new Error(
          "VITE_KAKAO_MAP_KEY가 설정되지 않았습니다."
        )
      );
      return;
    }

    const script = document.createElement("script");

    script.src =
      `https://dapi.kakao.com/v2/maps/sdk.js` +
      `?appkey=${appKey}&autoload=false&libraries=services`;

    script.async = true;

    script.onload = () => {
      console.log("[loadKakaoMap] SDK script loaded");

      if (!window.kakao?.maps?.load) {
        kakaoMapPromise = null;

        reject(
          new Error(
            "Kakao Maps 객체를 찾을 수 없습니다."
          )
        );

        return;
      }

      window.kakao.maps.load(() => {
        console.log(
          "[loadKakaoMap] maps initialized"
        );

        resolve(window.kakao);
      });
    };

    script.onerror = (event) => {
      console.error(
        "[loadKakaoMap] script load error",
        event
      );
      
      kakaoMapPromise = null;

      reject(
        new Error(
          "Kakao Map SDK 로딩에 실패했습니다."
        )
      );
    };

    document.head.appendChild(script);
  });

  return kakaoMapPromise;
}