# 진실판별기 안드로이드 앱

웹앱(PWA)과 같은 판별 기능에 **자동 보호**(문자·카톡 알림 확인, 모르는 전화 경고)를 더한 네이티브 앱입니다.
계획은 [`docs/android-plan.md`](../docs/android-plan.md), 화면은 [`docs/design/`](../docs/design/README.md)을 따릅니다.

## 빌드

Android Studio(JDK 17 이상)와 Android SDK 36이 필요합니다.

```bash
cd android
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # 규칙 판별·서버 응답·자동 보호 단위 테스트
```

`android/local.properties` (git에 올라가지 않음):

```properties
sdk.dir=C:/Users/<이름>/AppData/Local/Android/Sdk
# 판별 서버 주소 (Vercel 배포 주소). 비우면 휴대폰 안 규칙 판별만 합니다.
API_BASE_URL=https://<배포 주소>.vercel.app
```

- 에뮬레이터에서 로컬 서버(`npm start`)에 붙이려면 `API_BASE_URL=http://10.0.2.2:3000` (디버그 빌드만 http 허용)
- **OpenRouter 키는 앱에 넣지 않습니다.** 앱은 `POST {API_BASE_URL}/api/analyze`만 부르고, 키는 Vercel 환경변수에만 있습니다.

## 구조

```
app/src/main/java/kr/jinsil/panbyeolgi/
  rules/      Rules.kt — public/js/rules.js를 그대로 옮긴 규칙 판별 (JsRegex.kt: 자바스크립트 정규식 뜻 맞추기)
  api/        AnalyzeApi.kt — 서버 /api/analyze 호출. 실패하면 null → 규칙 판별 결과만
  ui/         Compose 화면: Home, Input, Result(위험·주의·안전), Settings + 디자인 토큰(theme/)
  alert/      AlertActivity(문자 자동 경고), CallWarningActivity(모르는 전화 경고), Alerts(알림)
  service/    ProtectionListenerService(알림 확인), CallGuardService(전화 확인), AutoCheck, CallRisk
  data/       SettingsStore — 스위치 3개, 보호자, 글씨 크기
tools/rules-fixtures.mjs   rules.js 결과를 비교용 예문으로 내보냄 (RulesParityTest가 사용)
```

## 판별 규칙을 고칠 때

규칙의 기준은 웹의 `public/js/rules.js`입니다. 고친 뒤:

```bash
node android/tools/rules-fixtures.mjs   # 저장소 루트에서. 비교용 예문 다시 만들기
npm test                                # 예문 파일이 최신인지 확인
```

그리고 `Rules.kt`에 같은 변경을 옮기고 `./gradlew testDebugUnitTest`로 49개 예문(웹 테스트의 사기 7·허위정보 2·안전 5 포함)이 같은 결과인지 확인합니다.
정규식은 자바스크립트 원문을 그대로 붙여 넣으면 됩니다. `jsRegex()`가 `\s \d \b . $`를 자바스크립트와 같은 뜻으로 바꿔 줍니다.

## 동작

| 기능 | 방식 |
|---|---|
| 직접 확인 | 홈 → 받은 문자 확인 / 보내기 전 확인 → 붙여넣기 → 결과. 기기 안 규칙 결과를 먼저 보여 주고, 서버(AI) 응답이 오면 바꿉니다. 서버도 같은 규칙에 AI를 더해 더 위험한 쪽을 따르므로 결과가 덜 위험해지지는 않습니다. |
| 공유받기 | 카톡·문자에서 `공유 → 진실판별기` → 홈에서 무엇을 확인할지 누르면 바로 판별 |
| 소리로 듣기 | `TextToSpeech` 한국어, 0.9배 속도. 한국어 음성이 없으면 버튼을 숨김 |
| 가족에게 물어보기 | 공유 시트 |
| 112 / 1332 | 전화 앱에 번호만 채워서 엶 |
| 문자·카톡 자동 확인 | 알림 접근(`NotificationListenerService`). 삼성 메시지·Google 메시지·카카오톡 알림 미리보기만 **기기 안 규칙**으로 판별. 위험 → 전체 화면 경고, 주의 → 일반 알림(누르면 자세히 확인) |
| 이 문자 지우기 | 문자를 직접 지울 수 없어서 원래 알림을 닫고, 문자 앱에서 지우는 방법을 안내 |
| 가족에게 알리기 | 보호자 번호가 있으면 문자 앱에 받는 사람·내용을 채워서 엶 (`SEND_SMS` 권한 없음) |
| 모르는 전화 경고 | 안드로이드 10 이상. "전화 확인 앱" 역할 + 연락처 권한. 연락처에 없는 번호면 경고 화면/알림. 070·해외 번호는 문구를 강하게. 전화를 막지는 않음 |
| 받지 않기 / 그래도 받기 | "전화 받기·끊기" 권한(`ANSWER_PHONE_CALLS`)이 있으면 끊기/받기, 없으면 안내만 |
| 글씨 크기 | 크게(1.0) / 아주 크게(1.2), 휴대폰 글씨 크기 설정 위에 곱함. 모든 화면은 스크롤되고 버튼 글씨는 줄바꿈되어 잘리지 않음 |

쓰지 않는 권한: `READ_SMS`, `RECEIVE_SMS`, `SEND_SMS`, `READ_PHONE_STATE`.

## 실제 폰에서 확인할 것 (docs/android-plan.md 완료 기준 3)

1. 홈 → 받은 문자 확인 → 예문 붙여넣기 → 위험 결과
2. 카톡에서 글 공유 → 진실판별기 → 결과
3. 설정에서 문자 자동 확인 켜기 → 알림 접근 허용 → 다른 폰에서 사기 예문 문자를 보내면 경고 화면
   - 안드로이드 14 이상은 "전체 화면 알림" 허용이 따로 필요합니다 (설정 화면의 "허용이 더 필요해요"에 버튼이 뜹니다). 허용하지 않으면 화면 위쪽 알림으로만 뜹니다.
4. 설정 → 글씨 "아주 크게"에서 글이 잘리지 않음

## 알려진 한계

- 글꼴은 디자인의 Noto Sans KR 대신 휴대폰 기본 한글 글꼴(대부분 Noto Sans CJK / 삼성 기본 글꼴)을 씁니다. 굵기 900은 기기에 따라 굵게(700)로 보일 수 있습니다.
- 전화 경고는 전화 앱의 수신 화면과 겹치므로, 기기에 따라 전체 화면 대신 위쪽 알림으로 뜨고 눌러야 경고 화면이 열립니다.
- 신고된 사기 번호 목록(경찰청·KISA)은 아직 연동하지 않았습니다.
