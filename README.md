# 진실판별기

70대 부모님이 **사기 문자에 속지 않고**, **가짜 정보를 남에게 퍼뜨리지 않도록** 돕는 공공 웹앱(MVP)입니다.

## 두 가지 기능

| 기능 | 하는 일 |
|---|---|
| 🛡️ **받은 문자·카톡 확인** | 보이스피싱·스미싱·메신저 피싱 신호를 찾아 `위험 / 주의 / 안전`으로 알려줍니다. 위험하면 경찰(112)·금감원(1332) 전화 버튼을 띄웁니다. |
| 📤 **보내기 전에 확인** | 다른 사람에게 보내려는 글이 근거 없는 건강 정보·가짜 뉴스인지 확인하고, "보내지 마세요 / 확인 후 보내세요"를 알려줍니다. |

어르신이 쓰기 쉽게:
- 큰 글씨, 큰 버튼, 쉬운 존댓말. 색만이 아니라 아이콘과 글자로도 결과를 보여줍니다.
- **공유하기로 바로 확인**: 앱을 홈 화면에 설치하면 카톡·문자에서 글을 길게 눌러 `공유 → 진실판별기`로 보낼 수 있습니다 (Android Chrome, Web Share Target).
- 🔊 **소리로 듣기**: 결과를 읽어줍니다.
- 👨‍👩‍👧 **가족에게 물어보기**: 원문과 판별 결과를 자녀에게 바로 공유합니다.
- 서버에 연결되지 않아도 휴대폰 안에서 기본 검사가 됩니다.

## 판별 방식

1. **규칙 판별** (`public/js/rules.js`): 한국에서 흔한 수법을 정규식으로 찾아 점수를 매깁니다.
   - 사기: 자녀 사칭(폰 고장), 상품권 요구, 검찰·금감원 사칭, 안전계좌, 앱 설치·원격제어, 저금리 대출, 택배·부고·과태료 미끼, 해외결제 문자, 단축 주소·숫자 주소 링크 등
   - 허위정보: 공유 재촉, "언론이 숨기는 진실", 기적의 치료법, 출처 불명, 자극적 표현 등
2. **AI 판별** (`src/ai.js`): `OPENROUTER_API_KEY`가 있으면 [OpenRouter](https://openrouter.ai)를 통해 LLM이 문맥을 보고 판단하고, 쉬운 말로 이유와 행동 요령을 씁니다.
3. **합치기**: 어르신 보호가 목적이므로 규칙과 AI 중 **더 위험하다고 본 쪽**을 따릅니다. AI가 실패하거나 25초 안에 답하지 않으면 규칙 결과만 보여줍니다.

API 키는 서버(Vercel 함수)에만 있고 브라우저로는 보내지 않습니다. AI로 보내기 전에 주민번호·카드번호는 `[주민번호]`, `[카드번호]`로 가립니다.

## Vercel 배포

1. [vercel.com](https://vercel.com) → **Add New → Project** → 이 GitHub 저장소 선택
   - Framework Preset: **Other** (빌드 명령 없음, 설정은 `vercel.json`에 있음)
2. **Settings → Environment Variables**에 추가

   | 이름 | 값 | 필수 |
   |---|---|---|
   | `OPENROUTER_API_KEY` | OpenRouter 키 (`sk-or-...`) | ✅ |
   | `OPENROUTER_MODEL` | 모델 ID. 비우면 `anthropic/claude-opus-5.5` | |
   | `AI_DISABLED` | `1`이면 AI를 끔 (비상용) | |

3. 환경변수를 바꾼 뒤에는 **Redeploy** 해야 적용됩니다.
4. 배포 주소 뒤에 `/api/health`를 열어 `"ai": true`인지 확인하세요.

> **모델 ID 확인**: [openrouter.ai/models](https://openrouter.ai/models)에서 쓰려는 모델의 ID를 그대로 복사해 `OPENROUTER_MODEL`에 넣으세요. ID가 틀리면 AI 판별이 조용히 꺼지고 규칙 판별만 됩니다 (Vercel 로그에 `[ai] OpenRouter 오류`가 남습니다).

> **비용 보호**: 누구나 쓰는 공공 앱이라 남용될 수 있습니다. OpenRouter 키 설정에서 **사용 한도(credit limit)를 꼭 걸어 두세요.** 앱은 같은 IP에서 1분에 10번이 넘으면 AI 없이 규칙 판별만 합니다.

## 로컬 실행

Node.js 22 이상이 필요합니다. 외부 패키지는 쓰지 않습니다.

```bash
cp .env.example .env      # OPENROUTER_API_KEY 입력 (없으면 규칙 판별만)
npm start                 # http://localhost:3000
npm test
```

## 구조

```
api/analyze.js         Vercel 함수: POST /api/analyze
api/health.js          Vercel 함수: GET /api/health (AI 켜짐 여부 확인)
src/analyze.js         요청 처리 + IP별 AI 호출 제한 (Vercel·로컬 공용)
src/ai.js              OpenRouter 호출 (JSON 형식 응답)
public/js/rules.js     규칙 판별 + 판정 합치기 (서버·브라우저 공용)
public/app.js          화면 동작, 공유받기, 소리로 듣기
public/sw.js           오프라인 동작용 서비스 워커
server.js              로컬 개발용 서버
vercel.json            Vercel 설정
test/                  node --test
```

## MVP의 한계와 다음 단계

웹앱은 전화·문자·카톡을 **자동으로 가로챌 수 없습니다.** 지금은 어르신이 글을 공유하거나 붙여넣어야 판별합니다. "사전 차단"까지 하려면 네이티브 앱이 필요합니다.

- **전화**: Android `CallScreeningService` + 신고된 사기 번호 목록(경찰청·KISA 등)으로 수신 전에 경고·차단
- **문자**: Android 알림 접근(`NotificationListenerService`)으로 문자가 오면 자동 판별 후 경고 / iOS는 `SMS 필터 확장`으로 모르는 번호 문자 분류
- **카카오톡**: 카톡 내용은 직접 읽을 수 없으므로 Android 알림 미리보기로 판별 (iOS는 불가)
- **보호자 연결**: 위험 판정이 나오면 자녀에게 자동 알림
- **최신 정보 확인**: 허위정보 판별에 웹 검색·팩트체크 기관 자료 연동 (현재 AI는 최근 소식을 모를 수 있음)
- **판별 품질**: 실제 사기 문자·정상 문자 표본으로 오탐/미탐률 측정
