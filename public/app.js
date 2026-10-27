import { analyzeText, buildVerdict } from './js/rules.js';

const $ = (id) => document.getElementById(id);

const MODES = {
  scam: {
    title: '받은 문자·카톡 확인',
    help: '받은 문자나 카톡 내용을 복사해서 아래에 붙여넣으세요.',
  },
  share: {
    title: '보내기 전에 확인',
    help: '다른 분께 보내려는 글을 복사해서 아래에 붙여넣으세요.',
  },
};

const LEVEL_UI = {
  danger: { icon: '⛔', label: '위험' },
  caution: { icon: '⚠️', label: '주의' },
  safe: { icon: '✅', label: '안전해 보여요' },
};

let mode = 'scam';
let lastVerdict = null;

function show(name) {
  for (const el of document.querySelectorAll('.screen')) {
    el.hidden = el.id !== `screen-${name}`;
  }
  window.scrollTo(0, 0);
  const heading = document.querySelector(`#screen-${name} h1, #screen-${name} h2`);
  heading?.setAttribute('tabindex', '-1');
  heading?.focus({ preventScroll: true });
}

// 휴대폰 '뒤로' 버튼이 화면 단위로 동작하도록 주소 뒤 #으로 화면을 기억한다.
function route() {
  const hash = location.hash.slice(1);
  if (hash === 'scam' || hash === 'share') {
    mode = hash;
    $('input-title').textContent = MODES[mode].title;
    $('input-help').textContent = MODES[mode].help;
    $('input-error').hidden = true;
    show('input');
  } else if (hash === 'result' && lastVerdict) {
    show('result');
  } else {
    show('home');
  }
}

function go(hash) {
  if (location.hash.slice(1) === hash) route();
  else location.hash = hash;
}

async function analyze(text) {
  try {
    const res = await fetch('/api/analyze', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text, mode }),
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return { ...(await res.json()), offline: false };
  } catch {
    // 인터넷이 안 되면 휴대폰 안에서 기본 검사만 한다.
    return { ...buildVerdict(mode, analyzeText(text)), offline: true };
  }
}

function render(verdict) {
  const ui = LEVEL_UI[verdict.level];
  $('result-card').className = `result ${verdict.level}`;
  $('result-icon').textContent = ui.icon;
  $('result-label').textContent = ui.label;
  $('result-headline').textContent = verdict.headline;
  $('result-advice').textContent = verdict.advice;

  const list = $('result-reasons');
  list.replaceChildren(
    ...(verdict.reasons.length ? verdict.reasons : ['위험 신호가 될 만한 표현을 찾지 못했어요.']).map((r) => {
      const li = document.createElement('li');
      li.textContent = r;
      return li;
    }),
  );

  $('factcheck-block').hidden = !verdict.factCheck;
  $('result-factcheck').textContent = verdict.factCheck;
  $('result-calls').hidden = !(verdict.category === 'scam' && verdict.level === 'danger');

  $('result-note').textContent = verdict.offline
    ? '인터넷 연결이 없어 기본 검사만 했어요. 판단이 틀릴 수 있으니 돈과 관련된 일은 꼭 가족과 상의하세요.'
    : verdict.usedAI
      ? 'AI의 판단은 틀릴 수 있어요. 돈과 관련된 일은 꼭 가족과 상의하세요.'
      : '기본 검사 결과예요. 판단이 틀릴 수 있으니 돈과 관련된 일은 꼭 가족과 상의하세요.';
}

async function check() {
  const text = $('message').value.trim();
  if (!text) {
    $('input-error').textContent = '확인할 글을 먼저 붙여넣어 주세요.';
    $('input-error').hidden = false;
    return;
  }
  $('input-error').hidden = true;
  show('loading');
  lastVerdict = await analyze(text);
  render(lastVerdict);
  go('result');
}

function speak() {
  if (!lastVerdict || !('speechSynthesis' in window)) return;
  const v = lastVerdict;
  const utter = new SpeechSynthesisUtterance(
    [`${LEVEL_UI[v.level].label}.`, v.headline, v.advice, ...v.reasons, v.factCheck].filter(Boolean).join(' '),
  );
  utter.lang = 'ko-KR';
  utter.rate = 0.9;
  speechSynthesis.cancel();
  speechSynthesis.speak(utter);
}

async function askFamily() {
  if (!lastVerdict) return;
  const message = `[진실판별기] 이 글이 괜찮은지 봐줄래요?\n\n"${$('message').value.trim()}"\n\n판별 결과: ${LEVEL_UI[lastVerdict.level].label} - ${lastVerdict.headline}`;
  if (navigator.share) {
    try {
      await navigator.share({ text: message });
    } catch {
      // 사용자가 공유를 취소한 경우
    }
  } else {
    location.href = `sms:?body=${encodeURIComponent(message)}`;
  }
}

async function paste() {
  try {
    const text = await navigator.clipboard.readText();
    if (text) $('message').value = text;
    else $('message').focus();
  } catch {
    $('input-error').textContent = '입력칸을 길게 누른 뒤 "붙여넣기"를 눌러 주세요.';
    $('input-error').hidden = false;
    $('message').focus();
  }
}

// 다른 앱의 '공유'로 들어온 글 (manifest의 share_target)
function receiveShared() {
  const params = new URLSearchParams(location.search);
  const shared = ['title', 'text', 'url'].map((k) => params.get(k)?.trim()).filter(Boolean);
  if (shared.length === 0) return false;
  $('message').value = [...new Set(shared)].join('\n');
  $('shared-notice').hidden = false;
  history.replaceState(null, '', '/');
  return true;
}

for (const btn of document.querySelectorAll('[data-mode]')) {
  btn.addEventListener('click', () => {
    if ($('shared-notice').hidden) {
      $('message').value = '';
      go(btn.dataset.mode);
    } else {
      // 공유받은 글은 입력 화면을 건너뛰고 바로 확인한다.
      $('shared-notice').hidden = true;
      mode = btn.dataset.mode;
      check();
    }
  });
}
for (const btn of document.querySelectorAll('[data-go]')) {
  btn.addEventListener('click', () => go(btn.dataset.go));
}
$('check').addEventListener('click', check);
$('paste').addEventListener('click', paste);
$('speak').addEventListener('click', speak);
$('ask-family').addEventListener('click', askFamily);
if (!('speechSynthesis' in window)) $('speak').hidden = true;

window.addEventListener('hashchange', route);
receiveShared();
route();

if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('/sw.js').catch(() => {});
}
