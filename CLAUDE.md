# 진실판별기

70대 부모님용 사기 문자·가짜 정보 판별 앱. 사용자 대상 글은 모두 한국어, 쉬운 존댓말.

## 구성

- 웹앱(PWA) + Vercel 함수: `public/`, `api/`, `src/`, `server.js` — 자세한 건 README.md
- 판별 규칙의 기준 구현: `public/js/rules.js` (서버·브라우저 공용). 안드로이드 Kotlin 판별 규칙도 이것과 같은 결과를 내야 함
- AI 판별: OpenRouter, 키는 Vercel 환경변수 `OPENROUTER_API_KEY` (클라이언트·앱에 절대 넣지 않음)
- 디자인: `docs/design/` (토큰은 README, 화면 원본은 `screens/*.dc.html`)
- 안드로이드 앱: `android/` (아직 없음) — 계획은 `docs/android-plan.md`

## 명령

- 웹 테스트: `npm test` (외부 패키지 없음, Node 22)
- 웹 로컬 실행: `npm start` → http://localhost:3000

## 원칙

- 어르신 보호가 목적: 규칙 판별과 AI 판별 중 더 위험한 쪽을 따른다
- 글씨는 크게 (본문 21sp 이상), 터치 영역 56dp 이상, 색만으로 구분하지 않기
- 자동 확인(알림·전화)은 기기 안에서만 판별, 개인정보를 서버로 보내지 않음
