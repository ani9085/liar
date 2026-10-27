import { test } from 'node:test';
import assert from 'node:assert/strict';
import { analyzeText, buildVerdict, findLinks } from '../public/js/rules.js';

const SCAM_DANGER = {
  '자녀 사칭 + 상품권': '엄마 나 폰 액정이 깨져서 수리 맡겼어. 이 번호로 문자 줘. 문화상품권 좀 사서 번호 보내줄 수 있어?',
  '해외결제 문자': '[국제발신] 해외결제 승인 1,890,000원 본인이 아닐 경우 고객센터로 문의 바랍니다',
  '검찰 사칭': '서울중앙지검 수사관입니다. 고객님 명의 계좌가 범죄에 연루되어 안전계좌로 이체하셔야 합니다.',
  '택배 스미싱': '[CJ대한통운] 주소지 미확인으로 반송 예정입니다. 주소 확인 http://bit.ly/3abcd',
  '부고 스미싱': '모바일 부고장 안내드립니다. han.gl/xYz12',
  '대출 + 앱 설치': '정부지원 저금리 대출 승인되었습니다. 아래 앱 설치 후 신청하세요.',
  '신분증 요구': '아빠 나 휴대폰 고장나서 그런데 신분증 사진 찍어서 보내줘',
};

const MISINFO_DANGER = {
  '기적의 치료법': '[긴급속보] 언론에서 절대 보도하지 않는 충격적인 진실! 양파를 매일 먹으면 암이 완치된다고 합니다. 지인들에게 꼭 퍼뜨려 주세요!!!',
  '숨겨진 건강정보': '의사들이 절대 말하지 않는 비밀. 이 차를 마시면 일주일 만에 당뇨가 낫는다고 하네요. 단톡방에 공유 부탁드려요',
};

const SAFE = {
  '가족 모임': '오늘 저녁 7시에 가족 모임 있어요. 늦지 마세요~',
  '자녀 안부': '엄마 오늘 날씨 추우니까 따뜻하게 입고 나가세요',
  '병원 예약': '병원 예약이 내일 오전 10시로 확정되었습니다.',
  '택배 도착': '택배가 도착했습니다. 문 앞에 두었습니다.',
  '금액 표기': '이번 달 관리비는 125,000원입니다.',
};

for (const [name, text] of Object.entries(SCAM_DANGER)) {
  test(`사기 위험: ${name}`, () => {
    assert.equal(analyzeText(text).scam.level, 'danger');
  });
}

for (const [name, text] of Object.entries(MISINFO_DANGER)) {
  test(`허위정보 위험: ${name}`, () => {
    assert.equal(analyzeText(text).misinfo.level, 'danger');
  });
}

for (const [name, text] of Object.entries(SAFE)) {
  test(`안전: ${name}`, () => {
    const r = analyzeText(text);
    assert.equal(r.scam.level, 'safe', JSON.stringify(r.scam.signals));
    assert.equal(r.misinfo.level, 'safe', JSON.stringify(r.misinfo.signals));
  });
}

test('링크 찾기: 주소 뒤에 붙은 한글과 문장부호는 빼고 찾는다', () => {
  assert.deepEqual(findLinks('여기 naver.com에서 확인하세요.'), ['naver.com']);
  assert.deepEqual(findLinks('http://1.2.3.4/a, 그리고'), ['http://1.2.3.4/a']);
  assert.deepEqual(findLinks('아무 링크 없음'), []);
});

test('보내기 전 확인에서 사기 문자를 보내려 하면 사기 경고를 앞세운다', () => {
  const v = buildVerdict('share', analyzeText(SCAM_DANGER['택배 스미싱']));
  assert.equal(v.category, 'scam');
  assert.equal(v.level, 'danger');
  assert.match(v.advice, /^다른 분께 보내지 마세요/);
});

test('AI가 더 위험하다고 보면 AI 판단을 따른다', () => {
  const ai = {
    scam_level: 'danger',
    misinfo_level: 'not_applicable',
    headline: '자녀를 사칭한 사기예요',
    reasons: ['새 번호로 돈을 요구해요.'],
    advice: '아드님께 원래 번호로 전화해 보세요.',
    fact_check: '',
  };
  const v = buildVerdict('scam', analyzeText('엄마 나 새 번호야. 저장해줘'), ai);
  assert.equal(v.level, 'danger');
  assert.equal(v.headline, ai.headline);
  assert.equal(v.reasons[0], ai.reasons[0]);
  assert.equal(v.usedAI, true);
});

test('AI가 안전하다고 해도 규칙이 위험을 찾으면 경고를 유지한다', () => {
  const ai = {
    scam_level: 'safe',
    misinfo_level: 'not_applicable',
    headline: '안전해 보여요',
    reasons: [],
    advice: '괜찮아요.',
    fact_check: '',
  };
  const v = buildVerdict('scam', analyzeText(SCAM_DANGER['검찰 사칭']), ai);
  assert.equal(v.level, 'danger');
  assert.notEqual(v.headline, ai.headline);
});
