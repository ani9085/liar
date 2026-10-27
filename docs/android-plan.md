# 안드로이드 네이티브 앱 계획

웹앱(PWA)과 같은 판별 기능에 **자동 보호**(문자·카톡 알림, 모르는 전화 경고)를 더한 안드로이드 앱.
화면은 [`docs/design`](design/README.md)의 디자인을 따른다.

> 구현됨: [`android/`](../android/README.md). 설정 저장은 DataStore 대신 SharedPreferences를 썼다
> (알림·전화 확인 서비스가 기다리지 않고 바로 읽어야 해서). 완료 기준 1·2는 확인했고, 3(실제 폰)은 남아 있다.

## 기술 선택

- Kotlin + Jetpack Compose (Material 3, 단 색·글씨는 디자인 토큰으로 직접 지정)
- minSdk 26, target/compileSdk는 로컬에 설치된 최신 안정 버전
- 폴더: 저장소 루트의 `android/` (웹앱과 같은 저장소)
- 패키지명: `kr.jinsil.panbyeolgi` (가칭)
- 앱 이름: 진실판별기

## 구조

```
android/
  app/src/main/java/.../
    rules/        판별 규칙 (public/js/rules.js를 Kotlin으로 옮김) ← 순수 Kotlin, 단위 테스트
    api/          서버 /api/analyze 호출 (AI 판별). 실패하면 규칙 판별만
    ui/           Compose 화면: Home, Input, Result, Alert, CallWarning, Settings
    service/      NotificationListener, CallScreening
    data/         설정 저장 (DataStore): 스위치, 보호자, 글씨 크기
```

## 기능별 구현

### 1. 직접 확인 (웹앱과 동일)
- 홈 → 받은 문자 확인 / 보내기 전 확인 → 입력 → 결과
- **공유받기**: `ACTION_SEND` + `text/plain` intent-filter. 카톡에서 공유 → 진실판별기 → "무엇을 확인할까요?" 선택 후 바로 판별
- **판별**: 먼저 기기 안 규칙 판별 → 서버 `POST {BASE_URL}/api/analyze` `{text, mode}` 응답이 오면 그 결과로 갱신 (서버 응답 형식은 `public/js/rules.js`의 `buildVerdict` 반환값)
  - `BASE_URL`은 Vercel 배포 주소, `BuildConfig`로 주입. **OpenRouter 키는 앱에 넣지 않는다** (서버에만)
- 소리로 듣기: `TextToSpeech` (`Locale.KOREAN`, 속도 0.9)
- 가족에게 물어보기: `ACTION_SEND` 공유 시트
- 112 / 1332: `ACTION_DIAL` (`tel:`)

### 2. 문자·카톡 자동 확인
- `NotificationListenerService` — 사용자가 설정에서 "알림 접근" 허용
- 대상 패키지: 삼성 메시지 `com.samsung.android.messaging`, Google 메시지 `com.google.android.apps.messaging`, 카카오톡 `com.kakao.talk`
- 알림의 `EXTRA_TEXT` / `EXTRA_BIG_TEXT` / `EXTRA_MESSAGES`에서 글을 꺼내 **기기 안 규칙 판별**
- 위험(danger)이면 `AlertSms` 화면: full-screen intent 알림(`USE_FULL_SCREEN_INTENT`), Android 14+ 권한 확인 필요
- 주의(caution)면 일반 알림만
- 카톡 대화 내용 전체는 읽을 수 없다. 알림 미리보기에 보이는 부분만
- `READ_SMS` / `RECEIVE_SMS`는 쓰지 않는다 (Play 스토어 정책 제한, 기본 문자 앱이어야 함)
- "이 문자 지우기"는 직접 지울 수 없으므로 해당 알림을 닫고 문자 앱을 열어 안내

### 3. 모르는 전화 경고
- `CallScreeningService` + `RoleManager.ROLE_CALL_SCREENING` 역할 요청
- 연락처에 없는 번호(`READ_CONTACTS`)면 경고 알림/화면 (`CallWarning`)
- 070, 해외 번호(+, 00X)는 경고 문구 강화
- 신고 사기 번호 DB는 아직 없음 (추후 공공 데이터 연동)
- 기본값은 꺼짐. 설정에서 켤 때 권한 요청

### 4. 설정
- 스위치 3개(문자 / 카톡 / 전화), 보호자 이름·번호, 글씨 크기(크게 1.0 / 아주 크게 1.2)
- 보호자 알림: 위험 판정 시 `ACTION_SENDTO sms:`로 보호자에게 보낼 문자를 미리 채워서 열기 (`SEND_SMS` 권한은 쓰지 않음)
- 개인정보: 자동 확인은 기기 안 규칙 판별만. 서버(AI)로는 사용자가 직접 확인할 때만 보내고, 주민번호·카드번호는 서버에서 가림

## 완료 기준

1. `./gradlew assembleDebug` 성공, 경고 없이 빌드
2. 규칙 판별 단위 테스트: `test/rules.test.js`와 같은 예문(사기 7, 허위정보 2, 안전 5)이 같은 등급으로 나옴
3. 실제 폰에 설치해서 확인
   - 홈 → 받은 문자 확인 → 예문 붙여넣기 → 위험 결과
   - 카톡에서 글 공유 → 진실판별기 → 결과
   - 알림 접근 허용 후, 다른 폰에서 사기 예문 문자를 보내면 경고 화면이 뜸
   - 글씨 "아주 크게"에서 글이 잘리지 않음
4. 화면이 디자인 캔버스와 같은 색·크기·간격
