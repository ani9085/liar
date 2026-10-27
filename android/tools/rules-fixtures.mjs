// public/js/rules.js의 판별 결과를 안드로이드 단위 테스트(RulesParityTest)용 예문 파일로 내보낸다.
// rules.js를 고친 뒤 실행: node android/tools/rules-fixtures.mjs
// npm test(test/android-fixtures.test.js)가 이 파일이 최신인지 확인한다.
import { writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { analyzeText, buildVerdict } from '../../public/js/rules.js';

export const FIXTURE_PATH = fileURLToPath(new URL('../app/src/test/resources/rules-fixtures.json', import.meta.url));

// test/rules.test.js와 같은 예문 (사기 7, 허위정보 2, 안전 5)
const EXPECTED = [
  ['사기: 자녀 사칭 + 상품권', 'danger', null, '엄마 나 폰 액정이 깨져서 수리 맡겼어. 이 번호로 문자 줘. 문화상품권 좀 사서 번호 보내줄 수 있어?'],
  ['사기: 해외결제 문자', 'danger', null, '[국제발신] 해외결제 승인 1,890,000원 본인이 아닐 경우 고객센터로 문의 바랍니다'],
  ['사기: 검찰 사칭', 'danger', null, '서울중앙지검 수사관입니다. 고객님 명의 계좌가 범죄에 연루되어 안전계좌로 이체하셔야 합니다.'],
  ['사기: 택배 스미싱', 'danger', null, '[CJ대한통운] 주소지 미확인으로 반송 예정입니다. 주소 확인 http://bit.ly/3abcd'],
  ['사기: 부고 스미싱', 'danger', null, '모바일 부고장 안내드립니다. han.gl/xYz12'],
  ['사기: 대출 + 앱 설치', 'danger', null, '정부지원 저금리 대출 승인되었습니다. 아래 앱 설치 후 신청하세요.'],
  ['사기: 신분증 요구', 'danger', null, '아빠 나 휴대폰 고장나서 그런데 신분증 사진 찍어서 보내줘'],
  ['허위정보: 기적의 치료법', null, 'danger', '[긴급속보] 언론에서 절대 보도하지 않는 충격적인 진실! 양파를 매일 먹으면 암이 완치된다고 합니다. 지인들에게 꼭 퍼뜨려 주세요!!!'],
  ['허위정보: 숨겨진 건강정보', null, 'danger', '의사들이 절대 말하지 않는 비밀. 이 차를 마시면 일주일 만에 당뇨가 낫는다고 하네요. 단톡방에 공유 부탁드려요'],
  ['안전: 가족 모임', 'safe', 'safe', '오늘 저녁 7시에 가족 모임 있어요. 늦지 마세요~'],
  ['안전: 자녀 안부', 'safe', 'safe', '엄마 오늘 날씨 추우니까 따뜻하게 입고 나가세요'],
  ['안전: 병원 예약', 'safe', 'safe', '병원 예약이 내일 오전 10시로 확정되었습니다.'],
  ['안전: 택배 도착', 'safe', 'safe', '택배가 도착했습니다. 문 앞에 두었습니다.'],
  ['안전: 금액 표기', 'safe', 'safe', '이번 달 관리비는 125,000원입니다.'],
];

// 자바·안드로이드 정규식이 자바스크립트와 다르게 동작하기 쉬운 경우들
const EDGE = [
  ['링크: 한글이 붙은 주소', '여기 naver.com에서 확인하세요.'],
  ['링크: 숫자 주소 뒤 쉼표', 'http://1.2.3.4/a, 그리고'],
  ['링크: 한글 바로 뒤 주소', '주소naver.com 확인'],
  ['링크: 대문자 주소', 'WWW.EXAMPLE.XYZ/path 접속하세요'],
  ['링크: 숫자 주소와 포트', 'http://192.168.0.1:8080/login 로그인'],
  ['링크: 괄호와 마침표', '여기를 보세요 (bit.ly/abc). 그리고 naver.com.'],
  ['링크: 한글 경로', 'https://site.kr/경로/안내 를 누르세요'],
  ['링크: 같은 주소 두 번', 'han.gl/a 그리고 han.gl/a'],
  ['공백: 줄바꿈 없는 공백', '지금 안전 계좌로 옮기세요'],
  ['공백: 전각 공백', '국가　안전　계좌 안내'],
  ['줄바꿈: s 플래그 규칙', '엄마\n나 폰\n고장났어'],
  ['줄바꿈: 일반 규칙', '[속보]\n충격적인 진실'],
  ['해외 번호: +1', '+1 202-555-0100 으로 연락 주세요'],
  ['해외 번호: +82 제외', '+82 10-1234-5678 로 연락 주세요'],
  ['해외 번호: 001', '001-1-202-555-0100 전화'],
  ['해외 번호: 국제전화', '국제전화가 왔어요'],
  ['본인 아님 문의', '본인 아니시면 연락 주세요'],
  ['원격 앱', 'TeamViewer 설치해 주세요'],
  ['apk 파일', '파일 abc.APK 받으세요'],
  ['apk 뒤 밑줄', 'abc.apk_file 이름'],
  ['인증번호 요구', 'OTP 번호 알려주세요'],
  ['전각 느낌표', '이것 좀 보세요！！！'],
  ['압박 + 단축 주소', '오늘 중 납부 안 하면 압류됩니다 http://bit.ly/x'],
  ['숫자 + 기간', '9일 만에 완치되는 비법'],
  ['분해된 한글(NFD)', '안전계좌로 이체하세요'.normalize('NFD')],
  ['빈 글', ''],
  ['투자 리딩방', '리딩방 무료 입장, 수익 보장 급등주 추천'],
  ['카드 번호 요구', '카드번호와 CVC를 입력해 주세요 https://pay-check.top/a'],
];

const SHARE = [
  ['보내기 전: 택배 스미싱', '[CJ대한통운] 주소지 미확인으로 반송 예정입니다. 주소 확인 http://bit.ly/3abcd'],
  ['보내기 전: 기적의 치료법', '양파를 매일 먹으면 암이 완치된대요. 지인들에게 꼭 알려주세요!'],
  ['보내기 전: 안전', '오늘 저녁 7시에 가족 모임 있어요. 늦지 마세요~'],
  ['보내기 전: 출처 불명', '아는 사람이 그러는데 내일부터 버스 요금이 오른다고 하네요'],
];

// test/rules.test.js의 AI 합치기 예
const WITH_AI = [
  [
    'AI가 더 위험하다고 본 경우',
    'scam',
    '엄마 나 새 번호야. 저장해줘',
    {
      scam_level: 'danger',
      misinfo_level: 'not_applicable',
      headline: '자녀를 사칭한 사기예요',
      reasons: ['새 번호로 돈을 요구해요.'],
      advice: '아드님께 원래 번호로 전화해 보세요.',
      fact_check: '',
    },
  ],
  [
    'AI가 안전하다고 해도 규칙이 위험을 찾은 경우',
    'scam',
    '서울중앙지검 수사관입니다. 고객님 명의 계좌가 범죄에 연루되어 안전계좌로 이체하셔야 합니다.',
    {
      scam_level: 'safe',
      misinfo_level: 'not_applicable',
      headline: '안전해 보여요',
      reasons: [],
      advice: '괜찮아요.',
      fact_check: '',
    },
  ],
  [
    'AI 허위정보 판정과 사실 확인',
    'share',
    '이 차를 마시면 혈압이 바로 내려간대요',
    {
      scam_level: 'safe',
      misinfo_level: 'danger',
      headline: '사실이 아닌 건강 정보예요',
      reasons: ['차 한 잔으로 혈압이 바로 내려간다는 근거는 없어요.', ' '],
      advice: '보내지 마시고 의사와 상의하세요.',
      fact_check: '혈압약을 대신할 수 있는 차는 확인되지 않았어요.',
    },
  ],
];

function evaluation(e) {
  return { score: e.score, level: e.level, signals: e.signals.map((s) => s.id) };
}

function fixture(name, text, mode, ai = null, expected = null) {
  const analysis = analyzeText(text);
  const v = buildVerdict(mode, analysis, ai);
  return {
    name,
    text,
    mode,
    ...(ai ? { ai } : {}),
    ...(expected ? { expected } : {}),
    links: analysis.links,
    scam: evaluation(analysis.scam),
    misinfo: evaluation(analysis.misinfo),
    verdict: {
      category: v.category,
      level: v.level,
      headline: v.headline,
      reasons: v.reasons,
      advice: v.advice,
      factCheck: v.factCheck,
      links: v.links,
      usedAI: v.usedAI,
    },
  };
}

export function buildFixtures() {
  return [
    ...EXPECTED.map(([name, scam, misinfo, text]) =>
      fixture(name, text, 'scam', null, { ...(scam ? { scam } : {}), ...(misinfo ? { misinfo } : {}) }),
    ),
    ...EDGE.map(([name, text]) => fixture(name, text, 'scam')),
    ...SHARE.map(([name, text]) => fixture(name, text, 'share')),
    ...WITH_AI.map(([name, mode, text, ai]) => fixture(name, text, mode, ai)),
  ];
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  writeFileSync(FIXTURE_PATH, `${JSON.stringify(buildFixtures(), null, 2)}\n`);
  console.log(`${FIXTURE_PATH} 에 저장했어요.`);
}
