import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';
import { createServer } from '../server.js';
import { redact } from '../src/ai.js';

let server;
let base;
const aiCalls = [];

before(async () => {
  server = createServer({
    aiAnalyze: async (text, mode) => {
      aiCalls.push({ text, mode });
      return null; // AI 실패 상황: 규칙 판정만 써야 한다
    },
  });
  await new Promise((resolve) => server.listen(0, resolve));
  base = `http://localhost:${server.address().port}`;
});

after(() => server.close());

const post = (body) =>
  fetch(`${base}/api/analyze`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: typeof body === 'string' ? body : JSON.stringify(body),
  });

test('사기 문자를 보내면 위험 판정을 돌려준다', async () => {
  const res = await post({ text: '안전계좌로 이체하셔야 합니다. 금감원 수사관입니다', mode: 'scam' });
  assert.equal(res.status, 200);
  const v = await res.json();
  assert.equal(v.level, 'danger');
  assert.equal(v.usedAI, false);
  assert.equal(aiCalls.at(-1).mode, 'scam');
});

test('알 수 없는 mode는 scam으로 처리한다', async () => {
  const v = await (await post({ text: '안녕하세요', mode: 'weird' })).json();
  assert.equal(v.mode, 'scam');
});

test('빈 글과 잘못된 요청은 400', async () => {
  assert.equal((await post({ text: '   ' })).status, 400);
  assert.equal((await post('{not json')).status, 400);
});

test('정적 파일을 제공하고 상위 폴더 접근은 막는다', async () => {
  const home = await fetch(`${base}/`);
  assert.equal(home.status, 200);
  assert.match(await home.text(), /진실판별기/);
  const manifest = await (await fetch(`${base}/manifest.webmanifest`)).json();
  assert.equal(manifest.share_target.method, 'GET');
  const traversal = await fetch(`${base}/..%2fpackage.json`);
  assert.notEqual(traversal.status, 200);
});

test('AI로 보내기 전 주민번호와 카드번호를 가린다', () => {
  assert.equal(redact('주민번호 800101-1234567 입니다'), '주민번호 [주민번호] 입니다');
  assert.equal(redact('카드 1234-5678-9012-3456'), '카드 [카드번호]');
});
