// 규칙 기반 판별 엔진. 브라우저(오프라인 판별)와 서버에서 같이 쓴다.

export const LEVELS = ['safe', 'caution', 'danger'];

const SHORTENERS = [
  'bit.ly', 'han.gl', 'me2.kr', 'url.kr', 'vo.la', 't.ly', 'tinyurl.com', 'buly.kr',
  'c11.kr', 'ko.gl', 'goo.gl', 'is.gd', 'cutt.ly', 'shorturl.at', 'rb.gy', 't.co', 'naver.me', 'zrr.kr',
];
const RISKY_TLDS = ['xyz', 'top', 'site', 'shop', 'click', 'icu', 'cyou', 'buzz', 'live', 'vip', 'cc'];

const URL_RE = /(https?:\/\/[^\s<>"']+|www\.[^\s<>"']+|\b[a-z0-9-]+(?:\.[a-z0-9-]+)*\.(?:kr|com|net|org|me|ly|gl|io|co|xyz|top|site|shop|click|icu|cyou|buzz|live|vip|cc|at|gd)(?![a-z0-9-])(?:\/[^\s<>"']*)?)/gi;

// 사기(보이스피싱·스미싱·메신저 피싱) 신호
const SCAM_RULES = [
  {
    id: 'family-impersonation',
    weight: 35,
    re: /(엄마|아빠|어머니|아버지|할머니|할아버지).{0,30}(폰|핸드폰|휴대폰|액정).{0,15}(고장|깨|잃어|분실|수리|망가)/s,
    reason: '자녀나 가족인 척 "폰이 고장 났다"며 다른 번호로 연락하는 수법과 비슷해요.',
  },
  {
    id: 'gift-card',
    weight: 30,
    re: /(문화상품권|구글\s?기프트|기프트\s?카드|상품권|기프티콘).{0,25}(사|구매|구입|보내|결제|번호)/s,
    reason: '상품권을 사서 번호를 보내 달라는 요구는 대표적인 사기 수법이에요.',
  },
  {
    id: 'urgent-money',
    weight: 25,
    re: /(대신|급하게|급히|빨리|지금\s?당장).{0,15}(결제|송금|이체|입금|돈)/s,
    reason: '급하게 돈을 보내거나 대신 결제해 달라고 재촉하고 있어요.',
  },
  {
    id: 'id-card-request',
    weight: 35,
    re: /(신분증|주민등록증|주민번호|운전면허증).{0,20}(사진|찍어|보내|알려)/s,
    reason: '신분증이나 주민번호를 보내 달라고 해요. 절대 보내면 안 돼요.',
  },
  {
    id: 'secret-info-request',
    weight: 25,
    re: /(비밀번호|OTP|보안카드|인증번호|카드\s?번호|CVC|계좌\s?번호).{0,15}(알려|보내|입력|불러)/is,
    reason: '비밀번호·인증번호 같은 중요한 정보를 요구해요.',
  },
  {
    id: 'authority-impersonation',
    weight: 40,
    re: /(검찰|검사|수사관|금융감독원|금감원|경찰청|사이버\s?수사대|법원|국세청).{0,40}(계좌|자산|이체|연루|구속|피해|범죄|수사|명의)/s,
    reason: '검찰·금감원 같은 기관인 척하며 계좌나 범죄 이야기를 해요. 기관은 문자·전화로 돈을 요구하지 않아요.',
  },
  {
    id: 'safe-account',
    weight: 40,
    re: /(안전\s?계좌|국가\s?안전\s?계좌|보호\s?계좌)/,
    reason: '"안전계좌"로 돈을 옮기라는 것은 100% 사기예요.',
  },
  {
    id: 'app-install',
    weight: 35,
    re: /((앱|어플|프로그램).{0,10}(설치|깔아|다운)|\.apk\b|팀뷰어|애니데스크|anydesk|teamviewer|원격\s?(제어|지원|조종))/is,
    reason: '앱 설치나 원격 조종을 요구해요. 휴대폰 정보를 빼가려는 수법일 수 있어요.',
  },
  {
    id: 'loan',
    weight: 25,
    re: /((저금리|대환|정부\s?지원|서민).{0,15}(대출|자금)|대출.{0,10}(승인|가능|한도|진행))/s,
    reason: '저금리 대출을 권하는 문자는 사기인 경우가 많아요.',
  },
  {
    id: 'delivery-lure',
    weight: 15,
    re: /(택배|배송|우체국|대한통운|한진|로젠|롯데택배|CJ).{0,30}(주소|미확인|반송|보관|확인|불가|지연)/is,
    reason: '택배 주소 확인을 핑계로 링크를 누르게 하는 문자와 비슷해요.',
  },
  {
    id: 'event-lure',
    weight: 15,
    re: /(부고|부음|청첩장|모바일\s?초대장|돌잔치|모바일\s?초대)/,
    reason: '부고·청첩장을 핑계로 링크를 누르게 하는 문자일 수 있어요.',
  },
  {
    id: 'payment-alert',
    weight: 30,
    re: /((국제|해외)\s?발신.{0,40}(결제|승인)|해외.{0,6}결제.{0,15}(승인|완료)|(결제|승인).{0,20}(본인이?\s?아닐|본인\s?아니))/s,
    reason: '"해외 결제가 됐다"며 놀라게 해서 전화를 걸게 하는 수법과 비슷해요.',
  },
  {
    id: 'not-me-call',
    weight: 25,
    re: /본인이?\s?(아닐|아니).{0,20}(문의|연락|전화|신고)/s,
    reason: '"본인이 아니면 연락하라"는 문구로 전화를 유도해요.',
  },
  {
    id: 'fine-refund-lure',
    weight: 15,
    re: /(과태료|범칙금|환급금|미납|건강검진\s?결과|보험금\s?(지급|청구)|지원금\s?(신청|지급))/,
    reason: '과태료·환급금·지원금을 핑계로 링크를 누르게 하는 문자일 수 있어요.',
  },
  {
    id: 'investment',
    weight: 25,
    re: /(리딩방|수익\s?보장|원금\s?보장|고수익|급등주|코인\s?(투자|추천)|무조건\s?수익)/,
    reason: '수익을 보장한다는 투자 권유는 사기일 가능성이 높아요.',
  },
  {
    id: 'pressure',
    weight: 10,
    re: /(즉시|오늘\s?중|24시간\s?(이내|안에)|정지\s?(예정|됩니다)|압류|체포|구속)/,
    reason: '겁을 주거나 서두르게 만드는 표현이 있어요.',
  },
  {
    id: 'foreign-number',
    weight: 15,
    re: /(국제\s?(발신|전화)|\+(?!82)\d{1,3}[\s-]?\d{2,}|(^|\D)00[1-9]\d{0,2}[\s-]\d)/,
    reason: '해외에서 보낸 번호예요.',
  },
];

// 허위정보(가짜뉴스·근거 없는 건강정보) 신호
const MISINFO_RULES = [
  {
    id: 'share-pressure',
    weight: 30,
    re: /(퍼뜨려|퍼트려|퍼날라|널리\s?알려|공유\s?(해|부탁)|단톡방에|지인들?(에게|한테)\s?(꼭\s?)?(알려|보내|공유)|많이\s?(보내|알려)|돌려\s?주세요)/,
    reason: '"널리 퍼뜨려 달라"고 재촉하는 글은 가짜 정보인 경우가 많아요.',
  },
  {
    id: 'hidden-truth',
    weight: 30,
    re: /(언론(이|에서|에)?\s?(절대\s?)?(안|못|숨기|보도하지|말하지)|방송(에|에서)\s?(절대\s?)?(안|못)\s?나오|정부가\s?숨기|아무도\s?모르는|충격(적인|적)?\s?(진실|사실)|의사(들)?(이|가)?\s?(절대\s?)?(말하지|알려주지)\s?않는)/,
    reason: '"언론이 숨기는 진실" 같은 표현은 근거 없는 글에서 자주 쓰여요.',
  },
  {
    id: 'sensational',
    weight: 15,
    re: /(충격|경악|긴급\s?속보|\[속보\]|발칵|난리\s?났|절대\s?(먹지|하지|사지)\s?마)/,
    reason: '자극적인 표현으로 놀라게 하고 있어요.',
  },
  {
    id: 'miracle-cure',
    weight: 30,
    re: /((암|당뇨|치매|고혈압|코로나|관절염|중풍).{0,25}(완치|낫는|나아|사라|특효|기적|잡는|뿌리\s?뽑)|(\d+\s?일|하루|일주일|한\s?달)\s?만에.{0,15}(완치|빠지|좋아|낫))/s,
    reason: '"먹기만 하면 병이 낫는다"는 식의 건강 정보는 대부분 사실이 아니에요.',
  },
  {
    id: 'vague-source',
    weight: 15,
    re: /(카더라|아는\s?사람(이|한테)|지인(이|의)\s?(말|얘기)|라고\s?(합니다|하네요|하더라)|들었습니다|받은\s?글)/,
    reason: '누가 말했는지 출처가 분명하지 않아요.',
  },
  {
    id: 'exclamations',
    weight: 10,
    re: /[!！]{3,}/,
    reason: '느낌표를 많이 써서 감정을 자극하고 있어요.',
  },
];

export function findLinks(text) {
  return [...new Set((text.match(URL_RE) || []).map((u) => u.replace(/[.,)\]]+$/, '')))];
}

function linkSignals(links) {
  if (links.length === 0) return [];
  const signals = [{ id: 'link', weight: 15, reason: '누르라고 하는 인터넷 주소(링크)가 들어 있어요.' }];
  const hosts = links.map((l) => l.replace(/^https?:\/\//i, '').replace(/^www\./i, '').split(/[/?#]/)[0].toLowerCase());
  if (hosts.some((h) => SHORTENERS.includes(h))) {
    signals.push({ id: 'short-link', weight: 25, reason: '진짜 주소를 숨기는 "단축 주소"가 쓰였어요.' });
  }
  if (hosts.some((h) => /^\d{1,3}(\.\d{1,3}){3}(:\d+)?$/.test(h))) {
    signals.push({ id: 'ip-link', weight: 30, reason: '정상 업체라면 쓰지 않는 숫자로 된 주소예요.' });
  }
  if (hosts.some((h) => RISKY_TLDS.includes(h.split('.').pop()))) {
    signals.push({ id: 'risky-domain', weight: 25, reason: '사기에 자주 쓰이는 낯선 주소예요.' });
  }
  return signals;
}

export function scoreToLevel(score) {
  if (score >= 50) return 'danger';
  if (score >= 20) return 'caution';
  return 'safe';
}

function evaluate(text, rules, extra = []) {
  const signals = rules
    .filter((r) => r.re.test(text))
    .map(({ id, weight, reason }) => ({ id, weight, reason }))
    .concat(extra);
  const score = Math.min(100, signals.reduce((sum, s) => sum + s.weight, 0));
  return { score, level: scoreToLevel(score), signals };
}

export function analyzeText(text) {
  const normalized = String(text || '').normalize('NFC');
  const links = findLinks(normalized);
  return {
    links,
    scam: evaluate(normalized, SCAM_RULES, linkSignals(links)),
    misinfo: evaluate(normalized, MISINFO_RULES),
  };
}

const HEADLINES = {
  scam: {
    danger: '사기일 가능성이 높아요!',
    caution: '조심하세요. 의심스러운 부분이 있어요',
    safe: '특별히 위험한 점은 못 찾았어요',
  },
  misinfo: {
    danger: '사실이 아닐 가능성이 높아요!',
    caution: '확인되지 않은 내용일 수 있어요',
    safe: '특별히 이상한 점은 못 찾았어요',
  },
};

const ADVICE = {
  scam: {
    danger: '링크를 누르지 마세요. 전화하지 마세요. 돈이나 개인정보를 절대 보내지 마세요. 이미 보냈다면 바로 112에 전화하세요.',
    caution: '보낸 사람에게 원래 알던 번호로 직접 전화해서 확인하세요. 링크는 누르지 마세요.',
    safe: '그래도 돈이나 개인정보를 달라고 하면 바로 의심하세요.',
  },
  misinfo: {
    danger: '다른 분께 보내지 마세요. 사실이 아닌 글을 보내면 받는 분이 피해를 볼 수 있어요.',
    caution: '보내기 전에 뉴스나 공식 기관에서 같은 내용이 나오는지 확인해 보세요.',
    safe: '그래도 보내기 전에 누가 쓴 글인지 한 번 더 확인해 주세요.',
  },
};

const rank = (level) => Math.max(0, LEVELS.indexOf(level));
const maxLevel = (a, b) => (rank(a) >= rank(b) ? a : b);

// 규칙 결과와 (있다면) AI 결과를 합쳐 화면에 보여줄 판정을 만든다.
// 어르신 보호가 목적이므로 둘 중 더 위험한 쪽을 따른다.
export function buildVerdict(mode, rules, ai = null) {
  const primary = mode === 'share' ? 'misinfo' : 'scam';
  const secondary = primary === 'scam' ? 'misinfo' : 'scam';

  const aiLevel = (cat) => {
    const v = ai?.[`${cat}_level`];
    return LEVELS.includes(v) ? v : 'safe';
  };
  const levels = {
    scam: maxLevel(rules.scam.level, aiLevel('scam')),
    misinfo: maxLevel(rules.misinfo.level, aiLevel('misinfo')),
  };
  const category = rank(levels[secondary]) > rank(levels[primary]) ? secondary : primary;
  const level = levels[category];

  const ruleReasons = [...rules[category].signals, ...rules[category === 'scam' ? 'misinfo' : 'scam'].signals]
    .sort((a, b) => b.weight - a.weight)
    .map((s) => s.reason);
  const aiReasons = Array.isArray(ai?.reasons) ? ai.reasons.filter((r) => typeof r === 'string' && r.trim()) : [];
  const reasons = [...new Set([...aiReasons, ...ruleReasons])].slice(0, 4);

  const aiAgrees = ai && maxLevel(aiLevel('scam'), aiLevel('misinfo')) === level;
  let advice = (aiAgrees && ai.advice) || ADVICE[category][level];
  if (mode === 'share' && level !== 'safe' && category === 'scam') {
    advice = `다른 분께 보내지 마세요. ${advice}`;
  }

  return {
    mode,
    category,
    level,
    headline: (aiAgrees && ai.headline) || HEADLINES[category][level],
    reasons,
    advice,
    factCheck: (ai && typeof ai.fact_check === 'string' && ai.fact_check) || '',
    links: rules.links,
    usedAI: Boolean(ai),
  };
}
